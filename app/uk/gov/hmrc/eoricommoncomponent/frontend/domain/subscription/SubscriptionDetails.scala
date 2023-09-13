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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription

import play.api.Logging

import java.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{
  AddressViewModel,
  CompanyRegisteredCountry,
  ContactDetailsModel,
  VatDetails
}

case class SubscriptionDetails(
  businessShortName: Option[BusinessShortName] = None,
  dateEstablished: Option[LocalDate] = None,
  vatRegisteredUk: Option[Boolean] = None,
  ukVatDetails: Option[VatDetails] = None,
  vatControlListResponse: Option[VatControlListResponse] = None,
  vatVerificationOption: Option[Boolean] = None,
  personalDataDisclosureConsent: Option[Boolean] = None,
  contactDetails: Option[ContactDetailsModel] = None,
  sicCode: Option[String] = None,
  eoriNumber: Option[String] = None,
  existingEoriNumber: Option[ExistingEori] = None,
  email: Option[String] = None,
  addressDetails: Option[AddressViewModel] = None,
  nameIdOrganisationDetails: Option[NameIdOrganisationMatchModel] = None,
  nameOrganisationDetails: Option[NameOrganisationMatchModel] = None,
  nameDobDetails: Option[NameDobMatchModel] = None,
  nameDetails: Option[NameMatchModel] = None,
  idDetails: Option[IdMatchModel] = None,
  customsId: Option[CustomsId] = None,
  formData: FormData = FormData(),
  registeredCompany: Option[CompanyRegisteredCountry] = None
) extends Logging {

  def name: Option[String] = {
    val idOrgDetailsName = nameIdOrganisationDetails.map(_.name)
    val orgDetailsName   = nameOrganisationDetails.map(_.name)
    val dobDetailsName   = nameDobDetails.map(_.name)
    val nameDetailsName  = nameDetails.map(_.name)

    idOrgDetailsName orElse orgDetailsName orElse dobDetailsName orElse nameDetailsName
  }

}

object SubscriptionDetails {
  implicit val format: Format[SubscriptionDetails] = Json.format[SubscriptionDetails]
}

case class FormData(
  utrMatch: Option[UtrMatchModel] = None,
  ninoMatch: Option[NinoMatchModel] = None,
  organisationType: Option[CdsOrganisationType] = None,
  ninoOrUtrChoice: Option[String] = None
)

object FormData {
  implicit val format: Format[FormData] = Json.format[FormData]
}
