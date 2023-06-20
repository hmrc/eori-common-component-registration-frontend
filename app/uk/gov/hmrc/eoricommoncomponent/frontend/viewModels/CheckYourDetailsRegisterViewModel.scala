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

package uk.gov.hmrc.eoricommoncomponent.frontend.viewModels

import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.Html
import play.twirl.api.utils.StringEscapeUtils
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, RegistrationDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  Eori,
  Nino,
  RegistrationDetails,
  RegistrationDetailsIndividual,
  Utr
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.helpers.noMarginParagraph
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._

import java.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers.DateFormatter
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, SummaryListRow, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Value}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.DataUnavailableException

import javax.inject.{Inject, Named, Singleton}

case class CheckYourDetailsRegisterViewModel(
  headerTitle: String,
  individualNameDob: Seq[SummaryListRow],
  organisationName: Seq[SummaryListRow],
  organisationUtr: Seq[SummaryListRow],
  customsId: Seq[SummaryListRow],
  individualUtr: Seq[SummaryListRow],
  registeredAddress: Seq[SummaryListRow],
  dateOfEstablishment: Seq[SummaryListRow],
  sicCodeDisplay: Seq[SummaryListRow],
  summary: Seq[SummaryListRow],
  vatDetails: Seq[SummaryListRow],
  contactName: Seq[SummaryListRow],
  email: Seq[SummaryListRow],
  contactTelephone: Seq[SummaryListRow],
  details: Seq[SummaryListRow]
)

@Singleton
class CheckYourDetailsRegisterConstructor @Inject() (dateFormatter: DateFormatter) {

  def getDateOfEstablishmentLabel(cdsOrgType: Option[CdsOrganisationType])(implicit messages: Messages) = {
    val isSoleTrader = cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
    cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader)
    if (isSoleTrader) messages("cds.date-of-birth.label")
    else messages("cds.date-established.label")
  }
  def orgNameLabel(cdsOrgType: Option[CdsOrganisationType], isPartnership: Boolean)(implicit messages: Messages) = {
    val orgNameLabel = cdsOrgType.contains(CdsOrganisationType.CharityPublicBodyNotForProfit) || cdsOrgType.contains(CdsOrganisationType.ThirdCountryOrganisation)
    (orgNameLabel, isPartnership) match {
      case (false, true) => messages("cds.partner-name.label")
      case (true, false) => messages("cds.organisation-name.label")
      case (_, _) => messages("cds.business-name.label")

    }
  }
  def businessDetailsLabel(isPartnership: Boolean, cdsOrgType: Option[CdsOrganisationType])(implicit messages: Messages) = {
    val soleAndIndividual =
        cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader) ||
        cdsOrgType.contains(CdsOrganisationType.Individual) ||
        cdsOrgType.contains(CdsOrganisationType.EUIndividual) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)
    val orgNameLabel =
        cdsOrgType.contains(CdsOrganisationType.CharityPublicBodyNotForProfit) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountryOrganisation)

    (isPartnership, soleAndIndividual, orgNameLabel) match {
      case (true, false, false) => messages("cds.form.partnership.contact-details")
      case (false, true, false) => messages("cds.form.contact-details")
      case (false, false, true) => messages("cds.form.organisation-address")
      case (_,_,_) => messages("cds.form.business-details")
    }
  }

  def ninoOrUtrLabel(registration: RegistrationDetails, cdsOrgType: Option[CdsOrganisationType], isPartnership: Boolean)(implicit messages: Messages) = {
    val soleAndIndividual = {
      cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader) ||
        cdsOrgType.contains(CdsOrganisationType.Individual) ||
        cdsOrgType.contains(CdsOrganisationType.EUIndividual) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)
    }
    def utrLabelChecker() = {
      val lLPOnly = cdsOrgType.contains(CdsOrganisationType.LimitedLiabilityPartnership)
      (soleAndIndividual, lLPOnly, isPartnership) match {
        case (true, false, false) => messages("cds.utr.label")
        case (false, true, false) => messages("cds.matching.name-id-organisation.company.utr")
        case (false, false, true) => messages("cds.check-your-details.utrnumber.partnership")
        case (false, false, false) => messages("cds.company.utr.label")
      }
    }
    registration.customsId match {
    case Some(Utr(_)) => utrLabelChecker()
    case Some(Nino(_)) => messages("cds.nino.label")
    case Some(Eori(_)) => messages("cds.subscription.enter-eori-number.eori-number.label")
    case _ => messages("cds.nino.label")
  }
  }


  def generateViewModel(
    cdsOrgType: Option[CdsOrganisationType],
    isPartnership: Boolean,
    registration: RegistrationDetails,
    subscription: SubscriptionDetails,
    personalDataDisclosureConsent: Boolean,
    service: Service,
    isUserIdentifiedByRegService: Boolean
  )(implicit messages: Messages): CheckYourDetailsRegisterViewModel = {

    val isIndividual: Boolean = cdsOrgType.contains(CdsOrganisationType.Individual) || cdsOrgType.contains(
      CdsOrganisationType.EUIndividual
    ) || cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)
    val isCharity = cdsOrgType.contains(CdsOrganisationType.CharityPublicBodyNotForProfit)
    val isSoleTrader = cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
      cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader)
    val isRowOrganisation = cdsOrgType.contains(CdsOrganisationType.ThirdCountryOrganisation)
    val isRowSoleTraderIndividual = cdsOrgType.contains(
      CdsOrganisationType.ThirdCountrySoleTrader
    ) || cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)





    val formattedIndividualDateOfBirth = {
      val dateOfBirth: Option[LocalDate] = (subscription.nameDobDetails, registration) match {
        case (Some(nameDobDetails), _)                         => Some(nameDobDetails.dateOfBirth)
        case (None, individual: RegistrationDetailsIndividual) => Some(individual.dateOfBirth)
        case _                                                 => None
      }
      dateOfBirth.map(formatDate)
    }

    val individualName = subscription.nameDobDetails match {
      case Some(nameDobDetails)           => nameDobDetails.name
      case _ if registration.name == null => ""
      case _                              => registration.name
    }

    val orgName = subscription.nameOrganisationDetails match {
      case Some(nameOrgDetails)           => nameOrgDetails.name
      case _ if subscription.name == null => ""
      case _                              => subscription.name
    }

    val orgType = cdsOrgType.fold("")(orgType => orgType.id)

    val headerTitle =
      if (isPartnership)
        messages("cds.form.check-answers-partnership-details")
      else if (isIndividual || isSoleTrader)
        messages("cds.form.check-answers-your-details")
      else if (isCharity || isRowOrganisation)
        messages("cds.form.check-answers-organisation-details")
      else
        messages("cds.form.check-answers-company-details")

    val disclosureLabel =
      if (isPartnership)
        messages("cds.form.disclosure.partnership")
      else if (isIndividual || isSoleTrader)
        messages("cds.form.disclosure.individual")
      else if (isCharity || isRowOrganisation)
        messages("cds.form.disclosure.organisation")
      else
        messages("cds.form.disclosure")

    val eoriCheckerConsentYes =
      if (isPartnership)
        messages("cds.eori-checker-consent.partnership.yes")
      else if (isIndividual || isSoleTrader)
        messages("cds.eori-checker-consent.individual-or-sole-trader.yes")
      else
        messages("cds.eori-checker-consent.yes")

    val email = Seq(
      summaryListRow(
        key = messages("subscription.enter-email.label"),
        value = subscription.contactDetails.map(cd => Html(cd.emailAddress)),
        call = None
      )
    )

    val individualNameDob =
      if (isIndividual || isSoleTrader)
        Seq(
          summaryListRow(
            key = messages("subscription.check-your-details.full-name.label"),
            value = Some(Html(StringEscapeUtils.escapeXml11(individualName))),
            call =
              if (!isUserIdentifiedByRegService)
                Some(RowIndividualNameDateOfBirthController.reviewForm(orgType, service))
              else None
          ),
          summaryListRow(
            key = messages("subscription.check-your-details.date-of-birth.label"),
            value = formattedIndividualDateOfBirth.map(dob => Html(dob)),
            call =
              if (!isUserIdentifiedByRegService)
                Some(RowIndividualNameDateOfBirthController.reviewForm(orgType, service))
              else None
          )
        )
      else Seq.empty[SummaryListRow]

    val organisationName =
      if (!isIndividual && !isSoleTrader)
        Seq(
          summaryListRow(
            key = orgNameLabel(cdsOrgType, isPartnership),
            value = Some(Text(orgName).asHtml),
            call =
              if (!isUserIdentifiedByRegService)
                Some(WhatIsYourOrgNameController.showForm(isInReviewMode = true, organisationType = orgType, service))
              else None
          )
        )
      else Seq.empty[SummaryListRow]

    val organisationUtr =
      if (isRowOrganisation && !isUserIdentifiedByRegService)
        Seq(
          summaryListRow(
            key = messages("cds.company.utr.label"),
            value = Some(Html(messages("cds.not-entered.label"))),
            call = cdsOrgType.map(orgType => DoYouHaveAUtrNumberController.form(orgType.id, service, false))
          )
        )
      else Seq.empty[SummaryListRow]

    val customsId =
      if (registration.customsId.isDefined)
        Seq(summaryListRow(key = ninoOrUtrLabel(registration, cdsOrgType, isPartnership), value = Some(Html(registration.customsId.get.id))))
      else Seq.empty[SummaryListRow]

    val individualUtr =
      if (registration.customsId.isEmpty && isRowSoleTraderIndividual)
        Seq(
          summaryListRow(
            key = messages("cds.utr.label"),
            value = Some(Html(messages("cds.not-entered.label"))),
            call = cdsOrgType.map(orgType => DoYouHaveAUtrNumberController.form(orgType.id, service, false))
          ),
          summaryListRow(
            key = messages("cds.nino.label"),
            value = Some(Html(messages("cds.not-entered.label"))),
            call = cdsOrgType.map(orgType => DoYouHaveNinoController.displayForm(service))
          )
        )
      else Seq.empty[SummaryListRow]

    val registeredAddress =
      if (isUserIdentifiedByRegService)
        subscription.addressDetails.fold {
          Seq(
            summaryListRow(
              key = businessDetailsLabel(isPartnership, cdsOrgType),
              value = Some(addressHtml(registration.address)),
              call = Some(SixLineAddressController.showForm(isInReviewMode = true, organisationType = orgType, service))
            )
          )
        } {
          address =>
            Seq(
              summaryListRow(
                key = businessDetailsLabel(isPartnership, cdsOrgType),
                value = Some(addressViewModelHtml(address)),
                call = Some(ConfirmContactDetailsController.form(service, isInReviewMode = true))
              )
            )
        }
      else
        Seq(
          summaryListRow(
            key = businessDetailsLabel(isPartnership, cdsOrgType),
            value = Some(addressHtml(registration.address)),
            call = Some(SixLineAddressController.showForm(isInReviewMode = true, organisationType = orgType, service))
          )
        )

    val dateOfEstablishment = Seq(subscription.dateEstablished.map { establishmentDate =>
      summaryListRow(
        key = getDateOfEstablishmentLabel(cdsOrgType),
        value = Some(Html(formatDate(establishmentDate))),
        call = Some(DateOfEstablishmentController.reviewForm(service))
      )
    }).flatten

    val contactName = Seq(subscription.contactDetails.map { cd =>
      summaryListRow(
        key = messages("cds.form.check-answers.contact-name"),
        value = Some(Text(cd.fullName).asHtml),
        call =
          Some(uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactDetailsController.reviewForm(service))
      )
    }).flatten

    val contactTelephone = Seq(subscription.contactDetails.map { cd =>
      summaryListRow(
        key = messages("cds.form.check-answers.contact-telephone"),
        value = Some(Html(cd.telephone)),
        call =
          Some(uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactDetailsController.reviewForm(service))
      )
    }).flatten

    val sicCodeDisplay = Seq(subscription.sicCode.map { sic =>
      summaryListRow(
        key = messages("cds.form.sic-code"),
        value = Some(Html(sic)),
        call = Some(SicCodeController.submit(isInReviewMode = true, service))
      )
    }).flatten

    val vatDetails =
      if (!isIndividual && subscription.vatVerificationOption.getOrElse(true) == true)
        Seq(
          summaryListRowNoChangeOption(
            key = messages("cds.form.gb-vat-number"),
            value = Some(Html(subscription.ukVatDetails.map(_.number).getOrElse(messages("cds.not-entered.label")))),
            call = Some(VatRegisteredUkController.reviewForm(service))
          ),
          summaryListRowNoChangeOption(
            key = messages("cds.form.gb-vat-postcode"),
            value = Some(Html(subscription.ukVatDetails.map(_.postcode).getOrElse(messages("cds.not-entered.label")))),
            call = Some(VatRegisteredUkController.reviewForm(service))
          ),
          summaryListRowNoChangeOption(
            key = messages("cds.form.gb-vat-date"),
            value = Some(
              Html(
                subscription.vatControlListResponse.map(
                  vat =>
                    formatDate(
                      LocalDate.parse(
                        vat.dateOfReg.getOrElse(
                          throw new DataUnavailableException("VAT registration date not found in cache")
                        )
                      )
                    )
                ).getOrElse(messages("cds.not-entered.label"))
              )
            ),
            call = Some(VatRegisteredUkController.reviewForm(service))
          )
        )
      else if (!isIndividual && subscription.vatVerificationOption.getOrElse(false) == false)
        Seq(
          summaryListRowNoChangeOption(
            key = messages("cds.form.gb-vat-number"),
            value = Some(Html(subscription.ukVatDetails.map(_.number).getOrElse(messages("cds.not-entered.label")))),
            call = Some(VatRegisteredUkController.reviewForm(service))
          ),
          summaryListRowNoChangeOption(
            key = messages("cds.form.gb-vat-postcode"),
            value = Some(Html(subscription.ukVatDetails.map(_.postcode).getOrElse(messages("cds.not-entered.label")))),
            call = Some(VatRegisteredUkController.reviewForm(service))
          ),
          summaryListRowNoChangeOption(
            key = messages("cds.form.gb-vat-amount"),
            value = Some(
              Html(
                subscription.vatControlListResponse.map(vat => vat.lastNetDue.get.toString).getOrElse(
                  messages("cds.not-entered.label")
                )
              )
            ),
            call = Some(VatRegisteredUkController.reviewForm(service))
          )
        )
      else Seq.empty[SummaryListRow]

    val summary = Seq(
      summaryListRow(
        key = disclosureLabel,
        value = Some(
          Html(
            if (personalDataDisclosureConsent) eoriCheckerConsentYes
            else messages("cds.eori-checker-consent.no")
          )
        ),
        call = Some(DisclosePersonalDetailsConsentController.reviewForm(service))
      )
    )

    val details = Seq(subscription.contactDetails.map { contactDetails =>
      summaryListRow(
        key = messages("cds.form.customs-contact-address"),
        value = Some(contactDetailsHtml(contactDetails)),
        call =
          Some(uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactAddressController.reviewForm(service))
      )
    }).flatten

    CheckYourDetailsRegisterViewModel(
      headerTitle,
      individualNameDob,
      organisationName,
      organisationUtr,
      customsId,
      individualUtr,
      registeredAddress,
      dateOfEstablishment,
      sicCodeDisplay,
      summary,
      vatDetails,
      contactName,
      email,
      contactTelephone,
      details
    )

  }

  private def addressViewModelHtml(ad: AddressViewModel)(implicit messages: Messages): Html = Html {
    val lines = Seq(
      noMarginParagraph(StringEscapeUtils.escapeXml11(ad.street)),
      noMarginParagraph(StringEscapeUtils.escapeXml11(ad.city))
    )
    val optionalLines: Seq[Option[Html]] = Seq(
      ad.postcode.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      transformCountryCodeToOptionalLabel(Some(ad.countryCode)).map(
        s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))
      )
    )
    (lines ++ optionalLines.flatten).map(_.toString).mkString("")
  }

  private def addressHtml(ad: Address)(implicit messages: Messages): Html = Html {
    val lines = Seq(noMarginParagraph(StringEscapeUtils.escapeXml11(ad.addressLine1)))

    val optionalLines: Seq[Option[Html]] = Seq(
      ad.addressLine2.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      ad.addressLine3.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      ad.addressLine4.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      ad.postalCode.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      transformCountryCodeToOptionalLabel(Some(ad.countryCode)).map(
        s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))
      )
    )
    (lines ++ optionalLines.flatten).map(_.toString).mkString("")
  }

  private def contactDetailsHtml(details: ContactDetailsModel)(implicit messages: Messages): Html = Html {
    val lines: Seq[Option[Html]] = Seq(
      details.street.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      details.city.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      details.postcode.map(s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))),
      transformCountryCodeToOptionalLabel(details.countryCode).map(
        s => noMarginParagraph(StringEscapeUtils.escapeXml11(s))
      )
    )
    lines.flatten.map(_.toString).mkString("")
  }

  def summaryListRow(key: String, value: Option[Html], call: Option[Call] = None, classes: String = "")(implicit
    messages: Messages
  ) =
    SummaryListRow(
      key = Key(content = Text(messages(key))),
      value = Value(content = HtmlContent(value.getOrElse("").toString)),
      actions = call.flatMap(
        c =>
          Some(
            Actions(items =
              Seq(
                ActionItem(
                  href = c.url,
                  content = Text(messages("cds.form.change")),
                  visuallyHiddenText = Some(messages(key))
                )
              )
            )
          )
      ),
      classes = classes
    )

  private def summaryListRowNoChangeOption(
    key: String,
    value: Option[Html],
    call: Option[Call] = None,
    classes: String = ""
  )(implicit messages: Messages) =
    SummaryListRow(
      key = Key(content = Text(messages(key))),
      value = Value(content = HtmlContent(value.getOrElse("").toString)),
      actions = None,
      classes = classes
    )

  private def euCountry(countryCode: String)(implicit messages: Messages) = messages(
    messageKeyForEUCountryCode(countryCode)
  )

  private def messageKeyForEUCountryCode(countryCode: String) = s"cds.country.$countryCode"

  private def isEUCountryCode(countryCode: String)(implicit messages: Messages) =
    messages.isDefinedAt(messageKeyForEUCountryCode(countryCode))

  private def transformCountryCodeToOptionalLabel(code: Option[String])(implicit messages: Messages) = code match {
    case Some(MatchingForms.countryCodeGB) => Some(messages("cds.country.GB"))
    case Some(c) if isEUCountryCode(c)     => Some(euCountry(c))
    case Some(nonEuCode)                   => Some(nonEuCode)
    case _                                 => None
  }

  private def formatDate(date: LocalDate)(implicit messages: Messages) = dateFormatter.formatLocalDate(date)

}
