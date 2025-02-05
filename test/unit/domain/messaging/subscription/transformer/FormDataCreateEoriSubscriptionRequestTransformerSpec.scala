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

package unit.domain.messaging.subscription.transformer

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.Embassy
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionRequest.ContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.transformer.FormDataCreateEoriSubscriptionRequestTransformer
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Uk
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{RegistrationDetailsEmbassy, SafeId}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service.gagmr
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.EtmpLegalStatus
import util.TestData

class FormDataCreateEoriSubscriptionRequestTransformerSpec
    extends AnyFreeSpec with Matchers with TestData with OptionValues {

  val transformer = new FormDataCreateEoriSubscriptionRequestTransformer

  "transform" - {
    "embassy" - {

      val createEoriSubscriptionRequest =
        transformer.transform(givenRegistrationDetailsEmbassy, givenSubscriptionDetailsEmbassy, Uk, gagmr)

      "should have legal status 'diplomatic mission'" in {
        createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.Embassy
      }

      "should have an empty date of establishment" in {
        createEoriSubscriptionRequest.organisation.dateOfEstablishment shouldBe empty
      }

      "should have an edge case type 01" in {
        createEoriSubscriptionRequest.edgeCaseType shouldBe "01"
      }

      "should not use embassy contact information when set in subscription details" in {
        createEoriSubscriptionRequest.contactInformation.value shouldBe ContactInformation(
          "Toby Bill",
          "Victoria Rd Greater London",
          "London",
          "GB",
          isAgent = true,
          isGroup = false,
          Some("tom.tell@gmail.com"),
          None,
          None,
          Some("NW11 1RP"),
          Some("07806674501")
        )
      }

      "should format CDS Establishment Address Street & Number correctly" in {
        createEoriSubscriptionRequest.cdsEstablishmentAddress.streetAndNumber shouldBe "101-104 Piccadilly, Greater London"
      }
    }
  }

  private def givenRegistrationDetailsEmbassy: RegistrationDetailsEmbassy = {
    RegistrationDetailsEmbassy(
      embassyName = "Embassy Of Japan",
      embassyAddress =
        Address("101-104 Piccadilly", Some("Greater London"), Some("London"), None, Some("W1J 7JT"), "GB"),
      embassyCustomsId = None,
      embassySafeId = SafeId("")
    )
  }

  private def givenSubscriptionDetailsEmbassy: SubscriptionDetails = {
    SubscriptionDetails(
      personalDataDisclosureConsent = Some(true),
      contactDetails = Some(
        ContactDetailsModel(
          "Toby Bill",
          "tom.tell@gmail.com",
          "07806674501",
          None,
          useAddressFromRegistrationDetails = false,
          Some("Victoria Rd Greater London"),
          Some("London"),
          Some("NW11 1RP"),
          Some("GB")
        )
      ),
      formData = FormData(organisationType = Some(Embassy)),
      embassyName = Some("Embassy Of Japan")
    )
  }

}
