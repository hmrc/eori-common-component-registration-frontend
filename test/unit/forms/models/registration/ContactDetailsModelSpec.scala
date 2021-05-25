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

package unit.forms.models.registration

import base.UnitSpec
import org.joda.time.DateTime
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.ContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.{ContactDetailsModel, ContactDetailsViewModel}

class ContactDetailsModelSpec extends UnitSpec {

  private val contactDetailsModel = ContactDetailsModel(
    "name",
    "a@b.com",
    "1234566",
    Some("1234556"),
    false,
    Some("street"),
    Some("city"),
    Some("postCode"),
    Some("GB")
  )

  private val contactDetailsViewModel = ContactDetailsViewModel(
    "name",
    Some("a@b.com"),
    "1234566",
    Some("1234556"),
    false,
    Some("street"),
    Some("city"),
    Some("postCode"),
    Some("GB")
  )

  private def pad(line: String) = s" $line "

  "ContactDetailsModel" should {
    "trim address" in {

      val withSpaces = contactDetailsModel.copy(
        street = Some(pad("street")),
        city = Some(pad("city")),
        postcode = Some(pad("postCode"))
      )

      withSpaces.contactDetails shouldBe contactDetailsModel.contactDetails
      withSpaces.toContactDetailsViewModel shouldBe contactDetailsModel.toContactDetailsViewModel
    }
  }

  "ContactDetailsViewModel" should {
    "trim address" in {

      val withSpaces = contactDetailsViewModel.copy(
        street = Some(pad("street")),
        city = Some(pad("city")),
        postcode = Some(pad("postCode"))
      )

      withSpaces.toContactDetailsModel shouldBe contactDetailsModel
    }
  }

  "Contact Details model" should {

    "correctly convert data with address data" in {

      val contactDetails =
        ContactDetailsModel(
          "Full name",
          "email",
          "01234123123",
          None,
          false,
          Some("street"),
          Some(""),
          Some(""),
          Some("")
        )

      def expectedContactInformation(emailVerificationTimestamp: Option[DateTime]) = ContactInformation(
        personOfContact = Some("Full name"),
        sepCorrAddrIndicator = Some(false),
        telephoneNumber = Some("01234123123"),
        emailAddress = Some("email"),
        emailVerificationTimestamp = emailVerificationTimestamp
      )

      val contactInformation = contactDetails.toRowContactInformation()

      val timestamp = contactInformation.emailVerificationTimestamp

      contactInformation shouldBe expectedContactInformation(timestamp)
    }
  }
}
