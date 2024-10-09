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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.isRow
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.HowCanWeIdentifyYouUtrViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GYEHowCanWeIdentifyYouUtrController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  howCanWeIdentifyYouView: how_can_we_identify_you_utr,
  orgTypeLookup: OrgTypeLookup,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  sessionCacheService: SessionCacheService,
  matchingService: MatchingService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
        Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
      else
        for {
          orgType <- orgTypeLookup.etmpOrgType
        } yield Ok(
          howCanWeIdentifyYouView(
            subscriptionUtrForm,
            isInReviewMode = false,
            routes.GYEHowCanWeIdentifyYouUtrController.submit(service),
            HowCanWeIdentifyYouUtrViewModel.getPageContent(orgType),
            service = service
          )
        )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      orgTypeLookup.etmpOrgType.flatMap(
        orgType =>
          subscriptionUtrForm.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  howCanWeIdentifyYouView(
                    formWithErrors,
                    isInReviewMode = false,
                    routes.GYEHowCanWeIdentifyYouUtrController.submit(service),
                    HowCanWeIdentifyYouUtrViewModel.getPageContent(orgType, orgType.toString()),
                    service = service
                  )
                )
              ),
            formData =>
              for {
                _   <- sessionCache.saveNinoOrUtrDetails(NinoOrUtr(Some(Utr(formData.id))))
                ind <- sessionCacheService.retrieveNameDobFromCache()
                _ = matchingService.matchIndividualWithNino(
                  formData.id,
                  ind,
                  GroupId(user.groupId.getOrElse(throw new Exception("GroupId does not exists")))
                )
              } yield Redirect(PostCodeController.createForm(service))
          )
      )
    }

}
