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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import org.joda.time.format.ISODateTimeFormat
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateVerifiedEmailService @Inject()(
  reqCommonGenerator: RequestCommonGenerator,
  updateVerifiedEmailConnector: UpdateVerifiedEmailConnector,
  customsDataStoreConnector: UpdateCustomsDataStoreConnector
)(implicit ec: ExecutionContext) {

  def updateVerifiedEmail(currentEmail: Option[String] = None, newEmail: String, eori: String)(
    implicit hc: HeaderCarrier
  ): Future[Option[Boolean]] = {

    val requestDetail = RequestDetail(
      IDType = "EORI",
      IDNumber = eori,
      emailAddress = newEmail,
      emailVerificationTimestamp = DateTimeUtil.dateTime
    )
    val request = VerifiedEmailRequest(UpdateVerifiedEmailRequest(reqCommonGenerator.generate(), requestDetail))
    val customsDataStoreRequest = CustomsDataStoreRequest(
      eori,
      newEmail,
      requestDetail.emailVerificationTimestamp.toString(ISODateTimeFormat.dateTimeNoMillis().withZoneUTC())
    )
    updateVerifiedEmailConnector.updateVerifiedEmail(request, currentEmail).map {
      case Right(res)
          if res.updateVerifiedEmailResponse.responseCommon.returnParameters
            .exists(msp => msp.head.paramName == MessagingServiceParam.formBundleIdParamName) =>
        CdsLogger.debug("[UpdateVerifiedEmailService][updateVerifiedEmail] - successfully updated verified email")
        customsDataStoreConnector.updateCustomsDataStore(customsDataStoreRequest)
        Some(true)
      case Right(res) =>
        val statusText = res.updateVerifiedEmailResponse.responseCommon.statusText
        CdsLogger.debug(
          "[UpdateVerifiedEmailService][updateVerifiedEmail]" +
            s" - updating verified email unsuccessful with business error/status code: ${statusText.getOrElse("Status text empty")}"
        )
        Some(false)
      case Left(res) =>
        CdsLogger.warn(
          s"[UpdateVerifiedEmailService][updateVerifiedEmail] - updating verified email unsuccessful with response: $res"
        )
        None
    }
  }
}
