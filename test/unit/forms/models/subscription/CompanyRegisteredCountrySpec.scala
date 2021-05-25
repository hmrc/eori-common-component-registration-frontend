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

package unit.forms.models.subscription

import base.{Injector, UnitSpec}
import play.api.data.FormError
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.CompanyRegisteredCountry

class CompanyRegisteredCountrySpec extends UnitSpec with Injector {

  implicit val messages = stubMessages()

  "Company Registered Country form" should {

    "has no errors" when {

      "the value is a correct country" in {

        val form = CompanyRegisteredCountry.form("errorMessage").bind(Map("countryCode" -> "United Kingdom"))

        form.errors shouldBe Seq.empty
      }
    }

    "has error" when {

      "input is empty" in {

        val form = CompanyRegisteredCountry.form("errorMessage").bind(Map("countryCode" -> ""))

        val expectedErrors = Seq(FormError("countryCode", "errorMessage"))

        form.errors shouldBe expectedErrors
      }

      "input has empty value" in {

        val form = CompanyRegisteredCountry.form("errorMessage").bind(
          Map("countryCode" -> "cds.subscription.address-details.country.emptyValueText")
        )

        val expectedErrors = Seq(FormError("countryCode", "errorMessage"))

        form.errors shouldBe expectedErrors
      }
    }
  }
}
