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

package util.builders.matching

import play.api.data.Form
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SixLineAddressMatchModel

trait OrganisationAddressFormBuilder {

  val LineOne: String
  val LineTwo: String
  val LineThree: String
  val LineFour: String
  val Postcode: String
  val Country: String
  val CountryCode: String

  def asOrganisationAddress: SixLineAddressMatchModel =
    SixLineAddressMatchModel(LineOne, Some(LineTwo), LineThree, Some(LineFour), Some(Postcode), CountryCode)

  def asForm(addressForm: Form[SixLineAddressMatchModel]): Map[String, String] =
    addressForm.mapping.unbind(asOrganisationAddress)

}
