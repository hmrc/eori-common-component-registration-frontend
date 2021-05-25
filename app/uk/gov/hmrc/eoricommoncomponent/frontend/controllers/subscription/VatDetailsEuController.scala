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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatDetailsEuConfirmController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails.EuVatDetailsLimit
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm.euVatForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionVatEUDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_details_eu
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatDetailsEuController @Inject() (
  authAction: AuthAction,
  vatEUDetailsService: SubscriptionVatEUDetailsService,
  mcc: MessagesControllerComponents,
  vatDetailsEuView: vat_details_eu
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service, journey: Journey.Value, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      isEuVatDetailsSeqOnLimit map {
        case true => Redirect(VatDetailsEuConfirmController.createForm(service, journey))
        case _ =>
          Ok(vatDetailsEuView(euVatForm, Countries.eu, isInReviewMode = false, service = service, journey = journey))
      }
    }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        isEuVatDetailsSeqOnLimit map {
          case true => Redirect(VatDetailsEuConfirmController.reviewForm(service, journey))
          case _ =>
            Ok(vatDetailsEuView(euVatForm, Countries.eu, isInReviewMode = true, service = service, journey = journey))
        }
    }

  def submit(service: Service, journey: Journey.Value, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatEUDetailsService.cachedEUVatDetails.flatMap { vatEUDetailsModel =>
        euVatForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                vatDetailsEuView(
                  formWithErrors,
                  Countries.eu,
                  isInReviewMode = isInReviewMode,
                  service = service,
                  journey = journey
                )
              )
            ),
          validFormModel =>
            validAddition(validFormModel, vatEUDetailsModel).fold(
              storeVatDetails(validFormModel, service, journey, isInReviewMode)
            )(
              badRequest(
                euVatForm.fill(validFormModel),
                _,
                isInReviewMode = isInReviewMode,
                service = service,
                journey = journey
              )
            )
        )
      }
    }

  def submitUpdate(index: Int, service: Service, journey: Journey.Value, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatEUDetailsService.vatEuDetails(index) flatMap {
        case Some(oldEuVatDetails) =>
          vatEUDetailsService.cachedEUVatDetails flatMap { vatEUDetailsModel =>
            euVatForm.bindFromRequest.fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    vatDetailsEuView(
                      formWithErrors,
                      Countries.eu,
                      service = service,
                      journey = journey,
                      isInReviewMode = isInReviewMode,
                      vatDetails = Option(oldEuVatDetails),
                      updateDetails = true
                    )
                  )
                ),
              newEuVatDetails =>
                validAddition(newEuVatDetails, vatEUDetailsModel, isChanged(oldEuVatDetails, newEuVatDetails))
                  .fold(updateDetails(oldEuVatDetails, newEuVatDetails, service, journey, isInReviewMode))(
                    badRequest(
                      euVatForm.fill(newEuVatDetails),
                      _,
                      service,
                      journey,
                      Option(oldEuVatDetails),
                      updateDetails = true,
                      isInReviewMode
                    )
                  )
            )
          }
        case _ => throw new IllegalStateException("Vat for update not found")
      }
    }

  def updateForm(index: Int, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        vatEUDetailsService.vatEuDetails(index) flatMap {
          case Some(vatDetails) =>
            Future.successful(
              Ok(
                vatDetailsEuView(
                  euVatForm.fill(vatDetails),
                  Countries.eu,
                  updateDetails = true,
                  service,
                  journey,
                  isInReviewMode = false,
                  Option(vatDetails)
                )
              )
            )
          case _ => goToConfirmVat(service, journey, isInReviewMode = false)
        }
    }

  def reviewUpdateForm(index: Int, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        vatEUDetailsService.vatEuDetails(index) flatMap {
          case Some(vatDetails) =>
            Future.successful(
              Ok(
                vatDetailsEuView(
                  euVatForm.fill(vatDetails),
                  Countries.eu,
                  updateDetails = true,
                  service,
                  journey,
                  isInReviewMode = true,
                  Option(vatDetails)
                )
              )
            )
          case _ => goToConfirmVat(service, journey, isInReviewMode = true)
        }
    }

  private def storeVatDetails(
    formData: VatEUDetailsModel,
    service: Service,
    journey: Journey.Value,
    isInReviewMode: Boolean
  )(implicit hc: HeaderCarrier): Future[Result] =
    vatEUDetailsService.saveOrUpdate(formData) flatMap (_ => goToConfirmVat(service, journey, isInReviewMode))

  private def updateDetails(
    oldVatDetails: VatEUDetailsModel,
    newVatDetails: VatEUDetailsModel,
    service: Service,
    journey: Journey.Value,
    isInReviewMode: Boolean
  )(implicit hc: HeaderCarrier): Future[Result] =
    vatEUDetailsService.updateVatEuDetailsModel(oldVatDetails, newVatDetails) flatMap { vatEuDetails =>
      vatEUDetailsService.saveOrUpdate(vatEuDetails) flatMap (_ => goToConfirmVat(service, journey, isInReviewMode))
    }

  private def isChanged(newEuVatDetails: VatEUDetailsModel, oldEuVatDetails: VatEUDetailsModel): Boolean =
    oldEuVatDetails != newEuVatDetails

  private def validAddition(
    newEuVatDetails: VatEUDetailsModel,
    cachedVats: Seq[VatEUDetailsModel],
    isChanged: Boolean = true
  )(implicit messages: Messages): Option[String] =
    if (cachedVats.contains(newEuVatDetails) && isChanged)
      Some(messages("cds.subscription.vat-details.page-duplicate-vat-error"))
    else None

  private def badRequest(
    form: Form[VatEUDetailsModel],
    error: String,
    service: Service,
    journey: Journey.Value,
    vatEUDetailsModel: Option[VatEUDetailsModel] = None,
    updateDetails: Boolean = false,
    isInReviewMode: Boolean
  )(implicit request: Request[AnyContent]): Future[Result] =
    Future.successful(
      BadRequest(
        vatDetailsEuView(
          form.withError("vatNumber", error),
          Countries.eu,
          updateDetails,
          service,
          journey,
          isInReviewMode,
          vatEUDetailsModel
        )
      )
    )

  private def isEuVatDetailsSeqOnLimit(implicit hc: HeaderCarrier): Future[Boolean] =
    vatEUDetailsService.cachedEUVatDetails map (_.size == EuVatDetailsLimit)

  private def goToConfirmVat(service: Service, journey: Journey.Value, isInReviewMode: Boolean) =
    isInReviewMode match {
      case false => Future.successful(Redirect(VatDetailsEuConfirmController.createForm(service, journey)))
      case _     => Future.successful(Redirect(VatDetailsEuConfirmController.reviewForm(service, journey)))
    }

}
