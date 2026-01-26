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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.Logging
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatVerificationOption, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.oneOf
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

object MatchingForms extends Mappings with Logging with Constraints {

  val Length35 = 35

  private val validYesNoAnswerOptions = Set("true", "false")

  def disclosePersonalDetailsYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] =
    createYesNoAnswerForm("cds.subscription.organisation-disclose-personal-details-consent.error.yes-no-answer")

  def contactAddressDetailsYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] =
    createYesNoAnswerForm("ecc.contact-address-details.error.yes-no-answer")

  def vatRegisteredUkYesNoAnswerForm(isPartnership: Boolean = false)(implicit messages: Messages): Form[YesNo] =
    if (isPartnership) createYesNoAnswerForm("cds.registration.vat-registered-uk.partnership.error.yes-no-answer")
    else createYesNoAnswerForm("cds.registration.vat-registered-uk.error.yes-no-answer")

  def vatGroupYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] =
    createYesNoAnswerForm("cds.subscription.vat-group.page-error.yes-no-answer")

  def vatVerificationOptionAnswerForm()(implicit messages: Messages): Form[VatVerificationOption] =
    createVatVerificationOptionAnswerForm("cds.subscription.vat-verification-option.error")

  private def createVatVerificationOptionAnswerForm(
    invalidErrorMsgKey: String
  )(implicit messages: Messages): Form[VatVerificationOption] = Form(
    mapping(
      "vat-verification-option" -> optional(
        text.verifying(messages(invalidErrorMsgKey), oneOf(validYesNoAnswerOptions))
      )
        .verifying(messages(invalidErrorMsgKey), _.isDefined)
        .transform[Boolean](str => str.get.toBoolean, bool => Option(String.valueOf(bool)))
    )(VatVerificationOption.apply)(VatVerificationOption.unapply)
  )

  private def createYesNoAnswerForm(invalidErrorMsgKey: String)(implicit messages: Messages): Form[YesNo] = Form(
    mapping(
      YesNo.yesAndNoAnswer -> optional(text.verifying(messages(invalidErrorMsgKey), oneOf(validYesNoAnswerOptions)))
        .verifying(messages(invalidErrorMsgKey), _.isDefined)
        .transform[Boolean](str => str.get.toBoolean, bool => Option(String.valueOf(bool)))
    )(YesNo.apply)(YesNo.unapply)
  )

  val countryCodeGB = "GB"
  val countryCodeGG = "GG"
  val countryCodeJE = "JE"
  val countryCodeIM = "IM"
}
