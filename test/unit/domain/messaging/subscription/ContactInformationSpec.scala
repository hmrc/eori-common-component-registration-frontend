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

package unit.domain.messaging.subscription

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.ContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{ContactDetail, EstablishmentAddress}

import java.time.LocalDateTime

class ContactInformationSpec extends UnitSpec {
  "ContactInformation" should {
    "convert contact Details to ContactInformation" in {
      val date: LocalDateTime = LocalDateTime.now()
      val contactDetail = ContactDetail(
        EstablishmentAddress("Line 1", "City Name", Some("SE28 1AA"), "GB"),
        "John Contact Doe",
        Some("1234567"),
        Some("89067"),
        Some("john.doe@example.com")
      )
      val result =
        ContactInformation.createContactInformation(contactDetail).copy(emailVerificationTimestamp = Some(date))
      val expectedResult = ContactInformation(
        Some("John Contact Doe"),
        Some(true),
        Some("Line 1"),
        Some("City Name"),
        Some("SE28 1AA"),
        Some("GB"),
        Some("1234567"),
        Some("89067"),
        Some("john.doe@example.com"),
        Some(date)
      )

      result shouldBe expectedResult
      result.withEmail("test@gmail.com") shouldBe expectedResult.copy(emailAddress = Some("test@gmail.com"))
    }
  }
}
