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

import play.api.Logger
import play.api.i18n.Messages

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.EoriConsentSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.disclose_personal_details_consent

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DisclosePersonalDetailsConsentController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionBusinessService: SubscriptionBusinessService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  disclosePersonalDetailsConsentView: disclose_personal_details_consent,
  subscriptionFlowManager: SubscriptionFlowManager
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {
  private val logger = Logger(this.getClass)

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(
          Ok(
            disclosePersonalDetailsConsentView(
              isInReviewMode = false,
              disclosePersonalDetailsYesNoAnswerForm,
              requestSessionData,
              service,
              textPara2(requestSessionData.userSelectedOrganisationType),
              questionLabel(requestSessionData.userSelectedOrganisationType)
            )
          )
        )
    }

  private def textPara2(cdsOrganisationType: Option[CdsOrganisationType])(implicit messages: Messages): String =
    cdsOrganisationType match {
      case orgType if orgType.contains(CdsOrganisationType.Company) =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.org.para2")
      case _
          if cdsOrganisationType.contains(CdsOrganisationType.Partnership) || cdsOrganisationType.contains(
            CdsOrganisationType.LimitedLiabilityPartnership
          ) =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.partnership.para2")
      case _
          if cdsOrganisationType.contains(
            CdsOrganisationType.CharityPublicBodyNotForProfit
          ) || cdsOrganisationType.contains(CdsOrganisationType.ThirdCountryOrganisation) =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.para2")
      case _ => messages("ecc.subscription.organisation-disclose-personal-details-consent.individual.para2")
    }

  private def questionLabel(cdsOrganisationType: Option[CdsOrganisationType])(implicit messages: Messages): String =
    cdsOrganisationType match {
      case orgType if orgType.contains(CdsOrganisationType.Company) =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.org.question")
      case _
          if cdsOrganisationType.contains(CdsOrganisationType.Partnership) || cdsOrganisationType.contains(
            CdsOrganisationType.LimitedLiabilityPartnership
          ) =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.partnership.question")
      case _
          if cdsOrganisationType.contains(
            CdsOrganisationType.CharityPublicBodyNotForProfit
          ) || cdsOrganisationType.contains(CdsOrganisationType.ThirdCountryOrganisation) =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.question")
      case _ => messages("ecc.subscription.organisation-disclose-personal-details-consent.individual.question")
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.getCachedPersonalDataDisclosureConsent.map { isConsentDisclosed =>
          Ok(
            disclosePersonalDetailsConsentView(
              isInReviewMode = true,
              disclosePersonalDetailsYesNoAnswerForm.fill(YesNo(isConsentDisclosed)),
              requestSessionData,
              service,
              textPara2(requestSessionData.userSelectedOrganisationType),
              questionLabel(requestSessionData.userSelectedOrganisationType)
            )
          )
        }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      disclosePersonalDetailsYesNoAnswerForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                disclosePersonalDetailsConsentView(
                  isInReviewMode,
                  formWithErrors,
                  requestSessionData,
                  service,
                  textPara2(requestSessionData.userSelectedOrganisationType),
                  questionLabel(requestSessionData.userSelectedOrganisationType)
                )
              )
            ),
          yesNoAnswer =>
            subscriptionDetailsService.cacheConsentToDisclosePersonalDetails(yesNoAnswer).flatMap { _ =>
              if (isInReviewMode)
                Future.successful(Redirect(DetermineReviewPageController.determineRoute(service).url))
              else
                subscriptionFlowManager.stepInformation(EoriConsentSubscriptionFlowPage) match {
                  case Right(flowInfo) =>
                    Future.successful(Redirect(flowInfo.nextPage.url(service)))
                  case Left(_) =>
                    logger.warn(s"Unable to identify subscription flow: key not found in cache")
                    Future.successful(Redirect(ApplicationController.startRegister(service)))
                }
            }
        )
    }

}
