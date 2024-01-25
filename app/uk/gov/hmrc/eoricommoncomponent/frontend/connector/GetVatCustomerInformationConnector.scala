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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import cats.data.EitherT
import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GetVatInformationResponse, VatControlListResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetVatCustomerInformationConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  val format = new java.text.SimpleDateFormat("yyyy-MM-dd")

  def getVatCustomerInformation(
    vrn: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ResponseError, VatControlListResponse] =
    EitherT {
      val url = url"${appConfig.handleSubscriptionBaseUrl}/vat-customer-information/$vrn"

      httpClient
        .get(url)
        .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
        .execute map { response =>
        // $COVERAGE-OFF$Loggers
        logger.debug(s"vat-customer-information success. response status: ${response.status}")
        // $COVERAGE-ON
        response.status match {
          case OK =>
            val vatResponse      = response.json.as[GetVatInformationResponse]
            val registrationDate = vatResponse.effectiveRegistrationDate.map(date => format.format(date))
            Right(VatControlListResponse(postcode = vatResponse.postCode, dateOfReg = registrationDate))
          case _ =>
            logger.warn(s"getVatCustomerInformation returned status: ${response.status}. Body: ${response.body}")
            Left(ResponseError(response.status, response.body))
        }
      }
    }

}
