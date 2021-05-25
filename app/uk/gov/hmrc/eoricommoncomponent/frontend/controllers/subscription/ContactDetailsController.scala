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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetailsSubscriptionFlowPageMigrate
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, NA}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.ContactDetailsSubscribeModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.ContactDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.contact_details
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  cdsFrontendDataCache: SessionCache,
  subscriptionFlowManager: SubscriptionFlowManager,
  subscriptionDetailsService: SubscriptionDetailsService,
  orgTypeLookup: OrgTypeLookup,
  mcc: MessagesControllerComponents,
  contactDetailsView: contact_details
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      val f = for {
        orgType <- orgTypeLookup.etmpOrgTypeOpt

        cachedCustomsId <- if (orgType == Some(NA)) subscriptionDetailsService.cachedCustomsId
        else Future.successful(None)
        cachedNameIdDetails <- if (orgType == Some(NA)) Future.successful(None)
        else subscriptionDetailsService.cachedNameIdDetails
      } yield (cachedCustomsId, cachedNameIdDetails) match {
        case (None, None) => populateForm(service)(false)
        case _ =>
          Future.successful(
            Redirect(
              subscriptionFlowManager
                .stepInformation(ContactDetailsSubscriptionFlowPageMigrate)
                .nextPage
                .url(service)
            )
          )
      }
      f.flatMap(identity)
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      populateForm(service)(true)
    }

  private def populateForm(
    service: Service
  )(isInReviewMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      email          <- cdsFrontendDataCache.email
      contactDetails <- subscriptionBusinessService.cachedContactDetailsModel
    } yield {
      val contactDetailsModel = contactDetails.map(ContactDetailsSubscribeModel.fromContactDetailsModel(_))
      val form                = contactDetailsModel.fold(ContactDetailsForm.form())(ContactDetailsForm.form().fill(_))

      Future.successful(Ok(contactDetailsView(form, email, isInReviewMode, service)))
    }
  }.flatMap(identity)

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      cdsFrontendDataCache.email flatMap { email =>
        ContactDetailsForm.form().bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(contactDetailsView(formWithErrors, email, isInReviewMode, service))),
          formData => storeContactDetails(formData, email, isInReviewMode, service)
        )
      }
    }

  private def storeContactDetails(
    formData: ContactDetailsSubscribeModel,
    email: String,
    inReviewMode: Boolean,
    service: Service
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    subscriptionDetailsService
      .cacheContactDetails(formData.toContactDetailsModel(email), isInReviewMode = inReviewMode)
      .map(
        _ =>
          if (inReviewMode) Redirect(DetermineReviewPageController.determineRoute(service, Journey.Subscribe))
          else
            Redirect(
              subscriptionFlowManager
                .stepInformation(ContactDetailsSubscriptionFlowPageMigrate)
                .nextPage
                .url(service)
            )
      )

}
