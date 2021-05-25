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

package unit.domain.messaging

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RegistrationInfoRequest

class RegistrationInfoRequestSpec extends UnitSpec {

  val id = java.util.UUID.randomUUID.toString

  "RegistrationInfoRequest.forMatchingId" should {
    "create proper request from UTR" in {
      val r = RegistrationInfoRequest.forCustomsId(Utr(id))
      r.regime shouldBe "CDS"
      r.idType shouldBe "UTR"
      r.idValue shouldBe id
    }

    "create proper request from EORI" in {
      val r = RegistrationInfoRequest.forCustomsId(Eori(id))
      r.regime shouldBe "CDS"
      r.idType shouldBe "EORI"
      r.idValue shouldBe id
    }

    "create proper request from NINO" in {
      val r = RegistrationInfoRequest.forCustomsId(Nino(id))
      r.regime shouldBe "CDS"
      r.idType shouldBe "NINO"
      r.idValue shouldBe id
    }

    "create proper request from SAFEID" in {
      val r = RegistrationInfoRequest.forCustomsId(SafeId(id))
      r.regime shouldBe "CDS"
      r.idType shouldBe "SAFE"
      r.idValue shouldBe id
    }

    "throw exception for TaxPayerId" in {
      val caught = intercept[IllegalArgumentException] {
        RegistrationInfoRequest.forCustomsId(TaxPayerId(id))
      }
      caught.getMessage shouldBe "TaxPayerId is not supported by RegistrationInfo service"
    }
  }
}
