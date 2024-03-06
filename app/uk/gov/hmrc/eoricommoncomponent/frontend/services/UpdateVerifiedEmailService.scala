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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.Logger
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{UpdateCustomsDataStoreConnector, UpdateVerifiedEmailConnector}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.CustomsDataStoreRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{MessagingServiceParam, RegistrationInfoRequest}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.ZoneOffset
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateVerifiedEmailService @Inject() (
  reqCommonGenerator: RequestCommonGenerator,
  updateVerifiedEmailConnector: UpdateVerifiedEmailConnector,
  customsDataStoreConnector: UpdateCustomsDataStoreConnector
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def updateVerifiedEmail(newEmail: String, eori: String)(implicit hc: HeaderCarrier): Future[Boolean] = {

    val requestDetail = RequestDetail(
      IDType = RegistrationInfoRequest.EORI,
      IDNumber = eori,
      emailAddress = newEmail,
      emailVerificationTimestamp =  DateTimeUtil.dateTime
    )
    val request = VerifiedEmailRequest(UpdateVerifiedEmailRequest(reqCommonGenerator.generate(), requestDetail))
    val customsDataStoreRequest = CustomsDataStoreRequest(
      eori,
      newEmail,
      requestDetail.emailVerificationTimestamp.atZone(ZoneOffset.UTC).toString
    )
    updateVerifiedEmailConnector.updateVerifiedEmail(request).map {
      case Right(res)
          if res.updateVerifiedEmailResponse.responseCommon.returnParameters
            .exists(msp => msp.head.paramName == MessagingServiceParam.formBundleIdParamName) =>
        // $COVERAGE-OFF$Loggers
        logger.debug("[UpdateVerifiedEmailService][updateVerifiedEmail] - successfully updated verified email")
        // $COVERAGE-ON

        customsDataStoreConnector.updateCustomsDataStore(customsDataStoreRequest)
        true
      case Right(res) =>
        val statusText = res.updateVerifiedEmailResponse.responseCommon.statusText
        // $COVERAGE-OFF$Loggers
        logger.warn(
          "[UpdateVerifiedEmailService][updateVerifiedEmail]" +
            s" - updating verified email unsuccessful with business error/status code: ${statusText.getOrElse("Status text empty")}"
        )
        // $COVERAGE-ON
        false
      case Left(res) =>
        // $COVERAGE-OFF$Loggers
        logger.warn(
          s"[UpdateVerifiedEmailService][updateVerifiedEmail] - updating verified email unsuccessful with response: $res"
        )
        // $COVERAGE-ON
        false
    }
  }

}
