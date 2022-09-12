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

package unit.controllers

import akka.util.ByteString
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.Request
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.PdfGeneratorConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.DownloadPDFController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.Sub02Outcome
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{eori_number_download, error_template, subscription_download}
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DownloadPDFControllerSpec extends ControllerSpec with AuthActionMock {
  private val mockAuthConnector                      = mock[AuthConnector]
  private val mockAuthAction                         = authAction(mockAuthConnector)
  private val mockPdfGenerator                       = mock[PdfGeneratorConnector]
  private val mockCdsFrontendDataCache: SessionCache = mock[SessionCache]

  private val errorTemplateView        = instanceOf[error_template]
  private val eoriNumberDownloadView   = instanceOf[eori_number_download]
  private val subscriptionDownloadView = instanceOf[subscription_download]

  private val controller = new DownloadPDFController(
    mockAuthAction,
    mockCdsFrontendDataCache,
    mcc,
    errorTemplateView,
    eoriNumberDownloadView,
    subscriptionDownloadView,
    mockPdfGenerator
  )

  "Download" should {

    assertNotLoggedInUserShouldBeRedirectedToLoginPage(
      mockAuthConnector,
      "EORI download",
      controller.download(eoriOnlyService)
    )

    "download EORI PDF for an authenticated user" in {
      val mockSubscribeOutcome = mock[Sub02Outcome]
      when(mockCdsFrontendDataCache.sub02Outcome(any[Request[_]]))
        .thenReturn(Future.successful(mockSubscribeOutcome))
      when(mockSubscribeOutcome.processedDate).thenReturn("01 May 2016")
      when(mockSubscribeOutcome.eori).thenReturn(Some("EN123456789012345"))
      when(mockSubscribeOutcome.fullName).thenReturn("John Doe")

      val pdf  = ByteString("this is a pdf")
      val html = eoriNumberDownloadView("EN123456789012345", "John Doe", "01 May 2016").toString()
      when(mockPdfGenerator.generatePdf(ArgumentMatchers.eq(html))(ArgumentMatchers.any[ExecutionContext])).thenReturn(
        Future(pdf)
      )
      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(controller.download(eoriOnlyService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/pdf")
      header(CONTENT_DISPOSITION, result) shouldBe Some("attachment; filename=EORI-number.pdf")
      contentAsBytes(result) shouldBe pdf
    }

    "display the service unavailable page if the eori is missing from Sub02Outcome" in {
      val mockSubscribeOutcome = mock[Sub02Outcome]
      when(mockCdsFrontendDataCache.sub02Outcome(any[Request[_]]))
        .thenReturn(Future.successful(mockSubscribeOutcome))
      when(mockSubscribeOutcome.processedDate).thenReturn("01 May 2016")
      when(mockSubscribeOutcome.eori).thenReturn(None)
      when(mockSubscribeOutcome.fullName).thenReturn("John Doe")

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(controller.download(eoriOnlyService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    assertNotLoggedInUserShouldBeRedirectedToLoginPage(
      mockAuthConnector,
      "Subscription download",
      controller.download(atarService)
    )

    "download Subscription PDF for an authenticated user" in {
      val mockSubscribeOutcome = mock[Sub02Outcome]
      when(mockCdsFrontendDataCache.sub02Outcome(any[Request[_]]))
        .thenReturn(Future.successful(mockSubscribeOutcome))
      when(mockSubscribeOutcome.processedDate).thenReturn("01 May 2016")
      when(mockSubscribeOutcome.eori).thenReturn(Some("EN123456789012345"))
      when(mockSubscribeOutcome.fullName).thenReturn("John Doe")

      val pdf  = ByteString("this is a pdf")
      val html = subscriptionDownloadView("EN123456789012345", "John Doe", "01 May 2016").toString()
      when(mockPdfGenerator.generatePdf(ArgumentMatchers.eq(html))(ArgumentMatchers.any[ExecutionContext])).thenReturn(
        Future(pdf)
      )
      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(controller.download(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/pdf")
      header(CONTENT_DISPOSITION, result) shouldBe Some("attachment; filename=EORI-number.pdf")
      contentAsBytes(result) shouldBe pdf
    }

    "display the service unavailable page if the subscription eori is missing from Sub02Outcome" in {
      val mockSubscribeOutcome = mock[Sub02Outcome]
      when(mockCdsFrontendDataCache.sub02Outcome(any[Request[_]]))
        .thenReturn(Future.successful(mockSubscribeOutcome))
      when(mockSubscribeOutcome.processedDate).thenReturn("01 May 2016")
      when(mockSubscribeOutcome.eori).thenReturn(None)
      when(mockSubscribeOutcome.fullName).thenReturn("John Doe")

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(controller.download(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
