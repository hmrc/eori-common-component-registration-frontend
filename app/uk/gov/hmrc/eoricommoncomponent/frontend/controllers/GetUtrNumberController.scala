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

import javax.inject.{Inject, Singleton}
import java.time.LocalDate
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Action, _}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.individualOrganisationIds
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.Organisation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{MatchingService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.HowCanWeIdentifyYouUtrViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.how_can_we_identify_you_utr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetUtrNumberController @Inject() (
  authAction: AuthAction,
  matchingService: MatchingService,
  mcc: MessagesControllerComponents,
  matchOrganisationUtrView: how_can_we_identify_you_utr,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(organisationType: String, service: Service, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(view(subscriptionUtrForm, organisationType, isInReviewMode, service)))
    }

  def submit(organisationType: String, service: Service, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      subscriptionUtrForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, organisationType, isInReviewMode, service))),
        formData =>
          matchBusinessOrIndividual(formData, isInReviewMode, service, organisationType, GroupId(loggedInUser.groupId))
      )
    }

  private def matchBusiness(
    id: CustomsId,
    name: String,
    dateEstablished: Option[LocalDate],
    matchingServiceType: String,
    groupId: GroupId
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Boolean] =
    matchingService.matchBusiness(id, Organisation(name, matchingServiceType), dateEstablished, groupId)

  private def matchIndividual(id: CustomsId, groupId: GroupId)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Boolean] =
    subscriptionDetailsService.cachedNameDobDetails flatMap {
      case Some(details) =>
        matchingService.matchIndividualWithId(
          id,
          Individual.withLocalDate(details.firstName, details.lastName, details.dateOfBirth),
          groupId
        )
      case None => Future.successful(false)
    }

  private def view(form: Form[IdMatchModel], organisationType: String, isInReviewMode: Boolean, service: Service)(
    implicit request: Request[AnyContent]
  ): HtmlFormat.Appendable =
    matchOrganisationUtrView(
      form,
      isInReviewMode,
      routes.GetUtrNumberController.submit(organisationType, service, isInReviewMode),
      HowCanWeIdentifyYouUtrViewModel.forHintMessage(EtmpOrganisationType(CdsOrganisationType(organisationType))),
      HowCanWeIdentifyYouUtrViewModel.heading(organisationType),
      service
    )

  private def matchBusinessOrIndividual(
    formData: IdMatchModel,
    isInReviewMode: Boolean,
    service: Service,
    organisationType: String,
    groupId: GroupId
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    (organisationType match {
      case CdsOrganisationType.ThirdCountrySoleTraderId | CdsOrganisationType.ThirdCountryIndividualId =>
        matchIndividual(Utr(formData.id), groupId)
      case orgType =>
        subscriptionDetailsService.cachedNameDetails.flatMap {
          case Some(NameOrganisationMatchModel(name)) =>
            matchBusiness(
              Utr(formData.id),
              name,
              None,
              EtmpOrganisationType(CdsOrganisationType(orgType)).toString,
              groupId
            )
          case None => Future.successful(false)
        }
    }).map { matched =>
      if (matched)
        Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false))
      else
        matchNotFoundBadRequest(organisationType, formData, isInReviewMode, service)
    }

  private def matchNotFoundBadRequest(
    organisationType: String,
    formData: IdMatchModel,
    isInReviewMode: Boolean,
    service: Service
  )(implicit request: Request[AnyContent]): Result = {
    val errorMsg = organisationType match {
      case orgType if individualOrganisationIds.contains(orgType) =>
        Messages("cds.matching-error.individual-not-found")
      case _ => Messages("cds.matching-error-organisation.not-found")
    }
    val errorForm = subscriptionUtrForm.withGlobalError(errorMsg).fill(formData)
    BadRequest(view(errorForm, organisationType, isInReviewMode, service))
  }

}
