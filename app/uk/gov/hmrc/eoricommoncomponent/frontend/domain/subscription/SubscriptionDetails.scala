/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{
  AddressViewModel,
  CompanyRegisteredCountry,
  ContactDetailsModel,
  VatDetails
}

import java.time.LocalDate

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
  registeredCompany: Option[CompanyRegisteredCountry] = None,
  postCodeForValidation: Option[String] = None,
  embassyName: Option[String] = None
) extends Logging {

  def name: Option[String] = {
    val idOrgDetailsName = nameIdOrganisationDetails.map(_.name)
    val orgDetailsName   = nameOrganisationDetails.map(_.name)
    val dobDetailsName   = nameDobDetails.map(_.name)
    val nameDetailsName  = nameDetails.map(_.name)

    idOrgDetailsName orElse orgDetailsName orElse dobDetailsName orElse nameDetailsName orElse embassyName
  }

}

object SubscriptionDetails {

  private type FirstTwenty =
    (
      Option[BusinessShortName],
      Option[LocalDate],
      Option[Boolean],
      Option[VatDetails],
      Option[VatControlListResponse],
      Option[Boolean],
      Option[Boolean],
      Option[ContactDetailsModel],
      Option[String],
      Option[String],
      Option[ExistingEori],
      Option[String],
      Option[AddressViewModel],
      Option[NameIdOrganisationMatchModel],
      Option[NameOrganisationMatchModel],
      Option[NameDobMatchModel],
      Option[NameMatchModel],
      Option[IdMatchModel],
      Option[CustomsId],
      FormData,
      Option[CompanyRegisteredCountry]
    )

  private type LastTwo = (Option[String], Option[String])

  implicit val firstTwenty: OFormat[FirstTwenty] =
    ((__ \ "businessShortName").formatNullable[BusinessShortName]
      ~ (__ \ "dateEstablished").formatNullable[LocalDate]
      ~ (__ \ "vatRegisteredUk").formatNullable[Boolean]
      ~ (__ \ "ukVatDetails").formatNullable[VatDetails]
      ~ (__ \ "vatControlListResponse").formatNullable[VatControlListResponse]
      ~ (__ \ "vatVerificationOption").formatNullable[Boolean]
      ~ (__ \ "personalDataDisclosureConsent").formatNullable[Boolean]
      ~ (__ \ "contactDetails").formatNullable[ContactDetailsModel]
      ~ (__ \ "sicCode").formatNullable[String]
      ~ (__ \ "eoriNumber").formatNullable[String]
      ~ (__ \ "existingEoriNumber").formatNullable[ExistingEori]
      ~ (__ \ "email").formatNullable[String]
      ~ (__ \ "addressDetails").formatNullable[AddressViewModel]
      ~ (__ \ "nameIdOrganisationDetails").formatNullable[NameIdOrganisationMatchModel]
      ~ (__ \ "nameOrganisationDetails").formatNullable[NameOrganisationMatchModel]
      ~ (__ \ "nameDobDetails").formatNullable[NameDobMatchModel]
      ~ (__ \ "nameDetails").formatNullable[NameMatchModel]
      ~ (__ \ "idDetails").formatNullable[IdMatchModel]
      ~ (__ \ "customsId").formatNullable[CustomsId]
      ~ (__ \ "formData").format[FormData]
      ~ (__ \ "registeredCompany").formatNullable[CompanyRegisteredCountry]).tupled

  implicit val lastTwo: OFormat[LastTwo] =
    ((__ \ "postCodeForValidation").formatNullable[String]
      ~ (__ \ "embassyName").formatNullable[String]).tupled

  implicit val subscriptionDetailsFormat: OFormat[SubscriptionDetails] =
    (firstTwenty ~ lastTwo)(
      {
        case (
              (
                businessShortName,
                dateEstablished,
                vatRegisteredUk,
                ukVatDetails,
                vatControlListResponse,
                vatVerificationOption,
                personalDataDisclosureConsent,
                contactDetails,
                sicCode,
                eoriNumber,
                existingEoriNumber,
                email,
                addressDetails,
                nameIdOrganisationDetails,
                nameOrganisationDetails,
                nameDobDetails,
                nameDetails,
                idDetails,
                customsId,
                formData,
                registeredCompany
              ),
              (postCodeForValidation, embassyName)
            ) =>
          SubscriptionDetails(
            businessShortName = businessShortName,
            dateEstablished = dateEstablished,
            vatRegisteredUk = vatRegisteredUk,
            ukVatDetails = ukVatDetails,
            vatControlListResponse = vatControlListResponse,
            vatVerificationOption = vatVerificationOption,
            personalDataDisclosureConsent = personalDataDisclosureConsent,
            contactDetails = contactDetails,
            sicCode = sicCode,
            eoriNumber = eoriNumber,
            existingEoriNumber = existingEoriNumber,
            email = email,
            addressDetails = addressDetails,
            nameIdOrganisationDetails = nameIdOrganisationDetails,
            nameOrganisationDetails = nameOrganisationDetails,
            nameDobDetails = nameDobDetails,
            nameDetails = nameDetails,
            idDetails = idDetails,
            customsId = customsId,
            formData = formData,
            registeredCompany = registeredCompany,
            postCodeForValidation = postCodeForValidation,
            embassyName = embassyName
          )
      }: (FirstTwenty, LastTwo) => SubscriptionDetails,
      (subscriptionDetails: SubscriptionDetails) =>
        (
          (
            subscriptionDetails.businessShortName,
            subscriptionDetails.dateEstablished,
            subscriptionDetails.vatRegisteredUk,
            subscriptionDetails.ukVatDetails,
            subscriptionDetails.vatControlListResponse,
            subscriptionDetails.vatVerificationOption,
            subscriptionDetails.personalDataDisclosureConsent,
            subscriptionDetails.contactDetails,
            subscriptionDetails.sicCode,
            subscriptionDetails.eoriNumber,
            subscriptionDetails.existingEoriNumber,
            subscriptionDetails.email,
            subscriptionDetails.addressDetails,
            subscriptionDetails.nameIdOrganisationDetails,
            subscriptionDetails.nameOrganisationDetails,
            subscriptionDetails.nameDobDetails,
            subscriptionDetails.nameDetails,
            subscriptionDetails.idDetails,
            subscriptionDetails.customsId,
            subscriptionDetails.formData,
            subscriptionDetails.registeredCompany
          ),
          (subscriptionDetails.postCodeForValidation, subscriptionDetails.embassyName)
        )
    )

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
