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
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsEuConfirmController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails.EuVatDetailsLimit
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm.euVatForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionVatEUDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_details_eu
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

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      isEuVatDetailsSeqOnLimit map {
        case true => Redirect(VatDetailsEuConfirmController.createForm(service))
        case _ =>
          Ok(vatDetailsEuView(euVatForm, Countries.eu, isInReviewMode = false, service = service))
      }
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        isEuVatDetailsSeqOnLimit map {
          case true => Redirect(VatDetailsEuConfirmController.reviewForm(service))
          case _ =>
            Ok(vatDetailsEuView(euVatForm, Countries.eu, isInReviewMode = true, service = service))
        }
    }

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatEUDetailsService.cachedEUVatDetails.flatMap { vatEUDetailsModel =>
        euVatForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                vatDetailsEuView(formWithErrors, Countries.eu, isInReviewMode = isInReviewMode, service = service)
              )
            ),
          validFormModel =>
            validAddition(validFormModel, vatEUDetailsModel).fold(
              storeVatDetails(validFormModel, service, isInReviewMode)
            )(badRequest(euVatForm.fill(validFormModel), _, isInReviewMode = isInReviewMode, service = service))
        )
      }
    }

  def submitUpdate(index: Int, service: Service, isInReviewMode: Boolean): Action[AnyContent] =
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
                      isInReviewMode = isInReviewMode,
                      vatDetails = Option(oldEuVatDetails),
                      updateDetails = true
                    )
                  )
                ),
              newEuVatDetails =>
                validAddition(newEuVatDetails, vatEUDetailsModel, isChanged(oldEuVatDetails, newEuVatDetails))
                  .fold(updateDetails(oldEuVatDetails, newEuVatDetails, service, isInReviewMode))(
                    badRequest(
                      euVatForm.fill(newEuVatDetails),
                      _,
                      service,
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

  def updateForm(index: Int, service: Service): Action[AnyContent] =
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
                  isInReviewMode = false,
                  Option(vatDetails)
                )
              )
            )
          case _ => goToConfirmVat(service, isInReviewMode = false)
        }
    }

  def reviewUpdateForm(index: Int, service: Service): Action[AnyContent] =
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
                  isInReviewMode = true,
                  Option(vatDetails)
                )
              )
            )
          case _ => goToConfirmVat(service, isInReviewMode = true)
        }
    }

  private def storeVatDetails(formData: VatEUDetailsModel, service: Service, isInReviewMode: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    vatEUDetailsService.saveOrUpdate(formData) flatMap (_ => goToConfirmVat(service, isInReviewMode))

  private def updateDetails(
    oldVatDetails: VatEUDetailsModel,
    newVatDetails: VatEUDetailsModel,
    service: Service,
    isInReviewMode: Boolean
  )(implicit hc: HeaderCarrier): Future[Result] =
    vatEUDetailsService.updateVatEuDetailsModel(oldVatDetails, newVatDetails) flatMap { vatEuDetails =>
      vatEUDetailsService.saveOrUpdate(vatEuDetails) flatMap (_ => goToConfirmVat(service, isInReviewMode))
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
          isInReviewMode,
          vatEUDetailsModel
        )
      )
    )

  private def isEuVatDetailsSeqOnLimit(implicit hc: HeaderCarrier): Future[Boolean] =
    vatEUDetailsService.cachedEUVatDetails map (_.size == EuVatDetailsLimit)

  private def goToConfirmVat(service: Service, isInReviewMode: Boolean) =
    isInReviewMode match {
      case false => Future.successful(Redirect(VatDetailsEuConfirmController.createForm(service)))
      case _     => Future.successful(Redirect(VatDetailsEuConfirmController.reviewForm(service)))
    }

}
