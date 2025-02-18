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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.testOnly

import play.api.Logging
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.testOnly.TaxudController.EtmpError.{
  BackendInternalServerError,
  MalformedJson,
  Value
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionResponse.SubscriptionBody
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.{
  CreateEoriSubscriptionRequest,
  CreateEoriSubscriptionResponse
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TaxudController @Inject() (action: DefaultActionBuilder, mcc: MessagesControllerComponents)
    extends FrontendController(mcc) with Logging {

  val X_CORRELATION_ID = "x-correlation-id"

  def createSubscription(): Action[AnyContent] = {
    action.async { implicit request: Request[AnyContent] =>
      request.body.asJson match {
        case None =>
          Future.successful(BadRequest(Json.toJson(MalformedJson())))
        case Some(jsValue) =>
          Json.fromJson[CreateEoriSubscriptionRequest](jsValue) match {
            case JsSuccess(createEoriSubscriptionRequest, _) => process(createEoriSubscriptionRequest, request)
            case JsError(errors) =>
              logger.error(errors.mkString("\n"))
              Future.successful(BadRequest(Json.toJson(MalformedJson())))
          }
      }
    }
  }

  private def process(
    createEoriSubscriptionRequest: CreateEoriSubscriptionRequest,
    request: Request[AnyContent]
  ): Future[Result] = {
    if (isEmbassyOfJapan(createEoriSubscriptionRequest)) {
      Future.successful(createdResponse(request))
    } else if (isleOfManCompanyLlp(createEoriSubscriptionRequest)) {
      Future.successful(createdResponse(request))
    } else if (isleOfManSoleTraderIndividual(createEoriSubscriptionRequest)) {
      Future.successful(createdResponse(request))
    } else if (isleOfManCharityPublicBody(createEoriSubscriptionRequest)) {
      Future.successful(createdResponse(request))
    } else if (ukCharityPublicBody(createEoriSubscriptionRequest)) {
      Future.successful(createdResponse(request))
    } else if (hasGiantVrn(createEoriSubscriptionRequest)) {
      Future.successful(createdResponse(request))
    } else {
      Future.successful(InternalServerError(Json.toJson(BackendInternalServerError())))
    }
  }

  private def createdResponse(request: Request[AnyContent]): Result = {
    Created(Json.toJson(createEoriSubResponse))
      .withHeaders(
        CONTENT_TYPE     -> MimeTypes.JSON,
        DATE             -> LocalDateTime.now().atOffset(ZoneOffset.UTC).format(RFC_1123_DATE_TIME),
        X_CORRELATION_ID -> correlationId(request)
      )
  }

  private def isEmbassyOfJapan(createEoriSubscriptionRequest: CreateEoriSubscriptionRequest): Boolean = {
    createEoriSubscriptionRequest.edgeCaseType == "01" && createEoriSubscriptionRequest.organisation.head.organisationName.toLowerCase == "embassy of japan"
  }

  private def isleOfManCompanyLlp(createEoriSubscriptionRequest: CreateEoriSubscriptionRequest): Boolean = {
    createEoriSubscriptionRequest.edgeCaseType == "02" && createEoriSubscriptionRequest.organisation.exists(
      _.organisationName.toLowerCase == "solutions ltd"
    )
  }

  private def isleOfManSoleTraderIndividual(createEoriSubscriptionRequest: CreateEoriSubscriptionRequest): Boolean = {
    createEoriSubscriptionRequest.edgeCaseType == "02" && createEoriSubscriptionRequest.individual.exists(
      _.firstName.toLowerCase == "thomas"
    )
  }

  private def isleOfManCharityPublicBody(createEoriSubscriptionRequest: CreateEoriSubscriptionRequest): Boolean = {
    createEoriSubscriptionRequest.edgeCaseType == "02" && createEoriSubscriptionRequest.organisation.exists(
      _.organisationName.toLowerCase == "cancer research"
    )
  }

  private def ukCharityPublicBody(createEoriSubscriptionRequest: CreateEoriSubscriptionRequest): Boolean = {
    createEoriSubscriptionRequest.edgeCaseType == "03"
  }

  private def createEoriSubResponse = {
    CreateEoriSubscriptionResponse(SubscriptionBody("123456789222", "WORKLIST", LocalDateTime.now, "XE0000123456789"))
  }

  private def correlationId(request: Request[AnyContent]): String = {
    request.headers.get(X_CORRELATION_ID).getOrElse(UUID.randomUUID().toString)
  }

  private def hasGiantVrn(createEoriSubscriptionRequest: CreateEoriSubscriptionRequest): Boolean = {
    createEoriSubscriptionRequest.vatIdentificationNumbers.exists(
      vids =>
        vids.exists(
          vidn => vidn.vatIdentificationNumber.startsWith("654") || vidn.vatIdentificationNumber.startsWith("8888")
        )
    )
  }

}

object TaxudController {

  sealed trait EtmpError {
    val summary: String
    val value: Value
  }

  object EtmpError {
    case class SourceFaultDetail(detail: Seq[String])

    case class ErrorDetail(
      correlationId: String,
      errorCode: String,
      errorMessage: String,
      source: String,
      sourceFaultDetail: SourceFaultDetail,
      timestamp: String
    )

    case class Value(errorDetail: ErrorDetail)

    case class MalformedJson(
      override val summary: String =
        "HTTP 400 is returned with below payload sample when EIS has received a malformed JSON message",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "400",
          errorMessage = "Invalid JSON document.",
          source = "journey-txe13-service-camel",
          sourceFaultDetail = SourceFaultDetail(Seq("MDGValidationException: Invalid JSON document.")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class BackendInternalServerError(
      override val summary: String =
        "HTTP 500 is returned because something has gone wrong whilst processing on this server",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "500",
          errorMessage = "something has gone wrong with the server",
          source = "Back End",
          sourceFaultDetail = SourceFaultDetail(Seq("Back end has occurred whilst processing the request")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class EtmpInternalServerError(
      override val summary: String =
        "HTTP 500 is returned from EIS",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "500",
          errorMessage = "EIS Server Error",
          source = "journey-eis-service-camel",
          sourceFaultDetail = SourceFaultDetail(Seq("Back end has occurred whilst processing the request")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class ExistingActiveSubscription(
      override val summary: String =
        "Business Partner already has an active Subscription",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "422",
          errorMessage = "Business Partner already has an active Subscription",
          source = "Backend",
          sourceFaultDetail = SourceFaultDetail(Seq("007 - Business Partner already has an active Subscription")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class ApplicationAlreadyInProgress(
      override val summary: String =
        "Application already in progress",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "422",
          errorMessage = "Application already in progress",
          source = "Backend",
          sourceFaultDetail = SourceFaultDetail(Seq("133 - Application already in progress")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class DuplicateSubmission(
      override val summary: String =
        "Duplicate submission reference",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "422",
          errorMessage = "Duplicate submission reference",
          source = "Backend",
          sourceFaultDetail = SourceFaultDetail(Seq("135 - Duplicate submission reference")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class InvalidEdgeCaseType(
      override val summary: String =
        "Invalid Edge Case Type",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "422",
          errorMessage = "Edge Case Type missing/invalid",
          source = "Backend",
          sourceFaultDetail = SourceFaultDetail(Seq("131 - Edge Case Type missing/invalid")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class InvalidIomPostcode(
      override val summary: String =
        "Invalid IoM Postcode",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "422",
          errorMessage = "Invalid IoM Postcode",
          source = "Backend",
          sourceFaultDetail = SourceFaultDetail(Seq("132 - Invalid IoM Postcode")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    case class InvalidRegime(
      override val summary: String =
        "Invalid Regime",
      override val value: Value = Value(
        ErrorDetail(
          correlationId = UUID.randomUUID.toString,
          errorCode = "422",
          errorMessage = "REGIME missing or invalid",
          source = "Backend",
          sourceFaultDetail = SourceFaultDetail(Seq("001 - REGIME missing or invalid")),
          timestamp = LocalDateTime.now.toString
        )
      )
    ) extends EtmpError

    implicit val sourceFaultDetailWrites: OWrites[SourceFaultDetail] = Json.writes[SourceFaultDetail]
    implicit val errorDetailWrites: OWrites[ErrorDetail]             = Json.writes[ErrorDetail]
    implicit val valueWrites: OWrites[Value]                         = Json.writes[Value]
    implicit val malformedJsonWrites: OWrites[MalformedJson]         = Json.writes[MalformedJson]

    implicit val backendInternalServerError: OWrites[BackendInternalServerError] =
      Json.writes[BackendInternalServerError]

    implicit val etmpInternalServerError: OWrites[EtmpInternalServerError] = Json.writes[EtmpInternalServerError]

    implicit val existingActiveSubscription: OWrites[ExistingActiveSubscription] =
      Json.writes[ExistingActiveSubscription]

    implicit val applicationAlreadyInProgress: OWrites[ApplicationAlreadyInProgress] =
      Json.writes[ApplicationAlreadyInProgress]

    implicit val duplicateSubmission: OWrites[DuplicateSubmission] = Json.writes[DuplicateSubmission]

    implicit val invalidEdgeCaseType: OWrites[InvalidEdgeCaseType] = Json.writes[InvalidEdgeCaseType]

    implicit val invalidIomPostcode: OWrites[InvalidIomPostcode] = Json.writes[InvalidIomPostcode]

    implicit val invalidRegime: OWrites[InvalidRegime] = Json.writes[InvalidRegime]

    implicit val writes: Writes[EtmpError] = Writes {
      case mJson: MalformedJson => malformedJsonWrites.writes(mJson)
    }

  }

}
