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

package unit.models.enrolmentRequest

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest.{ES1QueryType, ES1Request, ES1Response}

class ES1ModelsSpec extends UnitSpec {

  "ES1QueryType" should {

    "have a correct value for all types" in {

      ES1QueryType.All.value shouldBe "all"
      ES1QueryType.Principal.value shouldBe "principal"
      ES1QueryType.Delegated.value shouldBe "delegated"
    }
  }

  "ES1Request" should {

    "be successfully created with service, eori and query type" in {

      val request = ES1Request(Service.withName("atar").get, "GB123456789123", ES1QueryType.Principal)

      val expectedEnrolment = "HMRC-ATAR-ORG~EORINumber~GB123456789123"
      val expectedRequest   = ES1Request(expectedEnrolment, ES1QueryType.Principal)

      request shouldBe expectedRequest
    }
  }

  "ES1Response" should {

    "correctly return information that enrolment is used" when {

      "response has principal and delegated groupIds" in {

        val enrolmentInUseResponse = ES1Response(Some(Seq("groupId")), Some(Seq("groupId")))

        enrolmentInUseResponse.isEnrolmentInUse shouldBe true
      }

      "response has only principal groupIds" in {

        val enrolmentInUseResponse = ES1Response(Some(Seq("groupId")), None)

        enrolmentInUseResponse.isEnrolmentInUse shouldBe true
      }

      "response has only delegated groupIds" in {

        val enrolmentInUseResponse = ES1Response(None, Some(Seq("groupId")))

        enrolmentInUseResponse.isEnrolmentInUse shouldBe true
      }
    }

    "correctly return information that enrolment is not used" when {

      "response has Nones as values" in {

        val notUsedEnrolmentResponse = ES1Response(None, None)

        notUsedEnrolmentResponse.isEnrolmentInUse shouldBe false
      }

      "response has principal groupIds defined but empty" in {

        val notUsedEnrolmentResponse = ES1Response(Some(Seq.empty), None)

        notUsedEnrolmentResponse.isEnrolmentInUse shouldBe false
      }

      "response has delegated groupIds defined but empty" in {

        val notUsedEnrolmentResponse = ES1Response(None, Some(Seq.empty))

        notUsedEnrolmentResponse.isEnrolmentInUse shouldBe false
      }

      "response has principal and delegated groupIds defined but empty" in {

        val notUsedEnrolmentResponse = ES1Response(Some(Seq.empty), Some(Seq.empty))

        notUsedEnrolmentResponse.isEnrolmentInUse shouldBe false
      }
    }
  }
}
