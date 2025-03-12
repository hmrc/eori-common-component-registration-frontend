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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{CharityPublicBodyNotForProfit, Company, Embassy, Individual, LimitedLiabilityPartnership, SoleTrader}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.transformer.FormDataCreateEoriSubscriptionRequestTransformer
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionRequest.{ContactInformation, VatIdentification}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.{Iom, Uk}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service.gagmr
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.EtmpLegalStatus
import util.TestData

import java.time.LocalDate

class FormDataCreateEoriSubscriptionRequestTransformerSpec extends AnyFreeSpec with Matchers with TestData with OptionValues {

  val transformer = new FormDataCreateEoriSubscriptionRequestTransformer

  "transform" - {
    "embassy" - {

      val createEoriSubscriptionRequest =
        transformer.transform(givenRegistrationDetailsEmbassy, givenSubscriptionDetailsEmbassy, Uk, gagmr)

      "should have legal status 'diplomatic mission'" in {
        createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.Embassy
      }

      "should have an empty date of establishment" in {
        createEoriSubscriptionRequest.organisation.head.dateOfEstablishment shouldBe empty
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

        "should have type of person as 2 (Legal Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "2"
        }

        "should have a date established when present" in {
          createEoriSubscriptionRequest.organisation.head.dateOfEstablishment.value shouldBe "1980-01-01"
        }

        "should have the organisation name" in {
          createEoriSubscriptionRequest.organisation.head.organisationName shouldBe "Solutions Ltd"
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

        "should have type of person as 2 (Legal Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "2"
        }

        "should have a date established when present" in {
          createEoriSubscriptionRequest.organisation.head.dateOfEstablishment.value shouldBe "1990-12-12"
        }

        "should have the organisation name" in {
          createEoriSubscriptionRequest.organisation.head.organisationName shouldBe "Top Lawyers"
        }
      }

      "Sole Trader" - {
        val createEoriSubscriptionRequest =
          transformer.transform(givenRegistrationDetailsSoleTrader, givenSubscriptionDetailsSoleTrader, Iom, gagmr)

        "should have legal status Llp" in {
          createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.UnincorporatedBody
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
          createEoriSubscriptionRequest.principalEconomicActivity.value shouldBe "5811"
          givenSubscriptionDetailsSoleTrader.sicCode.value.take(
            4
          ) shouldBe createEoriSubscriptionRequest.principalEconomicActivity.value
        }

        "should have type of person as 1 (Natural Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "1"
        }

        "should have Name & DOB details" in {
          createEoriSubscriptionRequest.individual.head.dateOfBirth shouldBe "1980-12-12"
          createEoriSubscriptionRequest.individual.head.firstName shouldBe "Thomas"
          createEoriSubscriptionRequest.individual.head.lastName shouldBe "Tell"
        }
      }

      "Individual" - {
        val createEoriSubscriptionRequest =
          transformer.transform(givenRegistrationDetailsIndividual, givenSubscriptionDetailsIndividual, Iom, gagmr)

        "should have legal status Llp" in {
          createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.UnincorporatedBody
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
          createEoriSubscriptionRequest.principalEconomicActivity.value shouldBe "3284"
          givenSubscriptionDetailsIndividual.sicCode.value.take(
            4
          ) shouldBe createEoriSubscriptionRequest.principalEconomicActivity.value
        }

        "should have type of person as 1 (Natural Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "1"
        }

        "should have Name & DOB details" in {
          createEoriSubscriptionRequest.individual.head.dateOfBirth shouldBe "1980-12-12"
          createEoriSubscriptionRequest.individual.head.firstName shouldBe "Phillip"
          createEoriSubscriptionRequest.individual.head.lastName shouldBe "Bailis"
        }
      }

      "Partnership" - {
        val createEoriSubscriptionRequest =
          transformer.transform(givenRegistrationDetailsPartnership, givenSubscriptionDetailsPartnership, Iom, gagmr)

        "should have legal status Corporate Body" in {
          createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.Partnership
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

        "should have type of person as 2 (Legal Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "2"
        }

        "should have a date established when present" in {
          createEoriSubscriptionRequest.organisation.head.dateOfEstablishment.value shouldBe "1980-01-01"
        }

        "should have the organisation name" in {
          createEoriSubscriptionRequest.organisation.head.organisationName shouldBe "Trust Partners"
        }
      }

      "Charity, Public Body, Not For Profit" - {
        val createEoriSubscriptionRequest =
          transformer.transform(
            givenRegistrationDetailsCharityPublicBody,
            givenSubscriptionDetailsCharityPublicBody,
            Iom,
            gagmr
          )

        "should have legal status Unincorporated" in {
          createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.UnincorporatedBody
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
          givenSubscriptionDetailsCharityPublicBody.sicCode.value.take(
            4
          ) shouldBe createEoriSubscriptionRequest.principalEconomicActivity.value
        }

        "should have type of person as 3 (Association Of Person)" in {
          createEoriSubscriptionRequest.typeOfPerson.value shouldBe "3"
        }

        "should not have a date established" in {
          createEoriSubscriptionRequest.organisation.head.dateOfEstablishment shouldBe empty
        }

        "should have a name" in {
          createEoriSubscriptionRequest.organisation.head.organisationName shouldBe "Wish Upon A Dream"
        }

        "should have a UTR ID Type & Number when present" in {
          createEoriSubscriptionRequest.id.head.idType shouldEqual "UTR"
          createEoriSubscriptionRequest.id.head.idNumber shouldEqual "1160902011"
        }
      }
    }

    "UK public bodies" - {
      val createEoriSubscriptionRequest =
        transformer.transform(
          givenRegistrationDetailsUkCharityPublicBody,
          givenSubscriptionDetailsUkCharityPublicBody,
          Uk,
          gagmr
        )

      "should have legal status Unincorporated Body" in {
        createEoriSubscriptionRequest.legalStatus shouldBe EtmpLegalStatus.UnincorporatedBody
      }

      "should have edge case type 03" in {
        createEoriSubscriptionRequest.edgeCaseType shouldBe "03"
      }

      "should have VAT identification when present" in {
        createEoriSubscriptionRequest.vatIdentificationNumbers.value shouldBe List(VatIdentification("GB", "888812345"))
      }

      "should contain principal economic activity, the first 4 digits of the SIC Code" in {
        createEoriSubscriptionRequest.principalEconomicActivity.value shouldBe "1073"
        givenSubscriptionDetailsCharityPublicBody.sicCode.value.take(
          4
        ) shouldBe createEoriSubscriptionRequest.principalEconomicActivity.value
      }

      "should have type of person as 3 (AssociationOfPerson)" in {
        createEoriSubscriptionRequest.typeOfPerson.value shouldBe "3"
      }

      "should not have a date established" in {
        createEoriSubscriptionRequest.organisation.head.dateOfEstablishment shouldBe empty
      }

      "should have the organisation name" in {
        createEoriSubscriptionRequest.organisation.head.organisationName shouldBe "Government Dept"
      }
    }
  }

  private def givenRegistrationDetailsEmbassy: RegistrationDetailsEmbassy = {
    RegistrationDetailsEmbassy(
      embassyName = "Embassy Of Japan",
      embassyAddress = Address("101-104 Piccadilly", Some("Greater London"), Some("London"), None, Some("W1J 7JT"), "GB"),
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

  private def givenRegistrationDetailsSoleTrader: RegistrationDetailsIndividual = {
    RegistrationDetailsIndividual(fullName = "Thomas Tell", dateOfBirth = LocalDate.of(1980, 12, 12))
      .copy(address = Address("Bay view road", Some("Bay Place"), Some("Port St. Mary"), None, Some("IM9 5AQ"), "GB"))
  }

  private def givenRegistrationDetailsIndividual: RegistrationDetailsIndividual = {
    RegistrationDetailsIndividual(fullName = "Phillip Bailis", dateOfBirth = LocalDate.of(1999, 12, 12))
      .copy(address = Address("Bay view road", Some("Bay Place"), Some("Port St. Mary"), None, Some("IM9 5AQ"), "GB"))
  }

  private def givenRegistrationDetailsPartnership: RegistrationDetailsOrganisation = {
    givenRegistrationDetailsCompany.copy(name = "Trust Partners")
  }

  private def givenRegistrationDetailsCharityPublicBody: RegistrationDetailsOrganisation = {
    givenRegistrationDetailsCompany.copy(
      name = "Wish Upon A Dream",
      etmpOrganisationType = Some(UnincorporatedBody),
      address = Address("33-37 Athol St", None, Some("Douglas"), None, Some("IM1 1LB"), "GB"),
      customsId = Some(CustomsId("UTR", "1160902011"))
    )
  }

  private def givenRegistrationDetailsUkCharityPublicBody: RegistrationDetailsOrganisation = {
    givenRegistrationDetailsCompany.copy(
      name = "Government Dept",
      etmpOrganisationType = Some(UnincorporatedBody),
      address = Address("33-37 Athol St", None, Some("Douglas"), None, Some("SE10 1LB"), "GB"),
      customsId = Some(CustomsId("UTR", "1160902011"))
    )
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

  private def givenSubscriptionDetailsSoleTrader: SubscriptionDetails = {
    givenSubscriptionDetails(FormData(organisationType = Some(SoleTrader)))
      .copy(
        nameDobDetails = Some(NameDobMatchModel("Thomas", "Tell", LocalDate.of(1980, 12, 12))),
        sicCode = Some("58110"),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "123456789")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2015-02-02")))
      )
  }

  private def givenSubscriptionDetailsIndividual: SubscriptionDetails = {
    givenSubscriptionDetails(FormData(organisationType = Some(Individual)))
      .copy(
        nameDobDetails = Some(NameDobMatchModel("Phillip", "Bailis", LocalDate.of(1980, 12, 12))),
        sicCode = Some("32845"),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "123456789")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2015-02-02")))
      )
  }

  private def givenSubscriptionDetailsPartnership: SubscriptionDetails = {
    givenSubscriptionDetails(FormData(organisationType = Some(CdsOrganisationType.Partnership)))
      .copy(
        dateEstablished = Some(LocalDate.of(1980, 1, 1)),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "123456789")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2017-01-01"))),
        sicCode = Some("10730"),
        nameOrganisationDetails = Some(NameOrganisationMatchModel("Trust Partners"))
      )
  }

  private def givenSubscriptionDetailsCharityPublicBody: SubscriptionDetails = {
    givenSubscriptionDetails(
      FormData(organisationType = Some(CharityPublicBodyNotForProfit), utrMatch = Some(UtrMatchModel(Some(true))))
    )
      .copy(
        dateEstablished = Some(LocalDate.of(1980, 1, 1)),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "123456789")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2017-01-01"))),
        sicCode = Some("10730"),
        nameOrganisationDetails = Some(NameOrganisationMatchModel("Wish Upon A Dream")),
        customsId = Some(CustomsId("UTR", "1160902011"))
      )
  }

  private def givenSubscriptionDetailsUkCharityPublicBody: SubscriptionDetails = {
    givenSubscriptionDetails(
      FormData(organisationType = Some(CharityPublicBodyNotForProfit), utrMatch = Some(UtrMatchModel(Some(true))))
    )
      .copy(
        dateEstablished = Some(LocalDate.of(1980, 1, 1)),
        vatRegisteredUk = Some(true),
        ukVatDetails = Some(VatDetails("NW11 5RP", "888812345")),
        vatControlListResponse = Some(VatControlListResponse(Some("SE28 1AA"), Some("2017-01-01"))),
        sicCode = Some("10730"),
        nameOrganisationDetails = Some(NameOrganisationMatchModel("Government Dept")),
        customsId = Some(CustomsId("UTR", "1160902011"))
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
