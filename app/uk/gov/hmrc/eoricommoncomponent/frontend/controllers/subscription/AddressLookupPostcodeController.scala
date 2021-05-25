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
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressLookupParams
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.address_lookup_postcode

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupPostcodeController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  addressLookupPostcodePage: address_lookup_postcode
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.addressLookupParams.map {
        case Some(addressLookupParams) =>
          Ok(prepareView(AddressLookupParams.form().fill(addressLookupParams), false, service))
        case _ => Ok(prepareView(AddressLookupParams.form(), false, service))
      }
    }

  def reviewPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.addressLookupParams.map {
        case Some(addressLookupParams) =>
          Ok(prepareView(AddressLookupParams.form().fill(addressLookupParams), true, service))
        case _ => Ok(prepareView(AddressLookupParams.form(), true, service))
      }
    }

  private def prepareView(form: Form[AddressLookupParams], isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): HtmlFormat.Appendable = {
    val selectedOrganisationType = requestSessionData.userSelectedOrganisationType.getOrElse(
      throw new IllegalStateException("Organisation type is not cached")
    )

    addressLookupPostcodePage(form, isInReviewMode, selectedOrganisationType, service)
  }

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      AddressLookupParams.form().bindFromRequest().fold(
        formWithError => Future.successful(BadRequest(prepareView(formWithError, isInReviewMode, service))),
        validAddressParams =>
          sessionCache.saveAddressLookupParams(validAddressParams).map { _ =>
            if (isInReviewMode) Redirect(routes.AddressLookupResultsController.reviewPage(service))
            else Redirect(routes.AddressLookupResultsController.displayPage(service))
          }
      )
    }

}
