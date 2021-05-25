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
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationDetailsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RegistrationDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  implicit val hc = mock[HeaderCarrier]

  private def startingDate = LocalDate.now

  private val mockSessionCache = mock[SessionCache]

  private val startingOrgName      = "BEFORE Blank"
  private val updatedOrgName       = "AFTER Test Org Name"
  private val startingSafeId       = SafeId("SAFEID")
  private val startingBlankAddress = Address("BLANK", None, None, None, None, "BLANK")
  private val updatedAddress       = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("SE28 1AA"), "GB")

  private val startingBlankFullName = "BEFORE Blank full name"
  private val updatedFullName       = "Full name UPDATED"
  private val updatedDateOfBirth    = LocalDate.parse("1976-04-08")

  private val emptyRegDetailsIndividual = RegistrationDetailsIndividual(
    None,
    TaxPayerId(""),
    SafeId(""),
    "",
    Address("", None, None, None, None, ""),
    startingDate
  )

  private val emptyRegDetailsOrganisation = RegistrationDetailsOrganisation(
    None,
    TaxPayerId(""),
    SafeId(""),
    "",
    Address("", None, None, None, None, ""),
    None,
    None
  )

  private val startingRegDetailsOrganisation = RegistrationDetailsOrganisation(
    None,
    TaxPayerId(""),
    startingSafeId,
    startingOrgName,
    startingBlankAddress,
    Some(startingDate),
    None
  )

  private val startingRegDetailsIndividual = RegistrationDetailsIndividual(
    None,
    TaxPayerId(""),
    startingSafeId,
    startingBlankFullName,
    startingBlankAddress,
    startingDate
  )

  private val updatedRegDetailsIndividual = RegistrationDetailsIndividual(updatedFullName, updatedDateOfBirth)

  private val registrationDetailsService = new RegistrationDetailsService(mockSessionCache)(global)

  val individualOrganisationTypes =
    Table("organisationType", ThirdCountryIndividual, ThirdCountrySoleTrader, Individual, SoleTrader)

  val nonIndividualOrganisationTypes = Table(
    "organisationType",
    Company,
    ThirdCountryOrganisation,
    CdsOrganisationType.Partnership,
    LimitedLiabilityPartnership,
    CharityPublicBodyNotForProfit
  )

  override def beforeEach {
    reset(mockSessionCache)
    when(mockSessionCache.saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))
    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(Future.successful(SubscriptionDetails()))
  }

  "Calling cacheOrgName" should {
    "save Org Name in the cache" in {
      when(mockSessionCache.registrationDetails).thenReturn(startingRegDetailsOrganisation)

      await(registrationDetailsService.cacheOrgName(updatedOrgName))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetails])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(hc))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))

      val holder: RegistrationDetails = requestCaptor.getValue
      holder.name shouldBe updatedOrgName
      holder.address shouldBe startingBlankAddress
      holder.safeId shouldBe startingSafeId
    }

    "save Address in the cache for Organisation" in {
      when(mockSessionCache.registrationDetails).thenReturn(startingRegDetailsOrganisation)

      await(registrationDetailsService.cacheAddress(updatedAddress))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetailsOrganisation])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(hc))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))

      val holder: RegistrationDetailsOrganisation = requestCaptor.getValue
      holder.address shouldBe updatedAddress
      holder.safeId shouldBe startingSafeId
      holder.name shouldBe startingOrgName
      holder.dateOfEstablishment shouldBe Some(startingDate)
    }

    "save Address in the cache for Individual" in {
      when(mockSessionCache.registrationDetails).thenReturn(startingRegDetailsIndividual)

      await(registrationDetailsService.cacheAddress(updatedAddress))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetailsIndividual])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(hc))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))

      val holder: RegistrationDetailsIndividual = requestCaptor.getValue
      holder.address shouldBe updatedAddress
      holder.safeId shouldBe startingSafeId
      holder.name shouldBe startingBlankFullName
      holder.dateOfBirth shouldBe startingDate
    }

    "save Name and Date of Birth in the cache" in {
      when(mockSessionCache.registrationDetails).thenReturn(startingRegDetailsIndividual)

      await(registrationDetailsService.cacheNameDateOfBirth(updatedRegDetailsIndividual))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetailsIndividual])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(hc))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))

      val holder: RegistrationDetailsIndividual = requestCaptor.getValue
      holder.address shouldBe startingBlankAddress
      holder.safeId shouldBe startingSafeId
      holder.name shouldBe updatedFullName
      holder.dateOfBirth shouldBe updatedDateOfBirth
    }
  }

  "Calling initialiseCacheWithRegistrationDetails" should {

    forAll(individualOrganisationTypes) { organisationType =>
      s"initialise session cache with RegistrationDetailsIndividual for organisation type $organisationType" in {

        await(registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType))

        val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetails])

        verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
        val holder: RegistrationDetails = requestCaptor.getValue

        holder shouldBe emptyRegDetailsIndividual
      }
    }

    forAll(nonIndividualOrganisationTypes) { organisationType =>
      s"initialise session cache with RegistrationDetailsOrganisation for remaining organisation types as $organisationType" in {

        await(registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType))

        val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetails])

        verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
        val holder: RegistrationDetails = requestCaptor.getValue

        holder shouldBe emptyRegDetailsOrganisation
      }
    }
  }
}
