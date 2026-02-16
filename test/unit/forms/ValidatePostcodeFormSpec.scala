/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._

class ValidatePostcodeFormSpec extends AnyFreeSpec with Matchers {

  "ValidatePostcodeForm" - {

    val ukCountryCodeList = List("GB", "GG", "JE")
    for {
      countryCode <- ukCountryCodeList
    } yield s"For $countryCode" - {
      "only accept valid postcode " in {
        val data = Map("postcode" -> "N9 0DL", "countryCode" -> countryCode)
        val res = form.bind(data)
        res.errors shouldBe Seq.empty
      }

      "fail when a postcode is invalid" in {
        val data = Map("postcode" -> "", "countryCode" -> countryCode)
        val res = form.bind(data)
        res.errors should not be empty
      }
    }
  }

  case class Model(countryCode: String, postcode: Option[String])

  lazy val form = Form(
    mapping("countryCode" -> nonEmptyText, "postcode" -> mandatoryOptPostCodeMapping)(Model.apply)(model => Some(model.countryCode, model.postcode))
  )

}
