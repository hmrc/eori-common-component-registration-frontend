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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.Logging
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  ConfirmContactDetailsController,
  DetermineReviewPageController,
  OrganisationTypeController,
  SignInWithDifferentDetailsController,
  SubscriptionRecoveryController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  EtmpOrganisationType,
  LLP,
  Partnership,
  RegistrationDetails,
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation,
  UnincorporatedBody
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{
  AddressViewModel,
  No,
  WrongAddress,
  Yes,
  YesNoWrong,
  YesNoWrongAddress
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.confirm_contact_details
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmContactDetailsService @Inject() (
  sessionCache: SessionCache,
  registrationConfirmService: RegistrationConfirmService,
  orgTypeLookup: OrgTypeLookup,
  requestSessionData: RequestSessionData,
  confirmContactDetailsView: confirm_contact_details,
  subscriptionFlowManager: SubscriptionFlowManager,
  taxEnrolmentsService: TaxEnrolmentsService
)(implicit ec: ExecutionContext)
    extends Logging {

  def checkAddressDetails(
    service: Service,
    isInReviewMode: Boolean,
    areDetailsCorrectAnswer: YesNoWrongAddress
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] =
    for {
      subDetails <- sessionCache.subscriptionDetails
      regDetails <- sessionCache.registrationDetails
      _          <- sessionCache.saveSubscriptionDetails(subDetails.copy(addressDetails = Some(concatenateAddress(regDetails))))
      result     <- determineRoute(areDetailsCorrectAnswer.areDetailsCorrect, service, isInReviewMode)
    } yield result

  private def concatenateAddress(registrationDetails: RegistrationDetails): AddressViewModel =
    AddressViewModel(registrationDetails.address)

  private def determineRoute(detailsCorrect: YesNoWrong, service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] =
    detailsCorrect match {
      case Yes =>
        registrationConfirmService.currentSubscriptionStatus(hc, service, request) flatMap {
          case NewSubscription | SubscriptionRejected =>
            onNewSubscription(service, isInReviewMode)
          case SubscriptionProcessing =>
            Future.successful(Redirect(ConfirmContactDetailsController.processing(service)))
          case SubscriptionExists =>
            onExistingSubscription(service)
          case status =>
            val error = s"Invalid subscription status : $status"
            // $COVERAGE-OFF$Loggers
            logger.warn(error)
            // $COVERAGE-ON
            throw new IllegalStateException(error)
        }
      case No =>
        registrationConfirmService
          .clearRegistrationData()
          .map(
            _ =>
              Redirect(
                uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.OrganisationTypeController
                  .form(service)
              )
          )
      case WrongAddress =>
        Future.successful(
          Redirect(
            uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.YouCannotChangeAddressController
              .page(service)
          )
        )

      case _ =>
        val error = "YesNoWrongAddressForm field somehow had a value that wasn't yes, no, wrong address, or empty"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
    }

  private def onNewSubscription(service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    lazy val noSelectedOrganisationType =
      requestSessionData.userSelectedOrganisationType.isEmpty
    if (isInReviewMode)
      Future.successful(Redirect(DetermineReviewPageController.determineRoute(service)))
    else
      sessionCache.registrationDetails flatMap {
        case _: RegistrationDetailsIndividual if noSelectedOrganisationType =>
          Future.successful(
            Redirect(
              uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmIndividualTypeController
                .form(service)
            )
          )
        case _ =>
          subscriptionFlowManager.startSubscriptionFlow(service).map {
            case (page, newSession) => Redirect(page.url(service)).withSession(newSession)
          }
      }
  }

  private def onExistingSubscription(
    service: Service
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] =
    for {
      regDetails      <- sessionCache.registrationDetails
      enrolmentExists <- taxEnrolmentsService.doesPreviousEnrolmentExists(regDetails.safeId)
    } yield
      if (enrolmentExists)
        Redirect(SignInWithDifferentDetailsController.form(service))
      else
        Redirect(SubscriptionRecoveryController.complete(service))

  private def isPartnershipOrLLP(orgType: Option[EtmpOrganisationType]) =
    orgType.contains(Partnership) || orgType.contains(LLP)

  private def isIndividual(orgType: Option[EtmpOrganisationType]) = orgType.isEmpty

  private def isCharityPublicBodyNotForProfit(orgType: Option[EtmpOrganisationType]) =
    orgType.contains(UnincorporatedBody)

  private def isEUCountryCode(countryCode: String)(implicit messages: Messages) =
    messages.isDefinedAt(messageKeyForEUCountryCode(countryCode))

  private def messageKeyForEUCountryCode(countryCode: String) = s"cds.country.$countryCode"

  private def pageTitleAndHeading(orgType: Option[EtmpOrganisationType], isUk: Boolean = true)(implicit
    messages: Messages
  ) =
    orgType match {
      case orgType if isPartnershipOrLLP(orgType) => messages("confirm-business-details.partnership.title-and-heading")
      case orgType if isIndividual(orgType)       => messages("confirm-business-details.individual.title-and-heading")
      case _ if !isUk                             => messages("confirm-business-details.row.title-and-heading")
      case orgType if isCharityPublicBodyNotForProfit(orgType) =>
        messages("confirm-business-details.row.title-and-heading")
      case _ => messages("confirm-business-details.title-and-heading")
    }

  private def countryCodeToLabel(countryCode: String)(implicit messages: Messages) = countryCode match {
    case MatchingForms.countryCodeGB   => messages("cds.country.GB")
    case code if isEUCountryCode(code) => messageKeyForEUCountryCode(countryCode)
    case nonEuCode                     => nonEuCode
  }

  private def displayInputRadioGroupOptions(orgType: Option[EtmpOrganisationType])(implicit messages: Messages) =
    Seq("yes" -> messages("confirm-business-details.yes"), "wrong-address" -> radioGroupWrongAddressText(orgType))

  private def radioGroupWrongAddressText(orgType: Option[EtmpOrganisationType])(implicit messages: Messages): String =
    orgType match {
      case orgType if isPartnershipOrLLP(orgType) => messages("confirm-business-details.partnership.yes-wrong-address")
      case orgType if isIndividual(orgType)       => messages("confirm-business-details.individual.yes-wrong-address")
      case _                                      => messages("confirm-business-details.yes-wrong-address")
    }

  def handleAddressAndPopulateView(service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent],
    messages: Messages
  ): Future[Result] =
    sessionCache.registrationDetails.flatMap {
      case individual: RegistrationDetailsIndividual =>
        if (!individual.address.isValidAddress)
          Future.successful(
            Redirect(
              uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressInvalidController
                .page(service)
            )
          )
        else
          Future.successful(
            Ok(
              confirmContactDetailsView(
                isInReviewMode,
                concatenateAddress(individual),
                YesNoWrongAddress.createForm(),
                service,
                pageTitleAndHeading(None),
                countryCodeToLabel(concatenateAddress(individual).countryCode),
                displayInputRadioGroupOptions(None)
              )
            )
          )
      case org: RegistrationDetailsOrganisation =>
        if (!org.address.isValidAddress)
          Future.successful(
            Redirect(
              uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressInvalidController
                .page(service)
            )
          )
        else
          orgTypeLookup.etmpOrgTypeOpt.flatMap {
            case Some(ot) =>
              Future.successful(
                Ok(
                  confirmContactDetailsView(
                    isInReviewMode,
                    concatenateAddress(org),
                    YesNoWrongAddress.createForm(),
                    service,
                    pageTitleAndHeading(
                      Some(ot),
                      requestSessionData.selectedUserLocation.getOrElse("uk").equalsIgnoreCase("uk")
                    ),
                    countryCodeToLabel(concatenateAddress(org).countryCode),
                    displayInputRadioGroupOptions(Some(ot))
                  )
                )
              )
            case None =>
              // $COVERAGE-OFF$Loggers
              logger.warn("[ConfirmContactDetailsController.form] organisation type None")
              // $COVERAGE-ON
              sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service)))
          }
      case _ =>
        // $COVERAGE-OFF$Loggers
        logger.warn("[ConfirmContactDetailsController.form] registrationDetails not found")
        // $COVERAGE-ON
        sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service)))
    }

  def handleFormWithErrors(
    isInReviewMode: Boolean = false,
    formWithErrors: Form[YesNoWrongAddress],
    service: Service
  )(implicit request: Request[AnyContent], messages: Messages): Future[Result] =
    sessionCache.registrationDetails.flatMap {
      case individual: RegistrationDetailsIndividual =>
        Future.successful(
          BadRequest(
            confirmContactDetailsView(
              isInReviewMode,
              concatenateAddress(individual),
              formWithErrors,
              service,
              pageTitleAndHeading(None),
              countryCodeToLabel(concatenateAddress(individual).countryCode),
              displayInputRadioGroupOptions(None)
            )
          )
        )
      case org: RegistrationDetailsOrganisation =>
        orgTypeLookup.etmpOrgTypeOpt.flatMap {
          case Some(ot) =>
            Future.successful(
              BadRequest(
                confirmContactDetailsView(
                  isInReviewMode,
                  concatenateAddress(org),
                  formWithErrors,
                  service,
                  pageTitleAndHeading(Some(ot)),
                  countryCodeToLabel(concatenateAddress(org).countryCode),
                  displayInputRadioGroupOptions(Some(ot))
                )
              )
            )
          case None =>
            // $COVERAGE-OFF$Loggers
            logger.warn("[ConfirmContactDetailsController.submit] organisation type None")
            // $COVERAGE-ON
            sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service)))
        }
      case _ =>
        // $COVERAGE-OFF$Loggers
        logger.warn("[ConfirmContactDetailsController.submit] registrationDetails not found")
        // $COVERAGE-ON
        sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service)))
    }

}
