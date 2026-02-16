/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.SubscriptionServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.*
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.MessagingServiceParam
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.*
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionCreateResponse.*
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionService @Inject() (connector: SubscriptionServiceConnector, auditor: Auditor)(implicit ec: ExecutionContext) extends Logging {

  def subscribe(
    registration: RegistrationDetails,
    subscription: SubscriptionDetails,
    cdsOrganisationType: Option[CdsOrganisationType],
    service: Service
  )(implicit hc: HeaderCarrier): Future[SubscriptionResult] = {
    val subRequest = createRequest(registration, subscription, cdsOrganisationType, service)
    val detail =
      JsObject.apply(Seq(("regDetails", Json.toJson(registration)), ("subDetails", Json.toJson(subscription)), ("subRequest", Json.toJson(subRequest))))
    auditor.sendSubscriptionFlowSessionFailureEvent(detail)
    subscribeWithConnector(subRequest)
  }

  def createRequest(
    reg: RegistrationDetails,
    subscription: SubscriptionDetails,
    cdsOrgType: Option[CdsOrganisationType],
    service: Service
  ): SubscriptionRequest =
    reg match {
      case individual: RegistrationDetailsIndividual =>
        SubscriptionCreateRequest.fromIndividual(individual, subscription, cdsOrgType, Some(service))
      case org: RegistrationDetailsOrganisation =>
        SubscriptionCreateRequest.fromOrganisation(org, subscription, cdsOrgType, Some(service))
      case _ =>
        val error = "Incomplete cache cannot complete journey"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
    }

  private def subscribeWithConnector(
    request: SubscriptionRequest
  )(implicit hc: HeaderCarrier): Future[SubscriptionResult] =
    connector.subscribe(request) map { (response: SubscriptionResponse) =>
      val responseCommon = response.subscriptionCreateResponse.responseCommon
      val processingDate = DateTimeFormatter.ofPattern("d MMM y").format(responseCommon.processingDate)
      val emailVerificationTimestamp =
        request.subscriptionCreateRequest.requestDetail.contactInformation.flatMap(_.emailVerificationTimestamp)

      extractPosition(responseCommon.returnParameters) match {
        case MessagingServiceParam.Generate | MessagingServiceParam.Link =>
          SubscriptionSuccessful(
            Eori(response.subscriptionCreateResponse.responseDetail.get.EORINo),
            formBundleId(response),
            processingDate,
            emailVerificationTimestamp
          )
        case MessagingServiceParam.Pending =>
          SubscriptionPending(formBundleId(response), processingDate, emailVerificationTimestamp)
        case MessagingServiceParam.Fail if responseCommon.statusText.exists(_.equalsIgnoreCase(EoriAlreadyExists)) =>
          SubscriptionFailed(EoriAlreadyExists, processingDate)
        case MessagingServiceParam.Fail if responseCommon.statusText.exists(_.equalsIgnoreCase(RequestNotProcessed)) =>
          val message =
            s"Response status of FAIL returned for a SUB02: Create Subscription. statusText: ${responseCommon.statusText.getOrElse("")}. returnParameters : ${responseCommon.returnParameters
                .mkString(", ")}"
          logger.warn(message)
          SubscriptionFailed(RequestNotProcessed, processingDate)
        case MessagingServiceParam.Fail if responseCommon.statusText.exists(_.equalsIgnoreCase(EoriAlreadyAssociated)) =>
          SubscriptionFailed(EoriAlreadyAssociated, processingDate)
        case MessagingServiceParam.Fail if responseCommon.statusText.exists(_.equalsIgnoreCase(SubscriptionInProgress)) =>
          SubscriptionFailed(SubscriptionInProgress, processingDate)
        case MessagingServiceParam.Fail =>
          val message =
            s"Response status of FAIL returned for a SUB02: Create Subscription.${responseCommon.statusText.map(text => s" $text").getOrElse("")}"
          logger.error(message)
          SubscriptionFailed(message, processingDate)
        case _ =>
          val message =
            s"Unknown error returned for a SUB02: Create Subscription"
          logger.error(message)
          SubscriptionFailed(message, processingDate)
      }
    }

  private def extractPosition: List[MessagingServiceParam] => String = { params =>
    extractValueFromMessageParams(MessagingServiceParam.positionParamName, params)
  }

  private def formBundleId: SubscriptionResponse => String = { resp =>
    val params = resp.subscriptionCreateResponse.responseCommon.returnParameters
    extractValueFromMessageParams(MessagingServiceParam.formBundleIdParamName, params)
  }

  private def extractValueFromMessageParams: (String, List[MessagingServiceParam]) => String = { (name, params) =>
    params
      .find(_.paramName == name)
      .fold {
        val error = s"$name parameter is missing in subscription create response"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
      }(_.paramValue)
  }

}
