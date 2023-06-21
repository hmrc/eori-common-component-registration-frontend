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

import java.util.UUID
import common.pages.matching.ConfirmPage
import common.pages.RegistrationProcessingPage
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{
  CheckYourDetailsRegisterController,
  ConfirmContactDetailsController,
  SubscriptionFlowManager
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionDetails, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{confirm_contact_details, sub01_outcome_processing}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._

class ConfirmContactDetailsControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRegistrationConfirmService = mock[RegistrationConfirmService]
  private val mockRequestSessionData         = mock[RequestSessionData]

  private val mockSubscriptionDetailsReviewController = mock[CheckYourDetailsRegisterController]

  private val mockSessionCache              = mock[SessionCache]
  private val mockSubscriptionFlowManager   = mock[SubscriptionFlowManager]
  private val mockOrgTypeLookup             = mock[OrgTypeLookup]
  private val mockHandleSubscriptionService = mock[HandleSubscriptionService]

  private val confirmContactDetailsView  = instanceOf[confirm_contact_details]
  private val sub01OutcomeProcessingView = instanceOf[sub01_outcome_processing]

  private val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]

  private val controller = new ConfirmContactDetailsController(
    mockAuthAction,
    mockRegistrationConfirmService,
    mockRequestSessionData,
    mockSessionCache,
    mockOrgTypeLookup,
    mockSubscriptionFlowManager,
    mcc,
    confirmContactDetailsView,
    sub01OutcomeProcessingView,
    mockTaxEnrolmentsService
  )

  private val mockSubscriptionPage         = mock[SubscriptionPage]
  private val mockSubscriptionStartSession = mock[Session]
  private val mockRequestHeader            = mock[RequestHeader]

  private val mockFlowStart = (mockSubscriptionPage, mockSubscriptionStartSession)

  private val mockSub01Outcome = mock[Sub01Outcome]
  private val mockRegDetails   = mock[RegistrationDetails]

  private val testSessionData = Map[String, String]("some_session_key" -> "some_session_value")

  private val testSubscriptionStartPageUrl = "some_page_url"

  private val subscriptionDetailsHolder = SubscriptionDetails()

  override def afterEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRegistrationConfirmService)
    reset(mockRequestSessionData)
    reset(mockSubscriptionDetailsReviewController)
    reset(mockSessionCache)
    reset(mockSubscriptionFlowManager)
    reset(mockOrgTypeLookup)
    reset(mockHandleSubscriptionService)
    reset(mockTaxEnrolmentsService)

    super.afterEach()
  }

  "Reviewing the details" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))

    "return ok when data has been provided" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockRequestSessionData.selectedUserLocation(any())) thenReturn None

      invokeConfirm() { result =>
        status(result) shouldBe OK
      }
    }

    "throw illegal state exception when Registration Details are not available" in {
      mockCacheWithRegistrationDetails(limitedLiabilityPartnershipRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenThrow(new IllegalStateException("No Registration details in cache."))

      invokeConfirm() { result =>
        val thrown = intercept[IllegalStateException] {
          await(result)
        }
        thrown.getMessage shouldBe "No Registration details in cache."
      }
    }

    "display all fields when all are provided from the cache" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockRequestSessionData.selectedUserLocation(any())) thenReturn None

      invokeConfirm() { result =>
        val page = CdsPage(contentAsString(result))

        page.getElementsText(ConfirmPage.addressXPath) shouldBe strim(
          """Line 1 line 2 line 3 SE28 1AA United Kingdom"""
        )
      }
    }

    "redirect to OrganisationTypeController when no org type found in cache" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(None))

      when(mockSessionCache.remove(any())) thenReturn Future.successful(true)

      invokeConfirm() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe OrganisationTypeController.form(atarService).url
      }
    }

    "redirect to OrganisationTypeController when org type is different than individual or organisation" in {
      val details = RegistrationDetailsSafeId(
        safeId = SafeId("123"),
        address = Address(
          addressLine1 = "test",
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          postalCode = None,
          countryCode = "123"
        ),
        sapNumber = TaxPayerId("123"),
        customsId = None,
        name = "name"
      )
      mockCacheWithRegistrationDetails(details)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockSessionCache.remove(any())) thenReturn Future.successful(true)

      invokeConfirm() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe OrganisationTypeController.form(atarService).url
      }
    }

    "display all fields when all are provided from the cache for sole trader" in {
      mockCacheWithRegistrationDetails(soleTraderRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      invokeConfirm() { result =>
        val page = CdsPage(contentAsString(result))

        page.getElementsText(ConfirmPage.addressXPath) shouldBe strim(
          """Line 1 line 2 line 3 SE28 1AA United Kingdom"""
        )
      }
    }

    "display all fields when all are provided from the cache for sole trader with nino" in {
      mockCacheWithRegistrationDetails(soleTraderRegistrationDetails.copy(customsId = Some(Nino("QQ123456C"))))
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      invokeConfirm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(ConfirmPage.addressXPath) shouldBe strim(
          """Line 1 line 2 line 3 SE28 1AA United Kingdom"""
        )
      }
    }

    "display back link correctly" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockRequestSessionData.selectedUserLocation(any())) thenReturn None

      invokeConfirm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementAttributeHref(ConfirmPage.backLinkXPath) shouldBe previousPageUrl
      }
    }
  }

  "Selecting Yes" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService))

    "redirect to the page defined by subscription flow start when service returns NewSubscription for organisation" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      mockNewSubscriptionFromSubscriptionStatus()
      mockSubscriptionFlowStart()
      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe testSubscriptionStartPageUrl
        testSessionData foreach (
          newSessionValue => result.session(mockRequestHeader).data should contain(newSessionValue)
        )
      }
    }

    "redirect to SubscriptionRecoveryController when service returns SubscriptionExists and no existing enrolments for organisation" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockRegistrationConfirmService
          .currentSubscriptionStatus(any[HeaderCarrier], any[Service], any[Request[_]])
      ).thenReturn(Future.successful(SubscriptionExists))

      when(
        mockTaxEnrolmentsService.doesPreviousEnrolmentExists(any[SafeId])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(false))

      mockSubscriptionFlowStart()
      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe SubscriptionRecoveryController.complete(atarService).url
      }
    }

    "redirect to SignInWithDifferentDetailsController when service returns SubscriptionExists and previous enrolment exists for organisation" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockRegistrationConfirmService
          .currentSubscriptionStatus(any[HeaderCarrier], any[Service], any[Request[_]])
      ).thenReturn(Future.successful(SubscriptionExists))

      when(
        mockTaxEnrolmentsService.doesPreviousEnrolmentExists(any[SafeId])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(true))

      mockSubscriptionFlowStart()
      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe SignInWithDifferentDetailsController.form(atarService).url
      }
    }

    "redirect to OrganisationTypeController when no org type found in cache" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(None))

      when(mockSessionCache.remove(any())) thenReturn Future.successful(true)

      invokeConfirmContactDetailsWithoutOptionSelected() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe OrganisationTypeController.form(atarService).url
      }
    }

    "redirect to OrganisationTypeController when org type is different than individual or organisation" in {
      val details = RegistrationDetailsSafeId(
        safeId = SafeId("123"),
        address = Address(
          addressLine1 = "test",
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          postalCode = None,
          countryCode = "123"
        ),
        sapNumber = TaxPayerId("123"),
        customsId = None,
        name = "name"
      )
      mockCacheWithRegistrationDetails(details)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockSessionCache.remove(any())) thenReturn Future.successful(true)

      invokeConfirmContactDetailsWithoutOptionSelected() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe OrganisationTypeController.form(atarService).url
      }
    }

    "redirect to Review page while on review mode when service returns NewSubscription for organisation" in {
      when(mockSubscriptionPage.url(atarService)).thenReturn(testSubscriptionStartPageUrl)
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      mockNewSubscriptionFromSubscriptionStatus()
      invokeConfirmContactDetailsWithSelectedOption(isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/register/matching/review-determine")
      }
    }

    "redirect to the page defined by subscription flow start when service returns NewSubscription for individual with selected type" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockRequestSessionData
          .userSelectedOrganisationType(any[Request[AnyContent]])
      ).thenReturn(Some(CdsOrganisationType.Individual))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(individualRegistrationDetails)
      mockNewSubscriptionFromSubscriptionStatus()
      mockSubscriptionFlowStart()
      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe testSubscriptionStartPageUrl
        testSessionData foreach (
          newSessionValue => result.session(mockRequestHeader).data should contain(newSessionValue)
        )
      }
    }

    "redirect to the confirm individual type page when service returns NewSubscription for individual without selected type" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(individualRegistrationDetails)
      when(
        mockRequestSessionData
          .userSelectedOrganisationType(any[Request[AnyContent]])
      ).thenReturn(None)

      mockNewSubscriptionFromSubscriptionStatus()
      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmIndividualTypeController
          .form(atarService)
          .url
      }
    }

    "redirect to the confirm individual type page when service returns SubscriptionRejected for individual without selected type" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      mockCacheWithRegistrationDetails(individualRegistrationDetails)
      when(
        mockRequestSessionData
          .userSelectedOrganisationType(any[Request[AnyContent]])
      ).thenReturn(None)

      when(
        mockRegistrationConfirmService
          .currentSubscriptionStatus(any[HeaderCarrier], any[Service], any[Request[_]])
      ).thenReturn(Future.successful(SubscriptionRejected))

      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmIndividualTypeController
          .form(atarService)
          .url
      }
    }

    val redirectUrl =
      uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
        .processing(atarService)
        .url
    val subscriptionStatus = SubscriptionProcessing
    s"redirect to $redirectUrl when subscription status is $subscriptionStatus" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any[Service], any[Request[_]]))
        .thenReturn(Future.successful(subscriptionStatus))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(organisationRegistrationDetails))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      invokeConfirmContactDetailsWithSelectedOption() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe redirectUrl
      }
    }

    "allow authenticated users to access the processing page" in {
      invokeProcessingPageWithAuthenticatedUser() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title() should startWith(RegistrationProcessingPage.title)
      }
    }
    "redirect to Address Invalid Page when the postcode return from REG01(Register with Id) response is invalid for a Organisation" in {
      val address: Address = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("AAA 123"), "GB")
      mockCacheWithRegistrationDetails(organisationRegistrationDetails.copy(address = address))

      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockSessionCache.subscriptionDetails(any())).thenReturn(
        Future.successful(SubscriptionDetails(addressDetails = None))
      )
      when(mockSessionCache.saveSubscriptionDetails(any())(any())).thenReturn(Future.successful(true))

      invokeConfirm() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressInvalidController
          .page(atarService)
          .url
      }
    }

    "redirect to Address Invalid Page when the postcode return from REG01(Register with Id) response is invalid for a SoleTrader/Individual" in {
      val address: Address = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), None, "GB")
      mockCacheWithRegistrationDetails(individualRegistrationDetails.copy(address = address))

      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      when(mockSessionCache.subscriptionDetails(any())).thenReturn(
        Future.successful(SubscriptionDetails(addressDetails = None))
      )
      when(mockSessionCache.saveSubscriptionDetails(any())(any())).thenReturn(Future.successful(true))

      invokeConfirm() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressInvalidController
          .page(atarService)
          .url
      }
    }
  }

  "Selecting No" should {
    "clear data and redirect to organisation type page" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockRegistrationConfirmService
          .clearRegistrationData()(any[Request[_]])
      ).thenReturn(Future.successful(()))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(organisationRegistrationDetails))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      invokeConfirmContactDetailsWithSelectedOption(selectedOption = "no") { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.OrganisationTypeController
          .form(atarService)
          .url
      }
    }

    "throw an exception when an unexpected error occurs" in {
      val emulatedFailure = new RuntimeException("Something bad happened")
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockRegistrationConfirmService
          .clearRegistrationData()(any[Request[_]])
      ).thenReturn(Future.failed(emulatedFailure))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(organisationRegistrationDetails))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      val caught = intercept[RuntimeException] {
        invokeConfirmContactDetailsWithSelectedOption(selectedOption = "no") { result =>
          await(result)
        }
      }
      caught shouldBe emulatedFailure
    }
  }

  "Selecting wrong address" should {
    "clear data and display contact companies house for organisation " in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(CorporateBody)
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockRegistrationConfirmService
          .clearRegistrationData()(any[Request[_]])
      ).thenReturn(Future.successful(()))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(organisationRegistrationDetails))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))

      invokeConfirmContactDetailsWithSelectedOption(selectedOption = "wrong-address") { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.YouCannotChangeAddressController
          .page(atarService)
          .url
      }
    }

    "clear data and display contact HMRC  for individual " in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(UnincorporatedBody)
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockRegistrationConfirmService
          .clearRegistrationData()(any[Request[_]])
      ).thenReturn(Future.successful(()))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(individualRegistrationDetails))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))
      when(
        mockRequestSessionData
          .isIndividualOrSoleTrader(any[Request[AnyContent]])
      ).thenReturn(true)

      invokeConfirmContactDetailsWithSelectedOption(selectedOption = "wrong-address") { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.YouCannotChangeAddressController
          .page(atarService)
          .url
      }
    }

    "clear data and display contact HMRC for partnership " in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Partnership)
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(subscriptionDetailsHolder))
      when(
        mockRegistrationConfirmService
          .clearRegistrationData()(any[Request[_]])
      ).thenReturn(Future.successful(()))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(individualRegistrationDetails))
      when(
        mockSessionCache
          .saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])
      ).thenReturn(Future.successful(true))
      when(
        mockRequestSessionData
          .isIndividualOrSoleTrader(any[Request[AnyContent]])
      ).thenReturn(true)

      invokeConfirmContactDetailsWithSelectedOption(selectedOption = "wrong-address") { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.YouCannotChangeAddressController
          .page(atarService)
          .url
      }
    }
  }

  "The Yes No WrongAddress Radio Button " should {

    "display a relevant error if nothing is chosen" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      invokeConfirmContactDetailsWithoutOptionSelected() { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(ConfirmPage.pageLevelErrorSummaryListXPath) shouldBe "Select yes if this is your address"
        page.getElementsText(
          ConfirmPage.fieldLevelErrorYesNoWrongAddress
        ) shouldBe "Error: Select yes if this is your address"
      }
    }

    "displays a relevant error if no option is chosen and org type is sole-trader" in {
      mockCacheWithRegistrationDetails(individualRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      invokeConfirmContactDetailsWithoutOptionSelected() { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(ConfirmPage.pageLevelErrorSummaryListXPath) shouldBe "Select yes if this is your address"
        page.getElementsText(
          ConfirmPage.fieldLevelErrorYesNoWrongAddress
        ) shouldBe "Error: Select yes if this is your address"
      }
    }

    "display a relevant error if an invalid answer option is selected" in {
      val invalidOption = UUID.randomUUID.toString
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgTypeOpt(any[Request[AnyContent]])
      ).thenReturn(Future.successful(Some(Partnership)))

      invokeConfirmContactDetailsWithSelectedOption(selectedOption = invalidOption) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(ConfirmPage.pageLevelErrorSummaryListXPath) shouldBe "Select yes if this is your address"
        page.getElementsText(
          ConfirmPage.fieldLevelErrorYesNoWrongAddress
        ) shouldBe "Error: Select yes if this is your address"
      }
    }
  }

  private def mockCacheWithRegistrationDetails(details: RegistrationDetails): Unit =
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(details)

  private def mockNewSubscriptionFromSubscriptionStatus() =
    when(
      mockRegistrationConfirmService
        .currentSubscriptionStatus(any[HeaderCarrier], any[Service], any[Request[_]])
    ).thenReturn(Future.successful(NewSubscription))

  private def mockSubscriptionFlowStart(): Unit = {
    when(mockSubscriptionPage.url(atarService)).thenReturn(testSubscriptionStartPageUrl)
    when(mockSubscriptionStartSession.data).thenReturn(testSessionData)
    when(
      mockSubscriptionFlowManager
        .startSubscriptionFlow(any[Service])(any[Request[AnyContent]])
    ).thenReturn(Future.successful(mockFlowStart))
  }

  private def invokeConfirm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .form(atarService)
        .apply(SessionBuilder.buildRequestWithSession(userId))
    )
  }

  private def invokeConfirmContactDetailsWithSelectedOption(
    userId: String = defaultUserId,
    selectedOption: String = "yes",
    isInReviewMode: Boolean = false
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(atarService, isInReviewMode)
        .apply(
          SessionBuilder.buildRequestWithSessionAndFormValues(userId, Map("yes-no-wrong-address" -> selectedOption))
        )
    )
  }

  private def invokeConfirmContactDetailsWithoutOptionSelected(
    userId: String = defaultUserId
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(atarService)
        .apply(SessionBuilder.buildRequestWithSession(userId))
    )
  }

  private def setupMocksForRejectedAndProcessingPages = {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(mockRegDetails)
    when(mockRegDetails.name).thenReturn("orgName")
    when(mockSessionCache.sub01Outcome(any[Request[_]]))
      .thenReturn(Future.successful(mockSub01Outcome))
    when(mockSub01Outcome.processedDate).thenReturn("22 May 2016")
  }

  def invokeProcessingPageWithAuthenticatedUser(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    setupMocksForRejectedAndProcessingPages
    test(
      controller.processing(atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/atar/subscribe", userId))
    )
  }

}
