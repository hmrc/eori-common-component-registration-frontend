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
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService,
  SubscriptionVatEUDetailsService
}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubscriptionVatEUDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService  = mock[SubscriptionDetailsService]

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  private val mockSubscriptionDetails                   = mock[SubscriptionDetails]
  private val mockVatEuDetails                          = mock[VatEUDetailsModel]
  private val previouslyCachedSubscriptionDetailsHolder = SubscriptionDetails(vatEUDetails = Seq(mockVatEuDetails))

  private val VatEuDetailsForUpdate = Seq(VatEUDetailsModel("12345", "FR"), VatEUDetailsModel("54321", "DE"))

  private val exampleSubscriptionDetails = SubscriptionDetails(vatEUDetails = VatEuDetailsForUpdate)

  private val emulatedFailure = new RuntimeException("Something went wrong!")

  private val service =
    new SubscriptionVatEUDetailsService(mockSubscriptionBusinessService, mockSubscriptionDetailsService)(global)

  override protected def beforeEach() {
    reset(mockSubscriptionBusinessService, mockSubscriptionDetails, mockSubscriptionDetailsService)
    when(mockSubscriptionBusinessService.retrieveSubscriptionDetailsHolder(meq(hc)))
      .thenReturn(previouslyCachedSubscriptionDetailsHolder)
  }

  "VAT EU Details caching" should {
    "save VAT EU Details for the first time" in {

      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(hc))
      ).thenReturn(Future.successful[Unit](()))

      await(service.saveOrUpdate(mockVatEuDetails)) should be(())

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(hc))
      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(
        meq(hc)
      )
    }

    "update EU Details in previously cached SubscriptionDetailsHolder" in {
      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(hc))
      ).thenReturn(Future.successful[Unit](()))

      await(service.saveOrUpdate(mockVatEuDetails)) should be(())

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(hc))
      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(
        meq(hc)
      )
    }

    "fail when cache fails accessing current SubscriptionDetailsHolder" in {
      when(mockSubscriptionBusinessService.retrieveSubscriptionDetailsHolder(meq(hc)))
        .thenReturn(Future.failed(emulatedFailure))

      intercept[RuntimeException] {
        await(service.saveOrUpdate(mockVatEuDetails))
      } shouldBe emulatedFailure

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(hc))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }

    "update EU Details in previously cached SubscriptionDetailsHolder using sequence" in {
      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(hc))
      ).thenReturn(Future.successful[Unit](()))

      await(service.saveOrUpdate(Seq(mockVatEuDetails))) should be(())

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(hc))
      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(
        meq(hc)
      )
    }

    "fail when cache fails accessing current SubscriptionDetailsHolder using sequence" in {
      when(mockSubscriptionBusinessService.retrieveSubscriptionDetailsHolder(meq(hc)))
        .thenReturn(Future.failed(emulatedFailure))

      intercept[RuntimeException] {
        await(service.saveOrUpdate(Seq(mockVatEuDetails)))
      } shouldBe emulatedFailure

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(hc))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }
  }

  "VAT EU Details retrieve from cache" should {
    "give Nil when cached SubscriptionDetailsHolder does not hold VAT EU Details" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc))).thenReturn(Future.successful(Seq()))
      await(service.cachedEUVatDetails) shouldBe Nil
      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(hc))
    }

    "give Some previously cached VAT Identifications" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(Seq(mockVatEuDetails)))
      await(service.cachedEUVatDetails) shouldBe List(mockVatEuDetails)

      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(hc))
    }

    "fail when cache fails accessing current SubscriptionDetailsHolder" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.failed(emulatedFailure))

      intercept[RuntimeException] {
        await(service.cachedEUVatDetails)
      } shouldBe emulatedFailure

      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(hc))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }
  }

  "Updating single vat details" should {
    "return subscription details's vat eu details updated" in {
      val vatEuUpdate = VatEUDetailsModel("54321", "ES")

      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      await(service.updateVatEuDetailsModel(VatEuDetailsForUpdate.head, vatEuUpdate)) shouldBe Seq(
        VatEUDetailsModel("54321", "ES"),
        VatEUDetailsModel("54321", "DE")
      )
    }

    "fails when details for update not found in cache" in {
      val vatEuUpdate             = VatEUDetailsModel("54321", "ES")
      val nonExistingVatReference = VatEUDetailsModel("12345", "PL")

      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))

      intercept[IllegalArgumentException] {
        await(service.updateVatEuDetailsModel(nonExistingVatReference, vatEuUpdate))
      } getMessage () shouldBe "Details for update do not exist in a cache"

      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(hc))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }
  }

  "Querying for specific vatEuDetails" should {
    "return vatEuDetails when index was found" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      await(service.vatEuDetails(VatEuDetailsForUpdate.head.index)) shouldBe Some(VatEUDetailsModel("12345", "FR"))
    }

    "return None when index not found" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      await(service.vatEuDetails(VatEuDetailsForUpdate.size)) shouldBe None
    }
  }

  "Asking for cached Eu Vat Details" should {
    "should call subscription business service " in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(Seq(mockVatEuDetails)))
      await(service.vatEuDetails(VatEuDetailsForUpdate.size))
      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(hc))
    }
  }

  "Removing single vat eu details" should {
    "call SubscriptionDetailsService to save filtered vatEuDetails when value to be removed was found" in {
      def sub = (subDet: SubscriptionDetails) => subDet.copy(vatEUDetails = Seq(VatEUDetailsModel("54321", "DE")))

      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(hc)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(hc))
      ).thenReturn(Future.successful(()))

      await(service.removeSingleEuVatDetails(VatEUDetailsModel("12345", "FR")))

      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails => SubscriptionDetails])

      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(hc))
      val f = requestCaptor.getValue.asInstanceOf[SubscriptionDetails => SubscriptionDetails]

      f(exampleSubscriptionDetails) should equal(sub(exampleSubscriptionDetails))
    }
  }
}
