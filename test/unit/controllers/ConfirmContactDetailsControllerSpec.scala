/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.controllers

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Results.Status
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector.matchFailureResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ConfirmContactDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  ContactResponse,
  IndividualResponse,
  MatchingResponse,
  RegisterWithIDResponse,
  ResponseDetail
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.YesNoWrongAddress.wrongAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, sub01_outcome_processing}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.YesNoFormBuilder.validRequest
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmContactDetailsControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                = mock[AuthConnector]
  private val mockAuthAction                   = authAction(mockAuthConnector)
  private val mockConfirmContactDetailsService = mock[ConfirmContactDetailsService]
  private val mockSessionCache                 = mock[SessionCache]
  private val mockRequestSessionData           = mock[RequestSessionData]
  private val mockMatchingService              = mock[MatchingService]
  private val sub01OutcomeProcessingView       = inject[sub01_outcome_processing]
  private val error_view                       = inject[error_template]

  private val sessionCacheService =
    new SessionCacheService(mockSessionCache, mockRequestSessionData, mockMatchingService, error_view)

  private val controller = new ConfirmContactDetailsController(
    mockAuthAction,
    mockConfirmContactDetailsService,
    mockSessionCache,
    sessionCacheService,
    mcc,
    sub01OutcomeProcessingView
  )(global)

  private val mockSub01Outcome = mock[Sub01Outcome]
  private val mockRegDetails   = mock[RegistrationDetails]

  private val testSubscriptionDetails =
    SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("John", "Doe", LocalDate.now())))

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val dobToday = LocalDate.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  private def matchingResponse(postCode: String = "SE28 2AA", dob: String = dobToday) =
    MatchingResponse(
      RegisterWithIDResponse(
        ResponseCommon(
          "OK",
          Some("002 - No match found"),
          LocalDate.now.atTime(8, 35, 2),
          Some(List(MessagingServiceParam("POSITION", "FAIL")))
        ),
        Some(
          ResponseDetail(
            SAFEID = "XE0000123456789",
            ARN = Some("ZARN1234567"),
            isAnIndividual = true,
            individual = Some(IndividualResponse("John", "Doe", Some(dob))),
            address = Address("address line 1", None, None, None, Some(postCode), "UK"),
            isEditable = false,
            isAnAgent = false,
            contactDetails = ContactResponse()
          )
        )
      )
    )

  "form" should {

    withAuthorisedUser(defaultUserId, mockAuthConnector)

    "user selects row and individual" when {
      "redirect to you need to use a different service page" in servicesToTest.foreach { testService =>
        when(
          mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
        ).thenReturn(Future.successful(Status(OK)))

        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.ThirdCountry))
        when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)

        val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          s"/customs-registration-services/${testService.code}/register/ind-st-use-a-different-service"
        )
      }
    }

    "nino is selected" when {
      "Correctly populate form" in servicesToTest.foreach { testService =>
        when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
        when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
        when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
          Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
        )
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
        )
        when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
          eitherT[MatchingResponse](matchingResponse())
        )

        when(
          mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
        ).thenReturn(Future.successful(Status(OK)))

        val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

        status(result) shouldBe OK
      }

      "Correctly redirect to You cannot use this service with PostCode not matching" in servicesToTest.foreach {
        testService =>
          when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

          when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
          when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
          when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
            Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
          )
          when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
            Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
          )
          when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
            eitherT[MatchingResponse](matchingResponse("SE28 2BB"))
          )

          when(
            mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
          ).thenReturn(Future.successful(Status(OK)))

          val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(
            s"/customs-registration-services/${testService.code}/register/you-cannot-change-address"
          )
      }

      "Correctly redirect to You cannot use this service with DOB not matching" in servicesToTest.foreach {
        testService =>
          when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

          when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
          when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
          when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
            Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
          )
          when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
            Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
          )
          when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
            eitherT[MatchingResponse](matchingResponse("SE28 2AA", "1980-02-02"))
          )

          when(
            mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
          ).thenReturn(Future.successful(Status(OK)))

          val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(
            s"/customs-registration-services/${testService.code}/register/you-cannot-change-address"
          )
      }

      "Correctly redirect to You cannot use this service when matching service responds back with failure response" in servicesToTest.foreach {
        testService =>
          when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

          when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
          when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
          when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
            Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
          )
          when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
            Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
          )
          when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
            eitherT[MatchingResponse](matchFailureResponse)
          )

          when(
            mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
          ).thenReturn(Future.successful(Status(OK)))

          val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(
            s"/customs-registration-services/${testService.code}/register/you-cannot-change-address"
          )
      }
    }
    "utr is selected" when {
      "Correctly populate form" in servicesToTest.foreach { testService =>
        when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
        when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
        when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
          Future.successful(Some(NinoOrUtr(Some(Utr("7280616009")))))
        )
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
        )
        when(mockMatchingService.matchIndividualWithId(any(), any(), any())(any(), any())).thenReturn(
          eitherT[MatchingResponse](matchingResponse())
        )

        when(
          mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
        ).thenReturn(Future.successful(Status(OK)))

        val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

        status(result) shouldBe OK
      }

      "Correctly populate form with PostCode not matching" in servicesToTest.foreach { testService =>
        when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
        when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
        when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
          Future.successful(Some(NinoOrUtr(Some(Utr("7280616009")))))
        )
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
        )
        when(mockMatchingService.matchIndividualWithId(any(), any(), any())(any(), any())).thenReturn(
          eitherT[MatchingResponse](matchingResponse("SE28 2BB"))
        )

        when(
          mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
        ).thenReturn(Future.successful(Status(OK)))

        val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          s"/customs-registration-services/${testService.code}/register/you-cannot-change-address"
        )
      }

      "Correctly populate form with DOB not matching" in servicesToTest.foreach { testService =>
        when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
        when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
        when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
          Future.successful(Some(NinoOrUtr(Some(Utr("7280616009")))))
        )
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
        )
        when(mockMatchingService.matchIndividualWithId(any(), any(), any())(any(), any())).thenReturn(
          eitherT[MatchingResponse](matchingResponse("SE28 2AA", "1980-02-02"))
        )

        when(
          mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
        ).thenReturn(Future.successful(Status(OK)))

        val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          s"/customs-registration-services/${testService.code}/register/you-cannot-change-address"
        )
      }
    }
  }

  "submit" should {

    withAuthorisedUser(defaultUserId, mockAuthConnector)

    "Correctly handle a form with errors" in servicesToTest.foreach { testService =>
      when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
      when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
        Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
      )
      when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
        Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
      )
      when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
        eitherT[MatchingResponse](matchingResponse())
      )

      when(
        mockConfirmContactDetailsService.handleFormWithErrors(any(), any(), any())(any[Request[AnyContent]], any())
      ).thenReturn(Future.successful(Status(OK)))

      val result = controller.submit(testService).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, validRequest + ("wrong-address" -> ""))
      )

      status(result) shouldBe OK

    }

    "Correctly handle a valid form" in servicesToTest.foreach { testService =>
      when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
      when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
        Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
      )
      when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
        Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
      )
      when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
        eitherT[MatchingResponse](matchingResponse())
      )

      when(
        mockConfirmContactDetailsService.checkAddressDetails(any(), any(), any())(any[Request[AnyContent]], any())
      ).thenReturn(Future.successful(Status(OK)))

      val result = controller.submit(testService).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(
          defaultUserId,
          validRequest + ("yes-no-wrong-address" -> wrongAddress)
        )
      )

      status(result) shouldBe OK

    }
  }

  "processing" should {

    "Correctly redirect to sub01OutcomeProcessingView" in servicesToTest.foreach { testService =>
      when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(testSubscriptionDetails))

      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
      when(mockSessionCache.getNinoOrUtrDetails(any())).thenReturn(
        Future.successful(Some(NinoOrUtr(Some(Nino("SX123412A")))))
      )
      when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
        Future.successful(Some(PostcodeViewModel("SE28 2AA", None)))
      )
      when(mockMatchingService.matchIndividualWithNino(any(), any(), any())(any(), any())).thenReturn(
        eitherT[MatchingResponse](matchingResponse())
      )

      when(mockSessionCache.registrationDetails(any[Request[AnyContent]])).thenReturn(Future.successful(mockRegDetails))
      when(mockSessionCache.sub01Outcome(any[Request[AnyContent]])).thenReturn(Future.successful(mockSub01Outcome))

      val result =
        controller.processing(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      status(result) shouldBe OK
    }
  }

}
