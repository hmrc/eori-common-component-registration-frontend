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

package common.pages.subscription

import common.pages.WebPage

trait SubscriptionDateCommon extends WebPage {

  def formId: String

  protected def dateFieldName: String

  def dateOfEstablishmentErrorXpath: String = s"//*[@id='date-of-establishment-fieldset']//span[@class='error-message']"

  def dateOfBirthFieldLevelErrorXpath: String = s"//*[@id='date-of-birth-fieldset']//span[@class='error-message']"

  def dayOfDateFieldXpath: String = s"//*[@id='$dateFieldName.day']"

  def monthOfDateFieldXpath: String = s"//*[@id='$dateFieldName.month']"

  def yearOfDateFieldXpath: String = s"//*[@id='$dateFieldName.year']"

  def dateOfEstablishmentHeadingXPath: String = "//h1"

}

object SubscriptionDateOfBirthPage extends SubscriptionDateCommon {
  override val title = "Your details"

  override val formId: String          = "date-of-birth-form"
  override protected val dateFieldName = "date-of-birth"
}

object SubscriptionDateOfEstablishmentPage extends SubscriptionDateCommon {
  override val title = "When was the organisation established?"

  override val formId: String          = "date-of-establishment-form"
  override protected val dateFieldName = "date-of-establishment"

  def dateOfEstablishmentLabelXPath: String = "//*[@id='date-of-establishment-label']"
}

object SubscriptionPartnershipDateOfEstablishmentPage extends SubscriptionDateCommon {
  override val title = "When was the partnership established?"

  override val formId: String          = "date-of-establishment-form"
  override protected val dateFieldName = "date-of-establishment"
}
