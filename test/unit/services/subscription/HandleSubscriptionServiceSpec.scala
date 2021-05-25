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
import common.support.testdata.TestData
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.HandleSubscriptionConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.HandleSubscriptionRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.RecipientDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, SafeId, TaxPayerId}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.HandleSubscriptionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class HandleSubscriptionServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter {

  private val mockHandleSubscriptionConnector = mock[HandleSubscriptionConnector]

  private val service                    = new HandleSubscriptionService(mockHandleSubscriptionConnector)
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    reset(mockHandleSubscriptionConnector)
  }

  val formBundleId: String = "formBundleId"

  val recipientDetails: RecipientDetails =
    RecipientDetails(Journey.Register, atarService.code, "Advance Tariff Rulings", "", "", None, None)

  val sapNumber: TaxPayerId                        = TaxPayerId("id")
  val eori: Option[Eori]                           = Some(Eori("eori"))
  val emailVerificationTimestamp: Option[DateTime] = Some(TestData.emailVerificationTimestamp)
  val safeId: SafeId                               = SafeId("id")

  val handleSubscriptionRequest = HandleSubscriptionRequest(
    recipientDetails,
    formBundleId,
    sapNumber.id,
    Some("eori"),
    s"${emailVerificationTimestamp.get}",
    safeId.id
  )

  "HandleSubscriptionService" should {

    "call handle subscription connector with a valid handle subscription request" in {
      when(mockHandleSubscriptionConnector.call(any[HandleSubscriptionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful {})
      service.handleSubscription(formBundleId, recipientDetails, sapNumber, eori, emailVerificationTimestamp, safeId)
      verify(mockHandleSubscriptionConnector).call(meq(handleSubscriptionRequest))(meq(hc))
    }

    "generate new time stamp for email verification when not available" in {
      when(mockHandleSubscriptionConnector.call(any[HandleSubscriptionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful {})
      service.handleSubscription(formBundleId, recipientDetails, sapNumber, eori, None, safeId)
      verify(mockHandleSubscriptionConnector).call(any[HandleSubscriptionRequest])(any[HeaderCarrier])
    }
  }
}
