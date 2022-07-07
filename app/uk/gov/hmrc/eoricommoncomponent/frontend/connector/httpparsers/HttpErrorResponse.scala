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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.{UpdateVerifiedEmailRequest, UpdateVerifiedEmailResponse}

sealed trait HttpErrorResponse
case object BadRequest         extends HttpErrorResponse
case object ServiceUnavailable extends HttpErrorResponse
case object Forbidden          extends HttpErrorResponse
case object UnhandledException extends HttpErrorResponse

sealed trait HttpSuccessResponse

case class VerifiedEmailResponse(updateVerifiedEmailResponse: UpdateVerifiedEmailResponse) extends HttpSuccessResponse

object VerifiedEmailResponse {
  implicit val format = Json.format[VerifiedEmailResponse]
}

case class VerifiedEmailRequest(updateVerifiedEmailRequest: UpdateVerifiedEmailRequest)

object VerifiedEmailRequest {
  implicit val formats = Json.format[VerifiedEmailRequest]
}
