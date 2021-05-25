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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}

import scala.concurrent.Future

@Singleton
class DetermineReviewPageController @Inject() (authAction: AuthAction, mcc: MessagesControllerComponents)
    extends CdsController(mcc) {

  def determineRoute(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { _: Request[AnyContent] => _: LoggedInUserWithEnrolments =>
      journey match {
        case Journey.Subscribe =>
          Future.successful(Redirect(CheckYourDetailsController.reviewDetails(service, journey).url))
        case _ => Future.successful(Redirect(CheckYourDetailsRegisterController.reviewDetails(service, journey).url))
      }
    }

}
