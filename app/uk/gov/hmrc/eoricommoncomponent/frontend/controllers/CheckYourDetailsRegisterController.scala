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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegisterWithoutIdWithSubscriptionService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.check_your_details_register

import scala.concurrent.ExecutionContext

@Singleton
class CheckYourDetailsRegisterController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  checkYourDetailsRegisterView: check_your_details_register,
  registerWithoutIdWithSubscription: RegisterWithoutIdWithSubscriptionService,
  featureFlags: FeatureFlags
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def reviewDetails(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          registration <- sessionCache.registrationDetails
          subscription <- sessionCache.subscriptionDetails
        } yield {
          val isUserIdentifiedByRegService = registration.safeId.id.nonEmpty
          Ok(
            checkYourDetailsRegisterView(
              featureFlags.useNewVATJourney,
              requestSessionData.userSelectedOrganisationType,
              requestSessionData.isPartnershipOrLLP,
              registration,
              subscription,
              subscription.personalDataDisclosureConsent.getOrElse(false),
              service,
              isUserIdentifiedByRegService
            )
          )
        }
    }

  def submitDetails(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      registerWithoutIdWithSubscription.rowRegisterWithoutIdWithSubscription(loggedInUser, service)
  }

}
