/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.*
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetailsSubscriptionFlowPageGetEori
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.contact_details

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  cdsFrontendDataCache: SessionCache,
  subscriptionFlowManager: SubscriptionFlowManager,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  contactDetailsView: contact_details,
  sessionCacheService: SessionCacheService,
  contactDetailsForm: ContactDetailsForm
)(implicit ec: ExecutionContext)
    extends CdsController(mcc)
    with Logging {

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (user: LoggedInUserWithEnrolments) =>
      populateFormGYE(user, service)(false)
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (user: LoggedInUserWithEnrolments) =>
      populateFormGYE(user, service)(true)
    }

  private def populateFormGYE(user: LoggedInUserWithEnrolments, service: Service)(
    isInReviewMode: Boolean
  )(implicit request: Request[AnyContent]): Future[Result] =
    subscriptionBusinessService.cachedContactDetailsModel.flatMap { contactDetails =>
      cdsFrontendDataCache.email.flatMap { email =>
        val form =
          contactDetails
            .map(_.toContactInfoViewModel)
            .fold(contactDetailsForm.contactDetailsCreateForm())(f => contactDetailsForm.contactDetailsCreateForm().fill(f))

        Future
          .successful(Ok(contactDetailsView(form, Some(email), isInReviewMode, service)))
          .flatMap(
            sessionCacheService.individualAndSoleTraderRouter(
              user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
              service,
              _
            )
          )
      }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      cdsFrontendDataCache.email flatMap { email =>
        contactDetailsForm
          .contactDetailsCreateForm()
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(contactDetailsView(formWithErrors, Some(email), isInReviewMode, service))),
            formData => storeContactDetails(formData, email, isInReviewMode, service)
          )
      }
    }

  private def storeContactDetails(
    formData: ContactDetailsViewModel,
    email: String,
    inReviewMode: Boolean,
    service: Service
  )(implicit request: Request[AnyContent]): Future[Result] =
    subscriptionBusinessService.cachedContactDetailsModel flatMap { contactDetails =>
      subscriptionDetailsService
        .cacheContactDetails(
          formData.copy(emailAddress = Some(email)).toContactInfoDetailsModel(contactDetails),
          isInReviewMode = inReviewMode
        )
        .map(_ =>
          if (inReviewMode) Redirect(DetermineReviewPageController.determineRoute(service))
          else
            subscriptionFlowManager.stepInformation(ContactDetailsSubscriptionFlowPageGetEori) match {
              case Right(flowInfo) =>
                Redirect(
                  flowInfo.nextPage
                    .url(service)
                )
              case Left(_) =>
                logger.warn(s"Unable to identify subscription flow: key not found in cache")
                Redirect(ApplicationController.startRegister(service))
            }
        )
    }

}
