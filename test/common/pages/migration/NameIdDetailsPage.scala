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

package common.pages.migration

import common.pages.WebPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameIdOrganisationMatchModel

trait NameIdDetailsPage extends WebPage {

  override val title = "What are your business details?"

  val formId = "nameUtrOrganisationForm"

  val pageTitleXPath = "//h1"

  val nameFieldXPath           = "//*[@id='name']"
  val nameFieldLevelErrorXPath = "//*[@id='name-outer']//span[@class='error-message']"
  val nameFieldLabel           = "Registered business name"
  val nameFieldId              = "name"
  val nameFieldName            = "name"

  val utrFieldXPath           = "//*[@id='utr']"
  val utrFieldLevelErrorXPath = "//*[@id='utr-outer']//span[@class='error-message']"
  val utrFieldLabel           = "Company Unique Taxpayer Reference (UTR) number"
  val utrFieldId              = "utr"
  val utrFieldName            = "utr"

  val continueButtonXpath = "//*[@class='button']"

  val filledValues = NameIdOrganisationMatchModel(name = "Test Business Name", id = "2108834503")

}

object NameIdDetailsPage extends NameIdDetailsPage
