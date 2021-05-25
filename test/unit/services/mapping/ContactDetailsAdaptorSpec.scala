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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel.{
  sixLineAddressLine1MaxLength,
  sixLineAddressLine2MaxLength,
  townCityMaxLength
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.ContactDetailsAdaptor

class ContactDetailsAdaptorSpec extends UnitSpec with GenTestRunner {

  private val contactDetailsAdaptor = new ContactDetailsAdaptor()

  "ContactDetailsAdaptor" should {

    "convert user inputs from Subscription Contact Details page and registered address" in {
      testWithGen(contactDetailsCreateViewModelAndAddressGenerator) {
        case (getContactDetailsModel, genAddress) =>
          val actual =
            contactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(getContactDetailsModel, genAddress)
          actual.fullName shouldBe getContactDetailsModel.fullName
          actual.emailAddress shouldBe getContactDetailsModel.emailAddress
          actual.telephone shouldBe getContactDetailsModel.telephone
          actual.fax shouldBe getContactDetailsModel.fax
          actual.street shouldBe Some(
            (genAddress.addressLine1.trim.take(sixLineAddressLine1MaxLength) + " " + genAddress.addressLine2
              .getOrElse("")
              .trim
              .take(sixLineAddressLine2MaxLength)).trim
          )
          actual.city shouldBe Some(genAddress.addressLine3.getOrElse("").trim.take(townCityMaxLength))
          actual.postcode shouldBe genAddress.postalCode
          actual.countryCode shouldBe Some(genAddress.countryCode)
      }
    }

    "convert user inputs from Subscription Contact Details page and registered address with empty postcode" in {
      testWithGen(contactDetailsCreateViewModelAndAddressWithEmptyPostcodeGenerator) {
        case (createViewModel, addressOverride) =>
          val actual =
            contactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(createViewModel, addressOverride)
          actual.postcode shouldBe None
      }
    }
  }
}
