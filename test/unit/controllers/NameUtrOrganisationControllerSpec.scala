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

import common.pages.matching.NameIdOrganisationPage._
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.NameIdOrganisationController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.Organisation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, NameOrganisationMatchModel, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{MatchingService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.InvalidUrlValueException
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, match_name_id_organisation}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.NameIdOrganisationFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NameUtrOrganisationControllerSpec
    extends ControllerSpec with MockitoSugar with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector             = mock[AuthConnector]
  private val mockAuthAction                = authAction(mockAuthConnector)
  private val mockMatchingService           = mock[MatchingService]
  private val mockSubscriptionDetailService = mock[SubscriptionDetailsService]
  private val matchNameIdOrganisationView   = instanceOf[match_name_id_organisation]

  private val partnershipId                   = CdsOrganisationType.PartnershipId
  private val companyId                       = CdsOrganisationType.CompanyId
  private val limitedLiabilityPartnershipId   = CdsOrganisationType.LimitedLiabilityPartnershipId
  private val charityPublicBodyNotForProfitId = CdsOrganisationType.CharityPublicBodyNotForProfitId
  private val errorView                       = instanceOf[error_template]

  private val controller =
    new NameIdOrganisationController(
      mockAuthAction,
      mcc,
      matchNameIdOrganisationView,
      mockMatchingService,
      mockSubscriptionDetailService,
      errorView
    )

  private val organisationTypeOrganisations =
    Table(
      ("organisationType", "organisation"),
      (companyId, CompanyOrganisation),
      (partnershipId, PartnershipOrganisation),
      (limitedLiabilityPartnershipId, LimitedLiabilityPartnershipOrganisation),
      (charityPublicBodyNotForProfitId, CharityPublicBodyNotForProfitOrganisation)
    )

  private val NameMaxLength = 105

  private val UtrInvalidErrorPage      = "Enter a valid UTR number"
  private val UtrInvalidErrorField     = "Error: Enter a valid UTR number"
  private val UtrWrongLengthErrorPage  = "The UTR number must be 10 numbers"
  private val UtrWrongLengthErrorField = "Error: The UTR number must be 10 numbers"

  private val BusinessNotMatchedError =
    "Your business details have not been found. Check that your details are correct and try again."

  private def defaultOrganisationType: String = organisationTypeOrganisations.head._1

  private def invalidOrganisationTypeMessage(organisationType: String): String =
    s"Invalid organisation type '$organisationType'."

  override def beforeEach(): Unit =
    reset(mockMatchingService)

  "Viewing the Name and Utr Organisation Matching form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(defaultOrganisationType, atarService)
    )

    "display the form" in {
      showForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(fieldLevelErrorUtr) shouldBe empty
        page.getElementsText(fieldLevelErrorName) shouldBe empty
      }
    }

    "display the form with 'no UTR' link for charity-public-body-not-for-profit" in {
      showForm(organisationType = "charity-public-body-not-for-profit") { result =>
        status(result) shouldBe OK
        val page: CdsPage = CdsPage(contentAsString(result))
        page.getElementsText(
          "//*[@id=\"matchNameUtrOrganisationForm\"]/details/summary/span"
        ) shouldBe "I do not have a Corporation Tax UTR"
      }
    }

    "ensure a valid Organisation Type has been passed" in {
      val invalidOrganisationType = UUID.randomUUID.toString
      val thrown = intercept[InvalidUrlValueException] {
        showForm(invalidOrganisationType) { result =>
          await(result)
        }
      }
      thrown.getMessage should endWith(invalidOrganisationTypeMessage(invalidOrganisationType))
    }

    "hide the option for registering with name and address instead of UTR for organisations" in {
      showForm(organisationType = "limited-liability-partnership") { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(registerWithNameAndAddressLink) shouldBe empty
      }

      showForm(organisationType = "charity-public-body-not-for-profit") { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(registerWithNameAndAddressLink) shouldBe empty
      }
    }
  }

  "Submitting the form for Organisation Types that have a UTR" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(defaultOrganisationType, atarService)
    )

    "ensure a valid Organisation Type has been passed" in {
      val invalidOrganisationType = UUID.randomUUID.toString
      val thrown = intercept[InvalidUrlValueException] {
        showForm(invalidOrganisationType) { result =>
          await(result)
        }
      }
      thrown.getMessage should endWith(invalidOrganisationTypeMessage(invalidOrganisationType))
    }

    forAll(organisationTypeOrganisations) { (organisationType, organisation) =>
      s"ensure name has been entered when organisation type is $organisationType" in {
        submitForm(form = ValidNameUtrRequest + ("name" -> ""), organisationType) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          if (organisationType == partnershipId || organisationType == limitedLiabilityPartnershipId) {
            page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your registered partnership name"
            page.getElementsText(fieldLevelErrorName) shouldBe "Error: Enter your registered partnership name"
          } else if (organisationType == companyId) {
            page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your registered company name"
            page.getElementsText(fieldLevelErrorName) shouldBe "Error: Enter your registered company name"
          } else {
            page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your registered organisation name"
            page.getElementsText(fieldLevelErrorName) shouldBe "Error: Enter your registered organisation name"
          }
          page.getElementsText("title") should startWith("Error: ")
        }
      }

      s"ensure UTR has been entered when organisation type is $organisationType" in {
        submitForm(form = ValidNameUtrRequest + ("utr" -> ""), organisationType) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your UTR number"
          page.getElementsText(fieldLevelErrorUtr) shouldBe "Error: Enter your UTR number"
          page.getElementsText("title") should startWith("Error: ")
        }
      }

      s"ensure name does not exceed maximum length when organisation type is $organisationType" in {
        submitForm(form = ValidNameUtrRequest + ("name" -> oversizedString(NameMaxLength)), organisationType) {
          result =>
            status(result) shouldBe BAD_REQUEST
            val page = CdsPage(contentAsString(result))
            if (organisationType == partnershipId || organisationType == limitedLiabilityPartnershipId) {
              page.getElementsText(
                pageLevelErrorSummaryListXPath
              ) shouldBe "The partnership name must be 105 characters or less"
              page.getElementsText(
                fieldLevelErrorName
              ) shouldBe "Error: The partnership name must be 105 characters or less"
            } else if (organisationType == companyId) {
              page.getElementsText(
                pageLevelErrorSummaryListXPath
              ) shouldBe "The company name must be 105 characters or less"
              page.getElementsText(
                fieldLevelErrorName
              ) shouldBe s"Error: The company name must be 105 characters or less"
            } else {
              page.getElementsText(
                pageLevelErrorSummaryListXPath
              ) shouldBe "The organisation name must be 105 characters or less"
              page.getElementsText(
                fieldLevelErrorName
              ) shouldBe "Error: The organisation name must be 105 characters or less"
            }
            page.getElementsText("title") should startWith("Error: ")
        }
      }

      s"ensure when UTR is present it is of expected length when organisation type is $organisationType" in {
        submitForm(form = ValidNameUtrRequest + ("utr" -> "123456789"), organisationType) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe UtrWrongLengthErrorPage
          page.getElementsText(fieldLevelErrorUtr) shouldBe UtrWrongLengthErrorField
          page.getElementsText("title") should startWith("Error: ")
        }
      }

      s"ensure when UTR is correctly formatted it is a valid UTR when organisation type is $organisationType" in {
        val invalidUtr = "0123456789"
        submitForm(form = ValidNameUtrRequest + ("utr" -> invalidUtr), organisationType) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe UtrInvalidErrorPage
          page.getElementsText(fieldLevelErrorUtr) shouldBe UtrInvalidErrorField
          page.getElementsText("title") should startWith("Error: ")
        }
      }

      s"send a request to the business matching service when organisation type is $organisationType" in {
        when(
          mockMatchingService.matchBusiness(any[Utr], any[Organisation], any[Option[LocalDate]], any())(
            any[Request[AnyContent]],
            any[HeaderCarrier]
          )
        ).thenReturn(eitherT(()))
        when(
          mockSubscriptionDetailService.cacheNameDetails(any[NameOrganisationMatchModel])(any[Request[AnyContent]])
        ).thenReturn(Future.successful((): Unit))
        submitForm(ValidNameUtrRequest, organisationType) { result =>
          await(result)
          verify(mockMatchingService).matchBusiness(meq(ValidUtr), meq(organisation), meq(None), any())(
            any[Request[AnyContent]],
            any[HeaderCarrier]
          )
        }
      }
    }

    "allow UTR of length 11 with suffix of K" in {
      when(
        mockMatchingService.matchBusiness(any[Utr], any[Organisation], any[Option[LocalDate]], any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT(()))

      val utr = "2108834503K"
      submitForm(Map("name" -> "My company name", "utr" -> utr)) { result =>
        await(result)
        verify(mockMatchingService).matchBusiness(meq(Utr(utr)), any[Organisation], meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      }
    }

    "allow UTR with 11 digits plus spaces and a suffix of lowercase K" in {
      when(
        mockMatchingService.matchBusiness(any[Utr], any[Organisation], any[Option[LocalDate]], any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT(()))

      val requestUtr  = "21 08 83 45 03k"
      val expectedUtr = "2108834503K"
      submitForm(Map("name" -> "My company name", "utr" -> requestUtr)) { result =>
        await(result)
        verify(mockMatchingService).matchBusiness(meq(Utr(expectedUtr)), any[Organisation], meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      }
    }

    "not allow UTR with length 11 and suffix k and K" in {
      when(
        mockMatchingService.matchBusiness(any[Utr], any[Organisation], any[Option[LocalDate]], any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT(()))

      val utr = "516081700kK"
      submitForm(Map("name" -> "My company name", "utr" -> utr)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe UtrInvalidErrorPage
        page.getElementsText(fieldLevelErrorUtr) shouldBe UtrInvalidErrorField
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "not allow UTR with length 12 and suffix K" in {
      when(
        mockMatchingService.matchBusiness(any[Utr], any[Organisation], any[Option[LocalDate]], any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT(()))

      val utr = "51348170012K"
      submitForm(Map("name" -> "My company name", "utr" -> utr)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe UtrWrongLengthErrorPage
        page.getElementsText(fieldLevelErrorUtr) shouldBe UtrWrongLengthErrorField
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "return a Bad Request when business match is unsuccessful" in {
      when(
        mockMatchingService.matchBusiness(meq(ValidUtr), meq(CompanyOrganisation), meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.matchFailureResponse))
      submitForm(ValidNameUtrRequest) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe BusinessNotMatchedError
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "return a error-template page when downstreamFailureResponse" in {
      when(
        mockMatchingService.matchBusiness(meq(ValidUtr), meq(CompanyOrganisation), meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.downstreamFailureResponse))
      submitForm(ValidNameUtrRequest) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml("h1") shouldBe messages("cds.error.title")
      }
    }

    "return  500 and a error-template page when unknown error occurred " in {
      when(
        mockMatchingService.matchBusiness(meq(ValidUtr), meq(CompanyOrganisation), meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.otherErrorHappen))
      submitForm(ValidNameUtrRequest) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml("h1") shouldBe messages("cds.error.title")
      }
    }

    "redirect to the confirm page when match is successful" in {
      when(
        mockMatchingService.matchBusiness(meq(ValidUtr), meq(CompanyOrganisation), meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(eitherT(()))
      submitForm(ValidNameUtrRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/customs-registration-services/atar/register/matching/confirm")
      }
    }
  }

  def showForm(organisationType: String = defaultOrganisationType, userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    test(controller.form(organisationType, atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  def submitForm(
    form: Map[String, String],
    organisationType: String = defaultOrganisationType,
    userId: String = defaultUserId
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    test(
      controller
        .submit(organisationType, atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

}
