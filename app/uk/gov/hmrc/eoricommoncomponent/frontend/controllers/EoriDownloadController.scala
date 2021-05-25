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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.PdfGeneratorConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, Sub02Outcome}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_number_download

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EoriDownloadController @Inject() (
  authAction: AuthAction,
  cdsFrontendDataCache: SessionCache,
  mcc: MessagesControllerComponents,
  errorTemplateView: error_template,
  eoriNumberDownloadView: eori_number_download,
  pdfGenerator: PdfGeneratorConnector
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def download(): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      cdsFrontendDataCache.sub02Outcome.map {
        case Sub02Outcome(processedDate, fullName, Some(eori)) =>
          Right(eoriNumberDownloadView(eori, fullName, processedDate).body)
        case _ => Left(InternalServerError(errorTemplateView()))
      }.flatMap {
        case Right(pdfAsHtml) =>
          pdfGenerator.generatePdf(pdfAsHtml).map { pdfByteStream =>
            Ok(pdfByteStream)
              .as("application/pdf")
              .withHeaders(CONTENT_DISPOSITION -> "attachment; filename=EORI-number.pdf")
          }
        case Left(errorTemplate) => Future.successful(errorTemplate)
      }
  }

}
