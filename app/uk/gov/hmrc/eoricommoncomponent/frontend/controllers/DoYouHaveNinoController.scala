/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  EmailController,
  GetNinoController,
  SixLineAddressController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.haveRowIndividualsNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_nino_row_individual

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DoYouHaveNinoController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  matchNinoRowIndividualView: match_nino_row_individual,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedNinoMatch.map { cachedNinoOpt =>
        val form = cachedNinoOpt.fold(haveRowIndividualsNinoForm)(haveRowIndividualsNinoForm.fill(_))

        Ok(matchNinoRowIndividualView(form, service))
      }
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) {
      implicit request => _: LoggedInUserWithEnrolments =>
        haveRowIndividualsNinoForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(matchNinoRowIndividualView(formWithErrors, service))),
          formData =>
            subscriptionDetailsService.cachedNinoMatch.flatMap { cachedNinoOpt =>
              formData.haveNino match {
                case Some(true) =>
                  subscriptionDetailsService
                    .cacheNinoMatch(Some(formData))
                    .map(_ => Redirect(GetNinoController.displayForm(service)))
                case Some(false) if cachedNinoOpt.exists(_.haveNino.exists(_ == false)) =>
                  Future.successful(noNinoRedirect(service))
                case Some(false) =>
                  subscriptionDetailsService.updateSubscriptionDetailsIndividual.flatMap { _ =>
                    subscriptionDetailsService.cacheNinoMatch(Some(formData)).map { _ =>
                      noNinoRedirect(service)
                    }
                  }
                case _ =>
                  throw new IllegalArgumentException("Have NINO must be Some(true) or Some(false) but was None")
              }
            }
        )
    }

  private def noNinoRedirect(service: Service)(implicit request: Request[AnyContent]): Result =
    requestSessionData.userSelectedOrganisationType match {
      case Some(cdsOrgType) =>
        Redirect(SixLineAddressController.showForm(false, cdsOrgType.id, service))
      case _ =>
        Redirect(EmailController.form(service))
    }

}
