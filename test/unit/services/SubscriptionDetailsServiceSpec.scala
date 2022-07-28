/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{ContactDetailsAdaptor, RegistrationDetailsCreator}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
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
      verify(mockSessionCache).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val holder: SubscriptionDetails = requestCaptor.getValue
      holder.contactDetails shouldBe Some(updatedContactDetailsModel)
    }

  }
}
