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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import play.api.Logging
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.RegistrationDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.transformer.FormDataCreateEoriSubscriptionRequestTransformer
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.CreateEoriSubscriptionNoIdentifier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TaxUDConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  audit: Auditor,
  formDataToRequestTransformer: FormDataCreateEoriSubscriptionRequestTransformer
)(implicit ec: ExecutionContext)
    extends HandleResponses {

  private val fullUrl = url"${appConfig.getServiceUrl("register-subscribe-without-id")}"
  private val X_CORRELATION_ID = "x-correlation-id"

  def createEoriSubscription(
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    userLocation: UserLocation,
    service: Service
  )(implicit hc: HeaderCarrier): Future[EoriHttpResponse] = {
    val createEoriSubscriptionRequest = formDataToRequestTransformer.transform(regDetails, subDetails, userLocation, service)

    httpClient
      .post(fullUrl)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken, ACCEPT -> MimeTypes.JSON, CONTENT_TYPE -> MimeTypes.JSON)
      .withBody(Json.toJson(createEoriSubscriptionRequest))
      .execute
      .flatMap { httpResponse =>
        httpResponse.status match {
          case CREATED =>
            handleResponse[CreateEoriSubscriptionResponse](httpResponse) match {
              case Left(_) =>
                // $COVERAGE-OFF$Loggers
                logger.error(
                  s"Create EORI Subscription succeeded but could not parse response body, Correlation ID is: ${httpResponse.header(X_CORRELATION_ID)}"
                )
                // $COVERAGE-ON
                Future.successful(InvalidResponse)
              case Right(response: CreateEoriSubscriptionResponse) =>
                audit
                  .sendSubscriptionDataEvent(
                    fullUrl.toString,
                    Json.toJson(CreateEoriSubscriptionNoIdentifier(createEoriSubscriptionRequest, response))
                  )
                  .map(_ => SuccessResponse.apply(response))
            }

          case UNPROCESSABLE_ENTITY | BAD_REQUEST | INTERNAL_SERVER_ERROR =>
            // $COVERAGE-OFF$Loggers
            logger.error(
              s"${httpResponse.status} received from EIS, error is: ${Json.prettyPrint(httpResponse.json)}, Correlation ID is: ${httpResponse.header(X_CORRELATION_ID)}"
            )
            // $COVERAGE-ON
            Future.successful(ErrorResponse)
          case _ =>
            // $COVERAGE-OFF$Loggers
            logger.error(
              s"call to create eori subscription failed with status: ${httpResponse.status}, Correlation ID is: ${httpResponse.header(X_CORRELATION_ID)}"
            )
            // $COVERAGE-ON

            Future.successful(ErrorResponse)
        }
      }
      .recover { case NonFatal(e) =>
        // $COVERAGE-OFF$Loggers
        logger.error(s"call to create eori subscription failed: $e")
        // $COVERAGE-ON

        ServiceUnavailableResponse
      }
  }

}
