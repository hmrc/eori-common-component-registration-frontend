/*
 * Copyright 2021 HM Revenue & Customs
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

package integration

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.http._
import util.externalservices.AuditService
import util.externalservices.ExternalServicesConfig._
import util.externalservices.Save4LaterService._

class Save4LaterConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.handle-subscription.host" -> Host,
        "microservice.services.handle-subscription.port" -> Port,
        "auditing.enabled"                               -> true,
        "auditing.consumer.baseUri.host"                 -> Host,
        "auditing.consumer.baseUri.port"                 -> Port
      )
    )
    .build()

  private lazy val save4LaterConnector = app.injector.instanceOf[Save4LaterConnector]
  implicit val hc: HeaderCarrier       = HeaderCarrier()

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "Save4LaterConnector" should {
    "return successful response with OK status and response body" in {
      stubSave4LaterGET_OK()
      await(save4LaterConnector.get[User](id, emailKey)) must be(Some(responseJson.as[User]))
    }

    "return successful response with NOT FOUND status" in {
      stubSave4LaterGET_NOTFOUND()
      await(save4LaterConnector.get[User](id, emailKey)) mustBe None
    }

    "return a response with BAD REQUEST exception for Get" in {
      stubSave4LaterGET_BAD_REQUEST()

      val caught = intercept[BadRequestException] {
        await(save4LaterConnector.get[User](id, emailKey))
      }
      caught.getMessage must startWith("Status:400")
    }

    "return successful response with Created status and response body" in {
      stubSave4LaterPUT()
      await(save4LaterConnector.put[User](id, emailKey, responseJson)) must be(())
    }

    "return a response with BAD REQUEST exception for Put" in {
      stubSave4LaterPUT_BAD_REQUEST()

      val caught = intercept[BadRequestException] {
        await(save4LaterConnector.put[User](id, emailKey, responseJson))
      }
      caught.getMessage must startWith("Status:400")
    }

    "return successful response with NoContent status for delete" in {
      stubSave4LaterDELETE()
      save4LaterConnector.delete[HttpResponse](id).futureValue mustBe ((): Unit)
    }

    "return BadRequestException with response status NOT FOUND status for unknown entry" in {
      stubSave4LaterNotFoundDELETE()
      intercept[BadRequestException] {
        await(save4LaterConnector.delete[String](id))
      }
    }

    "return successful response with NoContent status for delete key" in {
      stubSave4LaterDELETEKey()
      save4LaterConnector.deleteKey[HttpResponse](id, emailKey).futureValue mustBe ((): Unit)
    }

    "return BadRequestException with response status NOT FOUND status for unknown key entry" in {
      stubSave4LaterNotFoundDELETEKey()
      intercept[BadRequestException] {
        await(save4LaterConnector.delete[String](id))
      }
    }
  }
}
