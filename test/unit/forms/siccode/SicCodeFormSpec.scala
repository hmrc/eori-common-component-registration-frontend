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

package unit.forms.siccode

import base.UnitSpec
import play.api.data.{Form, FormError}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.siccode.{SicCodeForm, SicCodeViewModel}

class SicCodeFormSpec extends UnitSpec {

  val formDataSic: Map[String, String] = Map("sic" -> "99111")

  lazy val sicCodeForm: Form[SicCodeViewModel] = new SicCodeForm().form()

  "sicCodeForm" should {

    "only accept valid form" in {
      val data = formDataSic
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept sic code with leading whitespace" in {
      val data = formDataSic.updated("sic", " 10009")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept sic code with trailing whitespace" in {
      val data = formDataSic.updated("sic", "10009 ")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept sic code with multiple whitespaces" in {
      val data = formDataSic.updated("sic", " 100 09 ")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail sic code with only whitespaces - empty string" in {
      val data = formDataSic.updated("sic", "    ")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.empty")))
    }
    "fail sic code with invalid characters - wrong format" in {
      val data = formDataSic.updated("sic", "111k2")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.wrong-format")))
    }
    "fail sic code too short" in {
      val data = formDataSic.updated("sic", "111")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.too-short")))
    }
    "fail sic code too long" in {
      val data = formDataSic.updated("sic", "111111")
      val res = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.too-long")))
    }
  }
}
