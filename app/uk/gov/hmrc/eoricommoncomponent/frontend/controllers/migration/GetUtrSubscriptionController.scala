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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{AddressController, DetermineReviewPageController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{RowIndividualFlow, RowOrganisationFlow}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_utr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetUtrSubscriptionController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  getUtrSubscriptionView: how_can_we_identify_you_utr,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        populateView(false, service, journey)
    }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        populateView(true, service, journey)
    }

  private def populateView(isInReviewMode: Boolean, service: Service, journey: Journey.Value)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ) =
    requestSessionData.userSelectedOrganisationType match {
      case Some(_) =>
        subscriptionDetailsService.cachedCustomsId.map {
          case Some(Utr(id)) =>
            Ok(
              getUtrSubscriptionView(
                subscriptionUtrForm.fill(IdMatchModel(id)),
                isInReviewMode,
                routes.GetUtrSubscriptionController.submit(isInReviewMode, service, journey)
              )
            )

          case _ =>
            Ok(
              getUtrSubscriptionView(
                subscriptionUtrForm,
                isInReviewMode,
                routes.GetUtrSubscriptionController.submit(isInReviewMode, service, journey)
              )
            )
        }
      case None => noOrgTypeSelected
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        requestSessionData.userSelectedOrganisationType match {
          case Some(orgType) =>
            subscriptionUtrForm.bindFromRequest.fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    getUtrSubscriptionView(
                      formWithErrors,
                      isInReviewMode,
                      routes.GetUtrSubscriptionController.submit(isInReviewMode, service, journey)
                    )
                  )
                ),
              formData => cacheAndContinue(isInReviewMode, formData, service, journey, orgType)
            )
          case None => noOrgTypeSelected
        }
    }

  private def cacheAndContinue(
    isInReviewMode: Boolean,
    form: IdMatchModel,
    service: Service,
    journey: Journey.Value,
    orgType: CdsOrganisationType
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] =
    cacheUtr(form, orgType).map(
      _ =>
        if (isInReviewMode && !isItRowJourney)
          Redirect(DetermineReviewPageController.determineRoute(service, journey))
        else
          Redirect(AddressController.createForm(service, journey))
    )

  private def isItRowJourney()(implicit request: Request[AnyContent]): Boolean =
    requestSessionData.userSubscriptionFlow == RowOrganisationFlow ||
      requestSessionData.userSubscriptionFlow == RowIndividualFlow

  private def cacheUtr(form: IdMatchModel, orgType: CdsOrganisationType)(implicit hc: HeaderCarrier): Future[Unit] =
    if (orgType == CdsOrganisationType.Company)
      subscriptionDetailsService.cachedNameDetails.flatMap {
        case Some(nameDetails) =>
          subscriptionDetailsService.cacheNameAndCustomsId(nameDetails.name, Utr(form.id))
        case _ => noBusinessName

      }
    else subscriptionDetailsService.cacheCustomsId(Utr(form.id))

  private lazy val noOrgTypeSelected = throw new IllegalStateException("No organisation type selected by user")
  private lazy val noBusinessName    = throw new IllegalStateException("No business name cached")

}
