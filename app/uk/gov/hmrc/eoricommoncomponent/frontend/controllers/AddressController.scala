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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm.addressDetailsCreateForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressController @Inject() (
  authorise: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  addressView: address
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionBusinessService.address.flatMap {
        populateOkView(_, isInReviewMode = false, service)
      }
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionBusinessService.addressOrException flatMap { cdm =>
        populateOkView(Some(cdm), isInReviewMode = true, service)
      }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authorise.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      addressDetailsCreateForm().bindFromRequest
        .fold(
          formWithErrors => populateCountriesToInclude(isInReviewMode, service, formWithErrors, BadRequest),
          address => {
            subscriptionDetailsService.cacheAddressDetails(address)
            updateRegistrationAddress(address).flatMap { _ =>
              showReviewPage(address, isInReviewMode, service)
            }
          }
        )
    }

  private def updateRegistrationAddress(address: AddressViewModel)(implicit hc: HeaderCarrier): Future[Boolean] =
    sessionCache.registrationDetails.map {
      case org: RegistrationDetailsOrganisation =>
        org.copy(address = Address(address))
      case ind: RegistrationDetailsIndividual =>
        ind.copy(address = Address(address))
    }.map { rd =>
      sessionCache.saveRegistrationDetails(rd)
    }.flatMap(identity)

  private def populateCountriesToInclude(
    isInReviewMode: Boolean,
    service: Service,
    form: Form[AddressViewModel],
    status: Status
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]) =
    sessionCache.registrationDetails flatMap { rd =>
      subscriptionDetailsService.cachedCustomsId flatMap { cid =>
        val (countriesToInclude, countriesInCountryPicker) =
          (rd.customsId, cid) match {
            case (Some(_: Utr | _: Nino), _) | (_, Some(_: Utr | _: Nino)) =>
              Countries.getCountryParameters(None)
            case _ =>
              Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)
          }
        val isRow = UserLocation.isRow(requestSessionData)
        Future.successful(
          status(
            addressView(
              form,
              countriesToInclude,
              countriesInCountryPicker,
              isInReviewMode,
              service,
              requestSessionData.isIndividualOrSoleTrader,
              requestSessionData.isPartnership,
              requestSessionData.isCompany,
              isRow
            )
          )
        )
      }
    }

  private def populateOkView(address: Option[AddressViewModel], isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] = {
    lazy val form = address.fold(addressDetailsCreateForm())(addressDetailsCreateForm().fill(_))
    populateCountriesToInclude(isInReviewMode, service, form, Ok)
  }

  private def showReviewPage(address: AddressViewModel, inReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    subscriptionDetailsService.cacheAddressDetails(address).flatMap { _ =>
      if (inReviewMode)
        Future.successful(Redirect(DetermineReviewPageController.determineRoute(service)))
      else
        Future.successful(Redirect(ConfirmContactDetailsController.form(service)))
    }

}
