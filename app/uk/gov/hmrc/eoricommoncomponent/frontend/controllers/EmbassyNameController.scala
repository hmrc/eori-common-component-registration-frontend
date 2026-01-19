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

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{DetermineReviewPageController, EmbassyAddressController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.embassy.EmbassyNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_embassy_name

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmbassyNameController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  embassyNameForm: EmbassyNameForm,
  whatIsYourEmbassyNameView: what_is_your_embassy_name,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  val form: Form[String] = embassyNameForm.embassyNameForm()

  def showForm(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      subscriptionDetailsService.cachedEmbassyName.flatMap { optEmbassyName =>
        val updatedForm = optEmbassyName.fold(form)(form.fill)
        Future.successful(Ok(whatIsYourEmbassyNameView(isInReviewMode, updatedForm, organisationType, service)))
      }
    }

  def submit(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(whatIsYourEmbassyNameView(isInReviewMode, formWithErrors, organisationType, service))
            ),
          embassyNameFormData =>
            subscriptionDetailsService.cacheEmbassyName(embassyNameFormData).flatMap { _ =>
              if (!isInReviewMode)
                subscriptionDetailsService
                  .updateSubscriptionDetailsEmbassyName(embassyNameFormData)
                  .map(_ => Redirect(EmbassyAddressController.showForm(isInReviewMode = false, service)))
              else
                Future.successful(Redirect(DetermineReviewPageController.determineRoute(service)))
            }
        )
    }

}
