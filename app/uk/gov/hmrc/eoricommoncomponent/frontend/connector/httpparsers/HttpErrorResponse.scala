package uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.{UpdateVerifiedEmailRequest, UpdateVerifiedEmailResponse}

sealed trait HttpErrorResponse
case object BadRequest extends HttpErrorResponse
case object ServiceUnavailable extends HttpErrorResponse
case object Forbidden extends HttpErrorResponse
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