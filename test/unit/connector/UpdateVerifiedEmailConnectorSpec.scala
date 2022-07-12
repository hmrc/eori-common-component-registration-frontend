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

package unit.connector

import base.UnitSpec
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{doNothing, reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Writes
import play.api.test.Injecting
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.UpdateVerifiedEmailConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.{UpdateVerifiedEmailRequest, UpdateVerifiedEmailResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{MessagingServiceParam, RequestCommon, ResponseCommon}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, MethodNotAllowedException, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.RequestDetail
import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class UpdateVerifiedEmailConnectorSpec
    extends UnitSpec with ScalaFutures with MockitoSugar with BeforeAndAfter with Injecting with GuiceOneAppPerTest {

  private val mockAppConfig  = mock[AppConfig]
  private val mockHttpClient = mock[HttpClient]

  private val forbiddenException      = new ForbiddenException("testMessage")
  private val badRequestException     = new BadRequestException("testMessage")
  private val internalServerException = new InternalServerException("testMessage")
  private val unhandledException      = new MethodNotAllowedException("testMessage")

  private val badRequest =
    UpstreamErrorResponse("testMessage", BAD_REQUEST, BAD_REQUEST)

  private val forbidden =
    UpstreamErrorResponse("testMessage", FORBIDDEN, FORBIDDEN)

  private val internalServerError = UpstreamErrorResponse("testMessage", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

  val dateTime = DateTime.now()

  private val requestDetail =
    RequestDetail("idType", "idNumber", "test@email.com", dateTime)

  private val requestDateInd = LocalDateTime.of(2001, 12, 17, 9, 30, 47, 0)

  private val requestCommon = RequestCommon("CDS", requestDateInd, "012345678901234")

  private val verifiedEmailResponse = VerifiedEmailResponse(
    UpdateVerifiedEmailResponse(
      ResponseCommon(
        "OK",
        None,
        LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), 0, ZoneOffset.UTC),
        Some(List(MessagingServiceParam("name", "value")))
      )
    )
  )

  private val verifiedEmailRequest = VerifiedEmailRequest(UpdateVerifiedEmailRequest(requestCommon, requestDetail))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector = new UpdateVerifiedEmailConnector(mockAppConfig, mockHttpClient)

  before {
    reset(mockAppConfig, mockHttpClient)
    when(mockAppConfig.getServiceUrl("update-verified-email"))
      .thenReturn("testUrl/update-verified-email")
  }

  "Calling updateVerifiedEmail" should {
    "return Right with VerifiedEmailResponse when call was successful with OK" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.successful(verifiedEmailResponse))
      val result = connector
        .updateVerifiedEmail(verifiedEmailRequest, Some("old-email-address"))
        .futureValue
      result shouldBe Right(verifiedEmailResponse)
    }

    "return Left with Forbidden when call returned NotFoundException" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(forbiddenException))

      val result =
        connector.updateVerifiedEmail(verifiedEmailRequest, None).futureValue
      result shouldBe Left(Forbidden)
    }

    "return Left with Forbidden when call returned Upstream4xxResponse with 403" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(forbidden))

      val result =
        connector.updateVerifiedEmail(verifiedEmailRequest, None).futureValue
      result shouldBe Left(Forbidden)
    }

    "return Left with BadRequest when call returned BadRequestException" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(badRequestException))

      val result =
        connector.updateVerifiedEmail(verifiedEmailRequest, None).futureValue
      result shouldBe Left(BadRequest)
    }

    "return Left with BadRequest when call returned Upstream4xxResponse with 400" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(badRequest))

      val result =
        connector.updateVerifiedEmail(verifiedEmailRequest, None).futureValue
      result shouldBe Left(BadRequest)
    }

    "return Left with ServiceUnavailable when call returned ServiceUnavailableException" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(internalServerException))

      val result =
        connector.updateVerifiedEmail(verifiedEmailRequest, None).futureValue
      result shouldBe Left(ServiceUnavailable)
    }

    "return Left with ServiceUnavailable when call returned Upstream5xxResponse with 500" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(internalServerError))

      val result =
        connector.updateVerifiedEmail(verifiedEmailRequest, None).futureValue
      result shouldBe Left(ServiceUnavailable)
    }

    "return Left with not handled exception" in {
      when(
        mockHttpClient.PUT[VerifiedEmailRequest, VerifiedEmailResponse](
          any(),
          meq(verifiedEmailRequest),
          any[Seq[(String, String)]]
        )(
          any[Writes[VerifiedEmailRequest]],
          any[HttpReads[VerifiedEmailResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.failed(unhandledException))

      val result =
        await(connector.updateVerifiedEmail(verifiedEmailRequest, None))
      result shouldBe Left(UnhandledException)
    }
  }
}
