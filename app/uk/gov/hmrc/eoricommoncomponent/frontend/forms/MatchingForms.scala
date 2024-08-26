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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.Logging
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

import java.time.LocalDate

object MatchingForms extends Mappings with Logging {

  val Length35            = 35
  private val Length2     = 2
  private val nameRegex   = "[a-zA-Z0-9-' ]*"
  private val noTagsRegex = "^[^<>]+$"

  private def validUtrFormat(utr: Option[String]): Boolean = {

    val ZERO  = 0
    val ONE   = 1
    val TWO   = 2
    val THREE = 3
    val FOUR  = 4
    val FIVE  = 5
    val SIX   = 6
    val SEVEN = 7
    val EIGHT = 8
    val NINE  = 9
    val TEN   = 10

    def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
      val mapOfRemainders = Map(
        ZERO  -> TWO,
        ONE   -> ONE,
        TWO   -> NINE,
        THREE -> EIGHT,
        FOUR  -> SEVEN,
        FIVE  -> SIX,
        SIX   -> FIVE,
        SEVEN -> FOUR,
        EIGHT -> THREE,
        NINE  -> TWO,
        TEN   -> ONE
      )
      mapOfRemainders.get(remainder).contains(checkDigit)
    }

    utr match {
      case Some(u) =>
        val utrWithoutK = u.trim.stripSuffix("K").stripSuffix("k")
        utrWithoutK.length == TEN && utrWithoutK.forall(_.isDigit) && {
          val actualUtr   = utrWithoutK.toList
          val checkDigit  = actualUtr.head.asDigit
          val restOfUtr   = actualUtr.tail
          val weights     = List(SIX, SEVEN, EIGHT, NINE, TEN, FIVE, FOUR, THREE, TWO)
          val weightedUtr = for ((w1, u1) <- weights zip restOfUtr) yield w1 * u1.asDigit
          val total       = weightedUtr.sum
          val remainder   = total % 11
          isValidUtr(remainder, checkDigit)
        }
      case None => false
    }
  }

  private def validFamilyName: Constraint[String] =
    Constraint("constraints.last-name")({
      case s if s.isEmpty => Invalid(ValidationError("cds.subscription.family-name.error.empty"))
      case s if !s.matches(nameRegex) =>
        Invalid(ValidationError("cds.subscription.family-name.error.wrong-format"))
      case s if s.length > 35 => Invalid(ValidationError("cds.subscription.family-name.error.too-long"))
      case _                  => Valid
    })

  private def validGivenName: Constraint[String] =
    Constraint("constraints.first-name")({
      case s if s.isEmpty => Invalid(ValidationError("cds.subscription.given-name.error.empty"))
      case s if !s.matches(nameRegex) =>
        Invalid(ValidationError("cds.subscription.given-name.error.wrong-format"))
      case s if s.length > 35 => Invalid(ValidationError("cds.subscription.given-name.error.too-long"))
      case _                  => Valid
    })

  val organisationTypeDetailsForm: Form[CdsOrganisationType] = Form(
    "organisation-type" -> optional(text)
      .verifying(
        "cds.matching.organisation-type.page-error.organisation-type-field.error.required",
        x => x.fold(false)(oneOf(CdsOrganisationType.validOrganisationTypes.keySet).apply(_))
      )
      .transform[CdsOrganisationType](
        o =>
          CdsOrganisationType(
            CdsOrganisationType
              .forId(o.getOrElse {
                val error = "Could not create CdsOrganisationType for empty ID."
                // $COVERAGE-OFF$Loggers
                logger.warn(error)
                // $COVERAGE-ON
                throw new IllegalArgumentException(error)
              })
              .id
          ),
        x => Some(x.id)
      )
  )

  val userLocationForm: Form[UserLocation] = Form(
    "location" -> enumerable[UserLocation]("cds.registration.user-location.error.location")
  )

  private val validYesNoAnswerOptions = Set("true", "false")

  def disclosePersonalDetailsYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] =
    createYesNoAnswerForm("cds.subscription.organisation-disclose-personal-details-consent.error.yes-no-answer")

  def contactAddressDetailsYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] =
    createYesNoAnswerForm("ecc.contact-address-details.error.yes-no-answer")

  def vatRegisteredUkYesNoAnswerForm(isPartnership: Boolean = false)(implicit messages: Messages): Form[YesNo] =
    if (isPartnership) createYesNoAnswerForm("cds.registration.vat-registered-uk.partnership.error.yes-no-answer")
    else createYesNoAnswerForm("cds.registration.vat-registered-uk.error.yes-no-answer")

  def vatGroupYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] =
    createYesNoAnswerForm("cds.subscription.vat-group.page-error.yes-no-answer")

  def vatVerificationOptionAnswerForm()(implicit messages: Messages): Form[VatVerificationOption] =
    createVatVerificationOptionAnswerForm("cds.subscription.vat-verification-option.error")

  private def createVatVerificationOptionAnswerForm(
    invalidErrorMsgKey: String
  )(implicit messages: Messages): Form[VatVerificationOption] = Form(
    mapping(
      "vat-verification-option" -> optional(
        text.verifying(messages(invalidErrorMsgKey), oneOf(validYesNoAnswerOptions))
      )
        .verifying(messages(invalidErrorMsgKey), _.isDefined)
        .transform[Boolean](str => str.get.toBoolean, bool => Option(String.valueOf(bool)))
    )(VatVerificationOption.apply)(VatVerificationOption.unapply)
  )

  private def createYesNoAnswerForm(invalidErrorMsgKey: String)(implicit messages: Messages): Form[YesNo] = Form(
    mapping(
      YesNo.yesAndNoAnswer -> optional(text.verifying(messages(invalidErrorMsgKey), oneOf(validYesNoAnswerOptions)))
        .verifying(messages(invalidErrorMsgKey), _.isDefined)
        .transform[Boolean](str => str.get.toBoolean, bool => Option(String.valueOf(bool)))
    )(YesNo.apply)(YesNo.unapply)
  )

  private def validBusinessName: Constraint[String] =
    Constraint({
      case s if s.isEmpty      => Invalid(ValidationError("cds.matching-error.business-details.business-name.isEmpty"))
      case s if s.length > 105 => Invalid(ValidationError("cds.matching-error.business-details.business-name.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching-error.business-details.business-name.invalid-chars"))
      case _ => Valid
    })

  private def validPartnershipName: Constraint[String] =
    Constraint({
      case s if s.isEmpty => Invalid(ValidationError("cds.matching-error.business-details.partnership-name.isEmpty"))
      case s if s.length > 105 =>
        Invalid(ValidationError("cds.matching-error.business-details.partnership-name.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching-error.business-details.partnership-name.invalid-chars"))
      case _ => Valid
    })

  private def validCompanyName: Constraint[String] =
    Constraint({
      case s if s.isEmpty      => Invalid(ValidationError("cds.matching-error.business-details.company-name.isEmpty"))
      case s if s.length > 105 => Invalid(ValidationError("cds.matching-error.business-details.company-name.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching-error.business-details.company-name.invalid-chars"))
      case _ => Valid
    })

  private def validOrganisationName: Constraint[String] =
    Constraint({
      case s if s.isEmpty      => Invalid(ValidationError("cds.matching.organisation-name.error.name"))
      case s if s.length > 105 => Invalid(ValidationError("cds.matching-error.business-details.business-name.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching-error.business-details.business-name.invalid-chars"))
      case _ => Valid
    })

  private def validUtr: Constraint[String] = {

    def validLength: String => Boolean = s => s.length == 10 || (s.endsWith("k") || s.endsWith("K") && s.length == 11)

    Constraint({
      case s if formatInput(s).isEmpty                => Invalid(ValidationError("cds.matching-error.business-details.utr.isEmpty"))
      case s if !validLength(formatInput(s))          => Invalid(ValidationError("cds.matching-error.utr.length"))
      case s if !validUtrFormat(Some(formatInput(s))) => Invalid(ValidationError("cds.matching-error.utr.invalid"))
      case _                                          => Valid
    })
  }

  val nameUtrOrganisationForm: Form[NameIdOrganisationMatchModel] = Form(
    mapping("name" -> text.verifying(validBusinessName), "utr" -> text.verifying(validUtr))(
      NameIdOrganisationMatchModel.apply
    )(NameIdOrganisationMatchModel.unapply)
  )

  val nameUtrPartnershipForm: Form[NameIdOrganisationMatchModel] = Form(
    mapping("name" -> text.verifying(validPartnershipName), "utr" -> text.verifying(validUtr))(
      NameIdOrganisationMatchModel.apply
    )(NameIdOrganisationMatchModel.unapply)
  )

  val nameUtrCompanyForm: Form[NameIdOrganisationMatchModel] = Form(
    mapping("name" -> text.verifying(validCompanyName), "utr" -> text.verifying(validUtr))(
      NameIdOrganisationMatchModel.apply
    )(NameIdOrganisationMatchModel.unapply)
  )

  val ninoForm: Form[NinoMatch] =
    Form(
      mapping(
        "first-name" -> text.verifying(validFirstName),
        "last-name"  -> text.verifying(validLastName),
        validateDateOfBirth,
        "nino" -> text.verifying(validNino)
      )(NinoMatch.apply)(NinoMatch.unapply)
    )

  val enterNameDobForm: Form[NameDobMatchModel] =
    Form(
      mapping(
        "first-name" -> text.verifying(validFirstName),
        "last-name"  -> text.verifying(validLastName),
        validateDateOfBirth
      )(NameDobMatchModel.apply)(NameDobMatchModel.unapply)
    )

  private def validateDateOfBirth = {

    val minimumDate = LocalDate.of(DateConverter.earliestYearDateOfBirth, 1, 1)
    val today       = LocalDate.now()

    "date-of-birth" -> localDate(emptyKey = "dob.error.empty-date", invalidKey = "dob.error.invalid-date").verifying(
      minDate(minimumDate, "dob.error.minMax", DateConverter.earliestYearDateOfBirth.toString)
    )
      .verifying(maxDate(today, "dob.error.minMax", DateConverter.earliestYearDateOfBirth.toString))
  }

  private def validFirstName: Constraint[String] =
    Constraint("constraints.first-name")({
      case s if s.isEmpty => Invalid(ValidationError("cds.subscription.first-name.error.empty"))
      case s if !s.matches(nameRegex) =>
        Invalid(ValidationError("cds.subscription.first-name.error.wrong-format"))
      case s if s.length > 35 => Invalid(ValidationError("cds.subscription.first-name.error.too-long"))
      case _                  => Valid
    })

  private def validLastName: Constraint[String] =
    Constraint("constraints.last-name")({
      case s if s.isEmpty => Invalid(ValidationError("cds.subscription.last-name.error.empty"))
      case s if !s.matches(nameRegex) =>
        Invalid(ValidationError("cds.subscription.last-name.error.wrong-format"))
      case s if s.length > 35 => Invalid(ValidationError("cds.subscription.last-name.error.too-long"))
      case _                  => Valid
    })

  private def validNino: Constraint[String] =
    Constraint({
      case s if formatInput(s).isEmpty                  => Invalid(ValidationError("cds.subscription.nino.error.empty"))
      case s if formatInput(s).length != 9              => Invalid(ValidationError("cds.subscription.nino.error.wrong-length"))
      case s if !formatInput(s).matches("[a-zA-Z0-9]*") => Invalid(ValidationError("cds.matching.nino.invalid"))
      case s if !Nino.isValid(formatInput(s))           => Invalid(ValidationError("cds.matching.nino.invalid"))
      case _                                            => Valid
    })

  val subscriptionNinoForm: Form[IdMatchModel] = Form(
    mapping("nino" -> text.verifying(validNino))(IdMatchModel.apply)(IdMatchModel.unapply)
  )

  val subscriptionUtrForm: Form[IdMatchModel] = Form(
    mapping("utr" -> text.verifying(validUtr))(IdMatchModel.apply)(IdMatchModel.unapply)
  )

  val ninoOrUtrChoiceForm: Form[NinoOrUtrChoice] = Form(
    mapping(
      "ninoOrUtrRadio" -> optional(text)
        .verifying("cds.subscription.nino.utr.invalid", _.fold(false)(x => x.trim.nonEmpty))
    )(NinoOrUtrChoice.apply)(NinoOrUtrChoice.unapply)
  )

  val countryCodeGB = "GB"
  val countryCodeGG = "GG"
  val countryCodeJE = "JE"
  val countryCodeIM = "IM"

  private val rejectGB: Constraint[String] = Constraint {
    case `countryCodeGB` => Invalid("cds.matching-error.country.unacceptable")
    case _               => Valid
  }

  val thirdCountrySixLineAddressForm: Form[SixLineAddressMatchModel] = sixLineAddressFormFactory(rejectGB)

  val channelIslandSixLineAddressForm: Form[SixLineAddressMatchModel] =
    Form(
      mapping(
        "line-1"      -> text.verifying(validLine1),
        "line-2"      -> optional(text.verifying(validLine2)),
        "line-3"      -> text.verifying(validLine3),
        "line-4"      -> optional(text.verifying(validLine4)),
        "postcode"    -> mandatoryOptPostCodeMapping,
        "countryCode" -> mandatoryString("cds.matching-error.country.invalid")(s => s.length == Length2)
      )(SixLineAddressMatchModel.apply)(SixLineAddressMatchModel.unapply)
    )

  val ukSixLineAddressForm: Form[SixLineAddressMatchModel] =
    Form(
      mapping(
        "line-1"      -> text.verifying(validLine1),
        "line-2"      -> optional(text.verifying(validLine2)),
        "line-3"      -> text.verifying(validLine3),
        "line-4"      -> optional(text.verifying(validLine4)),
        "postcode"    -> mandatoryOptPostCodeMapping,
        "countryCode" -> default(text, countryCodeGB)
      )(SixLineAddressMatchModel.apply)(SixLineAddressMatchModel.unapply)
    )

  private def sixLineAddressFormFactory(countryConstraints: Constraint[String]*): Form[SixLineAddressMatchModel] =
    Form(
      mapping(
        "line-1"   -> text.verifying(validLine1),
        "line-2"   -> optional(text.verifying(validLine2)),
        "line-3"   -> text.verifying(validLine3),
        "line-4"   -> optional(text.verifying(validLine4)),
        "postcode" -> postcodeMapping,
        "countryCode" -> mandatoryString("cds.matching-error.country.invalid")(s => s.length == Length2)
          .verifying(countryConstraints: _*)
      )(SixLineAddressMatchModel.apply)(SixLineAddressMatchModel.unapply)
    )

  private def validLine1: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching.organisation-address.line-1.error.empty"))
      case s if s.trim.length > 35 =>
        Invalid(ValidationError("cds.matching.organisation-address.line-1.error.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching.organisation-address.line.error.invalid-chars"))
      case _ => Valid
    })

  private def validLine2: Constraint[String] =
    Constraint({
      case s if s.trim.length > 34 =>
        Invalid(ValidationError("cds.matching.organisation-address.line-2.error.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching.organisation-address.line.error.invalid-chars"))
      case _ => Valid
    })

  private def validLine3: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching.organisation-address.line-3.error.empty"))
      case s if s.trim.length > 34 =>
        Invalid(ValidationError("cds.matching.organisation-address.line-3.error.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching.organisation-address.line.error.invalid-chars"))
      case _ => Valid
    })

  private def validLine4: Constraint[String] =
    Constraint({
      case s if s.trim.length > 35 =>
        Invalid(ValidationError("cds.matching.organisation-address.line-4.error.too-long"))
      case s if !s.matches(noTagsRegex) =>
        Invalid(ValidationError("cds.matching.organisation-address.line.error.invalid-chars"))
      case _ => Valid
    })

  def createSixLineAddress(value: Address): SixLineAddressMatchModel =
    SixLineAddressMatchModel(
      value.addressLine1,
      value.addressLine2,
      value.addressLine3.getOrElse(""),
      value.addressLine4,
      value.postalCode,
      value.countryCode
    )

  val thirdCountryIndividualNameDateOfBirthForm: Form[IndividualNameAndDateOfBirth] =
    Form(
      mapping(
        "given-name"  -> text.verifying(validGivenName),
        "family-name" -> text.verifying(validFamilyName),
        validateDateOfBirth
      )(IndividualNameAndDateOfBirth.apply)(IndividualNameAndDateOfBirth.unapply)
    )

  val organisationNameForm: Form[NameMatchModel] = Form(
    mapping("name" -> text.verifying(validOrganisationName))(NameMatchModel.apply)(NameMatchModel.unapply)
  )

  private def validHaveUtr: Constraint[Option[Boolean]] =
    Constraint({
      case None => Invalid(ValidationError("cds.matching.organisation-utr.field-error.have-utr"))
      case _    => Valid
    })

  val haveUtrForm: Form[UtrMatchModel] = Form(
    mapping("have-utr" -> optional(boolean).verifying(validHaveUtr))(UtrMatchModel.apply)(model => Some(model.haveUtr))
  )

  private def validHaveNino: Constraint[Option[Boolean]] =
    Constraint({
      case None => Invalid(ValidationError("cds.matching.nino.row.yes-no.error"))
      case _    => Valid
    })

  val haveRowIndividualsNinoForm: Form[NinoMatchModel] = Form(
    mapping("have-nino" -> optional(boolean).verifying(validHaveNino))(NinoMatchModel.apply)(
      model => Some(model.haveNino)
    )
  )

}
