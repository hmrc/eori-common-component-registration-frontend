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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.BusinessShortNameController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  EtmpOrganisationType,
  LLP,
  LoggedInUserWithEnrolments,
  Partnership,
  UnincorporatedBody,
  YesNo
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.businessShortNameYesNoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_short_name_yes_no
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessShortNameYesNoController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionFlowManager: SubscriptionFlowManager,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  businessShortNameYesNoPage: business_short_name_yes_no,
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

        val form = companyShortName.fold(businessShortNameYesNoForm(emptyErrorMessage(orgType, isRow)))(
          shortName =>
            if (shortName.shortNameProvided)
              businessShortNameYesNoForm(emptyErrorMessage(orgType, isRow)).fill(YesNo(true))
            else businessShortNameYesNoForm(emptyErrorMessage(orgType, isRow)).fill(YesNo(false))
        )

        Ok(businessShortNameYesNoPage(form, orgType, isRow, isInReviewMode, service))
      }
    }

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      orgTypeLookup.etmpOrgType.flatMap { orgType =>
        val isRow = !requestSessionData.isRegistrationUKJourney

        businessShortNameYesNoForm(emptyErrorMessage(orgType, isRow)).bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(businessShortNameYesNoPage(formWithErrors, orgType, isRow, isInReviewMode, service))
            ),
          formData =>
            if (formData.isYes) saveYesOption(isInReviewMode, service) else saveNoOption(isInReviewMode, service)
        )
      }
    }

  private def saveYesOption(isInReviewMode: Boolean, service: Service)(implicit hc: HeaderCarrier): Future[Result] =
    subscriptionDetailsService.cachedCompanyShortName.flatMap { businessShortName =>
      val updatedBusinessShortName =
        businessShortName.map(_.copy(shortNameProvided = true)).getOrElse(BusinessShortName(true, None))
      subscriptionDetailsService.cacheCompanyShortName(updatedBusinessShortName).map { _ =>
        if (isInReviewMode)
          Redirect(BusinessShortNameController.reviewPage(service))
        else
          Redirect(BusinessShortNameController.displayPage(service))
      }
    }

  private def saveNoOption(isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionDetailsService.cacheCompanyShortName(BusinessShortName(false, None)).map { _ =>
      redirect(isInReviewMode, service)
    }

  private def redirect(isInReviewMode: Boolean, service: Service)(implicit request: Request[AnyContent]): Result =
    if (isInReviewMode)
      Redirect(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController.determineRoute(
          service,
          Journey.Register
        )
      )
    else
      Redirect(subscriptionFlowManager.stepInformation(BusinessShortNameSubscriptionFlowPage).nextPage.url(service))

  private def emptyErrorMessage(orgType: EtmpOrganisationType, isRow: Boolean): String = orgType match {
    case _ if isRow         => "ecc.business-short-name-yes-no.organisation.empty"
    case Partnership        => "ecc.business-short-name-yes-no.partnership.empty"
    case LLP                => "ecc.business-short-name-yes-no.partnership.empty"
    case UnincorporatedBody => "ecc.business-short-name-yes-no.charity.empty"
    case _                  => "ecc.business-short-name-yes-no.company.empty"
  }

}
