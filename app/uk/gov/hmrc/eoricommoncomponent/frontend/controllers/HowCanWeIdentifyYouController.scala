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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.ninoOrUtrChoiceForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.how_can_we_identify_you
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HowCanWeIdentifyYouController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  howCanWeIdentifyYouView: how_can_we_identify_you,
  subscriptionDetailsHolderService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      populateView(service)
    }

  private def populateView(service: Service)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    subscriptionBusinessService.getCachedNinoOrUtrChoice.map { choice =>
      Ok(howCanWeIdentifyYouView(ninoOrUtrChoiceForm.fill(NinoOrUtrChoice(choice)), service))
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      ninoOrUtrChoiceForm
        .bindFromRequest()
        .fold(
          invalidForm => Future.successful(BadRequest(howCanWeIdentifyYouView(invalidForm, service))),
          form => storeChoice(form, service)
        )
    }

  private def storeChoice(formData: NinoOrUtrChoice, service: Service)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    subscriptionDetailsHolderService
      .cacheNinoOrUtrChoice(formData)
      .map(
        _ =>
          formData.ninoOrUtrRadio match {
            case Some("nino") =>
              Redirect(GYEHowCanWeIdentifyYouNinoController.form(service))
            case Some("utr") =>
              Redirect(GYEHowCanWeIdentifyYouUtrController.form(service))
          }
      )

}
