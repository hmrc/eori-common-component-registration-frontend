/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.embassyNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_embassy_name

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmbassyNameController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  whatIsYourEmbassyNameView: what_is_your_embassy_name,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def showForm(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedEmbassyName.flatMap { optEmbassyName =>
        val updatedForm = optEmbassyName.fold(embassyNameForm)(embassyNameForm.fill)
        Future.successful(Ok(whatIsYourEmbassyNameView(isInReviewMode, updatedForm, organisationType, service)))
      }
    }

  // todo submit
}
