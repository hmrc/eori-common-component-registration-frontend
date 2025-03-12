/*
 * Copyright 2025 HM Revenue & Customs
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

package unit.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{AnyContent, Request, Session}
import play.api.test.Helpers.await
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, UnincorporatedBody}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.YesNoWrongAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.YesNoWrongAddress.{noAnswered, wrongAddress, yesAnswered}
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.confirm_contact_details
import uk.gov.hmrc.http.HeaderCarrier
import util.ViewSpec
import util.builders.RegistrationDetailsBuilder
import util.builders.SubscriptionFormBuilder.detailsHolderWithAllFields

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ConfirmContactDetailsServiceSpec extends ViewSpec with MockitoSugar with Injecting {

  implicit val hc: HeaderCarrier       = HeaderCarrier()
  implicit val rq: Request[AnyContent] = withFakeCSRF(FakeRequest())

  private val mockSessionCache               = mock[SessionCache]
  private val mockRegistrationConfirmService = mock[RegistrationConfirmService]
  private val mockOrgTypeLookup              = mock[OrgTypeLookup]
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val confirmContactDetailsView      = inject[confirm_contact_details]
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockTaxEnrolmentsService       = mock[TaxEnrolmentsService]
  private val mockSubscriptionPage           = mock[SubscriptionPage]
  private val mockSubscriptionStartSession   = mock[Session]
  private val mockFlowStart                  = (mockSubscriptionPage, mockSubscriptionStartSession)

  private val yesNoWrongAddressForm: Form[YesNoWrongAddress] =
    YesNoWrongAddress.createForm().bind(Map("yes-no-answer" -> ""))

  private val subscriptionToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  val service = new ConfirmContactDetailsService(
    mockSessionCache,
    mockRegistrationConfirmService,
    mockOrgTypeLookup,
    mockRequestSessionData,
    confirmContactDetailsView,
    mockSubscriptionFlowManager,
    mockTaxEnrolmentsService
  )

  "checkAddressDetails" should {

    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(
      Future.successful(detailsHolderWithAllFields)
    )
    when(mockSessionCache.saveSubscriptionDetails(any())(any[Request[_]])).thenReturn(Future.successful(true))

    "get cached details and redirect to YouCannotChangeAddressController when individual with wrongAddress selected" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails)
        )

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(wrongAddress)))
        )
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${testService.code}/register/you-cannot-change-address"

    }

    "get cached details and redirect to OrganisationTypeController when individual with No selected" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails)
        )
        when(mockRegistrationConfirmService.clearRegistrationData()).thenReturn(Future.successful((): Unit))

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(noAnswered)))
        )
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${testService.code}/register/matching/organisation-type"

    }

    "redirect a onNewSubscription to startSubscriptionFlow when organisation and answer is yes" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
        )
        when(
          mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any(), any[Request[_]])
        ).thenReturn(Future.successful(NewSubscription))
        when(mockSubscriptionFlowManager.startSubscriptionFlow(any())(any[Request[AnyContent]])).thenReturn(
          Future.successful(mockFlowStart)
        )
        when(mockSubscriptionPage.url(any())).thenReturn("/test")

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(yesAnswered)))
        )
        result.header.status mustBe SEE_OTHER

    }

    "redirect a SubscriptionProcessing to ConfirmContactDetailsController when organisation and answer is yes" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
        )
        when(
          mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any(), any[Request[_]])
        ).thenReturn(Future.successful(SubscriptionProcessing))

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(yesAnswered)))
        )
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${testService.code}/register/processing"

    }

    "redirect a SubscriptionExists to SignInWithDifferentDetailsController when enrolment exists" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
        )
        when(
          mockTaxEnrolmentsService.doesPreviousEnrolmentExists(any())(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(true))
        when(
          mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any(), any[Request[_]])
        ).thenReturn(Future.successful(SubscriptionExists))

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(yesAnswered)))
        )
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${testService.code}/register/you-need-to-sign-in-with-different-details"

    }

    "redirect a SubscriptionExists to SubscriptionRecoveryController when enrolment does not exist" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
        )
        when(
          mockTaxEnrolmentsService.doesPreviousEnrolmentExists(any())(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.successful(false))
        when(
          mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any(), any[Request[_]])
        ).thenReturn(Future.successful(SubscriptionExists))

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(yesAnswered)))
        )
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${testService.code}/register/complete-enrolment"

    }

    "throw IllegalStateException when invalid option selected" in subscriptionToTest.foreach { testService =>
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
        Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
      )

      val result = intercept[IllegalStateException] {
        await(
          service.checkAddressDetails(
            testService,
            isInReviewMode = false,
            YesNoWrongAddress.apply(Some("Invalid answer"))
          )
        )
      }
      result.getMessage mustBe "YesNoWrongAddressForm field somehow had a value that wasn't yes, no, wrong address, or empty"

    }

    "redirect to DetermineReviewPageController when 'isInReview' and orgType is empty" in subscriptionToTest.foreach {
      testService =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
        )
        when(
          mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any(), any[Request[_]])
        ).thenReturn(Future.successful(NewSubscription))

        val result = await(
          service.checkAddressDetails(testService, isInReviewMode = true, YesNoWrongAddress.apply(Some(yesAnswered)))
        )
        result.header.status mustBe SEE_OTHER

    }

    "redirect to ConfirmIndividualTypeController when is individual" in subscriptionToTest.foreach { testService =>
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
        Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails)
      )
      when(
        mockRegistrationConfirmService.currentSubscriptionStatus(any[HeaderCarrier], any(), any[Request[_]])
      ).thenReturn(Future.successful(NewSubscription))
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)

      val result = await(
        service.checkAddressDetails(testService, isInReviewMode = false, YesNoWrongAddress.apply(Some(yesAnswered)))
      )
      result.header.status mustBe SEE_OTHER

    }

    "handleAddressAndPopulateView" should {

      "Redirect to AddressInvalidController when invalid address submitted for individual" in subscriptionToTest.foreach {
        testService =>
          when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
            Future.successful(
              RegistrationDetailsBuilder.individualRegistrationDetails.copy(address =
                Address.apply("", None, None, None, None, "")
              )
            )
          )

          val result = await(service.handleAddressAndPopulateView(testService, isInReviewMode = false))
          result.header.headers(
            "Location"
          ) mustBe s"/customs-registration-services/${testService.code}/register/address-invalid"
      }

      "Populate confirmContactDetailsView when valid address submitted for individual" in subscriptionToTest.foreach {
        testService =>
          when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
            Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails)
          )

          val result = await(service.handleAddressAndPopulateView(testService, isInReviewMode = false))
          result.header.status mustBe OK
      }

      "Redirect to AddressInvalidController when invalid address submitted for organisation" in subscriptionToTest.foreach {
        testService =>
          when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
            Future.successful(
              RegistrationDetailsBuilder.organisationRegistrationDetails.copy(address =
                Address.apply("", None, None, None, None, "")
              )
            )
          )

          val result = await(service.handleAddressAndPopulateView(testService, isInReviewMode = false))
          result.header.headers(
            "Location"
          ) mustBe s"/customs-registration-services/${testService.code}/register/address-invalid"
      }

      "Populate confirmContactDetailsView when valid address submitted for organisation" in subscriptionToTest.foreach {
        testService =>
          when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
            Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
          )
          when(mockOrgTypeLookup.etmpOrgTypeOpt).thenReturn(Future.successful(Some(CorporateBody)))
          when(mockRequestSessionData.selectedUserLocation).thenReturn(Some(UserLocation.Uk))

          val result = await(service.handleAddressAndPopulateView(testService, isInReviewMode = false))
          result.header.status mustBe OK
      }

      "Populate confirmContactDetailsView when valid address submitted for UnincorporatedBody" in subscriptionToTest.foreach {
        testService =>
          when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
            Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
          )
          when(mockOrgTypeLookup.etmpOrgTypeOpt).thenReturn(Future.successful(Some(UnincorporatedBody)))
          when(mockRequestSessionData.selectedUserLocation).thenReturn(Some(UserLocation.Uk))

          val result = await(service.handleAddressAndPopulateView(testService, isInReviewMode = false))
          result.header.status mustBe OK
      }

      "Populate confirmContactDetailsView when valid address submitted for None type" in subscriptionToTest.foreach {
        testService =>
          when(mockOrgTypeLookup.etmpOrgTypeOpt).thenReturn(Future.successful(None))
          when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
            Future.successful(RegistrationDetailsBuilder.emptyETMPOrgTypeRegistrationDetails)
          )
          when(mockSessionCache.remove).thenReturn(Future.successful(true))

          val result = await(service.handleAddressAndPopulateView(testService, isInReviewMode = false))
          result.header.status mustBe SEE_OTHER
          result.header.headers(
            "Location"
          ) mustBe s"/customs-registration-services/${testService.code}/register/matching/organisation-type"
      }
    }
  }

  "handleFormWithErrors" should {

    "redirect to confirmContactDetailsView for individual" in subscriptionToTest.foreach { testService =>
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
        Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails)
      )

      val result = await(service.handleFormWithErrors(isInReviewMode = false, yesNoWrongAddressForm, testService))
      result.header.status mustBe BAD_REQUEST
    }

    "redirect to confirmContactDetailsView for organisation" in subscriptionToTest.foreach { testService =>
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
        Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
      )
      when(mockOrgTypeLookup.etmpOrgTypeOpt).thenReturn(Future.successful(Some(CorporateBody)))

      val result = await(service.handleFormWithErrors(isInReviewMode = false, yesNoWrongAddressForm, testService))
      result.header.status mustBe BAD_REQUEST
    }

    "redirect to confirmContactDetailsView for orgType None" in subscriptionToTest.foreach { testService =>
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
        Future.successful(RegistrationDetailsBuilder.organisationRegistrationDetails)
      )
      when(mockOrgTypeLookup.etmpOrgTypeOpt).thenReturn(Future.successful(None))
      when(mockSessionCache.remove).thenReturn(Future.successful(true))

      val result = await(service.handleFormWithErrors(isInReviewMode = false, yesNoWrongAddressForm, testService))
      result.header.status mustBe SEE_OTHER
    }
  }

}
