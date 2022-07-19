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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetailsSubscriptionFlowPageGetEori
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{
  AddressViewModel,
  ContactDetailsViewModel,
  YesNoWrongAddress
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.contact_address
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactAddressController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  cdsFrontendDataCache: SessionCache,
  subscriptionFlowManager: SubscriptionFlowManager,
  mcc: MessagesControllerComponents,
  contactAddressView: contact_address
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      populateFormGYE(service)(false)
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      populateFormGYE(service)(true)
    }

  private def populateFormGYE(
    service: Service
  )(isInReviewMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] =
    populateOkView(isInReviewMode = isInReviewMode, service)

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      YesNoWrongAddress
        .createForm()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            createContactDetails().map { contactAddressDetails =>
              BadRequest(contactAddressView(contactAddressDetails, isInReviewMode, formWithErrors, service))
            },
          formData => storeContactDetails(formData, isInReviewMode, service)
        )
    }

  private def createContactDetails()(implicit request: Request[AnyContent]): Future[AddressViewModel] =
    cdsFrontendDataCache.registrationDetails.map(rd => AddressViewModel(rd.address))

  private def populateOkView(isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    createContactDetails() map (
      contactDetails => Ok(contactAddressView(contactDetails, isInReviewMode, YesNoWrongAddress.createForm(), service))
    )

  private def storeContactDetails(formData: YesNoWrongAddress, inReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    if (inReviewMode) Future.successful(Redirect(DetermineReviewPageController.determineRoute(service)))
    else
      Future.successful(
        Redirect(
          subscriptionFlowManager
            .stepInformation(ContactDetailsSubscriptionFlowPageGetEori)
            .nextPage
            .url(service)
        )
      )

}
