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

package unit.services

import base.UnitSpec
import java.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionBusinessService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{ContactDetailsAdaptor, RegistrationDetailsCreator}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.util.Random

class SubscriptionBusinessServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  private val mockCdsFrontendDataCache          = mock[SessionCache]
  private val mockRegistrationDetailsCreator    = mock[RegistrationDetailsCreator]
  private val registrationInfo                  = mock[RegistrationInfo]
  private val mockRegistrationDetails           = mock[RegistrationDetails]
  private val mockContactDetailsAdaptor         = mock[ContactDetailsAdaptor]
  private val mockSubscriptionDetailsHolder     = mock[SubscriptionDetails]
  private val mockpersonalDataDisclosureConsent = mock[Option[Boolean]]
  private val mockContactDetailsModel           = mock[ContactDetailsModel]

  private val expectedDate      = LocalDate.now()
  private val maybeExpectedDate = Some(expectedDate)

  val sicCode = Some("someSicCode")

  private val subscriptionBusinessService =
    new SubscriptionBusinessService(mockCdsFrontendDataCache)(global)

  private val eoriNumericLength   = 15
  private val eoriId              = "GB" + Random.nextString(eoriNumericLength)
  private val eori                = Eori(eoriId)
  val maybeEoriId                 = Some(eoriId)
  val mayBeCachedAddressViewModel = Some(AddressViewModel("Address Line 1", "city", Some("postcode"), "GB"))
  val nameIdOrganisationDetails   = Some(NameIdOrganisationMatchModel("OrgName", "ID"))
  val customsIDUTR                = Some(Utr("ID"))

  val email = Some("OrgName@example.com")

  val emulatedFailure = new UnsupportedOperationException("Emulation of failure")

  override def beforeEach {
    reset(
      mockCdsFrontendDataCache,
      mockRegistrationDetailsCreator,
      registrationInfo,
      mockRegistrationDetails,
      mockSubscriptionDetailsHolder,
      mockContactDetailsAdaptor
    )

    when(
      mockCdsFrontendDataCache
        .saveRegistrationDetails(ArgumentMatchers.any[RegistrationDetails])(ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(Future.successful(true))

    when(
      mockCdsFrontendDataCache
        .saveSubscriptionDetails(ArgumentMatchers.any[SubscriptionDetails])(ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(Future.successful(true))

    val existingHolder = SubscriptionDetails(contactDetails = Some(mock[ContactDetailsModel]))

    when(mockCdsFrontendDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(existingHolder)
    when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(mockpersonalDataDisclosureConsent)

    when(mockRegistrationDetailsCreator.registrationDetails(Some(eori))(registrationInfo))
      .thenReturn(mockRegistrationDetails)
  }

  "Calling maybeCachedContactDetailsModel" should {
    "retrieve cached data if already stored in cdsFrontendCache" in {
      val contactDetailsModel = Some(mockContactDetailsModel)

      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(contactDetailsModel)
      await(subscriptionBusinessService.cachedContactDetailsModel) shouldBe contactDetailsModel
      verify(mockCdsFrontendDataCache).subscriptionDetails
    }

    "return None if no data has been found in the cache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(None)
      await(subscriptionBusinessService.cachedContactDetailsModel) shouldBe None
    }
  }

  "Calling maybeCachedSicCode" should {
    "retrieve cached data if already stored in cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.sicCode).thenReturn(sicCode)
      await(subscriptionBusinessService.cachedSicCode) shouldBe sicCode
      verify(mockCdsFrontendDataCache).subscriptionDetails
    }

    "return None if no data has been found in the cache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.sicCode).thenReturn(None)
      await(subscriptionBusinessService.cachedSicCode) shouldBe None
    }
  }

  "Calling retrieveSubscriptionDetailsHolder" should {
    "fail when cache fails accessing current SubscriptionDetailsHolder" in {
      when(mockCdsFrontendDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[RuntimeException] {
        await(subscriptionBusinessService.retrieveSubscriptionDetailsHolder)
      }
      caught shouldBe emulatedFailure
    }
  }

  "Calling getCachedDateEstablished" should {
    "retrieve any previously cached Date Of Establishment Details from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.dateEstablished).thenReturn(Some(expectedDate))
      await(subscriptionBusinessService.getCachedDateEstablished) shouldBe expectedDate

    }

    "throw exception when there are no Date Of Establishment details in the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.dateEstablished).thenReturn(None)
      val thrown = intercept[IllegalStateException] {
        await(subscriptionBusinessService.getCachedDateEstablished)
      }
      thrown.getMessage shouldBe "No Date Of Establishment Cached"
    }
  }

  "Calling maybeCachedDateEstablished" should {
    "retrieve cached date established if already stored in cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.dateEstablished).thenReturn(maybeExpectedDate)
      await(subscriptionBusinessService.maybeCachedDateEstablished) shouldBe maybeExpectedDate
      verify(mockCdsFrontendDataCache).subscriptionDetails
    }

    "return None if no data has been found in the cache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.dateEstablished).thenReturn(None)
      await(subscriptionBusinessService.maybeCachedDateEstablished) shouldBe None
    }
  }

  "Calling getCachedSicCode" should {
    "retrieve any previously cached Sic Code from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.sicCode).thenReturn(sicCode)
      await(subscriptionBusinessService.getCachedSicCode) shouldBe "someSicCode"

    }

    "throw exception when there are no SIC Code details in the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.sicCode).thenReturn(None)
      val thrown = intercept[IllegalStateException] {
        await(subscriptionBusinessService.getCachedSicCode)
      }
      thrown.getMessage shouldBe "No SIC Code Cached"
    }
  }

  "Calling getCachedPersonalDataDisclosureConsent" should {
    "retrieve any previously cached consent Details from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(Some(false))
      await(subscriptionBusinessService.getCachedPersonalDataDisclosureConsent) shouldBe false
    }

    "throw exception when there are no consent details in the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(None)
      val thrown = intercept[IllegalStateException] {
        await(subscriptionBusinessService.getCachedPersonalDataDisclosureConsent)
      }
      thrown.getMessage shouldBe "No Personal Data Disclosure Consent Cached"
    }
  }

  "Calling mayBeCachedAddressViewModel" should {
    "retrieve cached data if already stored in cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.addressDetails).thenReturn(mayBeCachedAddressViewModel)
      await(subscriptionBusinessService.address) shouldBe mayBeCachedAddressViewModel
      verify(mockCdsFrontendDataCache).subscriptionDetails
    }

    "return None if no data has been found in the cache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.addressDetails).thenReturn(None)
      await(subscriptionBusinessService.address) shouldBe None
    }
  }

  "Calling getCachedAddressViewModel" should {
    "retrieve any previously cached Address Details from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.addressDetails).thenReturn(mayBeCachedAddressViewModel)
      await(subscriptionBusinessService.addressOrException) shouldBe mayBeCachedAddressViewModel.get
    }

    "throw exception when cache address details is not saved in cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.addressDetails).thenReturn(None)
      val thrown = intercept[IllegalStateException] {
        await(subscriptionBusinessService.addressOrException)
      }
      thrown.getMessage shouldBe "No Address Details Cached"
    }
  }

  "Calling getCachedCustomsId" should {
    "retrieve any previously cached Named Id from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.customsId).thenReturn(customsIDUTR)
      await(subscriptionBusinessService.getCachedCustomsId) shouldBe customsIDUTR
    }

    "return None when cache Name Id is not saved in cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.customsId).thenReturn(None)
      await(subscriptionBusinessService.getCachedCustomsId) shouldBe None
    }
  }

  "Calling getCachedVatRegisteredEu" should {
    "retrieve any previously cached vat registered EU boolean from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.vatRegisteredEu).thenReturn(Some(true))
      await(subscriptionBusinessService.getCachedVatRegisteredEu) shouldBe true
    }

    "throw exception when there is no vat registered EU boolean in the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.vatRegisteredEu).thenReturn(None)
      val thrown = intercept[IllegalStateException] {
        await(subscriptionBusinessService.getCachedVatRegisteredEu)
      }
      thrown.getMessage shouldBe "Whether the business is VAT registered in the EU has not been Cached"
    }
  }

  "Calling getCachedVatRegisteredUk" should {
    "retrieve any previously cached vat registered UK boolean from the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.vatRegisteredUk).thenReturn(Some(true))
      await(subscriptionBusinessService.getCachedVatRegisteredUk) shouldBe true
    }

    "throw exception when there is no vat registered UK boolean in the cdsFrontendCache" in {
      when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(mockSubscriptionDetailsHolder)
      when(mockSubscriptionDetailsHolder.vatRegisteredUk).thenReturn(None)
      val thrown = intercept[IllegalStateException] {
        await(subscriptionBusinessService.getCachedVatRegisteredUk)
      }
      thrown.getMessage shouldBe "Whether the business is VAT registered in the UK has not been Cached"
    }
  }
}
