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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{address_invalid_individual, address_invalid_organisation}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressInvalidController @Inject() (
  authAction: AuthAction,
  sessionCacheService: SessionCacheService,
  requestSessionData: RequestSessionData,
  addressInvalidOrganisation: address_invalid_organisation,
  addressInvalidIndividual: address_invalid_individual,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def page(service: Service): Action[AnyContent] = authAction.enrolledUserWithSessionAction(service) {
    implicit request => user: LoggedInUserWithEnrolments =>
      val result =
        if (requestSessionData.isIndividualOrSoleTrader(request))
          Future.successful(Ok(addressInvalidIndividual(service)))
        else Future.successful(Ok(addressInvalidOrganisation(service)))

      result.flatMap(
        sessionCacheService.individualAndSoleTraderRouter(
          user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
          service,
          _
        )
      )

  }

}
