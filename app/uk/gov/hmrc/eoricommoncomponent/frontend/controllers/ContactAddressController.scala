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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{CharityPublicBodyNotForProfit, Embassy}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactAddressSubscriptionFlowPageGetEori
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.contact_address

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactAddressController @Inject() (
  authAction: AuthAction,
  sessionCacheService: SessionCacheService,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionBusinessService: SubscriptionBusinessService,
  cdsFrontendDataCache: SessionCache,
  subscriptionFlowManager: SubscriptionFlowManager,
  mcc: MessagesControllerComponents,
  contactAddressView: contact_address
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val logger = Logger(this.getClass)

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      populateFormGYE(user, service)(false)
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      populateFormGYE(user, service)(true)
    }

  private def populateFormGYE(user: LoggedInUserWithEnrolments, service: Service)(
    isInReviewMode: Boolean
  )(implicit request: Request[AnyContent]): Future[Result] =
    populateOkView(isInReviewMode = isInReviewMode, service).flatMap(
      sessionCacheService.individualAndSoleTraderRouter(
        user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
        service,
        _
      )
    )

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      contactAddressDetailsYesNoAnswerForm()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            fetchContactDetails().map { contactAddressDetails =>
              BadRequest(contactAddressView(contactAddressDetails, isInReviewMode, formWithErrors, service))
            },
          yesNoAnswer => saveAddressAndRedirect(isInReviewMode, yesNoAnswer, service)
        )
    }

  private def fetchContactDetails()(implicit request: Request[AnyContent]): Future[AddressViewModel] = {
    cdsFrontendDataCache.subscriptionDetails flatMap { sd =>
      sd.contactDetails match {
        case Some(contactDetails) if contactDetails.street.isDefined =>
          Future.successful(
            AddressViewModel(
              contactDetails.street.getOrElse(""),
              contactDetails.city.getOrElse(""),
              contactDetails.postcode,
              contactDetails.countryCode.getOrElse("")
            )
          )
        case _ =>
          cdsFrontendDataCache.registrationDetails.map(rd => AddressViewModel(rd.address))
      }
    }
  }

  private def saveAddressAndRedirect(isInReviewMode: Boolean, yesNoAnswer: YesNo, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    for {
      addressDetails <- fetchContactDetails()
      contactDetails <- subscriptionBusinessService.cachedContactDetailsModel
      redirection    <- redirect(contactDetails, addressDetails, isInReviewMode, yesNoAnswer, service)
    } yield redirection

  private def redirect(
    contactDetails: Option[ContactDetailsModel],
    addressDetails: AddressViewModel,
    isInReviewMode: Boolean,
    yesNoAnswer: YesNo,
    service: Service
  )(implicit request: Request[AnyContent]): Future[Result] =
    contactDetails match {
      case Some(details) =>
        for {
          _      <- subscriptionDetailsService.cacheContactAddressDetails(addressDetails, details)
          result <- locationByAnswer(isInReviewMode, yesNoAnswer, service)
        } yield result
      case None => Future.successful(Redirect(routes.EmailController.form(service)))
    }

  private def populateOkView(isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    fetchContactDetails() map {
      contactDetails =>
        Ok(contactAddressView(contactDetails, isInReviewMode, contactAddressDetailsYesNoAnswerForm(), service))
    }

  private def locationByAnswer(isInReviewMode: Boolean, yesNoAnswer: YesNo, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    subscriptionDetailsService.cachedOrganisationType.map { optOrgType =>
      if (optOrgType.contains(Embassy) || optOrgType.contains(CharityPublicBodyNotForProfit)) {
        if (yesNoAnswer.isYes) {
          Redirect(DetermineReviewPageController.determineRoute(service))
        } else {
          Redirect(WhatIsYourContactAddressController.showForm(service))
        }
      } else {
        yesNoAnswer match {
          case theAnswer if theAnswer.isYes =>
            if (isInReviewMode) {
              Redirect(DetermineReviewPageController.determineRoute(service))
            } else {
              subscriptionFlowManager.stepInformation(ContactAddressSubscriptionFlowPageGetEori) match {
                case Right(flowInfo) => Redirect(flowInfo.nextPage.url(service))
                case Left(_) =>
                  logger.warn(s"Unable to identify subscription flow: key not found in cache")
                  Redirect(ApplicationController.startRegister(service))
              }
            }
          case _ =>
            Redirect(AddressController.createForm(service))
        }
      }
    }
  }

}
