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

package unit.forms.registration

import base.UnitSpec
import play.api.data.FormError
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, LLP, Partnership, UnincorporatedBody}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.registration.BusinessShortNameForm

import scala.util.Random

class BusinessShortNameFormSpec extends UnitSpec {

  "Business Short Name forms" should {

    "return errors" when {

      "short name is empty" in {

        val form = BusinessShortNameForm.form(CorporateBody, false)

        val result = form.bind(Map("short-name" -> ""))

        result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.company.empty"))
      }

      "short name is longer than 20 characters" in {

        val form = BusinessShortNameForm.form(CorporateBody, false)

        val result = form.bind(Map("short-name" -> Random.alphanumeric.take(31).mkString("")))

        result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.error"))
      }
    }

    "return partnership error" in {

      val form = BusinessShortNameForm.form(Partnership, false)

      val result = form.bind(Map("short-name" -> ""))

      result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.partnership.empty"))
    }

    "return partnership error for Limited Liability Partnership" in {

      val form = BusinessShortNameForm.form(LLP, false)

      val result = form.bind(Map("short-name" -> ""))

      result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.partnership.empty"))
    }

    "return charity error" in {

      val form = BusinessShortNameForm.form(UnincorporatedBody, false)

      val result = form.bind(Map("short-name" -> ""))

      result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.charity.empty"))
    }

    "return company error" in {

      val form = BusinessShortNameForm.form(CorporateBody, false)

      val result = form.bind(Map("short-name" -> ""))

      result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.company.empty"))
    }

    "return organisation error" when {

      "user is ROW" in {

        val form = BusinessShortNameForm.form(CorporateBody, true)

        val result = form.bind(Map("short-name" -> ""))

        result.errors shouldBe Seq(FormError("short-name", "ecc.business-short-name.organisation.empty"))
      }
    }

    "return no errors" when {

      "short name is correct" in {

        val form = BusinessShortNameForm.form(CorporateBody, true)

        val result = form.bind(Map("short-name" -> "Short name"))

        result.errors shouldBe Seq.empty
      }
    }
  }
}
