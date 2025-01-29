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

import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Iom
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.{VatRegistrationDate, VatRegistrationDateFormProvider}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsForm.vatDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  DataUnavailableException,
  RequestSessionData,
  SessionCacheService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatDetailsController @Inject() (
  authAction: AuthAction,
  VatDetailsService: VatDetailsService,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  vatDetailsView: vat_details,
  errorTemplate: error_template,
  weCannotConfirmYourIdentity: date_of_vat_registration,
  subscriptionDetailsService: SubscriptionDetailsService,
  form: VatRegistrationDateFormProvider,
  sessionCacheService: SessionCacheService,
  requestSessionData: RequestSessionData
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  val dateForm: Form[VatRegistrationDate] = form()

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) {
      implicit request => user: LoggedInUserWithEnrolments =>
        val userLocation =
          requestSessionData.selectedUserLocation.getOrElse(throw DataUnavailableException("User Location not set"))
        sessionCacheService.individualAndSoleTraderRouter(
          user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
          service,
          Ok(
            vatDetailsView(
              vatDetailsForm,
              isInReviewMode = false,
              userLocation,
              requestSessionData.isIndividualOrSoleTrader,
              service
            )
          )
        )
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) {
      implicit request => user: LoggedInUserWithEnrolments =>
        val userLocation =
          requestSessionData.selectedUserLocation.getOrElse(throw DataUnavailableException("User Location not set"))
        subscriptionBusinessService.getCachedUkVatDetails.map {
          case Some(vatDetails) =>
            Ok(
              vatDetailsView(
                vatDetailsForm.fill(vatDetails),
                isInReviewMode = true,
                userLocation,
                requestSessionData.isIndividualOrSoleTrader,
                service
              )
            )
          case None =>
            Ok(
              vatDetailsView(
                vatDetailsForm,
                isInReviewMode = true,
                userLocation,
                requestSessionData.isIndividualOrSoleTrader,
                service
              )
            )
        }.flatMap(
          sessionCacheService.individualAndSoleTraderRouter(
            user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
            service,
            _
          )
        )
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      val userLocation =
        requestSessionData.selectedUserLocation.getOrElse(throw DataUnavailableException("User Location not set"))
      vatDetailsForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              vatDetailsView(
                formWithErrors,
                isInReviewMode,
                userLocation,
                requestSessionData.isIndividualOrSoleTrader,
                service
              )
            )
          ),
        formData => lookupVatDetails(formData, isInReviewMode, service)
      )
    }

  private def lookupVatDetails(vatForm: VatDetails, isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    VatDetailsService.getVatCustomerInformation(vatForm.number).foldF(
      responseError =>
        responseError.status match {
          case NOT_FOUND | BAD_REQUEST =>
            Future.successful(Redirect(redirectNext(service)))
          case SERVICE_UNAVAILABLE => Future.successful(Results.ServiceUnavailable(errorTemplate(service)))
          case _                   => Future.successful(Results.InternalServerError(errorTemplate(service)))
        },
      response =>
        if (response.isPostcodeAssociatedWithVrn(vatForm))
          subscriptionDetailsService
            .cacheUkVatDetails(vatForm)
            .map {
              _ =>
                subscriptionDetailsService.cacheVatControlListResponse(response)
                if (isInReviewMode)
                  Redirect(
                    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
                      .determineRoute(service)
                  )
                else
                  Redirect(DateOfVatRegistrationController.createForm(service))
            }
        else
          subscriptionDetailsService.clearCachedVatControlListResponse().flatMap(
            _ => Future.successful(Redirect(redirectNext(service)))
          )
    )

  def vatDetailsNotMatched(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      sessionCacheService.individualAndSoleTraderRouter(
        user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
        service,
        Ok(weCannotConfirmYourIdentity(dateForm, service))
      )
    }

  private def redirectNext(service: Service)(implicit request: Request[AnyContent]) = {
    val userLocation =
      requestSessionData.selectedUserLocation.getOrElse(throw DataUnavailableException("User Location not set"))
    if (userLocation == Iom) {
      YourVatDetailsController.vatDetailsNotMatched(service)
    } else {
      VatDetailsController.vatDetailsNotMatched(service)
    }
  }

}
