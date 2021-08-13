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

package unit.services

import java.time.{Clock, Instant}

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RequestCommon
import java.time.ZoneOffset
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.services._

class RequestCommonGeneratorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockUUIDGenerator = mock[RandomUUIDGenerator]
  private val mockClock         = mock[UtcClock](RETURNS_DEEP_STUBS)
  private val expectedReference = "a83f4bfed34d445cba186c2e97f7d133"

  private val generator = new RequestCommonGenerator(mockUUIDGenerator, mockClock)

  private val in: Instant = Instant.now(Clock.systemUTC())

  override def beforeEach(): Unit = {
    when(mockUUIDGenerator.generateUUIDAsString).thenReturn(expectedReference)
    when(mockClock.generateUtcTime.instant()).thenReturn(in)
  }

  "RequestCommonGenerator" should {

    "generate request date" in {
      generator.receiptDate.toString() should equal(in.toString)
    }

    "call generate without requestParameters" should {

      def withFixture(test: RequestCommon => Unit): Unit = test(generator.generate())

      "create object with CDS regime" in withFixture { requestCommon =>
        requestCommon.regime shouldBe "CDS"
      }

      "create object with receipt date as current time in UTC timezone" in withFixture { requestCommon =>
        requestCommon.receiptDate.getZone should equal(ZoneOffset.UTC)
        requestCommon.receiptDate.toString() shouldBe in.toString
      }

      "create object with acknowledgementReference that is unique and of 32 characters" in withFixture {
        requestCommon =>
          requestCommon.acknowledgementReference shouldBe expectedReference
      }

      "create object with no requestParameters when none are passed" in withFixture { requestCommon =>
        requestCommon.requestParameters shouldBe None
      }
    }
  }
}
