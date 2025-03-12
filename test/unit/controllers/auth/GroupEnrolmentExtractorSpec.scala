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

package unit.controllers.auth

import base.UnitSpec
import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.ResponseError
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.GroupEnrolmentExtractor
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, GroupId}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.EnrolmentStoreProxyService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GroupEnrolmentExtractorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  private val enrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val enrolmentResponse = EnrolmentResponse("HMRC-CUS-ORG", "ACTIVATED", List.empty)

  private val hc = HeaderCarrier()

  private val groupEnrolmentExtractor = new GroupEnrolmentExtractor(enrolmentStoreProxyService)

  private val groupId = "123456789"

  override protected def afterEach(): Unit = {
    reset(enrolmentStoreProxyService)

    super.afterEach()
  }

  "GroupEnrolmentExtractor" should {
    "groupId has enrolments" in {

      val rightValueForEitherT: Either[ResponseError, List[EnrolmentResponse]] = Right(List(enrolmentResponse))

      mockEnrolmentsForGroup(groupId)(EitherT[Future, ResponseError, List[EnrolmentResponse]] {
        Future.successful(rightValueForEitherT)
      })

      groupEnrolmentExtractor.groupIdEnrolments(groupId)(hc).value.futureValue.map(
        res => res shouldBe List(EnrolmentResponse("HMRC-CUS-ORG", "ACTIVATED", List.empty))
      )
    }
  }

  def mockEnrolmentsForGroup(groupId: String)(response: EitherT[Future, ResponseError, List[EnrolmentResponse]]): Unit =
    when(
      enrolmentStoreProxyService.enrolmentsForGroup(ArgumentMatchers.eq(GroupId(groupId)))(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ) thenReturn response

}
