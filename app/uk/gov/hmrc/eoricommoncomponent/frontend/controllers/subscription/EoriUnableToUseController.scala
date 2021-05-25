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

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{ExistingEori, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.EoriUnableToUse
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  EnrolmentStoreProxyService,
  SubscriptionBusinessService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_unable_to_use

import scala.concurrent.{ExecutionContext, Future}

class EoriUnableToUseController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  mcc: MessagesControllerComponents,
  eoriUnableToUsePage: eori_unable_to_use
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionBusinessService.cachedEoriNumber.flatMap { eoriOpt =>
        eoriOpt match {
          case Some(eori) =>
            enrolmentStoreProxyService.isEnrolmentInUse(service, ExistingEori(eori, service.enrolmentKey)).map {
              existingEori =>
                if (existingEori.isDefined) Ok(eoriUnableToUsePage(service, eori, EoriUnableToUse.form()))
                else
                  Redirect(
                    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.WhatIsYourEoriController.createForm(
                      service
                    )
                  )
            }
          case _ =>
            Future.successful(
              Redirect(
                uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.WhatIsYourEoriController.createForm(
                  service
                )
              )
            )
        }
      }
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      EoriUnableToUse.form().bindFromRequest().fold(
        formWithErrors =>
          subscriptionBusinessService.cachedEoriNumber.map { eoriOpt =>
            eoriOpt match {
              case Some(eori) => BadRequest(eoriUnableToUsePage(service, eori, formWithErrors))
              case _ =>
                Redirect(
                  uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.WhatIsYourEoriController.createForm(
                    service
                  )
                )
            }
          },
        answer =>
          if (answer.isAnswerChangeEori())
            Future.successful(Redirect(routes.WhatIsYourEoriController.createForm(service)))
          else Future.successful(Redirect(routes.EoriUnableToUseSignoutController.displayPage(service)))
      )
    }

}
