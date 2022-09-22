/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthAction, EnrolmentExtractor}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingIdController @Inject() (
  authAction: AuthAction,
  featureFlags: FeatureFlags,
  matchingService: MatchingService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with EnrolmentExtractor {

  def matchWithIdOnly(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      matchLoggedInUserAndRedirect(loggedInUser) {
        Redirect(UserLocationController.form(service))
      } {
        Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false))
      }
  }

  private def matchLoggedInUserAndRedirect(loggedInUser: LoggedInUserWithEnrolments)(
    redirectOrganisationTypePage: => Result
  )(redirectToConfirmationPage: => Result)(implicit hc: HeaderCarrier, request: Request[_]) =
    if (featureFlags.matchingEnabled) {
      lazy val ctUtr = enrolledCtUtr(loggedInUser)
      lazy val saUtr = enrolledSaUtr(loggedInUser)
      lazy val nino  = enrolledNino(loggedInUser)

      (ctUtr orElse saUtr orElse nino).fold(ifEmpty = Future.successful(redirectOrganisationTypePage)) { utrOrNino =>
        matchingService.matchBusinessWithIdOnly(utrOrNino, loggedInUser) map {
          case true  => redirectToConfirmationPage
          case false => redirectOrganisationTypePage
        }
      }
    } else
      Future.successful(redirectOrganisationTypePage)

}
