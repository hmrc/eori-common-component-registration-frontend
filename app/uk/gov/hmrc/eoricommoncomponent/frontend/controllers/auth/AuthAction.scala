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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth

import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, internalId, email => ggEmail, _}
import uk.gov.hmrc.auth.core.retrieve.{~, Credentials}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject() (
  override val config: Configuration,
  override val env: Environment,
  override val authConnector: AuthConnector,
  action: DefaultActionBuilder,
  sessionCache: SessionCache,
  parser: BodyParsers.Default,
  cacheClearOnCompletionAction: CacheClearOnCompletionAction
)(implicit ec: ExecutionContext)
    extends AuthRedirectSupport with AuthorisedFunctions with AccessController {

  private type RequestProcessorSimple =
    Request[AnyContent] => LoggedInUserWithEnrolments => Future[Result]

  private type RequestProcessorExtended =
    Request[AnyContent] => Option[String] => LoggedInUserWithEnrolments => Future[Result]

  private val baseRetrievals     = ggEmail and credentialRole and affinityGroup
  private val extendedRetrievals = baseRetrievals and internalId and allEnrolments and groupIdentifier and credentials

  def enrolledUserClearingCacheOnCompletionAction(requestProcessor: RequestProcessorSimple) =
    cacheClearOnCompletionAction.async(implicit request => authorise(requestProcessor))

  def enrolledUserWithSessionAction(service: Service)(requestProcessor: RequestProcessorSimple) = {
    val filter = new CompletedJourneyFilter(service, sessionCache, parser)
    filter.async(implicit request => authorise(requestProcessor))
  }

  /**
    * Allows Gov Gateway user with correct user type, affinity group and no enrolment to service
    */
  def ggAuthorisedUserWithEnrolmentsAction(requestProcessor: RequestProcessorSimple): Action[AnyContent] =
    action.async { implicit request =>
      authorise(requestProcessor)
    }

  /**
    * Allows Gov Gateway user with correct user type and affinity group but no check for enrolment to service
    */
  def ggAuthorisedUserWithServiceAction(requestProcessor: RequestProcessorSimple): Action[AnyContent] =
    action.async { implicit request =>
      authorise(requestProcessor, checkServiceEnrolment = false)
    }

  /**
    * Allows Gov Gateway user without checks for user type, affinity group or enrolment to service
    */
  def ggAuthorisedUserAction(requestProcessor: RequestProcessorSimple): Action[AnyContent] =
    action.async { implicit request =>
      authorise(requestProcessor, checkPermittedAccess = false)
    }

  private def authorise(
    requestProcessor: RequestProcessorSimple,
    checkPermittedAccess: Boolean = true,
    checkServiceEnrolment: Boolean = true
  )(implicit request: Request[AnyContent]) = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AuthProviders(GovernmentGateway))
      .retrieve(extendedRetrievals) {
        case currentUserEmail ~ userCredentialRole ~ userAffinityGroup ~ userInternalId ~ userAllEnrolments ~ groupId ~ Some(
              Credentials(credId, _)
            ) =>
          transformRequest(
            Right(requestProcessor),
            LoggedInUserWithEnrolments(
              userAffinityGroup,
              userInternalId,
              userAllEnrolments,
              currentUserEmail,
              groupId,
              userCredentialRole,
              credId
            ),
            checkPermittedAccess,
            checkServiceEnrolment
          )
        case _ => Future.failed(new Exception("Auth did not contain user credentials"))

      } recover withAuthRecovery(request)
  }

  private def transformRequest(
    requestProcessor: Either[RequestProcessorExtended, RequestProcessorSimple],
    loggedInUser: LoggedInUserWithEnrolments,
    checkPermittedAccess: Boolean,
    checkServiceEnrolment: Boolean
  )(implicit request: Request[AnyContent]) = {

    def enrolments: Set[Enrolment] = if (checkServiceEnrolment) loggedInUser.enrolments.enrolments else Set.empty

    def action: Future[Result] =
      requestProcessor.fold(
        processor => processor(request)(loggedInUser.internalId)(loggedInUser),
        processor => processor(request)(loggedInUser)
      )

    if (checkPermittedAccess)
      permitUserOrRedirect(loggedInUser.affinityGroup, loggedInUser.userCredentialRole, enrolments)(action)
    else
      action
  }

}
