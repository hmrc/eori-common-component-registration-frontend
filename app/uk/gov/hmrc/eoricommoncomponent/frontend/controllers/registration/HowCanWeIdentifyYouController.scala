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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.ninoOrUtrChoiceForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you
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

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        populateView(service, journey, isInReviewMode = false)
    }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        populateView(service, journey, isInReviewMode = true)
    }

  private def populateView(service: Service, journey: Journey.Value, isInReviewMode: Boolean)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ) =
    subscriptionBusinessService.getCachedNinoOrUtrChoice.map { choice =>
      Ok(howCanWeIdentifyYouView(ninoOrUtrChoiceForm.fill(NinoOrUtrChoice(choice)), isInReviewMode, service, journey))
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      ninoOrUtrChoiceForm
        .bindFromRequest()
        .fold(
          invalidForm =>
            Future.successful(BadRequest(howCanWeIdentifyYouView(invalidForm, isInReviewMode, service, journey))),
          form => storeChoice(form, isInReviewMode, service, journey)
        )
    }

  private def storeChoice(formData: NinoOrUtrChoice, inReviewMode: Boolean, service: Service, journey: Journey.Value)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    subscriptionDetailsHolderService
      .cacheNinoOrUtrChoice(formData)
      .map(
        _ =>
          formData.ninoOrUtrRadio match {
            case Some("nino") =>
              Redirect(continueNino(inReviewMode, service, journey))
            case Some("utr") =>
              Redirect(continueUtr(inReviewMode, service, journey))
          }
      )

  private def continueNino(inReviewMode: Boolean, service: Service, journey: Journey.Value) = journey match {
    case Journey.Subscribe =>
      if (inReviewMode) HowCanWeIdentifyYouNinoController.reviewForm(service, journey)
      else HowCanWeIdentifyYouNinoController.createForm(service, journey)
    case Journey.Register =>
      if (inReviewMode) GYEHowCanWeIdentifyYouNinoController.form(service, journey)
      else GYEHowCanWeIdentifyYouNinoController.form(service, journey)
  }

  private def continueUtr(inReviewMode: Boolean, service: Service, journey: Journey.Value) = journey match {
    case Journey.Subscribe =>
      if (inReviewMode) HowCanWeIdentifyYouUtrController.reviewForm(service, journey)
      else HowCanWeIdentifyYouUtrController.createForm(service, journey)
    case Journey.Register =>
      if (inReviewMode) GYEHowCanWeIdentifyYouUtrController.form(service, journey)
      else GYEHowCanWeIdentifyYouUtrController.form(service, journey)
  }

}
