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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegisterWithoutIdWithSubscriptionService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.check_your_details_register

import scala.concurrent.ExecutionContext

@Singleton
class CheckYourDetailsRegisterController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  checkYourDetailsRegisterView: check_your_details_register,
  registerWithoutIdWithSubscription: RegisterWithoutIdWithSubscriptionService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def reviewDetails(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          registration <- sessionCache.registrationDetails
          subscription <- sessionCache.subscriptionDetails
        } yield {
          val consent = subscription.personalDataDisclosureConsent.getOrElse(
            throw new IllegalStateException("Consent to disclose personal data is missing")
          )
          val isUserIdentifiedByRegService = registration.safeId.id.nonEmpty
          Ok(
            checkYourDetailsRegisterView(
              requestSessionData.userSelectedOrganisationType,
              requestSessionData.isPartnership,
              registration,
              subscription,
              consent,
              service,
              journey,
              isUserIdentifiedByRegService
            )
          )
        }
    }

  def submitDetails(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => loggedInUser: LoggedInUserWithEnrolments =>
        registerWithoutIdWithSubscription
          .rowRegisterWithoutIdWithSubscription(loggedInUser, service, journey)
    }

}
