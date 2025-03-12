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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{eori_number_text_download, subscription_text_download}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DownloadTextController @Inject() (
  authAction: AuthAction,
  cdsFrontendDataCache: SessionCache,
  eoriNumberTextDownloadView: eori_number_text_download,
  subscriptionTextDownloadView: subscription_text_download,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val plainText          = "plain/text"
  private val attachmentTextFile = "attachment; filename=EORI-number.txt"

  def download(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        Some(eori) <- cdsFrontendDataCache.sub02Outcome.map(_.eori)
        name       <- cdsFrontendDataCache.sub02Outcome.map(_.fullName.replaceAll("\\s+", " "))
        processedDate <- cdsFrontendDataCache.sub02Outcome
          .map(_.processedDate)
      } yield
        if (service.code.equalsIgnoreCase(Service.eoriOnly.code))
          Ok(eoriNumberTextDownloadView(eori, name, processedDate))
            .as(plainText)
            .withHeaders(CONTENT_DISPOSITION -> attachmentTextFile)
        else
          Ok(subscriptionTextDownloadView(eori, processedDate))
            .as(plainText)
            .withHeaders(CONTENT_DISPOSITION -> attachmentTextFile)
  }

}
