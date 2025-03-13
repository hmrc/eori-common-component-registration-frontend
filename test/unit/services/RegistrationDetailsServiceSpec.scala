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

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor1
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegistrationDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RegistrationDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  private def startingDate = LocalDate.now

  private val mockSessionCache = mock[SessionCache]

  private val startingOrgName = "BEFORE Blank"
  private val startingSafeId = SafeId("SAFEID")
  private val startingBlankAddress = Address("BLANK", None, None, None, None, "BLANK")
  private val updatedAddress = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("SE28 1AA"), "GB")

  private val startingBlankFullName = "BEFORE Blank full name"

  private val emptySubDetailsIndividual =
    SubscriptionDetails(formData = FormData(organisationType = Some(CdsOrganisationType("third-country-individual"))))

  private val emptySubDetailsOrganisation =
    SubscriptionDetails(formData = FormData(organisationType = Some(CdsOrganisationType("third-country-organisation"))))

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

  private val startingRegDetailsEmbassy =
    RegistrationDetailsEmbassy("U.S. Embassy", startingBlankAddress, None, startingSafeId)

  private val startingSubDetailsOrganisation =
    SubscriptionDetails(
      nameDetails = Some(NameMatchModel("Jimbob")),
      formData = FormData(organisationType = Some(CdsOrganisationType("third-country-individual")))
    )

  private val startingSubDetailsIndividual =
    SubscriptionDetails(
      nameOrganisationDetails = Some(NameOrganisationMatchModel("pre-populated orgName")),
      formData = FormData(organisationType = Some(CdsOrganisationType("third-country-organisation")))
    )

  private val registrationDetailsService = new RegistrationDetailsService(mockSessionCache)(global)

  val individualOrganisationTypes: TableFor1[CdsOrganisationType] =
    Table("organisationType", ThirdCountryIndividual, ThirdCountrySoleTrader, Individual, SoleTrader)

  val nonIndividualOrganisationTypes: TableFor1[CdsOrganisationType] = Table(
    "organisationType",
    Company,
    ThirdCountryOrganisation,
    CdsOrganisationType.Partnership,
    LimitedLiabilityPartnership
  )

  implicit val request: Request[Any] = mock[Request[Any]]

  override def beforeEach(): Unit = {
    reset(mockSessionCache)
    when(mockSessionCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])).thenReturn(
      Future.successful(true)
    )
    when(mockSessionCache.saveRegistrationDetails(any[RegistrationDetails])(any[Request[_]]))
      .thenReturn(Future.successful(true))
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(SubscriptionDetails()))
  }

  "Calling cacheOrgName" should {

    "save Address in the cache for Organisation" in {
      when(mockSessionCache.registrationDetails).thenReturn(Future.successful(startingRegDetailsOrganisation))

      await(registrationDetailsService.cacheAddress(updatedAddress))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetailsOrganisation])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(request))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))

      val holder: RegistrationDetailsOrganisation = requestCaptor.getValue
      holder.address shouldBe updatedAddress
      holder.safeId shouldBe startingSafeId
      holder.name shouldBe startingOrgName
      holder.dateOfEstablishment shouldBe Some(startingDate)
    }

    "save Address in the cache for Individual" in {
      when(mockSessionCache.registrationDetails).thenReturn(Future.successful(startingRegDetailsIndividual))

      await(registrationDetailsService.cacheAddress(updatedAddress))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetailsIndividual])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(request))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))

      val holder: RegistrationDetailsIndividual = requestCaptor.getValue
      holder.address shouldBe updatedAddress
      holder.safeId shouldBe startingSafeId
      holder.name shouldBe startingBlankFullName
      holder.dateOfBirth shouldBe startingDate
    }

    "save Address in the cache for Embassy" in {
      when(mockSessionCache.registrationDetails).thenReturn(Future.successful(startingRegDetailsEmbassy))

      await(registrationDetailsService.cacheAddress(updatedAddress))
      val requestCaptor = ArgumentCaptor.forClass(classOf[RegistrationDetailsEmbassy])

      verify(mockSessionCache).registrationDetails(ArgumentMatchers.eq(request))
      verify(mockSessionCache).saveRegistrationDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))

      val holder: RegistrationDetailsEmbassy = requestCaptor.getValue
      holder.address shouldBe updatedAddress
      holder.safeId shouldBe startingSafeId
      holder.name shouldBe "U.S. Embassy"
      holder.orgType shouldBe Some(EmbassyId)
    }
  }

  "Calling initialiseCacheWithRegistrationDetails" should {

    forAll(individualOrganisationTypes) { organisationType =>
      s"initialise session cache with RegistrationDetailsIndividual for organisation type $organisationType" in {

        await(registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType))

        val requestCaptorReg = ArgumentCaptor.forClass(classOf[RegistrationDetails])

        verify(mockSessionCache).saveRegistrationDetails(requestCaptorReg.capture())(ArgumentMatchers.eq(request))

        val actualRegistrationDetails: RegistrationDetails = requestCaptorReg.getValue

        actualRegistrationDetails shouldBe emptyRegDetailsIndividual
      }
    }

    forAll(nonIndividualOrganisationTypes) { organisationType =>
      s"initialise session cache with RegistrationDetailsOrganisation for remaining organisation types as $organisationType" in {

        await(registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType))

        val requestCaptorReg = ArgumentCaptor.forClass(classOf[RegistrationDetails])

        verify(mockSessionCache).saveRegistrationDetails(requestCaptorReg.capture())(ArgumentMatchers.eq(request))

        val actualRegistrationDetails: RegistrationDetails = requestCaptorReg.getValue

        actualRegistrationDetails shouldBe emptyRegDetailsOrganisation
      }
    }

    s"initialise session cache with RegistrationDetailsOrganisation for remaining $CharityPublicBodyNotForProfit" in {

      await(
        registrationDetailsService.initialiseCacheWithRegistrationDetails(
          CdsOrganisationType.CharityPublicBodyNotForProfit
        )
      )

      val requestCaptorReg = ArgumentCaptor.forClass(classOf[RegistrationDetails])

      verify(mockSessionCache).saveRegistrationDetails(requestCaptorReg.capture())(ArgumentMatchers.eq(request))

      val actualRegistrationDetails: RegistrationDetails = requestCaptorReg.getValue

      actualRegistrationDetails shouldBe RegistrationDetailsOrganisation.charityPublicBodyNotForProfit
    }
  }

  "initialise session cache with emptySubDetailsIndividual for organisation type third-country-individual" in {

    when(mockSessionCache.subscriptionDetails).thenReturn(Future.successful(startingSubDetailsOrganisation))

    await(registrationDetailsService.initialiseCacheWithRegistrationDetails(CdsOrganisationType.ThirdCountryIndividual))

    val requestCaptorReg = ArgumentCaptor.forClass(classOf[RegistrationDetails])
    val requestCaptorSub = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

    verify(mockSessionCache).saveRegistrationDetails(requestCaptorReg.capture())(ArgumentMatchers.eq(request))
    val actualRegistrationDetails: RegistrationDetails = requestCaptorReg.getValue

    verify(mockSessionCache).saveSubscriptionDetails(requestCaptorSub.capture())(ArgumentMatchers.eq(request))
    val actualSubscriptionDetails: SubscriptionDetails = requestCaptorSub.getValue

    actualRegistrationDetails shouldBe emptyRegDetailsIndividual
    actualSubscriptionDetails shouldBe emptySubDetailsIndividual
    await(mockSessionCache.subscriptionDetails).name shouldBe startingSubDetailsOrganisation.name
  }

  "initialise session cache with RegistrationDetailsOrganisation for remaining organisation types as third-country-organisation" in {

    when(mockSessionCache.subscriptionDetails).thenReturn(Future.successful(startingSubDetailsIndividual))

    await(
      registrationDetailsService.initialiseCacheWithRegistrationDetails(CdsOrganisationType.ThirdCountryOrganisation)
    )

    val requestCaptorReg = ArgumentCaptor.forClass(classOf[RegistrationDetails])
    val requestCaptorSub = ArgumentCaptor.forClass(classOf[SubscriptionDetails])

    verify(mockSessionCache).saveRegistrationDetails(requestCaptorReg.capture())(ArgumentMatchers.eq(request))
    val actualRegistrationDetails: RegistrationDetails = requestCaptorReg.getValue

    verify(mockSessionCache).saveSubscriptionDetails(requestCaptorSub.capture())(ArgumentMatchers.eq(request))
    val actualSubscriptionDetails: SubscriptionDetails = requestCaptorSub.getValue

    actualRegistrationDetails shouldBe emptyRegDetailsOrganisation
    actualSubscriptionDetails shouldBe emptySubDetailsOrganisation
    await(mockSessionCache.subscriptionDetails).name shouldBe startingSubDetailsIndividual.name
  }

  "initialise session cache with RegistrationDetailsEmbassy for embassy type" in {
    await(registrationDetailsService.initialiseCacheWithRegistrationDetails(Embassy))

    val requestCaptorReg = ArgumentCaptor.forClass(classOf[RegistrationDetails])

    verify(mockSessionCache).saveRegistrationDetails(requestCaptorReg.capture())(ArgumentMatchers.eq(request))

    val actualRegistrationDetails: RegistrationDetails = requestCaptorReg.getValue

    actualRegistrationDetails shouldBe RegistrationDetailsEmbassy.initEmpty()
  }
}
