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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.UserLocationController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration._

import scala.concurrent.ExecutionContext

@Singleton
class UseThisEoriController @Inject() (
  authAction: AuthAction,
  detailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  useThisEoriView: use_this_eori
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def display(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        detailsService.cachedExistingEoriNumber.map { eori =>
          Ok(useThisEoriView(eori.getOrElse(throw MissingExistingEori()).id, service))
        }

    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      detailsService.cachedExistingEoriNumber.flatMap { eori =>
        detailsService.cacheEoriNumber(eori.getOrElse(throw MissingExistingEori()).id).map { _ =>
          Redirect(UserLocationController.form(service, Journey.Subscribe))
        }
      }
    }

}

case class MissingExistingEori() extends Exception(s"Existing EORI missing from cache")
