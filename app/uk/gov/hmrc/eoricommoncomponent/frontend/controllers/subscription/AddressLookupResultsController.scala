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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.AddressLookupErrorController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.AddressDetailsSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{AddressLookupParams, AddressResultsForm}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{
  AddressLookup,
  AddressLookupFailure,
  AddressLookupSuccess
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.address_lookup_results
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupResultsController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  subscriptionFlowManager: SubscriptionFlowManager,
  addressLookupConnector: AddressLookupConnector,
  mcc: MessagesControllerComponents,
  addressLookupResultsPage: address_lookup_results
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      displayPage(service, false)
    }

  def reviewPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      displayPage(service, true)
    }

  private def displayPage(service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    sessionCache.addressLookupParams.flatMap {
      case Some(addressLookupParams) =>
        addressLookupConnector.lookup(addressLookupParams.postcode, addressLookupParams.line1).flatMap { response =>
          response match {
            case AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.nonEmpty) =>
              Future.successful(
                Ok(
                  prepareView(
                    AddressResultsForm.form(addresses.map(_.dropDownView)),
                    addressLookupParams,
                    addresses,
                    isInReviewMode,
                    service
                  )
                )
              )
            case AddressLookupSuccess(_) if addressLookupParams.line1.exists(_.nonEmpty) =>
              repeatQueryWithoutLine1(addressLookupParams, service, isInReviewMode)
            case AddressLookupSuccess(_) =>
              Future.successful(redirectToNoResultsPage(service, isInReviewMode))
            case AddressLookupFailure => throw AddressLookupException
          }
        }.recoverWith {
          case _: AddressLookupException.type => Future.successful(redirectToErrorPage(service, isInReviewMode))
        }
      case _ => Future.successful(redirectToPostcodePage(service, isInReviewMode))
    }

  private def prepareView(
    form: Form[AddressResultsForm],
    addressLookupParams: AddressLookupParams,
    addresses: Seq[AddressLookup],
    isInReviewMode: Boolean,
    service: Service
  )(implicit request: Request[AnyContent]): HtmlFormat.Appendable = {
    val selectedOrganisationType = requestSessionData.userSelectedOrganisationType.getOrElse(
      throw new IllegalStateException("Organisation type is not cached")
    )

    addressLookupResultsPage(form, addressLookupParams, addresses, isInReviewMode, selectedOrganisationType, service)
  }

  private def repeatQueryWithoutLine1(
    addressLookupParams: AddressLookupParams,
    service: Service,
    isInReviewMode: Boolean
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    val addressLookupParamsWithoutLine1 = AddressLookupParams(addressLookupParams.postcode, None, true)

    addressLookupConnector.lookup(addressLookupParamsWithoutLine1.postcode, None).flatMap { secondResponse =>
      secondResponse match {
        case AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.nonEmpty) =>
          sessionCache.saveAddressLookupParams(addressLookupParamsWithoutLine1).map { _ =>
            Ok(
              prepareView(
                AddressResultsForm.form(addresses.map(_.dropDownView)),
                addressLookupParamsWithoutLine1,
                addresses,
                isInReviewMode,
                service
              )
            )
          }
        case AddressLookupSuccess(_) => Future.successful(redirectToNoResultsPage(service, isInReviewMode))
        case AddressLookupFailure    => throw AddressLookupException
      }
    }
  }

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.addressLookupParams.flatMap {
        case Some(addressLookupParams) =>
          addressLookupConnector.lookup(addressLookupParams.postcode, addressLookupParams.line1).flatMap { response =>
            response match {
              case AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.nonEmpty) =>
                val addressesMap  = addresses.map(address => address.dropDownView -> address).toMap
                val addressesView = addressesMap.keys.toSeq

                AddressResultsForm.form(addressesView).bindFromRequest.fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(prepareView(formWithErrors, addressLookupParams, addresses, isInReviewMode, service))
                    ),
                  validAnswer => {
                    val address = addressesMap(validAnswer.address).toAddressViewModel

                    subscriptionDetailsService.cacheAddressDetails(address).map { _ =>
                      if (isInReviewMode)
                        Redirect(DetermineReviewPageController.determineRoute(service, Journey.Subscribe))
                      else
                        Redirect(
                          subscriptionFlowManager
                            .stepInformation(AddressDetailsSubscriptionFlowPage)
                            .nextPage
                            .url(service)
                        )
                    }
                  }
                )
              case AddressLookupSuccess(_) => Future.successful(redirectToNoResultsPage(service, isInReviewMode))
              case AddressLookupFailure    => throw AddressLookupException
            }
          }.recoverWith {
            case _: AddressLookupException.type => Future.successful(redirectToErrorPage(service, isInReviewMode))
          }
        case _ => Future.successful(redirectToPostcodePage(service, isInReviewMode))

      }
    }

  private def redirectToNoResultsPage(service: Service, isInReviewMode: Boolean): Result =
    if (isInReviewMode) Redirect(AddressLookupErrorController.reviewNoResultsPage(service))
    else Redirect(AddressLookupErrorController.displayNoResultsPage(service))

  private def redirectToPostcodePage(service: Service, isInReviewMode: Boolean): Result =
    if (isInReviewMode) Redirect(routes.AddressLookupPostcodeController.reviewPage(service))
    else Redirect(routes.AddressLookupPostcodeController.displayPage(service))

  private def redirectToErrorPage(service: Service, isInReviewMode: Boolean): Result =
    if (isInReviewMode) Redirect(AddressLookupErrorController.reviewErrorPage(service))
    else Redirect(AddressLookupErrorController.displayErrorPage(service))

  case object AddressLookupException extends Exception("Address Lookup service is not available")
}
