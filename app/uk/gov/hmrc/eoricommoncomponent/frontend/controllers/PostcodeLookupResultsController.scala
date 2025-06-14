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

import play.api.data.Form
import play.api.mvc._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{ConfirmContactDetailsController, ManualAddressController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressResultsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{AddressLookupFailure, AddressLookupSuccess}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegistrationDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.postcode_address_result
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostcodeLookupResultsController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  registrationDetailsService: RegistrationDetailsService,
  addressLookupConnector: AddressLookupConnector,
  mcc: MessagesControllerComponents,
  addressLookupResultsPage: postcode_address_result,
  addressResultsForm: AddressResultsForm
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      displayPage(service)
    }

  private def displayPage(service: Service)(implicit request: Request[AnyContent]): Future[Result] =
    sessionCache.getPostcodeAndLine1Details.flatMap {
      case Some(addressLookupParams) =>
        addressLookupConnector
          .lookup(
            addressLookupParams.postcode.replaceAll(" ", ""),
            addressLookupParams.addressLine1
          )
          .flatMap {
            case AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.lookupFieldsDefined) =>
              Future.successful(
                Ok(prepareView(addressResultsForm.form(addresses), addressLookupParams, addresses, service))
              )
            case AddressLookupSuccess(_) if addressLookupParams.addressLine1.exists(_.nonEmpty) =>
              repeatQueryWithoutLine1(addressLookupParams, service)
            case AddressLookupSuccess(_) =>
              Future.successful(redirectToManualAddressPage(service))
            case AddressLookupFailure => throw AddressLookupException
          }
          .recoverWith { case _: AddressLookupException.type =>
            Future.successful(redirectToManualAddressPage(service))
          }
      case _ => Future.successful(redirectToManualAddressPage(service))
    }

  private def prepareView(
    form: Form[Address],
    postcodeViewModel: PostcodeViewModel,
    addresses: Seq[Address],
    service: Service
  )(implicit request: Request[AnyContent]): HtmlFormat.Appendable =
    addressLookupResultsPage(form, postcodeViewModel, addresses, service)

  private def repeatQueryWithoutLine1(postcodeViewModel: PostcodeViewModel, service: Service)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] = {
    val addressLookupParamsWithoutLine1 = PostcodeViewModel(postcodeViewModel.postcode, None)

    addressLookupConnector.lookup(addressLookupParamsWithoutLine1.postcode.replaceAll(" ", ""), None).flatMap {
      case AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.lookupFieldsDefined) =>
        sessionCache.savePostcodeAndLine1Details(addressLookupParamsWithoutLine1).map { _ =>
          Ok(prepareView(addressResultsForm.form(addresses), addressLookupParamsWithoutLine1, addresses, service))
        }
      case AddressLookupSuccess(_) => Future.successful(redirectToManualAddressPage(service))
      case AddressLookupFailure => throw AddressLookupException
    }
  }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.getPostcodeAndLine1Details.flatMap {
        case Some(addressLookupParams) =>
          addressLookupConnector
            .lookup(
              addressLookupParams.postcode.replaceAll(" ", ""),
              addressLookupParams.addressLine1
            )
            .flatMap {
              case AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.lookupFieldsDefined) =>
                addressResultsForm
                  .form(addresses)
                  .bindFromRequest()
                  .fold(
                    formWithErrors => Future.successful(BadRequest(prepareView(formWithErrors, addressLookupParams, addresses, service))),
                    address => Future.successful(Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false)))
                  )
              case AddressLookupSuccess(_) => Future.successful(redirectToManualAddressPage(service))
              case AddressLookupFailure => throw AddressLookupException
            }
            .recoverWith { case _: AddressLookupException.type =>
              Future.successful(redirectToManualAddressPage(service))
            }
        case _ => Future.successful(redirectToManualAddressPage(service))

      }
    }

  private def redirectToManualAddressPage(service: Service): Result =
    Redirect(ManualAddressController.createForm(service))

  case object AddressLookupException extends Exception("Address Lookup service is not available")
}
