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

package common.pages.matching

import common.pages.WebPage

trait OrganisationUtrPage extends WebPage {

  val fieldLevelErrorUtr     = "//*[@id='utr-outer']//span[@class='error-message']"
  val labelForUtrXpath       = "//*[@id='utr-outer']/label"
  val linkInUtrHintTextXpath = "//*[@id='utr-hint']/a"

  override val title = "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) number?"

  val haveUtrYesRadioButtonXpath = "//*[@id='have-utr-yes']"
  val haveUtrNoRadioButtonXpath  = "//*[@id='have-utr-no']"

}

object OrganisationUtrPage extends OrganisationUtrPage

object SubscriptionRowIndividualsUtr extends OrganisationUtrPage {
  override val title = "Do you have a Self Assessment Unique Taxpayer Reference (UTR) issued in the UK?"
  val formId         = "SubscriptionUtrForm"
}

object SubscriptionRowCompanyUtr extends OrganisationUtrPage {
  override val title = "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) issued in the UK?"
  val formId         = "SubscriptionUtrForm"
}
