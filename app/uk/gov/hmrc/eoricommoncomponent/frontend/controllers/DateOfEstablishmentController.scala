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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EtmpOrganisationType, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.date_of_establishment

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DateOfEstablishmentController @Inject() (
  authAction: AuthAction,
  subscriptionFlowManager: SubscriptionFlowManager,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsHolderService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  dateOfEstablishmentView: date_of_establishment,
  orgTypeLookup: OrgTypeLookup,
  sessionCacheService: SessionCacheService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {
  private val logger = Logger(this.getClass)

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      (for {
        maybeCachedDateModel <- subscriptionBusinessService.maybeCachedDateEstablished
        orgType              <- orgTypeLookup.etmpOrgType
      } yield populateView(maybeCachedDateModel, isInReviewMode = false, orgType, service)).flatMap(
        sessionCacheService.individualAndSoleTraderRouter(
          user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
          service,
          _
        )
      )
    }

  private def populateView(
    cachedDate: Option[LocalDate],
    isInReviewMode: Boolean,
    orgType: EtmpOrganisationType,
    service: Service
  )(implicit request: Request[AnyContent]): Result = {
    val form = cachedDate.fold(subscriptionDateOfEstablishmentForm)(subscriptionDateOfEstablishmentForm.fill)
    Ok(dateOfEstablishmentView(form, isInReviewMode, orgType, UserLocation.isRow(requestSessionData), service))
  }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      (for {
        cachedDateModel <- fetchDate
        orgType         <- orgTypeLookup.etmpOrgType
      } yield populateView(Some(cachedDateModel), isInReviewMode = true, orgType, service)).flatMap(
        sessionCacheService.individualAndSoleTraderRouter(
          user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
          service,
          _
        )
      )
    }

  private def fetchDate(implicit request: Request[_]): Future[LocalDate] =
    subscriptionBusinessService.getCachedDateEstablished

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDateOfEstablishmentForm.bindFromRequest().fold(
        formWithErrors =>
          orgTypeLookup.etmpOrgType map { orgType =>
            BadRequest(
              dateOfEstablishmentView(
                formWithErrors,
                isInReviewMode,
                orgType,
                UserLocation.isRow(requestSessionData),
                service
              )
            )
          },
        date =>
          saveDateEstablished(date).map { _ =>
            if (isInReviewMode)
              Redirect(DetermineReviewPageController.determineRoute(service))
            else
              subscriptionFlowManager.stepInformation(DateOfEstablishmentSubscriptionFlowPage) match {
                case Right(flowInfo) => Redirect(flowInfo.nextPage.url(service))
                case Left(_) =>
                  logger.warn(s"Unable to identify subscription flow: key not found in cache")
                  Redirect(ApplicationController.startRegister(service))
              }
          }
      )
    }

  private def saveDateEstablished(date: LocalDate)(implicit request: Request[_]) =
    subscriptionDetailsHolderService.cacheDateEstablished(date)

}
