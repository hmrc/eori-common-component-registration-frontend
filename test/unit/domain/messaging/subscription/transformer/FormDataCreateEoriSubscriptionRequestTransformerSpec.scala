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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{
  Company,
  Embassy,
  LimitedLiabilityPartnership
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.transformer.FormDataCreateEoriSubscriptionRequestTransformer
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionRequest.{
  ContactInformation,
  VatIdentification
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.{Iom, Uk}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service.gagmr
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.EtmpLegalStatus
import util.TestData

import java.time.LocalDate

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

    "Isle Of Man" - {
      "Company" - {
        val createEoriSubscriptionRequest =
          transformer.transform(givenRegistrationDetailsCompany, givenSubscriptionDetailsCompany, Iom, gagmr)

        "should have legal status Corporate Body" in {
          createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.CorporateBody
        }

        "should have edge case type 02" in {
          createEoriSubscriptionRequest.edgeCaseType shouldBe "02"
        }

        "should have VAT identification when present" in {
          createEoriSubscriptionRequest.vatIdentificationNumbers.value shouldBe List(
            VatIdentification("IM", "123456789")
          )
        }

        "should contain principal economic activity, the first 4 digits of the SIC Code" in {
          createEoriSubscriptionRequest.principalEconomicActivity.value shouldBe "1073"
          givenSubscriptionDetailsCompany.sicCode.value.take(
            4
          ) shouldBe createEoriSubscriptionRequest.principalEconomicActivity.value
        }

        "should have type of person as 02 (Legal Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "2"
        }

        "should have a date established when present" in {
          createEoriSubscriptionRequest.organisation.dateOfEstablishment.value shouldBe "1980-01-01"
        }

        "should have the organisation name" in {
          createEoriSubscriptionRequest.organisation.organisationName shouldBe "Solutions Ltd"
        }
      }

      "LLP" - {
        val createEoriSubscriptionRequest =
          transformer.transform(givenRegistrationDetailsLlp, givenSubscriptionDetailsLLP, Iom, gagmr)

        "should have legal status Llp" in {
          createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.Llp
        }

        "should have edge case type 02" in {
          createEoriSubscriptionRequest.edgeCaseType shouldBe "02"
        }

        "should have VAT identification when present" in {
          createEoriSubscriptionRequest.vatIdentificationNumbers.value shouldBe List(
            VatIdentification("IM", "123456789")
          )
        }

        "should contain principal economic activity, the first 4 digits of the SIC Code" in {
          createEoriSubscriptionRequest.principalEconomicActivity.value shouldBe "8111"
          givenSubscriptionDetailsLLP.sicCode.value.take(
            4
          ) shouldBe createEoriSubscriptionRequest.principalEconomicActivity.value
        }

        "should have type of person as 02 (Legal Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "2"
        }

        "should have a date established when present" in {
          createEoriSubscriptionRequest.organisation.dateOfEstablishment.value shouldBe "1990-12-12"
        }

        "should have the organisation name" in {
          createEoriSubscriptionRequest.organisation.organisationName shouldBe "Top Lawyers"
        }
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

  private def givenRegistrationDetailsCompany: RegistrationDetailsOrganisation = {
    RegistrationDetailsOrganisation(
      None,
      TaxPayerId(""),
      SafeId(""),
      "Solutions Ltd",
      Address("101-104 Piccadilly", Some("Greater London"), Some("London"), None, Some("W1J 7JT"), "GB"),
      None,
      Some(CorporateBody)
    )
  }

  private def givenRegistrationDetailsLlp: RegistrationDetailsOrganisation = {
    givenRegistrationDetailsCompany.copy(name = "Top Lawyers")
  }

  private def givenSubscriptionDetailsEmbassy: SubscriptionDetails = {
    givenSubscriptionDetails(FormData(organisationType = Some(Embassy))).copy(embassyName = Some("Embassy Of Japan"))
  }

  private def givenSubscriptionDetailsCompany: SubscriptionDetails = {
    givenSubscriptionDetails(FormData(organisationType = Some(Company)))
      .copy(
        dateEstablished = Some(LocalDate.of(1980, 1, 1)),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "123456789")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2017-01-01"))),
        sicCode = Some("10730"),
        nameOrganisationDetails = Some(NameOrganisationMatchModel("Solutions Ltd"))
      )
  }

  private def givenSubscriptionDetailsLLP: SubscriptionDetails = {
    givenSubscriptionDetails(FormData(organisationType = Some(LimitedLiabilityPartnership)))
      .copy(
        dateEstablished = Some(LocalDate.of(1990, 12, 12)),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "123456789")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2015-02-02"))),
        sicCode = Some("8111"),
        nameOrganisationDetails = Some(NameOrganisationMatchModel("Top Lawyers"))
      )
  }

  private def givenSubscriptionDetails(fd: FormData): SubscriptionDetails = {
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
      formData = fd
    )
  }

}
