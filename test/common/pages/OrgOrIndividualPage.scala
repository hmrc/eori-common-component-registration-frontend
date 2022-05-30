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

package common.pages

trait OrgOrIndividualPage extends WebPage {

  val fieldLevelErrorOrganisationType: String =
    "//p[contains(@id, 'organisation-type-error') and contains(@class, 'govuk-error-message']"

  override val title = "What do you want to apply as?"

  val organisationXpath: String
  val individualXpath: String
}

object EuOrgOrIndividualPage extends OrgOrIndividualPage {

  override val organisationXpath = "//*[@id='organisation-type-eu-organisation']"
  override val individualXpath   = "//*[@id='organisation-type-eu-individual']"
}
