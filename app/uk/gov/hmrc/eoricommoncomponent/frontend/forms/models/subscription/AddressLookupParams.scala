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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.postcodeRegex

case class AddressLookupParams(postcode: String, line1: Option[String], skippedLine1: Boolean = false) {

  def isEmpty(): Boolean = postcode == "" && line1.forall(_.isEmpty)

  def nonEmpty(): Boolean = !isEmpty()
}

object AddressLookupParams {
  implicit val format = Json.format[AddressLookupParams]

  def form(): Form[AddressLookupParams] = Form(
    mapping(
      "postcode" -> text.verifying("cds.subscription.contact-details.error.postcode", _.matches(postcodeRegex.regex)),
      "line1"    -> optional(text.verifying("ecc.address-lookup.postcode.line1.error", _.length < 36))
    )((postcode, line1) => AddressLookupParams(postcode.toUpperCase, line1))(
      params => Some((params.postcode, params.line1))
    )
  )

}
