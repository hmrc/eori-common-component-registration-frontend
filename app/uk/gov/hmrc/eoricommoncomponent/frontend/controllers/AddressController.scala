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
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetailsSubscriptionFlowPageGetEori
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm.addressDetailsCreateForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressController @Inject() (
  authorise: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionFlowManager: SubscriptionFlowManager,
  mcc: MessagesControllerComponents,
  addressView: address,
  errorTemplate: error_template
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      populateOkView(None, isInReviewMode = false, service)
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionBusinessService.cachedContactDetailsModel.flatMap {
        case Some(cdm) =>
          populateOkView(cdm.toAddressViewModel, isInReviewMode = true, service)
        case _ =>
          Future.successful(InternalServerError(errorTemplate()))
      }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      addressDetailsCreateForm().bindFromRequest
        .fold(
          formWithErrors => populateCountriesToInclude(isInReviewMode, service, formWithErrors, BadRequest),
          address =>
            saveAddress(address).flatMap(
              _ =>
                Future.successful(
                  Redirect(
                    subscriptionFlowManager
                      .stepInformation(ContactDetailsSubscriptionFlowPageGetEori)
                      .nextPage.url(service)
                  )
                )
            )
        )
    }

  private def saveAddress(ad: AddressViewModel)(implicit hc: HeaderCarrier, request: Request[AnyContent]) =
    for {
      contactDetails <- subscriptionBusinessService.cachedContactDetailsModel
    } yield subscriptionDetailsService.cacheContactAddressDetails(
      ad,
      contactDetails.getOrElse(throw new IllegalStateException("Address not found in cache"))
    )

  private def populateCountriesToInclude(
    isInReviewMode: Boolean,
    service: Service,
    form: Form[AddressViewModel],
    status: Status
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]) =
    Future.successful(status(addressView(form, Countries.all, isInReviewMode, service)))

  private def populateOkView(address: Option[AddressViewModel], isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    if (!isInReviewMode)
      populateCountriesToInclude(isInReviewMode, service, addressDetailsCreateForm, Ok)
    else {
      lazy val form = address.fold(addressDetailsCreateForm())(addressDetailsCreateForm().fill(_))
      populateCountriesToInclude(isInReviewMode, service, form, Ok)
    }

}
