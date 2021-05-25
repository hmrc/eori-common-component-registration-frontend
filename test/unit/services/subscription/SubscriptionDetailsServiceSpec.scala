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

package unit.services.subscription

import base.UnitSpec
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{NameOrganisationMatchModel, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{AddressViewModel, CompanyRegisteredCountry}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{ContactDetailsAdaptor, RegistrationDetailsCreator}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import util.builders.RegistrationDetailsBuilder._
import util.builders.SubscriptionInfoBuilder._
import util.builders.{RegistrationDetailsBuilder, SubscriptionContactDetailsFormBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.util.Random

class SubscriptionDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  private val mockSessionCache               = mock[SessionCache]
  private val mockRegistrationDetailsCreator = mock[RegistrationDetailsCreator]
  private val registrationInfo               = mock[RegistrationInfo]
  private val mockRegistrationDetails        = mock[RegistrationDetails]
  private val mockSave4LaterConnector        = mock[Save4LaterConnector]

  private val mockContactDetailsAdaptor         = mock[ContactDetailsAdaptor]
  private val mockSubscriptionDetailsHolder     = mock[SubscriptionDetails]
  private val mockpersonalDataDisclosureConsent = mock[Option[Boolean]]

  private val expectedDate = LocalDate.now()
  private val sicCode      = "someSicCode"

  private val addressDetails =
    AddressViewModel(street = "street", city = "city", postcode = Some("postcode"), countryCode = "GB")

  private val nameId       = NameIdOrganisationMatchModel(name = "orgname", id = "ID")
  private val customsIdUTR = Utr("utrxxxxx")
  private val utrMatch     = UtrMatchModel(Some(true), Some("utrxxxxx"))
  private val ninoMatch    = NinoMatchModel(Some(true), Some("ninoxxxxx"))

  private val subscriptionDetailsHolderService =
    new SubscriptionDetailsService(mockSessionCache, mockContactDetailsAdaptor, mockSave4LaterConnector)(global)

  private val eoriNumericLength = 15
  private val eoriId            = "GB" + Random.nextString(eoriNumericLength)
  private val eori              = Eori(eoriId)

  private val contactDetailsViewModelWhenNotUsingRegisteredAddress =
    SubscriptionContactDetailsFormBuilder.createContactDetailsViewModelWhenNotUsingRegAddress

  private val contactDetailsViewModelWhenUsingRegisteredAddress =
    SubscriptionContactDetailsFormBuilder.createContactDetailsViewModelWhenUseRegAddress

  override def beforeEach {
    reset(
      mockSessionCache,
      mockRegistrationDetailsCreator,
      registrationInfo,
      mockRegistrationDetails,
      mockSubscriptionDetailsHolder,
      mockContactDetailsAdaptor,
      mockSave4LaterConnector
    )

    when(mockSessionCache.saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSessionCache.saveSub01Outcome(any[Sub01Outcome])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSessionCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    val existingHolder = SubscriptionDetails(contactDetails = Some(mock[ContactDetailsModel]))

    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(existingHolder)
    when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(mockpersonalDataDisclosureConsent)

    when(mockRegistrationDetailsCreator.registrationDetails(Some(eori))(registrationInfo))
      .thenReturn(mockRegistrationDetails)

  }

  "Calling saveKeyIdentifiers" should {
    "save saveKeyIdentifiers in mongo" in {
      val groupId    = GroupId("groupId")
      val internalId = InternalId("internalId")
      val safeId     = SafeId("safeId")
      val key        = "cachedGroupId"
      val cacheIds   = CacheIds(internalId, safeId, Some("atar"))
      when(mockSessionCache.safeId).thenReturn(Future.successful(SafeId("safeId")))
      when(
        mockSave4LaterConnector.put[CacheIds](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(key),
          ArgumentMatchers.eq(cacheIds)
        )(any())
      ).thenReturn(Future.successful(()))
      val expected = await(subscriptionDetailsHolderService.saveKeyIdentifiers(groupId, internalId, atarService))
      expected shouldBe ((): Unit)
    }
  }

  "Calling cacheAddressDetails" should {
    "save Address Details in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheAddressDetails(addressDetails))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.addressDetails shouldBe Some(addressDetails)

    }

    "should not save emptry strings in postcode field" in {

      await(subscriptionDetailsHolderService.cacheAddressDetails(addressDetails.copy(postcode = Some(""))))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.addressDetails shouldBe Some(addressDetails.copy(postcode = None))
    }
  }

  "Calling cachedAddressDetails" should {
    "return Some of AddressViewModel for addressDetails when found in subscription Details" in {
      val mockAddressViewModel = mock[AddressViewModel]
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(SubscriptionDetails(addressDetails = Option(mockAddressViewModel)))
      await(subscriptionDetailsHolderService.cachedAddressDetails(hc)) shouldBe Some(mockAddressViewModel)
    }

    "return None when no value found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedAddressDetails(hc)) shouldBe None
    }
  }

  "Calling cacheNameIdDetails" should {
    "save Name Id Details in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheNameIdDetails(nameId))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.nameIdOrganisationDetails shouldBe Some(nameId)

    }
  }

  "Calling cacheCustomsIdAndNinoMatch" should {
    "save CustomsId an NinoMatchModel in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheNinoMatchForNoAnswer(Some(ninoMatch)))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.customsId shouldBe None
      holder.formData.ninoMatch shouldBe Some(ninoMatch)
    }
  }

  "Calling cacheUtrMatchForNoAnswer" should {
    "save utrMatch in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheUtrMatchForNoAnswer(Some(utrMatch)))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.formData.utrMatch shouldBe Some(utrMatch)
    }
  }

  "Calling cache SIC Code" should {
    "save SIC Code in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheSicCode(sicCode))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.sicCode shouldBe Some(sicCode)
    }
  }

  "Calling cache EORI number" should {
    "save EORI number in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheEoriNumber(eoriId))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.eoriNumber shouldBe Some(eoriId)

    }
  }

  "Calling cacheDateEstablished" should {
    "save Date Of Establishment Details in frontend cache" in {

      await(subscriptionDetailsHolderService.cacheDateEstablished(expectedDate))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.dateEstablished shouldBe Some(expectedDate)

    }
  }

  "Calling cachePersonalDataDisclosureConsent" should {
    "save consent details in frontend cache" in {

      await(subscriptionDetailsHolderService.cachePersonalDataDisclosureConsent(consent = true))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.personalDataDisclosureConsent shouldBe Some(true)
    }
  }

  "Calling createContactDetails" should {
    "create and cache SubscriptionDetailsHolder with contact details when not using registered address" in {

      await(subscriptionDetailsHolderService.cacheContactDetails(contactDetailsViewModelWhenNotUsingRegisteredAddress))

      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.contactDetails shouldBe Some(contactDetailsViewModelWhenNotUsingRegisteredAddress)
    }

    "Update SubscriptionDetailsHolder with contact details when using registered address" in {
      when(mockSessionCache.registrationDetails)
        .thenReturn(RegistrationDetailsBuilder.withBusinessAddress(defaultAddress))

      when(
        mockContactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(
          contactDetailsViewModelWhenUsingRegisteredAddress,
          defaultAddress
        )
      ).thenReturn(contactDetailsViewModelWhenUsingRegisteredAddress)

      when(mockSessionCache.subscriptionDetails).thenReturn(SubscriptionDetails())
      when(mockSessionCache.subscriptionDetails).thenReturn(
        SubscriptionDetails(sicCode = Some(principalEconomicActivity), dateEstablished = Some(dateOfEstablishment))
      )

      await(subscriptionDetailsHolderService.cacheContactDetails(contactDetailsViewModelWhenUsingRegisteredAddress))

      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.contactDetails shouldBe Some(contactDetailsViewModelWhenUsingRegisteredAddress)
      holder.sicCode shouldBe Some(principalEconomicActivity)
      holder.dateEstablished shouldBe Some(dateOfEstablishment)
    }
  }

  "Calling cache IdDetails" should {
    "save CustomsId in frontend cache" in {
      await(subscriptionDetailsHolderService.cacheIdDetails(IdMatchModel("id")))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.idDetails shouldBe Some(IdMatchModel("id"))
    }
  }

  "Calling cache CustomsId" should {
    "save CustomsId in frontend cache" in {
      await(subscriptionDetailsHolderService.cacheCustomsId(customsIdUTR))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.customsId shouldBe Some(customsIdUTR)
    }
  }

  "Clearing customsId from cache" should {
    "set CustomsId in frontend cache to None" in {
      await(subscriptionDetailsHolderService.clearCachedCustomsId)

      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.customsId shouldBe None
    }
  }

  "Calling cachedCustomsId" should {
    "return Some of customsID when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(SubscriptionDetails(customsId = Option(Utr("12345"))))
      await(subscriptionDetailsHolderService.cachedCustomsId(hc)) shouldBe Some(Utr("12345"))
    }

    "return None for customsId when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedCustomsId(hc)) shouldBe None
    }
  }

  "Calling cachedUtrMatch" should {
    "return Some utrMatch when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(SubscriptionDetails(formData = FormData(utrMatch = Option(utrMatch))))
      await(subscriptionDetailsHolderService.cachedUtrMatch(hc)) shouldBe Some(utrMatch)
    }

    "return None for utrMatch when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedUtrMatch(hc)) shouldBe None
    }
  }

  "Calling cachedNinoMatch" should {
    "return Some ninoMatch when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(SubscriptionDetails(formData = FormData(ninoMatch = Option(ninoMatch))))
      await(subscriptionDetailsHolderService.cachedNinoMatch(hc)) shouldBe Some(ninoMatch)
    }

    "return None for ninoMatch when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedNinoMatch(hc)) shouldBe None
    }
  }

  "Calling cachedOrganisationType" should {
    "return Some company when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(SubscriptionDetails(formData = FormData(organisationType = Option(CdsOrganisationType.Company))))
      await(subscriptionDetailsHolderService.cachedOrganisationType(hc)) shouldBe Some(CdsOrganisationType.Company)
    }

    "return None for utrMatch when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedOrganisationType(hc)) shouldBe None
    }
  }

  "Operating on nameDetails" should {
    val nameOrganisationMatchModel = NameOrganisationMatchModel("testName")

    "save Name Details in frontend cache when cacheNameDetails is called" in {
      await(subscriptionDetailsHolderService.cacheNameDetails(nameOrganisationMatchModel))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.nameOrganisationDetails shouldBe Some(nameOrganisationMatchModel)
    }

    "retrieve Name Details from frontend cache when cachedNameDetails is called" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(SubscriptionDetails(nameOrganisationDetails = Some(nameOrganisationMatchModel))))
      await(subscriptionDetailsHolderService.cachedNameDetails(hc)) shouldBe Some(nameOrganisationMatchModel)
    }
  }

  "Clearing ukVatDetails from cache" should {
    "set ukVatDetails in frontend cache to None" in {
      await(subscriptionDetailsHolderService.clearCachedUkVatDetails)

      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.ukVatDetails shouldBe None
    }
  }

  "Calling cache nameDobDetails" should {
    "save nameDobDetails in frontend cache" in {
      await(
        subscriptionDetailsHolderService
          .cacheNameDobDetails(NameDobMatchModel("fname", Some("mname"), "lname", new LocalDate(2019, 1, 1)))
      )
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.nameDobDetails shouldBe Some(NameDobMatchModel("fname", Some("mname"), "lname", new LocalDate(2019, 1, 1)))
    }
  }

  "Calling cached nameDobDetails" should {
    "return nameDobDetails from frontend cache" in {
      val nameDobDetails = NameDobMatchModel("fname", Some("mname"), "lname", new LocalDate(2019, 1, 1))
      when(mockSessionCache.subscriptionDetails)
        .thenReturn(Future.successful(SubscriptionDetails(nameDobDetails = Some(nameDobDetails))))
      await(subscriptionDetailsHolderService.cachedNameDobDetails) shouldBe Some(nameDobDetails)
    }
  }

  "Calling updateSubscriptionDetails" should {
    "update cache with subscription details having name and dob details only" in {
      val businessShortName = BusinessShortName("shortName")
      val nameDobDetails    = NameDobMatchModel("fname", Some("mname"), "lname", new LocalDate(2019, 1, 1))
      when(mockSessionCache.subscriptionDetails).thenReturn(
        Future.successful(
          SubscriptionDetails(nameDobDetails = Some(nameDobDetails), businessShortName = Some(businessShortName))
        )
      )
      when(mockSessionCache.remove).thenReturn(Future.successful(true))
      await(subscriptionDetailsHolderService.updateSubscriptionDetails) shouldBe true
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.nameDobDetails shouldBe Some(nameDobDetails)
      holder shouldBe SubscriptionDetails(nameDobDetails = Some(nameDobDetails))
    }

    "update cache with subscription details having organisation name details only" in {
      val nameOrganisationDetails = NameOrganisationMatchModel("fname")
      val businessShortName       = BusinessShortName("shortName")
      when(mockSessionCache.subscriptionDetails).thenReturn(
        Future.successful(
          SubscriptionDetails(
            nameOrganisationDetails = Some(nameOrganisationDetails),
            businessShortName = Some(businessShortName)
          )
        )
      )
      when(mockSessionCache.remove).thenReturn(Future.successful(true))
      await(subscriptionDetailsHolderService.updateSubscriptionDetails) shouldBe true
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.nameOrganisationDetails shouldBe Some(nameOrganisationDetails)
      holder shouldBe SubscriptionDetails(nameOrganisationDetails = Some(nameOrganisationDetails))
    }
  }

  "Calling cacheVatRegisteredUk" should {
    "save VatRegisteredUk value in frontend cache" in {
      await(subscriptionDetailsHolderService.cacheVatRegisteredUk(YesNo(true)))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.vatRegisteredUk shouldBe Some(true)
    }
  }

  "Calling cacheConsentToDisclosePersonalDetails" should {
    "save personalDataDisclosureConsent value in frontend cache" in {
      await(subscriptionDetailsHolderService.cacheConsentToDisclosePersonalDetails(YesNo(true)))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder = requestCaptor.getValue
      holder.personalDataDisclosureConsent shouldBe Some(true)
    }
  }

  "Calliing cacheRegisteredCountry" should {
    "save country value in frontend cache" in {
      await(subscriptionDetailsHolderService.cacheRegisteredCountry(CompanyRegisteredCountry("United Kingdom")))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val details = requestCaptor.getValue
      details.registeredCompany shouldBe Some(CompanyRegisteredCountry("United Kingdom"))
    }
  }
}
