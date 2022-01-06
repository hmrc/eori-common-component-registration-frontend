/*
 * Copyright 2022 HM Revenue & Customs
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

trait VatRegisterUKPage extends WebPage {

  override val title = "Is your organisation VAT registered in the UK?"

  val fieldLevelErrorYesNoAnswerForUKVat = "//*[@id='yes-no-answer-field']/*[@class='error-message']"

  val ukVatNoXpath  = "//*[@id='yes-no-answer-false']"
  val ukVatYesXpath = "//*[@id='yes-no-answer-true']"

}

object VatRegisterUKPage extends VatRegisterUKPage

object VatRegisterUKPageReviewPage extends VatRegisterUKPage {
  override val title = "Is your organisation VAT registered in the UK?"
}

object VatRegisterUKIndividualPage extends VatRegisterUKPage {
  override val title = "Are you VAT registered in the UK?"
}

object VatRegisterUKPartnershipPage extends VatRegisterUKPage {
  override val title = "Is your partnership VAT registered in the UK?"
}
