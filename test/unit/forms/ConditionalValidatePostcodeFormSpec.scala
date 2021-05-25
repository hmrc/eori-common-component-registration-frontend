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

package unit.forms

import base.UnitSpec
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._

class ConditionalValidatePostcodeFormSpec extends UnitSpec {

  "ValidatePostcode" should {

    val ukCountryCodeList = List("GB", "GG", "JE")
    for {
      countryCode <- ukCountryCodeList
    } yield s"For $countryCode" should {
      "only accept valid postcode " in {
        val data = Map("postcode" -> "N9 0DL", "countryCode" -> countryCode)
        val res  = form.bind(data)
        assert(res.errors.isEmpty)
      }

      "fail when a postcode is invalid" in {
        val data = Map("postcode" -> "", "countryCode" -> countryCode)
        val res  = form.bind(data)
        assert(res.errors.nonEmpty)
      }
    }

    "For Non-Uk Postcodes" should {
      "only accept up to 9 Chars" in {
        val data = Map("postcode" -> "123456789", "countryCode" -> "FR")
        val res  = form.bind(data)
        assert(res.errors.isEmpty)
      }
      "fail when a postcode is over 9 Chars" in {
        val data = Map("postcode" -> "A10Character", "countryCode" -> "FR")
        val res  = form.bind(data)
        assert(res.errors.nonEmpty)
      }
      "accept an empty postcode" in {
        val data = Map("postcode" -> "", "countryCode" -> "FR")
        val res  = form.bind(data)
        assert(res.errors.isEmpty)
      }
    }
  }

  case class Model(countryCode: String, postcode: Option[String])

  lazy val form = Form(
    mapping("countryCode" -> nonEmptyText, "postcode" -> postcodeMapping)(Model.apply)(Model.unapply)
  )

}
