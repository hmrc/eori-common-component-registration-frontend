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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel

case class ContactDetailsSubscribeModel(fullName: String, telephone: String) {

  def toContactDetailsModel(email: String): ContactDetailsModel = ContactDetailsModel(
    fullName = fullName,
    emailAddress = email,
    telephone = telephone,
    fax = None,
    useAddressFromRegistrationDetails = false,
    street = None,
    city = None,
    postcode = None,
    countryCode = None
  )

}

object ContactDetailsSubscribeModel {
  implicit val format: OFormat[ContactDetailsSubscribeModel] = Json.format[ContactDetailsSubscribeModel]

  def fromContactDetailsModel(details: ContactDetailsModel): ContactDetailsSubscribeModel =
    ContactDetailsSubscribeModel(details.fullName, details.telephone)

}
