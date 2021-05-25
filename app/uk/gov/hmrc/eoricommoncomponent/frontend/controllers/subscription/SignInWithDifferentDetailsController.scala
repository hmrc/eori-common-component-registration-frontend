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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sign_in_with_different_details

import scala.concurrent.ExecutionContext

@Singleton
class SignInWithDifferentDetailsController @Inject() (
  authAction: AuthAction,
  cdsFrontendDataCache: SessionCache,
  signInWithDifferentDetailsView: sign_in_with_different_details,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        val name = journey match {
          case Journey.Register  => cdsFrontendDataCache.registrationDetails.map(_.name)
          case Journey.Subscribe => cdsFrontendDataCache.subscriptionDetails.map(_.name)
          case _                 => throw new IllegalArgumentException("No a valid journey")
        }

        name map { n =>
          val optionalName = Option(n) filter (_.nonEmpty)
          Ok(signInWithDifferentDetailsView(optionalName))
        }
    }

}
