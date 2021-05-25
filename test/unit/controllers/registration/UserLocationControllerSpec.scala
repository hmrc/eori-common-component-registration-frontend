/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.controllers.registration

import java.util.UUID

import common.pages.registration.UserLocationPageOrganisation._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, mock => _}
import play.api.mvc.{AnyContent, Request, Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.ServiceUnavailableResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.UserLocationController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  ContactResponse,
  IndividualResponse,
  OrganisationResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.{
  RegistrationDisplayResponse,
  ResponseCommon,
  ResponseDetail
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  BusinessDetailsRecoveryPage,
  ContactDetailsSubscriptionFlowPageGetEori,
  SubscriptionPage,
  UserLocationPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationDisplayService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.user_location
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{
  sub01_outcome_processing,
  sub01_outcome_rejected
}
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserLocationControllerSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)

  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSessionCache               = mock[SessionCache]
  private val mockSave4LaterService          = mock[Save4LaterService]
  private val mockSubscriptionStatusService  = mock[SubscriptionStatusService]
  private val mockRegistrationDisplayService = mock[RegistrationDisplayService]
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockEnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val userLocationView = instanceOf[user_location]

  private val sub01OutcomeProcessing = instanceOf[sub01_outcome_processing]

  private val sub01OutcomeRejected = instanceOf[sub01_outcome_rejected]

  private val errorTemplate = instanceOf[error_template]

  private val controller = new UserLocationController(
    mockAuthAction,
    mockRequestSessionData,
    mockSave4LaterService,
    mockSubscriptionStatusService,
    mockSessionCache,
    mockRegistrationDisplayService,
    mcc,
    userLocationView,
    sub01OutcomeProcessing,
    sub01OutcomeRejected,
    errorTemplate
  )

  private val ProblemWithSelectionError = "Select where you are based"

  private val locationFieldName = "location"

  override def beforeEach(): Unit = {
    reset(
      mockRequestSessionData,
      mockSave4LaterService,
      mockSubscriptionStatusService,
      mockRegistrationDisplayService,
      mockSubscriptionFlowManager
    )
    when(mockRequestSessionData.sessionWithOrganisationTypeAdded(any(), any()))
      .thenReturn(Session())
    when(
      mockRequestSessionData
        .sessionWithUserLocationAdded(any[String])(any[Request[AnyContent]])
    ).thenReturn(Session())
    when(mockRequestSessionData.existingSessionWithUserLocationAdded(any[Session], any[String])).thenReturn(Session())
    when(mockRegistrationDisplayService.cacheDetails(any())(any()))
      .thenReturn(Future.successful(true))
    when(mockSave4LaterService.fetchSafeId(any[GroupId]())(any[HeaderCarrier]())).thenReturn(Future.successful(None))
    when(
      mockEnrolmentStoreProxyService
        .enrolmentForGroup(any(), any())(any())
    ).thenReturn(Future.successful(None))
    when(mockSessionCache.saveRegistrationDetails(any())(any())).thenReturn(Future.successful(true))
  }

  "Viewing the user location form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(atarService, Journey.Register)
    )
    "display the form with no errors" in {
      showForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
      }
    }
  }

  "Submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(atarService, Journey.Register)
    )

    "ensure a location option has been selected" in {
      submitForm(Map.empty) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe ProblemWithSelectionError
        page.getElementsText(fieldLevelErrorLocation) shouldBe s"Error: $ProblemWithSelectionError"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "ensure a valid answer option has been selected" in {
      val invalidOption = UUID.randomUUID.toString
      submitForm(Map(locationFieldName -> invalidOption)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe ProblemWithSelectionError
        page.getElementsText(fieldLevelErrorLocation) shouldBe s"Error: $ProblemWithSelectionError"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect to uk vat registered page  when 'iom' is selected" in {
      when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier])).thenReturn(Future.successful(None))

      submitForm(Map(locationFieldName -> UserLocation.Iom)) { result =>
        status(result) shouldBe SEE_OTHER
        val expectedUrl =
          YouNeedADifferentServiceIomController.form(atarService, Journey.Register).url
        result.header.headers(LOCATION) should endWith(expectedUrl)
      }
    }

    assertCorrectSessionDataAndRedirect(UserLocation.Uk)

    assertCorrectSessionDataAndRedirect(UserLocation.Eu)

    assertCorrectSessionDataAndRedirect(UserLocation.ThirdCountry)

    assertCorrectSessionDataAndRedirect(UserLocation.Islands)
  }

  "subscriptionStatus when sessionInfoBasedOnJourney for Row" should {
    "redirect to OrganisationTypeController form when 'Iom' is selected" in {
      subscriptionStatus() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          OrganisationTypeController.form(atarService, Journey.Register).url
        )
      }
    }

    "return IllegalStateException when invalid location is selected" in {
      val error = intercept[IllegalStateException] {
        subscriptionStatus(location = Some("location")) { result =>
          await(result)
        }
      }
      error.getMessage shouldBe "User Location not set"
    }

    "return IllegalStateException when Subscribe journey is selected and location is not set" in {
      val error = intercept[IllegalStateException] {
        subscriptionStatus(Journey.Subscribe, None) { result =>
          await(result)
        }
      }
      error.getMessage shouldBe "User Location not set"
    }
  }

  "cacheAndRedirect when registrationDisplay is Enabled" should {
    val controller = new UserLocationController(
      mockAuthAction,
      mockRequestSessionData,
      mockSave4LaterService,
      mockSubscriptionStatusService,
      mockSessionCache,
      mockRegistrationDisplayService,
      mcc,
      userLocationView,
      sub01OutcomeProcessing,
      sub01OutcomeRejected,
      errorTemplate
    ) {}
    implicit val fakeRequest = FakeRequest()
    implicit val hc          = mock[HeaderCarrier]

    "cache registration display response and redirect to BusinessDetailsRecoveryPage for individual response" in {
      val mockSession   = mock[Session]
      val mockFlowStart = (BusinessDetailsRecoveryPage, mockSession)
      val responseDetail = ResponseDetail(
        "",
        None,
        None,
        false,
        false,
        true,
        Some(mock[IndividualResponse]),
        None,
        mock[Address],
        mock[ContactResponse]
      )
      when(
        mockSubscriptionFlowManager.startSubscriptionFlow(
          meq(Some(UserLocationPage)),
          meq(CdsOrganisationType.ThirdCountryIndividual),
          meq(atarService),
          meq(Journey.Register)
        )(any[HeaderCarrier], any[Request[AnyContent]])
      ).thenReturn(Future.successful(mockFlowStart))

      val test =
        controller.cacheAndRedirect(atarService, Journey.Register, "third-country")
      val result = await(test(Right(RegistrationDisplayResponse(mock[ResponseCommon], Some(responseDetail)))))

      status(result) shouldBe SEE_OTHER
      result.header.headers(LOCATION) should endWith(
        BusinessDetailsRecoveryController.form(atarService, Journey.Register).url
      )
    }

    "cache registration display response and redirect to BusinessDetailsRecoveryPage for organisation response" in {
      val mockSession   = mock[Session]
      val mockFlowStart = (BusinessDetailsRecoveryPage, mockSession)
      val responseDetail = ResponseDetail(
        "",
        None,
        None,
        false,
        false,
        false,
        None,
        Some(mock[OrganisationResponse]),
        mock[Address],
        mock[ContactResponse]
      )

      when(
        mockSubscriptionFlowManager.startSubscriptionFlow(
          meq(Some(UserLocationPage)),
          meq(CdsOrganisationType.ThirdCountryOrganisation),
          meq(atarService),
          meq(Journey.Register)
        )(any[HeaderCarrier], any[Request[AnyContent]])
      ).thenReturn(Future.successful(mockFlowStart))

      val test =
        controller.cacheAndRedirect(atarService, Journey.Register, "third-country")
      val result = await(test(Right(RegistrationDisplayResponse(mock[ResponseCommon], Some(responseDetail)))))

      status(result) shouldBe SEE_OTHER
      result.header.headers(LOCATION) should endWith(
        BusinessDetailsRecoveryController.form(atarService, Journey.Register).url
      )
    }

    "return service unavailable response when failed to retrieve registration display response" in {
      val test =
        controller.cacheAndRedirect(atarService, Journey.Register, "third-country")
      val result = await(test(Left(ServiceUnavailableResponse)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  private def showForm(userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    test(
      controller
        .form(atarService, Journey.Register)
        .apply(SessionBuilder.buildRequestWithSession(userId))
    )
  }

  private def submitForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    test(
      controller
        .submit(atarService, Journey.Register)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def processing(userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    test(
      controller.processing(atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/atar/subscribe", userId))
    )
  }

  private def subscriptionStatus(
    journey: Journey.Value = Journey.Register,
    location: Option[String] = Some(UserLocation.Iom)
  )(test: Future[Result] => Any) = {

    val subStatus: PreSubscriptionStatus = NewSubscription
    implicit val hc: HeaderCarrier       = mock[HeaderCarrier]
    implicit val rq: Request[AnyContent] = mock[Request[AnyContent]]

    test(controller.subscriptionStatus(subStatus, GroupId("GroupId"), atarService, journey, location)(rq, hc))
  }

  private def assertCorrectSessionDataAndRedirect(selectedOptionValue: String): Unit = {
    s"store the correct organisation type when '$selectedOptionValue' is selected" in {
      val selectedOptionToJourney = selectedOptionValue match {
        case UserLocation.Eu           => "eu"
        case UserLocation.ThirdCountry => "third-country"
        case UserLocation.Uk           => "uk"
        case UserLocation.Iom          => "iom"
        case UserLocation.Islands      => "islands"
      }

      when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier])).thenReturn(Future.successful(None))

      submitForm(Map(locationFieldName -> selectedOptionValue)) { result =>
        status(result)
        verify(mockRequestSessionData).sessionWithUserLocationAdded(ArgumentMatchers.eq(selectedOptionToJourney))(
          any[Request[AnyContent]]
        )
      }
    }

    if (selectedOptionValue != UserLocation.Uk) {

      s"redirect to SubscriptionProcessing page when '$selectedOptionValue' is selected" in {
        when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(SafeId("safeid"))))
        when(
          mockSubscriptionStatusService
            .getStatus(any[String], any[String])(any[HeaderCarrier])
        ).thenReturn(Future.successful(SubscriptionProcessing))

        submitForm(Map(locationFieldName -> selectedOptionValue)) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(LOCATION) should endWith(UserLocationController.processing(atarService).url)
        }
      }

      s"redirect to CompleteEnrolmentAfterSubscriptionTimeoutController when SubscriptionExists status and enrolment exists is false and when '$selectedOptionValue' is selected" in {
        when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(SafeId("safeid"))))
        when(
          mockSubscriptionStatusService
            .getStatus(any[String], any[String])(any[HeaderCarrier])
        ).thenReturn(Future.successful(SubscriptionExists))

        submitForm(Map(locationFieldName -> selectedOptionValue)) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(LOCATION) should endWith(
            SubscriptionRecoveryController
              .complete(atarService, Journey.Register)
              .url
          )
        }
      }
      s"redirect to BusinessDetailsRecoveryController when NewSubscription status and registration display is enabled and when '$selectedOptionValue' is selected" in {
        val mockResponseCommon = mock[ResponseCommon]
        val mockResponseDetail = mock[ResponseDetail]
        val mockSession        = mock[Session]
        val mockFlowStart =
          (ContactDetailsSubscriptionFlowPageGetEori, mockSession)

        when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(SafeId("safeid"))))
        when(
          mockSubscriptionStatusService
            .getStatus(any[String], any[String])(any[HeaderCarrier])
        ).thenReturn(Future.successful(SubscriptionRejected))
        when(mockRegistrationDisplayService.requestDetails(any())(any(), any()))
          .thenReturn(
            Future.successful(Right(RegistrationDisplayResponse(mockResponseCommon, Some(mockResponseDetail))))
          )
        when(
          mockSubscriptionFlowManager.startSubscriptionFlow(
            any[Option[SubscriptionPage]],
            any[CdsOrganisationType],
            any[Service],
            any[Journey.Value]
          )(any[HeaderCarrier], any[Request[AnyContent]])
        ).thenReturn(Future.successful(mockFlowStart))

        val controller = new UserLocationController(
          mockAuthAction,
          mockRequestSessionData,
          mockSave4LaterService,
          mockSubscriptionStatusService,
          mockSessionCache,
          mockRegistrationDisplayService,
          mcc,
          userLocationView,
          sub01OutcomeProcessing,
          sub01OutcomeRejected,
          errorTemplate
        ) {}

        val result = controller
          .submit(atarService, Journey.Register)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              defaultUserId,
              Map(locationFieldName -> selectedOptionValue)
            )
          )

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          BusinessDetailsRecoveryController.form(atarService, Journey.Register).url
        )
      }

      s"redirect to BusinessDetailsRecoveryController when SubscriptionRejected status and registration display is enabled and when '$selectedOptionValue' is selected" in {
        val mockResponseCommon = mock[ResponseCommon]
        val mockResponseDetail = mock[ResponseDetail]
        val mockSession        = mock[Session]
        val mockFlowStart =
          (ContactDetailsSubscriptionFlowPageGetEori, mockSession)

        when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(SafeId("safeid"))))
        when(
          mockSubscriptionStatusService
            .getStatus(any[String], any[String])(any[HeaderCarrier])
        ).thenReturn(Future.successful(NewSubscription))
        when(mockRegistrationDisplayService.requestDetails(any())(any(), any()))
          .thenReturn(
            Future.successful(Right(RegistrationDisplayResponse(mockResponseCommon, Some(mockResponseDetail))))
          )
        when(
          mockSubscriptionFlowManager.startSubscriptionFlow(
            any[Option[SubscriptionPage]],
            any[CdsOrganisationType],
            any[Service],
            any[Journey.Value]
          )(any[HeaderCarrier], any[Request[AnyContent]])
        ).thenReturn(Future.successful(mockFlowStart))

        val controller = new UserLocationController(
          mockAuthAction,
          mockRequestSessionData,
          mockSave4LaterService,
          mockSubscriptionStatusService,
          mockSessionCache,
          mockRegistrationDisplayService,
          mcc,
          userLocationView,
          sub01OutcomeProcessing,
          sub01OutcomeRejected,
          errorTemplate
        ) {}

        val result = controller
          .submit(atarService, Journey.Register)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              defaultUserId,
              Map(locationFieldName -> selectedOptionValue)
            )
          )

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          BusinessDetailsRecoveryController.form(atarService, Journey.Register).url
        )
      }

    } else if (selectedOptionValue == UserLocation.Uk)
      s"redirect to organisation type page  when '$selectedOptionValue' is selected" in {
        when(mockSave4LaterService.fetchSafeId(any[GroupId])(any[HeaderCarrier])).thenReturn(Future.successful(None))

        submitForm(Map(locationFieldName -> selectedOptionValue)) { result =>
          status(result) shouldBe SEE_OTHER
          val expectedUrl =
            OrganisationTypeController.form(atarService, Journey.Register).url
          result.header.headers(LOCATION) should endWith(expectedUrl)
        }
      }
  }

  "Viewing application state" should {
    "redirect to the processing page" in {

      val processedDate = Sub01Outcome("01/01/2011")

      when(mockSessionCache.sub01Outcome(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedDate))
      processing() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("The ATaR application is being processed")
      }
    }
  }

}
