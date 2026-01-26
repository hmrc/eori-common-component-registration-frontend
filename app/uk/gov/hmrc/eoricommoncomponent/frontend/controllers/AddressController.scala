/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.AddressService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCacheService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddressController @Inject() (
  authorise: AuthAction,
  addressDetailsForm: AddressDetailsForm,
  addressService: AddressService,
  sessionCacheService: SessionCacheService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => user: LoggedInUserWithEnrolments =>
      addressService
        .populateOkView(None, isInReviewMode = false, service)
        .flatMap(
          sessionCacheService.individualAndSoleTraderRouter(
            user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
            service,
            _
          )
        )
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => user: LoggedInUserWithEnrolments =>
      addressService
        .populateViewIfContactDetailsCached(service)
        .flatMap(
          sessionCacheService.individualAndSoleTraderRouter(
            user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
            service,
            _
          )
        )
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      addressService.handleFormDataAndRedirect(addressDetailsForm.addressDetailsCreateForm(), isInReviewMode, service)
    }

}
