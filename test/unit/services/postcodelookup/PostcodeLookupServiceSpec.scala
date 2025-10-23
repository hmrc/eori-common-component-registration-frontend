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

package unit.services.postcodelookup

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector.AddressLookupException
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.AddressLookupSuccess
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.postcodelookup.PostcodeLookupService
import uk.gov.hmrc.http.HeaderCarrier
import util.TestData

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostcodeLookupServiceSpec extends AnyWordSpec with Matchers with TestData with OptionValues with ScalaFutures with BeforeAndAfterEach {

  val mockSessionCache: SessionCache = mock[SessionCache]
  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]

  val postcodeLookupService = new PostcodeLookupService(mockSessionCache, mockAddressLookupConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val postcodeViewModel: PostcodeViewModel = PostcodeViewModel("NW11 5RP", Some("Rose Avenue"))
  val fullAddress: Address = Address("Rose Avenue", Some("Chelsea"), Some("Kensington"), Some("London"), Some(postcodeViewModel.postcode), "GB")
  val addressPartial: Address = Address("", Some("Chelsea"), Some("Kensington"), Some("London"), Some(postcodeViewModel.postcode), "")
  val registrationDetails: RegistrationDetails =
    RegistrationDetailsIndividual(None, TaxPayerId(""), SafeId(""), "John Doe", addressPartial, LocalDate.parse("1989-12-21"))

  override protected def afterEach(): Unit = {
    reset(mockAddressLookupConnector)
  }

  "lookup" should {
    "return None" when {
      "session cache contains no postcode and line 1 details" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(None))
        verifyNoInteractions(mockAddressLookupConnector)
        postcodeLookupService.lookup().futureValue shouldEqual None
      }

      "address lookup service returns an error" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.failed(AddressLookupException))
        verifyNoMoreInteractions(mockAddressLookupConnector)
        postcodeLookupService.lookup().futureValue shouldEqual None
      }

      "address lookup returns no addresses both times" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupSuccess(Seq())))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupSuccess(Seq())))
        postcodeLookupService.lookup().futureValue shouldEqual None
      }
    }

    "return the addresses, the saved postcode & line 1 details" in {
      when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
      val addressLookupSuccess = AddressLookupSuccess(Seq(fullAddress))
      when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(addressLookupSuccess))
      postcodeLookupService.lookup().futureValue shouldEqual Some((addressLookupSuccess, postcodeViewModel))
      verify(mockAddressLookupConnector, times(1)).lookup(any(), any())(any())
    }

    "retry" when {
      "any one of the address fields are missing in the response from address lookup service" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupSuccess(Seq(addressPartial))))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupSuccess(Seq(fullAddress))))
        postcodeLookupService.lookup().futureValue shouldEqual Some((AddressLookupSuccess(Seq(fullAddress)), postcodeViewModel))
      }
    }
  }

  "lookupNoRepeat" should {
    "return None" when {
      "session cache contains no postcode and line 1 details" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(None))
        verifyNoInteractions(mockAddressLookupConnector)
        postcodeLookupService.lookupNoRepeat().futureValue shouldEqual None
      }

      "address lookup service returns an error" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.failed(AddressLookupException))
        verifyNoMoreInteractions(mockAddressLookupConnector)
        postcodeLookupService.lookupNoRepeat().futureValue shouldEqual None
      }

      "address lookup returns no addresses" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupSuccess(Seq())))
        postcodeLookupService.lookupNoRepeat().futureValue shouldEqual None
      }

      "address lookup service returns addresses with some missing fields" in {
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupSuccess(Seq(addressPartial))))
        postcodeLookupService.lookupNoRepeat().futureValue shouldEqual None
      }
    }

    "return the addresses, the saved postcode & line 1 details" in {
      when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(Some(postcodeViewModel)))
      val addressLookupSuccess = AddressLookupSuccess(Seq(fullAddress))
      when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(addressLookupSuccess))
      postcodeLookupService.lookupNoRepeat().futureValue shouldEqual Some((addressLookupSuccess, postcodeViewModel))
    }
  }

  "ensuringAddressPopulated" should {
    "return Address" when {
      "session cache contains whole address" in {
        val newRegistrationDetails = RegistrationDetailsIndividual().copy(address = fullAddress)
        when(mockSessionCache.registrationDetails(any())).thenReturn(Future.successful(newRegistrationDetails))
        when(mockSessionCache.saveRegistrationDetails(newRegistrationDetails)).thenReturn(Future.successful(true))
        postcodeLookupService.ensuringAddressPopulated(fullAddress).futureValue shouldEqual true
      }

      "line 1 details or country code is missing and save new address" in {
        when(mockSessionCache.registrationDetails).thenReturn(Future.successful(registrationDetails))
        val newAddressDetails = registrationDetails.address.copy(addressLine1 = "Rose Avenue", countryCode = "GB")
        val updatedRegDetails = registrationDetails.asInstanceOf[RegistrationDetailsIndividual].copy(address = newAddressDetails)
        when(mockSessionCache.saveRegistrationDetails(updatedRegDetails)).thenReturn(Future.successful(true))
        postcodeLookupService.ensuringAddressPopulated(fullAddress).futureValue shouldEqual true
      }
    }
  }

}
