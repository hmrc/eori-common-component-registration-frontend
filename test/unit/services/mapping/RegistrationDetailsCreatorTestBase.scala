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

package unit.services.mapping

import base.UnitSpec
import common.support.testdata.GenTestRunner
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{MessagingServiceParam, ResponseCommon}
import java.time.ZonedDateTime
import util.scalacheck.TestDataGenerators

private[mapping] abstract class RegistrationDetailsCreatorTestBase
    extends UnitSpec with GenTestRunner with TestDataGenerators {

  protected def uuid() = java.util.UUID.randomUUID.toString

  protected val sapNumber = uuid()
}

private[mapping] trait TestMatchingModels {
  this: RegistrationDetailsCreatorTestBase =>

  val responseCommon = ResponseCommon(
    status = "someStatus",
    statusText = Some("Status text"),
    processingDate = ZonedDateTime.now(),
    returnParameters = Some(List(MessagingServiceParam("SAP_NUMBER", sapNumber)))
  )

}
