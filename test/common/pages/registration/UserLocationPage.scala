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

package common.pages.registration

import common.pages.WebPage

trait UserLocationPage extends WebPage {

  override val title = "Where are you based?"

  val fieldLevelErrorLocation = "//*[@id='location-fieldset']//span[@class='error-message']"

  val locationUkField                = "//*[@id='location-uk']"
  val locationIomField               = "//*[@id='location-iom']"
  val locationIslandsField           = "//*[@id='location-islands']"
  val locationEuField                = "//*[@id='location-eu']"
  val locationThirdCountryField      = "//*[@id='location-third-country']"
  val locationThirdCountryIncEuField = "//*[@id='location-third-country-inc-eu']"

  val countriesInTheEuTitleElement    = "//*[@id='user-location-form']/div/details/summary"
  val countriesInTheEuContentsElement = "//*[@id='user-location-form']/div/details/div"

}

object UserLocationPageOrganisation extends UserLocationPage {
  override val title = "Where is your organisation established?"
}
