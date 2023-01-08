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

package unit.services

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{ContactDetailsAdaptor, RegistrationDetailsCreator}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.util.Random

class SubscriptionDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier     = mock[HeaderCarrier]
  implicit val request: Request[Any] = mock[Request[Any]]

  private val mockSessionCache               = mock[SessionCache]
  private val mockRegistrationDetailsCreator = mock[RegistrationDetailsCreator]
  private val registrationInfo               = mock[RegistrationInfo]
  private val mockRegistrationDetails        = mock[RegistrationDetails]
  private val mockSave4LaterConnector        = mock[Save4LaterConnector]

  private val mockContactDetailsAdaptor         = mock[ContactDetailsAdaptor]
  private val mockSubscriptionDetailsHolder     = mock[SubscriptionDetails]
  private val mockpersonalDataDisclosureConsent = mock[Option[Boolean]]

  private val utrMatch  = UtrMatchModel(Some(true), Some("utrxxxxx"))
  private val ninoMatch = NinoMatchModel(Some(true), Some("ninoxxxxx"))

  private val subscriptionDetailsHolderService =
    new SubscriptionDetailsService(mockSessionCache, mockContactDetailsAdaptor, mockSave4LaterConnector)(global)

  private val eoriNumericLength = 15
  private val eoriId            = "GB" + Random.nextString(eoriNumericLength)
  private val eori              = Eori(eoriId)

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

    when(mockSessionCache.saveRegistrationDetails(any[RegistrationDetails])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSessionCache.saveSub01Outcome(any[Sub01Outcome])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSessionCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    val existingHolder = SubscriptionDetails(contactDetails = Some(mock[ContactDetailsModel]))

    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(existingHolder)
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

  "Calling cachedCustomsId" should {
    "return Some of customsID when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(SubscriptionDetails(customsId = Option(Utr("12345"))))
      await(subscriptionDetailsHolderService.cachedCustomsId(request)) shouldBe Some(Utr("12345"))
    }

    "return None for customsId when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedCustomsId(request)) shouldBe None
    }
  }

  "Calling cachedUtrMatch" should {
    "return Some utrMatch when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(SubscriptionDetails(formData = FormData(utrMatch = Option(utrMatch))))
      await(subscriptionDetailsHolderService.cachedUtrMatch(request)) shouldBe Some(utrMatch)
    }

    "return None for utrMatch when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedUtrMatch(request)) shouldBe None
    }
  }

  "Calling cachedNinoMatch" should {
    "return Some ninoMatch when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(SubscriptionDetails(formData = FormData(ninoMatch = Option(ninoMatch))))
      await(subscriptionDetailsHolderService.cachedNinoMatch(request)) shouldBe Some(ninoMatch)
    }

    "return None for ninoMatch when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedNinoMatch(request)) shouldBe None
    }
  }

  "Calling cachedOrganisationType" should {
    "return Some company when found in subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(SubscriptionDetails(formData = FormData(organisationType = Option(CdsOrganisationType.Company))))
      await(subscriptionDetailsHolderService.cachedOrganisationType(request)) shouldBe Some(CdsOrganisationType.Company)
    }

    "return None for utrMatch when no value found for subscription Details" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(SubscriptionDetails())
      await(subscriptionDetailsHolderService.cachedOrganisationType(request)) shouldBe None
    }
  }

  "Operating on nameDetails" should {
    val nameOrganisationMatchModel = NameOrganisationMatchModel("testName")

    "save Name Details in frontend cache when cacheNameDetails is called" in {
      await(subscriptionDetailsHolderService.cacheNameDetails(nameOrganisationMatchModel))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.nameOrganisationDetails shouldBe Some(nameOrganisationMatchModel)
    }

    "retrieve Name Details from frontend cache when cachedNameDetails is called" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionDetails(nameOrganisationDetails = Some(nameOrganisationMatchModel))))
      await(subscriptionDetailsHolderService.cachedNameDetails(request)) shouldBe Some(nameOrganisationMatchModel)
    }
  }

  "Calling cached nameDobDetails" should {
    "return nameDobDetails from frontend cache" in {
      val nameDobDetails = NameDobMatchModel("fname", Some("mname"), "lname", LocalDate.of(2019, 1, 1))
      when(mockSessionCache.subscriptionDetails)
        .thenReturn(Future.successful(SubscriptionDetails(nameDobDetails = Some(nameDobDetails))))
      await(subscriptionDetailsHolderService.cachedNameDobDetails) shouldBe Some(nameDobDetails)
    }
  }

  "calling cacheContactAddressDetails" should {
    val addressViewModel = AddressViewModel("Address Line 1", "city", Some("postcode"), "GB")
    val contactDetailsModel = ContactDetailsModel(
      fullName = "John Doe",
      emailAddress = "john.doe@example.com",
      telephone = "234234",
      None,
      false,
      Some("streetName"),
      Some("cityName"),
      Some("SE281AA"),
      Some("GB")
    )
    val updatedContactDetailsModel = ContactDetailsModel(
      fullName = "John Doe",
      emailAddress = "john.doe@example.com",
      telephone = "234234",
      None,
      false,
      Some("Address Line 1"),
      Some("city"),
      Some("postcode"),
      Some("GB")
    )

    "save contact address details in frontend cache when cacheContactAddressDetails is called" in {
      await(subscriptionDetailsHolderService.cacheContactAddressDetails(addressViewModel, contactDetailsModel))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.contactDetails shouldBe Some(updatedContactDetailsModel)
    }

  }

  "cacheContactDetails" should {
    val contactDetails = ContactDetailsModel(
      fullName = "Name",
      emailAddress = "email@mail.com",
      telephone = "01234567",
      fax = None,
      street = None,
      city = None,
      postcode = None,
      countryCode = None
    )

    val registrationDetails = RegistrationDetailsIndividual(fullName = "Name", dateOfBirth = LocalDate.now())
    "save subscription details with contact details" in {
      when(mockSessionCache.registrationDetails) thenReturn Future.successful(registrationDetails)
      when(
        mockContactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(any(), any())
      ) thenReturn contactDetails
      await(subscriptionDetailsHolderService.cacheContactDetails(contactDetails))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.contactDetails shouldBe Some(contactDetails)
    }

    "save subscription details with contact details in review mode" in {
      when(mockSessionCache.registrationDetails) thenReturn Future.successful(registrationDetails)
      when(
        mockContactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(any(), any())
      ) thenReturn contactDetails
      await(subscriptionDetailsHolderService.cacheContactDetails(contactDetails, true))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.contactDetails shouldBe Some(contactDetails)
    }
  }

  "cacheSicCode" should {
    val sicCode = "1234"
    "save subscription details with sic code" in {
      await(subscriptionDetailsHolderService.cacheSicCode(sicCode))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.sicCode shouldBe Some(sicCode)
    }
  }

  "cacheDateEstablished" should {
    val dateEstablished = LocalDate.now()
    "save subscription details with date established" in {
      await(subscriptionDetailsHolderService.cacheDateEstablished(dateEstablished))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.dateEstablished shouldBe Some(dateEstablished)
    }
  }

  "cacheNameDobDetails" should {
    val nameDobDetails =
      NameDobMatchModel(firstName = "Name", middleName = None, lastName = "Lastname", dateOfBirth = LocalDate.now())
    "save subscription details with date established" in {
      await(subscriptionDetailsHolderService.cacheNameDobDetails(nameDobDetails))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.nameDobDetails shouldBe Some(nameDobDetails)
    }
  }

  "cacheNinoOrUtrChoice" should {
    val ninoOrUtrChoice = NinoOrUtrChoice(ninoOrUtrRadio = Some("utr"))
    "save subscription details with nino or utr choice" in {
      await(subscriptionDetailsHolderService.cacheNinoOrUtrChoice(ninoOrUtrChoice))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.formData.ninoOrUtrChoice shouldBe ninoOrUtrChoice.ninoOrUtrRadio
    }
  }

  "cacheUtrMatch" should {
    val utrMatch = UtrMatchModel(Some(true))
    "save subscription details with utr match" in {
      await(subscriptionDetailsHolderService.cacheUtrMatch(Some(utrMatch)))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.formData.utrMatch shouldBe Some(utrMatch)
    }
  }

  "cacheNinoMatch" should {
    val ninoMatch = NinoMatchModel(Some(true))
    "save subscription details with nino match" in {
      await(subscriptionDetailsHolderService.cacheNinoMatch(Some(ninoMatch)))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.formData.ninoMatch shouldBe Some(ninoMatch)
    }
  }

  "cacheUkVatDetails" should {
    val ukVatDetails = VatDetails(postcode = "12345", number = "12345", effectiveDate = LocalDate.now())
    "save subscription details with vat details" in {
      await(subscriptionDetailsHolderService.cacheUkVatDetails(ukVatDetails))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.ukVatDetails shouldBe Some(ukVatDetails)
    }
  }

  "clearCachedUkVatDetails" should {
    val ukVatDetails = VatDetails(postcode = "12345", number = "12345", effectiveDate = LocalDate.now())

    val subscriptionDetails = SubscriptionDetails(ukVatDetails = Some(ukVatDetails))
    "save subscription details with vat details set to none" in {
      when(mockSessionCache.subscriptionDetails) thenReturn Future.successful(subscriptionDetails)
      await(subscriptionDetailsHolderService.clearCachedUkVatDetails(request))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.ukVatDetails shouldBe None
    }
  }

  "cacheVatRegisteredUk" should {
    val yesNoAnswer = YesNo(true)
    "save subscription details with vat registered uk" in {
      await(subscriptionDetailsHolderService.cacheVatRegisteredUk(yesNoAnswer))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.vatRegisteredUk shouldBe Some(yesNoAnswer.isYes)
    }
  }

  "cacheConsentToDisclosePersonalDetails" should {
    val yesNoAnswer = YesNo(true)
    "save subscription details with consent to disclose personal details" in {
      await(subscriptionDetailsHolderService.cacheConsentToDisclosePersonalDetails(yesNoAnswer))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.personalDataDisclosureConsent shouldBe Some(yesNoAnswer.isYes)
    }
  }

  "updateSubscriptionDetails" should {
    val subscriptionDetails = SubscriptionDetails()
    "save subscription details with details updated from cache" in {
      when(mockSessionCache.subscriptionDetails) thenReturn Future.successful(subscriptionDetails)
      when(mockSessionCache.saveRegistrationDetails(any())(any())) thenReturn Future.successful(true)
      when(mockSessionCache.saveSub01Outcome(any())(any())) thenReturn Future.successful(true)
      when(mockSessionCache.saveSubscriptionDetails(any())(any())) thenReturn Future.successful(true)
      await(subscriptionDetailsHolderService.updateSubscriptionDetails(request))
      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails])
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.nameDobDetails shouldBe subscriptionDetails.nameDobDetails
      holder.nameOrganisationDetails shouldBe subscriptionDetails.nameOrganisationDetails
      holder.formData shouldBe subscriptionDetails.formData
    }
  }
}
