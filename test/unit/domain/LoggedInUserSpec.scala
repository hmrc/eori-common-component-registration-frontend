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

package unit.domain

import base.UnitSpec
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments

class LoggedInUserSpec extends UnitSpec {

  val loggedInUser: LoggedInUserWithEnrolments = LoggedInUserWithEnrolments(
    Some(AffinityGroup.Individual),
    internalId = Some("id"),
    Enrolments(Set.empty),
    None,
    None,
    None,
    "cred"
  )

  "LoggedInUser" should {
    "return an userId when internalId has a value" in {
      loggedInUser.userId() shouldBe "id"
    }

    "throw an Exception when internalId is 'None'" in {
      val user = loggedInUser.copy(internalId = None)
      intercept[Exception](user.userId())
    }

  }
}
