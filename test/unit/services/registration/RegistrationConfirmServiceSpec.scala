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

package unit.services.registration

import base.UnitSpec
import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  ClearCacheAndRegistrationIdentificationService,
  RequestSessionData,
  SessionCache
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  NewSubscription,
  PreSubscriptionStatus,
  SubscriptionProcessing,
  SubscriptionStatusService
}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RegistrationConfirmServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {
  private val mockCdsFrontendDataCache                           = mock[SessionCache]
  private val mockSubscriptionStatusService                      = mock[SubscriptionStatusService]
  private val mockRequestSessionData                             = mock[RequestSessionData]
  private val mockClearCacheAndRegistrationIdentificationService = mock[ClearCacheAndRegistrationIdentificationService]

  val service = new RegistrationConfirmService(
    mockCdsFrontendDataCache,
    mockSubscriptionStatusService,
    mockRequestSessionData,
    mockClearCacheAndRegistrationIdentificationService
  )(global)

  implicit val hc: HeaderCarrier                = mock[HeaderCarrier]
  implicit val mockLoggedInUser: LoggedInUser   = mock[LoggedInUser]
  implicit val mockRequest: Request[AnyContent] = mock[Request[AnyContent]]

  private val mockRegistrationDetailsFunction = mock[(RegistrationDetails) => String]
  val registrationDetailsFunctionResult       = "Success!"
  private val mockRegistrationDetails         = mock[RegistrationDetails]

  val emulatedFailure = new UnsupportedOperationException("Emulated service call failure.")

  val sapNumber = "1234567890"

  override protected def beforeEach() {
    reset(
      mockCdsFrontendDataCache,
      mockSubscriptionStatusService,
      mockRequestSessionData,
      mockClearCacheAndRegistrationIdentificationService,
      mockRegistrationDetailsFunction,
      mockRegistrationDetails
    )
  }

  "currentSubscriptionStatus" should {
    "get registration details from cache and call SubscriptionStatusService with SAP number" in {
      mockGetStatus(mock[PreSubscriptionStatus])

      await(service.currentSubscriptionStatus)

      val inOrder = Mockito.inOrder(mockCdsFrontendDataCache, mockSubscriptionStatusService)
      inOrder.verify(mockCdsFrontendDataCache).registrationDetails(meq(hc))
      inOrder
        .verify(mockSubscriptionStatusService)
        .getStatus(meq("taxPayerID"), meq(TaxPayerId(sapNumber).mdgTaxPayerId))(meq(hc))
    }

    "return expected status for EU Individual registered without ID when subscription status is new" in {
      mockGetStatus(NewSubscription)
      mockOrganisationTypeWithRegistration(CdsOrganisationType.EUIndividual, maybeCustomsId = None)

      await(service.currentSubscriptionStatus) shouldBe NewSubscription
    }

    "return expected status for EU Individual registered without ID when subscription status is not new" in {
      mockGetStatus(SubscriptionProcessing)
      mockOrganisationTypeWithRegistration(CdsOrganisationType.EUIndividual, maybeCustomsId = None)

      await(service.currentSubscriptionStatus) shouldBe SubscriptionProcessing
    }

    "return expected status for EU Organisation registered without ID when subscription status is new" in {
      mockGetStatus(NewSubscription)
      mockOrganisationTypeWithRegistration(CdsOrganisationType.EUOrganisation, maybeCustomsId = None)

      await(service.currentSubscriptionStatus) shouldBe NewSubscription

      val inOrder = Mockito.inOrder(mockRequestSessionData, mockRegistrationDetails)
      inOrder.verify(mockRegistrationDetails).sapNumber
      inOrder.verifyNoMoreInteractions()
    }

    "return expected Status when Not EU Subscription and is not new" in {
      mockGetStatus(SubscriptionProcessing)

      await(service.currentSubscriptionStatus) shouldBe SubscriptionProcessing

      val inOrder = Mockito.inOrder(mockCdsFrontendDataCache, mockSubscriptionStatusService)
      inOrder.verify(mockCdsFrontendDataCache).registrationDetails(meq(hc))
      inOrder
        .verify(mockSubscriptionStatusService)
        .getStatus(meq("taxPayerID"), meq(TaxPayerId(sapNumber).mdgTaxPayerId))(meq(hc))
      inOrder.verifyNoMoreInteractions()
    }

    "return expected Status when Not EU Subscription and is new" in {
      mockGetStatus(NewSubscription)

      mockOrganisationTypeWithRegistration(CdsOrganisationType.Company, maybeCustomsId = None)

      await(service.currentSubscriptionStatus) shouldBe NewSubscription

      val inOrder = Mockito.inOrder(mockCdsFrontendDataCache, mockSubscriptionStatusService)
      inOrder.verify(mockCdsFrontendDataCache).registrationDetails(meq(hc))
      inOrder
        .verify(mockSubscriptionStatusService)
        .getStatus(meq("taxPayerID"), meq(TaxPayerId(sapNumber).mdgTaxPayerId))(meq(hc))
      inOrder.verifyNoMoreInteractions()
    }

    "propagate cache access error" in {
      mockGetStatus(mock[PreSubscriptionStatus])
      when(mockCdsFrontendDataCache.registrationDetails(meq(hc))).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[Exception] {
        await(service.currentSubscriptionStatus)
      }
      caught shouldBe emulatedFailure
    }

    "propagate SubscriptionStatusService access error" in {
      mockGetStatus(mock[PreSubscriptionStatus])
      when(
        mockSubscriptionStatusService.getStatus(meq("taxPayerID"), meq(TaxPayerId(sapNumber).mdgTaxPayerId))(meq(hc))
      ).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[Exception] {
        await(service.currentSubscriptionStatus)
      }
      caught shouldBe emulatedFailure
    }

    def mockGetStatus(expectedResult: PreSubscriptionStatus): Unit = {
      when(mockRegistrationDetails.sapNumber).thenReturn(TaxPayerId(sapNumber))
      when(mockCdsFrontendDataCache.registrationDetails(meq(hc))).thenReturn(Future.successful(mockRegistrationDetails))
      when(
        mockSubscriptionStatusService.getStatus(meq("taxPayerID"), meq(TaxPayerId(sapNumber).mdgTaxPayerId))(meq(hc))
      ).thenReturn(Future.successful(expectedResult))

    }

    def mockOrganisationTypeWithRegistration(organisationType: CdsOrganisationType, maybeCustomsId: Option[CustomsId]) {
      when(mockRequestSessionData.userSelectedOrganisationType(meq(mockRequest))).thenReturn(Some(organisationType))
      when(mockRegistrationDetails.customsId).thenReturn(maybeCustomsId)
    }
  }

  "clearRegistrationData" should {
    "call registration data clean service" in {
      service.clearRegistrationData(mockLoggedInUser)

      verify(mockClearCacheAndRegistrationIdentificationService).clear(meq(mockLoggedInUser))(meq(hc))
    }

    "return success when data clean succeeds" in {
      when(mockClearCacheAndRegistrationIdentificationService.clear(meq(mockLoggedInUser))(meq(hc)))
        .thenReturn(Future.successful(()))

      await(service.clearRegistrationData(mockLoggedInUser)) should be(())
    }

    "propagate error when accessing registration data clean service" in {
      when(mockClearCacheAndRegistrationIdentificationService.clear(meq(mockLoggedInUser))(meq(hc)))
        .thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[Exception] {
        await(service.clearRegistrationData(mockLoggedInUser))
      }
      caught shouldBe emulatedFailure
    }
  }
}
