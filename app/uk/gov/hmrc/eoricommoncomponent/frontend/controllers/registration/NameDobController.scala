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
import play.api.mvc.{Action, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.enterNameDobForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.match_namedob
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NameDobController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  matchNameDobView: match_namedob,
  cdsFrontendDataCache: SessionCache
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(organisationType: String, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(matchNameDobView(enterNameDobForm, organisationType, service, journey)))
    }

  def submit(organisationType: String, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      enterNameDobForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(matchNameDobView(formWithErrors, organisationType, service, journey))),
        formData => submitNewDetails(formData, organisationType, service, journey)
      )
    }

  private def submitNewDetails(
    formData: NameDobMatchModel,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier): Future[Result] =
    cdsFrontendDataCache.saveSubscriptionDetails(SubscriptionDetails(nameDobDetails = Some(formData))).map { _ =>
      Redirect(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.HowCanWeIdentifyYouController
          .createForm(service, journey)
      )
    }

}
