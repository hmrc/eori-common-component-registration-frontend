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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationConfirmService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.confirm_contact_details
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{
  sub01_outcome_processing,
  sub01_outcome_rejected
}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmContactDetailsController @Inject() (
  authAction: AuthAction,
  registrationConfirmService: RegistrationConfirmService,
  requestSessionData: RequestSessionData,
  sessionCache: SessionCache,
  orgTypeLookup: OrgTypeLookup,
  subscriptionFlowManager: SubscriptionFlowManager,
  mcc: MessagesControllerComponents,
  confirmContactDetailsView: confirm_contact_details,
  sub01OutcomeProcessingView: sub01_outcome_processing,
  sub01OutcomeRejected: sub01_outcome_rejected
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val logger = Logger(this.getClass)

  def form(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => implicit loggedInUser =>
      sessionCache.registrationDetails.flatMap {
        case individual: RegistrationDetailsIndividual =>
          if (!individual.address.isValidAddress())
            checkAddressDetails(journey, service, YesNoWrongAddress(Some("wrong-address")))
          else
            Future.successful(
              Ok(
                confirmContactDetailsView(
                  individual.name,
                  concatenateAddress(individual),
                  individual.customsId,
                  None,
                  YesNoWrongAddress.createForm(),
                  service,
                  journey
                )
              )
            )
        case org: RegistrationDetailsOrganisation =>
          if (!org.address.isValidAddress())
            checkAddressDetails(journey, service, YesNoWrongAddress(Some("wrong-address")))
          else
            orgTypeLookup.etmpOrgTypeOpt.flatMap {
              case Some(ot) =>
                Future.successful(
                  Ok(
                    confirmContactDetailsView(
                      org.name,
                      concatenateAddress(org),
                      org.customsId,
                      Some(ot),
                      YesNoWrongAddress.createForm(),
                      service,
                      journey
                    )
                  )
                )
              case None =>
                logger.warn("[ConfirmContactDetailsController.form] organisation type None")
                sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service, journey)))
            }
        case _ =>
          logger.warn("[ConfirmContactDetailsController.form] registrationDetails not found")
          sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service, journey)))
      }
    }

  def submit(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => implicit loggedInUser =>
      YesNoWrongAddress
        .createForm()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            sessionCache.registrationDetails.flatMap {
              case individual: RegistrationDetailsIndividual =>
                Future.successful(
                  BadRequest(
                    confirmContactDetailsView(
                      individual.name,
                      concatenateAddress(individual),
                      individual.customsId,
                      None,
                      formWithErrors,
                      service,
                      journey
                    )
                  )
                )
              case org: RegistrationDetailsOrganisation =>
                orgTypeLookup.etmpOrgTypeOpt.flatMap {
                  case Some(ot) =>
                    Future.successful(
                      BadRequest(
                        confirmContactDetailsView(
                          org.name,
                          concatenateAddress(org),
                          org.customsId,
                          Some(ot),
                          formWithErrors,
                          service,
                          journey
                        )
                      )
                    )
                  case None =>
                    logger.warn("[ConfirmContactDetailsController.submit] organisation type None")
                    sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service, journey)))
                }
              case _ =>
                logger.warn("[ConfirmContactDetailsController.submit] registrationDetails not found")
                sessionCache.remove.map(_ => Redirect(OrganisationTypeController.form(service, journey)))
            },
          areDetailsCorrectAnswer => checkAddressDetails(journey, service, areDetailsCorrectAnswer)
        )
    }

  private def checkAddressDetails(
    journey: Journey.Value,
    service: Service,
    areDetailsCorrectAnswer: YesNoWrongAddress
  )(implicit request: Request[AnyContent], loggedInUser: LoggedInUserWithEnrolments): Future[Result] =
    sessionCache.subscriptionDetails.flatMap { subDetails =>
      sessionCache.registrationDetails.flatMap { details =>
        sessionCache
          .saveSubscriptionDetails(subDetails.copy(addressDetails = Some(concatenateAddress(details))))
          .flatMap { _ =>
            determineRoute(areDetailsCorrectAnswer.areDetailsCorrect, service, journey)
          }
      }
    }

  def processing(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        name          <- sessionCache.registrationDetails.map(_.name)
        processedDate <- sessionCache.sub01Outcome.map(_.processedDate)
      } yield Ok(sub01OutcomeProcessingView(Some(name), processedDate))
  }

  def rejected(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        name          <- sessionCache.registrationDetails.map(_.name)
        processedDate <- sessionCache.sub01Outcome.map(_.processedDate)
      } yield Ok(sub01OutcomeRejected(Some(name), processedDate, service))
  }

  private def determineRoute(detailsCorrect: YesNoWrong, service: Service, journey: Journey.Value)(implicit
    request: Request[AnyContent],
    loggedInUser: LoggedInUserWithEnrolments
  ): Future[Result] =
    detailsCorrect match {
      case Yes =>
        registrationConfirmService.currentSubscriptionStatus flatMap {
          case NewSubscription | SubscriptionRejected =>
            onNewSubscription(service, journey)
          case SubscriptionProcessing =>
            Future.successful(Redirect(ConfirmContactDetailsController.processing(service)))
          case SubscriptionExists =>
            Future.successful(Redirect(SubscriptionRecoveryController.complete(service, journey)))
          case status =>
            throw new IllegalStateException(s"Invalid subscription status : $status")
        }
      case No =>
        registrationConfirmService
          .clearRegistrationData(loggedInUser)
          .map(
            _ =>
              Redirect(
                uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.OrganisationTypeController
                  .form(service, journey)
              )
          )
      case WrongAddress =>
        Future.successful(
          Redirect(
            uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressController
              .createForm(service, journey)
          )
        )
      case _ =>
        throw new IllegalStateException(
          "YesNoWrongAddressForm field somehow had a value that wasn't yes, no, wrong address, or empty"
        )
    }

  private def onNewSubscription(service: Service, journey: Journey.Value)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    lazy val noSelectedOrganisationType =
      requestSessionData.userSelectedOrganisationType.isEmpty
    sessionCache.registrationDetails flatMap {
      case _: RegistrationDetailsIndividual if noSelectedOrganisationType =>
        Future.successful(
          Redirect(
            uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.ConfirmIndividualTypeController
              .form(service, journey)
          )
        )

      case _ =>
        subscriptionFlowManager.startSubscriptionFlow(service, journey).map {
          case (page, newSession) => Redirect(page.url(service)).withSession(newSession)
        }
    }
  }

  private def concatenateAddress(registrationDetails: RegistrationDetails): AddressViewModel =
    AddressViewModel(registrationDetails.address)

}
