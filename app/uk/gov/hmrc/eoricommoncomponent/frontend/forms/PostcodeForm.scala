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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.mandatoryPostCodeMapping
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel

object PostcodeForm {

  private val noTagsRegex = "^[^<>]+$"

  def postCodeCreateForm: Form[PostcodeViewModel] =
    Form(
      mapping(
        "postcode"    -> mandatoryPostCodeMapping,
        "addressLine1" -> optional(text.verifying(addressLine1))
      )(PostcodeViewModel.apply)(PostcodeViewModel.unapply)
    )

  private def addressLine1: Constraint[String] =
    Constraint({
      case s if s.trim.length > 35 =>
        Invalid(ValidationError("ecc.address-lookup.postcode.line1.too-long.error"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("ecc.address-lookup.postcode.line1.invalid-chars.error"))
      case _ => Valid
    })

}
