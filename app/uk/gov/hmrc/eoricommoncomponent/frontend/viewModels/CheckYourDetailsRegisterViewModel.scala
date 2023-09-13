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
import play.api.mvc.{AnyContent, Call, Request}
import play.twirl.api.Html
import play.twirl.api.utils.StringEscapeUtils
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, RegistrationDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, Nino, RegistrationDetailsIndividual, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.helpers.noMarginParagraph
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactDetailsController
import java.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers.DateFormatter
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, SummaryListRow, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Value}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class CheckYourDetailsRegisterViewModel(
  headerTitle: String,
  providedDetails: SummaryList,
  vatDetails: Seq[SummaryListRow],
  providedContactDetails: SummaryList
)

@Singleton
class CheckYourDetailsRegisterConstructor @Inject() (
  dateFormatter: DateFormatter,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData
) {

  def getDateOfEstablishmentLabel(cdsOrgType: Option[CdsOrganisationType])(implicit messages: Messages): String = {
    val isSoleTrader = cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
      cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader)
    if (isSoleTrader) messages("cds.date-of-birth.label")
    else messages("cds.date-established.label")
  }

  def orgNameLabel(cdsOrgType: Option[CdsOrganisationType], isPartnership: Boolean)(implicit
    messages: Messages
  ): String = {
    val orgNameLabel = cdsOrgType.contains(CdsOrganisationType.CharityPublicBodyNotForProfit) || cdsOrgType.contains(
      CdsOrganisationType.ThirdCountryOrganisation
    )
    (orgNameLabel, isPartnership) match {
      case (false, true) => messages("cds.partner-name.label")
      case (true, false) => messages("cds.organisation-name.label")
      case (_, _)        => messages("cds.business-name.label")

    }
  }

  private def businessDetailsLabel(isPartnership: Boolean, cdsOrgType: Option[CdsOrganisationType])(implicit
    messages: Messages
  ) = {
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
      case (_, _, _)            => messages("cds.form.business-details")
    }
  }

  def ninoOrUtrLabel(
    registration: RegistrationDetails,
    cdsOrgType: Option[CdsOrganisationType],
    isPartnership: Boolean
  )(implicit messages: Messages): String = {
    val soleAndIndividual = {
      cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
      cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader) ||
      cdsOrgType.contains(CdsOrganisationType.Individual) ||
      cdsOrgType.contains(CdsOrganisationType.EUIndividual) ||
      cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)
    }

    registration.customsId match {
      case Some(Utr(_)) =>
        if (soleAndIndividual)
          messages("cds.utr.label")
        else if (cdsOrgType.contains(CdsOrganisationType.LimitedLiabilityPartnership))
          messages("cds.matching.name-id-organisation.company.utr")
        else if (isPartnership)
          messages("cds.check-your-details.utrnumber.partnership")
        else
          messages("cds.company.utr.label")
      case Some(Nino(_)) => messages("cds.nino.label")
      case Some(Eori(_)) => messages("cds.subscription.enter-eori-number.eori-number.label")
      case _             => messages("cds.nino.label")
    }
  }

  def generateViewModel(service: Service)(implicit
    messages: Messages,
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Option[CheckYourDetailsRegisterViewModel]] =
    for {
      registration <- sessionCache.registrationDetails
      subscription <- sessionCache.subscriptionDetails
    } yield {

      val cdsOrgType    = requestSessionData.userSelectedOrganisationType
      val isPartnership = requestSessionData.isPartnershipOrLLP

      val isIndividual: Boolean = cdsOrgType.contains(CdsOrganisationType.Individual) ||
        cdsOrgType.contains(CdsOrganisationType.EUIndividual) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)
      val isCharity = cdsOrgType.contains(CdsOrganisationType.CharityPublicBodyNotForProfit)
      val isSoleTrader = cdsOrgType.contains(CdsOrganisationType.SoleTrader) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader)
      val isRowOrganisation = cdsOrgType.contains(CdsOrganisationType.ThirdCountryOrganisation)

      val headerTitle =
        if (isPartnership)
          messages("cds.form.check-answers-partnership-details")
        else if (isIndividual || isSoleTrader)
          messages("cds.form.check-answers-your-details")
        else if (isCharity || isRowOrganisation)
          messages("cds.form.check-answers-organisation-details")
        else
          messages("cds.form.check-answers-company-details")

      val providedDetails = getProvidedDetails(
        isIndividual,
        isSoleTrader,
        isRowOrganisation,
        isPartnership,
        subscription,
        registration,
        cdsOrgType,
        service
      )

      for {
        providedDetailsList <- providedDetails
        vatDetails          <- getVatDetails(isIndividual, subscription, service)
        providedContactDetails = getProvidedContactDetails(subscription, service)
      } yield CheckYourDetailsRegisterViewModel(headerTitle, providedDetailsList, vatDetails, providedContactDetails)
    }

  def getProvidedDetails(
    isIndividual: Boolean,
    isSoleTrader: Boolean,
    isRowOrganisation: Boolean,
    isPartnership: Boolean,
    subscription: SubscriptionDetails,
    registration: RegistrationDetails,
    cdsOrgType: Option[CdsOrganisationType],
    service: Service
  )(implicit messages: Messages): Option[SummaryList] = {

    def individualName: Option[String] = subscription.nameDobDetails.map(_.name) orElse Option(registration.name)
    def orgName: Option[String]        = subscription.nameOrganisationDetails.map(_.name) orElse subscription.name
    val nameOpt: Option[String]        = if (isIndividual || isSoleTrader) individualName else orgName

    nameOpt.map { name =>
      val isRowSoleTraderIndividual = cdsOrgType.contains(CdsOrganisationType.ThirdCountrySoleTrader) ||
        cdsOrgType.contains(CdsOrganisationType.ThirdCountryIndividual)

      val isUserIdentifiedByRegService = registration.safeId.id.nonEmpty

      val personalDataDisclosureConsent = subscription.personalDataDisclosureConsent.getOrElse(false)

      val formattedIndividualDateOfBirth = {
        val dateOfBirth: Option[LocalDate] = (subscription.nameDobDetails, registration) match {
          case (Some(nameDobDetails), _)                         => Some(nameDobDetails.dateOfBirth)
          case (None, individual: RegistrationDetailsIndividual) => Some(individual.dateOfBirth)
          case _                                                 => None
        }
        dateOfBirth.map(formatDate)
      }

      val orgType = cdsOrgType.fold("")(orgType => orgType.id)

      val eoriCheckerConsentYes =
        if (isPartnership)
          messages("cds.eori-checker-consent.partnership.yes")
        else if (isIndividual || isSoleTrader)
          messages("cds.eori-checker-consent.individual-or-sole-trader.yes")
        else
          messages("cds.yes")

      val individualNameDob =
        if (isIndividual || isSoleTrader)
          Seq(
            summaryListRow(
              key = messages("subscription.check-your-details.full-name.label"),
              value = Some(Html(StringEscapeUtils.escapeXml11(name))),
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
              value = Some(Text(name).asHtml),
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
          Seq(
            summaryListRow(
              key = ninoOrUtrLabel(registration, cdsOrgType, isPartnership),
              value = Some(Html(registration.customsId.get.id))
            )
          )
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
              call = cdsOrgType.map(_ => DoYouHaveNinoController.displayForm(service))
            )
          )
        else Seq.empty[SummaryListRow]

      val registeredAddress = {
        val (value, call) = (isUserIdentifiedByRegService, subscription.addressDetails) match {
          case (true, None) | (false, _) =>
            (
              Some(addressHtml(registration.address)),
              Some(SixLineAddressController.showForm(isInReviewMode = true, organisationType = orgType, service))
            )
          case (true, Some(address)) =>
            (
              Some(addressViewModelHtml(address)),
              Some(ConfirmContactDetailsController.form(service, isInReviewMode = true))
            )
        }
        Seq(summaryListRow(key = businessDetailsLabel(isPartnership, cdsOrgType), value = value, call = call))
      }

      val dateOfEstablishment = Seq(subscription.dateEstablished.map { establishmentDate =>
        summaryListRow(
          key = getDateOfEstablishmentLabel(cdsOrgType),
          value = Some(Html(formatDate(establishmentDate))),
          call = Some(DateOfEstablishmentController.reviewForm(service))
        )
      }).flatten

      val sicCodeDisplay = Seq(subscription.sicCode.map { sic =>
        summaryListRow(
          key = messages("cds.form.sic-code"),
          value = Some(Html(sic)),
          call = Some(SicCodeController.submit(isInReviewMode = true, service))
        )
      }).flatten

      val personalDataDisclosure = Seq(
        summaryListRow(
          key = messages("cds.form.disclosure"),
          value = Some(
            Html(
              if (personalDataDisclosureConsent) eoriCheckerConsentYes
              else messages("cds.no")
            )
          ),
          call = Some(DisclosePersonalDetailsConsentController.reviewForm(service))
        )
      )

      SummaryList(rows =
        individualNameDob ++
          organisationName ++
          organisationUtr ++
          customsId ++
          individualUtr ++
          registeredAddress ++
          dateOfEstablishment ++
          sicCodeDisplay ++
          personalDataDisclosure
      )

    }

  }

  def getProvidedContactDetails(subscription: SubscriptionDetails, service: Service)(implicit
    messages: Messages
  ): SummaryList = {
    val contactName = Seq(subscription.contactDetails.map { cd =>
      summaryListRow(
        key = messages("cds.form.check-answers.contact-name"),
        value = Some(Text(cd.fullName).asHtml),
        call =
          Some(ContactDetailsController.reviewForm(service))
      )
    }).flatten

    val email = Seq(
      summaryListRow(
        key = messages("subscription.enter-email.label"),
        value = subscription.contactDetails.map(cd => Html(cd.emailAddress)),
        call = None
      )
    )

    val contactTelephone = Seq(subscription.contactDetails.map { cd =>
      summaryListRow(
        key = messages("cds.form.check-answers.contact-telephone"),
        value = Some(Html(cd.telephone)),
        call =
          Some(ContactDetailsController.reviewForm(service))
      )
    }).flatten

    val details = Seq(subscription.contactDetails.map { contactDetails =>
      summaryListRow(
        key = messages("cds.form.customs-contact-address"),
        value = Some(contactDetailsHtml(contactDetails)),
        call =
          Some(uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactAddressController.reviewForm(service))
      )
    }).flatten

    SummaryList(rows =
      contactName ++
        email ++
        contactTelephone ++
        details
    )
  }

  def getVatDetails(isIndividual: Boolean, subscription: SubscriptionDetails, service: Service)(implicit
    messages: Messages
  ): Option[Seq[SummaryListRow]] =
    if (!isIndividual)
      for {
        controlListResponse <- subscription.vatControlListResponse
        vatDateOfReg        <- controlListResponse.dateOfReg
      } yield Seq(
        summaryListRowNoChangeOption(
          key = messages("cds.form.gb-vat-number"),
          value = Some(Html(subscription.ukVatDetails.map(_.number).getOrElse(messages("cds.not-entered.label")))),
          call = Some(VatRegisteredUkController.reviewForm(service))
        ),
        summaryListRowNoChangeOption(
          key = messages("cds.form.gb-vat-postcode"),
          value =
            Some(Html(subscription.ukVatDetails.map(_.postcode).getOrElse(messages("cds.not-entered.label")))),
          call = Some(VatRegisteredUkController.reviewForm(service))
        ),
        summaryListRowNoChangeOption(
          key = messages("cds.form.gb-vat-date"),
          value = Some(Html(formatDate(LocalDate.parse(vatDateOfReg)))),
          call = Some(VatRegisteredUkController.reviewForm(service))
        )
      )
    else Some(Seq.empty[SummaryListRow])

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
