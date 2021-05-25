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

package unit.forms.models.subscription

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.ContactDetailsSubscribeModel

class ContactDetailsSubscribeModelSpec extends UnitSpec {

  "Contact Details Subscribe Model" should {

    "correctly generate Contact Details Model" in {

      val email     = "john.doe@example.com"
      val fullName  = "Full name"
      val telephone = "01234123123"

      val contactDetailsSubscribeModel = ContactDetailsSubscribeModel(fullName, telephone)

      val expectedContactDetails = ContactDetailsModel(
        fullName = fullName,
        emailAddress = email,
        telephone = telephone,
        None,
        false,
        None,
        None,
        None,
        None
      )

      contactDetailsSubscribeModel.toContactDetailsModel(email) shouldBe expectedContactDetails
    }

    "correctly build contact details subscribe model based on contact details model" in {

      val fullName  = "Full name"
      val telephone = "01234123123"

      val contactDetails = ContactDetailsModel(
        fullName = fullName,
        emailAddress = "john.doe@example.com",
        telephone = telephone,
        None,
        false,
        Some("street"),
        Some("city"),
        Some("postcode"),
        Some("country")
      )

      val expectedContactDetailsSubscribe = ContactDetailsSubscribeModel(fullName, telephone)

      ContactDetailsSubscribeModel.fromContactDetailsModel(contactDetails) shouldBe expectedContactDetailsSubscribe
    }
  }
}
