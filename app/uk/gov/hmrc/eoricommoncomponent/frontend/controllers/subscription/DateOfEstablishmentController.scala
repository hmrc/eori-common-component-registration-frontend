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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EtmpOrganisationType, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.date_of_establishment
import uk.gov.hmrc.http.HeaderCarrier

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
  orgTypeLookup: OrgTypeLookup
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        maybeCachedDateModel <- subscriptionBusinessService.maybeCachedDateEstablished
        orgType              <- orgTypeLookup.etmpOrgType
      } yield populateView(maybeCachedDateModel, isInReviewMode = false, orgType, service, journey)
    }

  private def populateView(
    cachedDate: Option[LocalDate],
    isInReviewMode: Boolean,
    orgType: EtmpOrganisationType,
    service: Service,
    journey: Journey.Value
  )(implicit request: Request[AnyContent]): Result = {
    val form = cachedDate.fold(subscriptionDateOfEstablishmentForm)(subscriptionDateOfEstablishmentForm.fill)
    Ok(dateOfEstablishmentView(form, isInReviewMode, orgType, UserLocation.isRow(requestSessionData), service, journey))
  }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        cachedDateModel <- fetchDate
        orgType         <- orgTypeLookup.etmpOrgType
      } yield populateView(Some(cachedDateModel), isInReviewMode = true, orgType, service, journey)
    }

  private def fetchDate(implicit hc: HeaderCarrier): Future[LocalDate] =
    subscriptionBusinessService.getCachedDateEstablished

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDateOfEstablishmentForm.bindFromRequest.fold(
        formWithErrors =>
          orgTypeLookup.etmpOrgType map { orgType =>
            BadRequest(
              dateOfEstablishmentView(
                formWithErrors,
                isInReviewMode,
                orgType,
                UserLocation.isRow(requestSessionData),
                service,
                journey
              )
            )
          },
        date =>
          saveDateEstablished(date).map { _ =>
            if (isInReviewMode)
              Redirect(DetermineReviewPageController.determineRoute(service, journey))
            else if (requestSessionData.isUKJourney)
              Redirect(routes.AddressLookupPostcodeController.displayPage(service))
            else {
              val page = subscriptionFlowManager
                .stepInformation(getSubscriptionPage(journey, UserLocation.isRow(requestSessionData)))
                .nextPage
              Redirect(
                page
                  .url(service)
              )
            }
          }
      )
    }

  private def saveDateEstablished(date: LocalDate)(implicit hc: HeaderCarrier) =
    subscriptionDetailsHolderService.cacheDateEstablished(date)

  private def getSubscriptionPage(journey: Journey.Value, location: Boolean) =
    (journey, location) match {
      case (Journey.Subscribe, true) => RowDateOfEstablishmentSubscriptionFlowPage
      case (Journey.Subscribe, false) =>
        DateOfEstablishmentSubscriptionFlowPageMigrate
      case _ => DateOfEstablishmentSubscriptionFlowPage
    }

}
