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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.Logging
import play.api.data.Form
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{CdsController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetailsSubscriptionFlowPageGetEori
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.DataUnavailableException
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{address, error_template}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressService @Inject() (
  addressDetailsForm: AddressDetailsForm,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionFlowManager: SubscriptionFlowManager,
  addressView: address,
  errorTemplate: error_template,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc)
    with Logging {

  private def saveAddress(ad: AddressViewModel)(implicit request: Request[AnyContent]): Future[Future[Unit]] =
    for {
      contactDetails <- subscriptionBusinessService.cachedContactDetailsModel
    } yield subscriptionDetailsService.cacheContactAddressDetails(
      ad,
      contactDetails.getOrElse {
        val error = "contactDetails not found in cache"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw DataUnavailableException(error)
      }
    )

  private def populateCountriesToInclude(
    isInReviewMode: Boolean,
    service: Service,
    form: Form[AddressViewModel],
    status: Status
  )(implicit request: Request[AnyContent]): Future[Result] =
    Future.successful(status(addressView(form, Countries.all, isInReviewMode, service)))

  def populateViewIfContactDetailsCached(service: Service)(implicit request: Request[AnyContent]): Future[Result] =
    subscriptionBusinessService.cachedContactDetailsModel.flatMap {
      case Some(cdm) =>
        populateOkView(cdm.toAddressViewModel, isInReviewMode = true, service)
      case _ =>
        Future.successful(InternalServerError(errorTemplate(service)))
    }

  def handleFormDataAndRedirect(form: Form[AddressViewModel], isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    form
      .bindFromRequest()
      .fold(
        formWithErrors => populateCountriesToInclude(isInReviewMode, service, formWithErrors, BadRequest),
        address =>
          saveAddress(address).flatMap(_ =>
            subscriptionFlowManager.stepInformation(ContactDetailsSubscriptionFlowPageGetEori) match {
              case Right(flowInfo) => Future.successful(Redirect(flowInfo.nextPage.url(service)))
              case Left(_) =>
                logger.warn(s"Unable to identify subscription flow: key not found in cache")
                Future.successful(Redirect(ApplicationController.startRegister(service)))
            }
          )
      )

  def populateOkView(address: Option[AddressViewModel], isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    if (isInReviewMode) {
      lazy val form = address.fold(addressDetailsForm.addressDetailsCreateForm())(addressDetailsForm.addressDetailsCreateForm().fill(_))
      populateCountriesToInclude(isInReviewMode, service, form, Ok)
    } else populateCountriesToInclude(isInReviewMode, service, addressDetailsForm.addressDetailsCreateForm(), Ok)

}
