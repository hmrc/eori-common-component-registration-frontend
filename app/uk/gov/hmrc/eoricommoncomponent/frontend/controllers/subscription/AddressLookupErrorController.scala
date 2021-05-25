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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{
  address_lookup_error,
  address_lookup_no_results
}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupErrorController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  mcc: MessagesControllerComponents,
  addressLookupErrorPage: address_lookup_error,
  addressLookupNoResultsPage: address_lookup_no_results
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayErrorPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(addressLookupErrorPage(service, false)))
    }

  def reviewErrorPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(addressLookupErrorPage(service, true)))
    }

  def displayNoResultsPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.addressLookupParams.map {
        case Some(addressLookupParams) => Ok(addressLookupNoResultsPage(addressLookupParams.postcode, service, false))
        case _                         => Redirect(routes.AddressLookupPostcodeController.displayPage(service))
      }
    }

  def reviewNoResultsPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.addressLookupParams.map {
        case Some(addressLookupParams) => Ok(addressLookupNoResultsPage(addressLookupParams.postcode, service, true))
        case _                         => Redirect(routes.AddressLookupPostcodeController.reviewPage(service))
      }
    }

}
