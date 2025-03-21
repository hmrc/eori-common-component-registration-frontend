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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Form
import play.api.data.Forms.{default, mapping, optional, text}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.ContactAddressMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Constraints

import javax.inject.Singleton

@Singleton
class ContactAddressFormProvider extends Constraints {

  private val countryCodeGB = "GB"

  def contactAddressForm: Form[ContactAddressMatchModel] = {
    Form(
      mapping(
        "line-1"      -> text.verifying(validLine1),
        "line-2"      -> optional(text.verifying(validLine2)),
        "townCity"    -> text.verifying(validLine3),
        "postcode"    -> text.verifying(validPostcode),
        "countryCode" -> default(text, countryCodeGB)
      )(ContactAddressMatchModel.apply)(ContactAddressMatchModel.unapply)
    )
  }
}
