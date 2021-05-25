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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.BusinessShortNameSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.registration.BusinessShortNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_short_name
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessShortNameController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionFlowManager: SubscriptionFlowManager,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  businessShortNamePage: business_short_name,
  orgTypeLookup: OrgTypeLookup
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments => populateView(service, false)
  }

  def reviewPage(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments => populateView(service, true)
  }

  private def populateView(service: Service, isInReviewMode: Boolean)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionDetailsService.cachedCompanyShortName.flatMap { companyShortName =>
      orgTypeLookup.etmpOrgType.map { orgType =>
        val isRow = !requestSessionData.isRegistrationUKJourney

        val form = companyShortName.fold(BusinessShortNameForm.form(orgType, isRow))(
          shortName => BusinessShortNameForm.form(orgType, isRow).fill(shortName)
        )

        Ok(businessShortNamePage(form, orgType, isRow, isInReviewMode, service))
      }
    }

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      orgTypeLookup.etmpOrgType.flatMap { orgType =>
        val isRow = !requestSessionData.isRegistrationUKJourney

        BusinessShortNameForm.form(orgType, isRow).bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(businessShortNamePage(formWithErrors, orgType, isRow, isInReviewMode, service))
            ),
          shortName =>
            subscriptionDetailsService.cacheCompanyShortName(shortName).map { _ =>
              redirect(service, isInReviewMode)
            }
        )
      }
    }

  private def redirect(service: Service, isInReviewMode: Boolean)(implicit request: Request[AnyContent]): Result =
    if (isInReviewMode)
      Redirect(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController.determineRoute(
          service,
          Journey.Register
        )
      )
    else
      Redirect(subscriptionFlowManager.stepInformation(BusinessShortNameSubscriptionFlowPage).nextPage.url(service))

}
