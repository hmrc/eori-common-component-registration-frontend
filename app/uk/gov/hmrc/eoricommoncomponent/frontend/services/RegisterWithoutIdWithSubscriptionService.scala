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
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{SuccessResponse, TaxUDConnector}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.Sub02Controller
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.Sub02Controller
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.EmbassyId
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.ResponseCommon
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.ResponseCommon._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{RecipientDetails, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterWithoutIdWithSubscriptionService @Inject() (
  registerWithoutIdService: RegisterWithoutIdService,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  orgTypeLookup: OrgTypeLookup,
  sub02Controller: Sub02Controller,
  taxudConnector: TaxUDConnector,
  handleSubscriptionService: HandleSubscriptionService,
  save4LaterService: Save4LaterService
)(implicit ec: ExecutionContext)
    extends Logging {

  def rowRegisterWithoutIdWithSubscription(loggedInUser: LoggedInUserWithEnrolments, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent],
    messages: Messages
  ): Future[Result] = {

    def isRow = UserLocation.isRow(requestSessionData)

    def applicableForRegistration(rd: RegistrationDetails): Boolean = rd.safeId.id.isEmpty && isRow

    val userLocation = requestSessionData.selectedUserLocation.getOrElse(
      throw DataUnavailableException("unable to retrieve user's location")
    )

    sessionCache.registrationDetails flatMap {
      case rd if userLocation == UserLocation.Iom => createSubscription(loggedInUser, rd, userLocation, service)
      case rd if applicableForRegistration(rd)    => rowServiceCall(loggedInUser, service)
      case rd if rd.orgType.contains(EmbassyId)   => createSubscription(loggedInUser, rd, userLocation, service)
      case _                                      => createSubscription(service)(request)
    }
  }

  def createSubscription(service: Service)(implicit request: Request[AnyContent]): Future[Result] =
    sub02Controller.subscribe(service)(request)

  private def createSubscription(
    loggedInUser: LoggedInUserWithEnrolments,
    regDetails: RegistrationDetails,
    userLocation: UserLocation,
    service: Service
  )(implicit request: Request[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    if (regDetails.safeId.id.nonEmpty) {
      Future.successful(Redirect(Sub02Controller.eoriAlreadyExists(service)))
    } else {
      sessionCache.subscriptionDetails.flatMap { subDetails =>
        taxudConnector.createEoriSubscription(regDetails, subDetails, userLocation, service)
          .flatMap {
            case SuccessResponse(formBundleNumber, sid, _) =>
              val updatedRegDetails = regDetails match {
                case rde: RegistrationDetailsEmbassy => rde.copy(safeId = sid)
                case rdo: RegistrationDetailsOrganisation => rdo.copy(safeId = sid)
              }

              sessionCache.saveRegistrationDetails(updatedRegDetails)
                .flatMap { saved =>
                  save4LaterService.fetchEmail(GroupId(loggedInUser.groupId)).flatMap { optEmailStatus =>
                    if (saved) {
                      handleSubscriptionService
                        .handleSubscription(
                          formBundleNumber,
                          RecipientDetails(
                            service,
                            optEmailStatus.head.email.head,
                            subDetails.contactDetails.map(_.fullName).head,
                            None,
                            None
                          ),
                          TaxPayerId(""),
                          None,
                          None,
                          sid
                        )
                        .flatMap(_ => Future.successful(Redirect(Sub02Controller.pending(service))))
                    } else {
                      Future.successful(Redirect(Sub02Controller.requestNotProcessed(service)))
                    }
                  }
                }
            case _ => Future.successful(Redirect(Sub02Controller.requestNotProcessed(service)))
          }
      }
    }
  }

  private def rowServiceCall(loggedInUser: LoggedInUserWithEnrolments, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ) = {

    def registerWithoutIdWithSubscription(
      orgType: Option[EtmpOrganisationType],
      regDetails: RegistrationDetails,
      subDetails: SubscriptionDetails
    ) =
      orgType match {
        case Some(NA) =>
          rowIndividualRegisterWithSubscription(
            loggedInUser,
            service,
            regDetails,
            subDetails,
            requestSessionData.userSelectedOrganisationType
          )
        case _ =>
          rowOrganisationRegisterWithSubscription(
            loggedInUser,
            service,
            regDetails,
            subDetails,
            requestSessionData.userSelectedOrganisationType
          )
      }

    for {
      orgType <- orgTypeLookup.etmpOrgTypeOpt
      rd      <- sessionCache.registrationDetails
      sd      <- sessionCache.subscriptionDetails
      call    <- registerWithoutIdWithSubscription(orgType, rd, sd)
    } yield call
  }

  private def rowIndividualRegisterWithSubscription(
    loggedInUser: LoggedInUserWithEnrolments,
    service: Service,
    registrationDetails: RegistrationDetails,
    subscriptionDetails: SubscriptionDetails,
    orgType: Option[CdsOrganisationType]
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]) =
    subscriptionDetails.nameDobDetails.map(
      details =>
        registerWithoutIdService
          .registerIndividual(
            IndividualNameAndDateOfBirth(details.firstName, details.lastName, details.dateOfBirth),
            registrationDetails.address,
            subscriptionDetails.contactDetails,
            loggedInUser,
            orgType
          )
          .flatMap {
            case RegisterWithoutIDResponse(ResponseCommon(status, _, _, _), _) if status == StatusOK =>
              sub02Controller.subscribe(service)(request)
            case _ =>
              val error = "Registration of individual FAILED"
              // $COVERAGE-OFF$Loggers
              logger.warn(error)
              // $COVERAGE-ON
              throw new RuntimeException(error)
          }
    ) match {
      case Some(f) => f
      case None =>
        val error = "Incorrect argument passed for cache Individual Registration"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalArgumentException(error)
    }

  private def rowOrganisationRegisterWithSubscription(
    loggedInUser: LoggedInUserWithEnrolments,
    service: Service,
    registrationDetails: RegistrationDetails,
    subscriptionDetails: SubscriptionDetails,
    orgType: Option[CdsOrganisationType]
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]) =
    registerWithoutIdService
      .registerOrganisation(
        subscriptionDetails.name.getOrElse(""),
        registrationDetails.address,
        subscriptionDetails.contactDetails,
        loggedInUser,
        orgType
      )
      .flatMap {
        case RegisterWithoutIDResponse(ResponseCommon(status, _, _, _), _) if status == StatusOK =>
          sub02Controller.subscribe(service)(request)
        case _ =>
          val error = "Registration of organisation FAILED"
          // $COVERAGE-OFF$Loggers
          logger.warn(error)
          // $COVERAGE-ON
          throw new RuntimeException(error)
      }

}
