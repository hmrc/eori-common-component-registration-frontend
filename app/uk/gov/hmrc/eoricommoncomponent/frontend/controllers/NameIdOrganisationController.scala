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
import play.api.i18n.Messages
import play.api.mvc._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.NameIdOrganisationModel._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.Organisation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.{
  nameUtrCompanyForm,
  nameUtrOrganisationForm,
  nameUtrPartnershipForm
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{MatchingService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.Require._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_name_id_organisation
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NameIdOrganisationController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  matchNameIdOrganisationView: match_name_id_organisation,
  matchingService: MatchingService,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  trait Configuration[M <: NameIdOrganisationMatch] {
    def matchingServiceType: String

    def displayMode: String

    def isNameAddressRegistrationAvailable: Boolean

    def form: Form[M]

    def createCustomsId(id: String): CustomsId
  }

  case class UtrConfiguration(
    matchingServiceType: String,
    displayMode: String,
    isNameAddressRegistrationAvailable: Boolean = false
  ) extends Configuration[NameIdOrganisationMatchModel] {

    val form: Form[NameIdOrganisationMatchModel] = matchingServiceType match {
      case mST if mST == "Partnership" || mST == "LLP" => nameUtrPartnershipForm
      case mST if mST == "Corporate Body"              => nameUtrCompanyForm
      case _                                           => nameUtrOrganisationForm
    }

    def createCustomsId(utr: String): Utr = Utr(utr)
  }

  private val OrganisationTypeConfigurations: Map[String, Configuration[_ <: NameIdOrganisationMatch]] = Map(
    CdsOrganisationType.CompanyId                     -> UtrConfiguration("Company", displayMode = CompanyDM),
    CdsOrganisationType.CompanyId                     -> UtrConfiguration("Corporate Body", displayMode = RegisteredCompanyDM),
    CdsOrganisationType.PartnershipId                 -> UtrConfiguration("Partnership", displayMode = PartnershipDM),
    CdsOrganisationType.LimitedLiabilityPartnershipId -> UtrConfiguration("LLP", displayMode = PartnershipLLpDM),
    CdsOrganisationType.CharityPublicBodyNotForProfitId -> UtrConfiguration(
      "Unincorporated Body",
      displayMode = OrganisationModeDM
    )
  )

  def form(organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      requireThatUrlValue(
        OrganisationTypeConfigurations.contains(organisationType),
        invalidOrganisationType(organisationType)
      )
      Future.successful(Ok(view(organisationType, OrganisationTypeConfigurations(organisationType), service)))
    }

  def submit(organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      requireThatUrlValue(
        OrganisationTypeConfigurations.contains(organisationType),
        invalidOrganisationType(organisationType)
      )
      val configuration = OrganisationTypeConfigurations(organisationType)
      bind(organisationType, configuration, service, GroupId(loggedInUser.groupId))
    }

  private def bind[M <: NameIdOrganisationMatch](
    organisationType: String,
    conf: Configuration[M],
    service: Service,
    groupId: GroupId
  )(implicit request: Request[AnyContent]): Future[Result] =
    conf.form.bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(organisationType, conf, formWithErrors, service))),
        formData =>
          matchBusiness(
            conf.createCustomsId(formData.id),
            formData.name,
            None,
            conf.matchingServiceType,
            groupId
          ) flatMap {
            case true =>
              subscriptionDetailsService.cacheNameDetails(NameOrganisationMatchModel(formData.name)) map {
                _ =>
                  Redirect(
                    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
                      .form(service, isInReviewMode = false)
                  )
              }
            case false => Future.successful(matchNotFoundBadRequest(organisationType, conf, formData, service))
          }
      )

  private def invalidOrganisationType(organisationType: String): Any = s"Invalid organisation type '$organisationType'."

  private def matchBusiness(
    id: CustomsId,
    name: String,
    dateEstablished: Option[LocalDate],
    matchingServiceType: String,
    groupId: GroupId
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Boolean] =
    matchingService.matchBusiness(id, Organisation(name, matchingServiceType), dateEstablished, groupId)

  private def matchNotFoundBadRequest[M <: NameIdOrganisationMatch](
    organisationType: String,
    conf: Configuration[M],
    formData: M,
    service: Service
  )(implicit request: Request[AnyContent]): Result = {
    val errorMsg  = Messages("cds.matching-error.not-found")
    val errorForm = conf.form.withGlobalError(errorMsg).fill(formData)
    BadRequest(view(organisationType, conf, errorForm, service))
  }

  private def view[M <: NameIdOrganisationMatch](
    organisationType: String,
    conf: Configuration[M],
    form: Form[_ <: M],
    service: Service
  )(implicit request: Request[AnyContent]): HtmlFormat.Appendable =
    matchNameIdOrganisationView(
      form,
      organisationType,
      conf.displayMode,
      conf.isNameAddressRegistrationAvailable,
      service
    )

  private def view[M <: NameIdOrganisationMatch](organisationType: String, conf: Configuration[M], service: Service)(
    implicit request: Request[AnyContent]
  ): HtmlFormat.Appendable =
    matchNameIdOrganisationView(
      conf.form,
      organisationType,
      conf.displayMode,
      conf.isNameAddressRegistrationAvailable,
      service
    )

}

object NameIdOrganisationModel {
  val CompanyDM           = "company"
  val RegisteredCompanyDM = "registered-company"
  val PartnershipDM       = "partnership"
  val OrganisationModeDM  = "organisation"
  val PartnershipLLpDM    = "limited-liability-partnership"
}
