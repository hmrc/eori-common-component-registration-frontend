/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{you_cannot_change_address_individual, you_cannot_change_address_organisation}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class YouCannotChangeAddressController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  youCannotChangeAddressOrganisation: you_cannot_change_address_organisation,
  youCannotChangeAddressIndividual: you_cannot_change_address_individual,
  mcc: MessagesControllerComponents
) extends CdsController(mcc)
    with Logging {

  def page(service: Service): Action[AnyContent] = authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
    if (requestSessionData.isIndividualOrSoleTrader(request) || requestSessionData.isPartnership(request))
      Future.successful(Ok(youCannotChangeAddressIndividual(service)))
    else {
      logger.info("Your answers do not match our records page loaded")
      Future.successful(Ok(youCannotChangeAddressOrganisation(service)))
    }
  }

}
