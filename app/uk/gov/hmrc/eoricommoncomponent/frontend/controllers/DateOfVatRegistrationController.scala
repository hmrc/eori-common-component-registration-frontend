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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.CharityPublicBodyNotForProfit
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, VatControlListResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.vat.registrationdate.{VatRegistrationDate, VatRegistrationDateFormProvider}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCacheService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.date_of_vat_registration

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DateOfVatRegistrationController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  dateOfVatRegistrationView: date_of_vat_registration,
  form: VatRegistrationDateFormProvider,
  sessionCacheService: SessionCacheService,
  subscriptionDetailsService: SubscriptionDetailsService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  val vatRegistrationDateForm = form()

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (user: LoggedInUserWithEnrolments) =>
      sessionCacheService.individualAndSoleTraderRouter(
        user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
        service,
        Ok(dateOfVatRegistrationView(vatRegistrationDateForm, service))
      )
    }

  private def lookupDateOfVatRegistration(vatRegistrationDateInput: VatRegistrationDate, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    subscriptionBusinessService.getCachedVatControlListResponse.map {
      case Some(response) if LocalDate.parse(response.dateOfReg.getOrElse("")) == vatRegistrationDateInput.dateOfRegistration =>
        Redirect(ContactDetailsController.createForm(service))
      case _ =>
        Redirect(VatReturnController.redirectToCannotConfirmIdentity(service))
    }
  }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      vatRegistrationDateForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(dateOfVatRegistrationView(formWithErrors, service))),
          formData =>
            subscriptionDetailsService.cachedOrganisationType.flatMap { optOrgType =>
              optOrgType.filter(_ == CharityPublicBodyNotForProfit && appConfig.allowNoIdJourney) match {
                case Some(_) => saveDateOfRegAndRedirect(formData.dateOfRegistration, service)
                case None => lookupDateOfVatRegistration(formData, service)
              }
            }
        )
    }

  private def saveDateOfRegAndRedirect(dateOfReg: LocalDate, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    subscriptionDetailsService
      .cacheVatControlListResponse(VatControlListResponse(dateOfReg = Some(dateOfReg.toString)))
      .map(_ => Redirect(ContactDetailsController.createForm(service)))
  }

}
