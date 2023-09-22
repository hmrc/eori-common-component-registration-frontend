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

package unit.domain

import base.UnitSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CaseClassAuditHelper
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{ContactResponse, IndividualResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.ResponseDetail
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.CustomsDataStoreRequest

import scala.collection.immutable.HashMap

class CaseClassAuditHelperSpec extends UnitSpec with CaseClassAuditHelper {

  "CustomsDataStoreRequest" should {

    "convert to map" in {

      val customsDataStoreRequest =
        CustomsDataStoreRequest(eori = "123456789000", address = "address", timestamp = "timestamp")

      val asMap = toMap(customsDataStoreRequest)
      asMap shouldBe Map("eori" -> "123456789000", "address" -> "address", "timestamp" -> "timestamp")
    }

    "convert optional to map" in {

      val responseDetail = ResponseDetail(
        "",
        Some("Hello world"),
        None,
        false,
        false,
        true,
        Some(mock[IndividualResponse]),
        None,
        mock[Address],
        mock[ContactResponse]
      )

      val asMap = toMap(responseDetail)
      asMap shouldBe HashMap(
        "isAnIndividual" -> "true",
        "ARN"            -> "Hello world",
        "isAnAgent"      -> "false",
        "isEditable"     -> "false",
        "SAFEID"         -> ""
      )
    }
  }
}
