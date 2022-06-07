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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionTextDownloadController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription_text_download
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionTextDownloadControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {
  val mockAuthConnector = mock[AuthConnector]
  val mockAuthAction    = authAction(mockAuthConnector)
  val mockCache         = mock[SessionCache]

  private val subEoriTextDownloadView = instanceOf[subscription_text_download]

  override def beforeEach: Unit = {
    val mockSubscribeOutcome = mock[Sub02Outcome]
    when(mockCache.sub02Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSubscribeOutcome))
    when(mockSubscribeOutcome.processedDate).thenReturn("23 June 2018")
    when(mockSubscribeOutcome.eori).thenReturn(Some("ZZ123456789000"))
    when(mockSubscribeOutcome.fullName).thenReturn("Test Company")
  }

  val controller = new SubscriptionTextDownloadController(mockAuthAction, mockCache, subEoriTextDownloadView, mcc)

  "download" should {

    assertNotLoggedInUserShouldBeRedirectedToLoginPage(mockAuthConnector, "Sub EORI download", controller.download())

    "download Sub EORI text file for authenticated user" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result = await(controller.download().apply(SessionBuilder.buildRequestWithSession(defaultUserId)))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("plain/text")
      header(CONTENT_DISPOSITION, result) shouldBe Some("attachment; filename=Subscription-EORI-number.txt")
      contentAsString(result).filterNot(_ == '\r') shouldBe
        """HM Revenue & Customs
          |
          |Subscription request received for Test Company
          |
          |issued by HMRC on 23 June 2018
          |
          |Your new EORI number starting with GB is: ZZ123456789000""".stripMargin
    }

    "have Windows-friendly line terminators in the Sub eori text file" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result = await(controller.download().apply(SessionBuilder.buildRequestWithSession(defaultUserId)))

      val content = contentAsString(result)
      val lines   = content.split('\n').drop(1)
      lines.length shouldBe 6
      lines.forall(_.endsWith('\r'.toString)) shouldBe true
    }
  }
}
