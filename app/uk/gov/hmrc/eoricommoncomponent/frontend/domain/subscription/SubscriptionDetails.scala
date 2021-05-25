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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{
  AddressViewModel,
  CompanyRegisteredCountry,
  VatDetails,
  VatEUDetailsModel
}

case class SubscriptionDetails(
  businessShortName: Option[BusinessShortName] = None,
  dateEstablished: Option[LocalDate] = None,
  vatRegisteredUk: Option[Boolean] = None,
  ukVatDetails: Option[VatDetails] = None,
  vatRegisteredEu: Option[Boolean] = None,
  vatEUDetails: Seq[VatEUDetailsModel] = Nil,
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
) {

  def name: String =
    nameIdOrganisationDetails.map(_.name) orElse nameOrganisationDetails.map(_.name) orElse nameDobDetails.map(
      _.name
    ) orElse nameDetails
      .map(_.name) getOrElse (throw new IllegalArgumentException("Name is missing"))

  def vatIdentificationList: List[VatIdentification] =
    vatEUDetails.map(x => VatIdentification(countryCode = x.vatCountry, number = x.vatNumber)).toList

}

object SubscriptionDetails {
  val EuVatDetailsLimit = 5

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
