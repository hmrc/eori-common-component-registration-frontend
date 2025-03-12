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
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address

import java.time.LocalDate

class RegistrationDetailsFormatSpec extends UnitSpec {

  import RegistrationDetails.formats

  val organisationDetails: RegistrationDetailsOrganisation = RegistrationDetailsOrganisation(
    customsId = Some(Eori("ZZZ1ZZZZ23ZZZZZZZ")),
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfEstablishment = None,
    etmpOrganisationType = Some(CorporateBody)
  )

  val organisationJson: JsValue = Json.parse("""
      |{
      |   "customsId": {
      |     "eori": "ZZZ1ZZZZ23ZZZZZZZ"
      |   },
      |   "sapNumber":{ "id": "sapNumber"},
      |   "safeId":{ "id": "safe-id"},
      |   "name":"name",
      |   "address":{
      |      "addressLine1":"add1",
      |      "addressLine2":"add2",
      |      "addressLine3":"add3",
      |      "addressLine4":"add4",
      |      "postalCode":"postcode",
      |      "countryCode":"GB"
      |   },
      |   "etmpOrganisationType":{
      |      "id" : "Corporate Body"
      |   }
      |}
    """.stripMargin)

  val organisationDetailsWithDate: RegistrationDetailsOrganisation = RegistrationDetailsOrganisation(
    customsId = None,
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfEstablishment = Some(LocalDate.parse("1961-04-12")),
    etmpOrganisationType = Some(CorporateBody)
  )

  val organisationJsonWithDate: JsValue = Json.parse("""
      |{
      |   "sapNumber":{ "id": "sapNumber"},
      |   "safeId":{ "id": "safe-id"},
      |   "name":"name",
      |   "address":{
      |      "addressLine1":"add1",
      |      "addressLine2":"add2",
      |      "addressLine3":"add3",
      |      "addressLine4":"add4",
      |      "postalCode":"postcode",
      |      "countryCode":"GB"
      |   },
      |   "dateOfEstablishment": "1961-04-12",
      |   "etmpOrganisationType":{
      |      "id" : "Corporate Body"
      |   }
      |}
    """.stripMargin)

  val dateOfBirth: LocalDate = {
    val year  = 1961
    val month = 4
    val day   = 12
    LocalDate.of(year, month, day)
  }

  val individualDetails: RegistrationDetailsIndividual = RegistrationDetailsIndividual(
    customsId = None,
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfBirth
  )

  val individualJson: JsValue = Json.parse("""
      |{
      |   "sapNumber":{ "id": "sapNumber"},
      |      "safeId":{ "id": "safe-id"},
      |   "name":"name",
      |   "address":{
      |      "addressLine1":"add1",
      |      "addressLine2":"add2",
      |      "addressLine3":"add3",
      |      "addressLine4":"add4",
      |      "postalCode":"postcode",
      |      "countryCode":"GB"
      |   },
      |   "dateOfBirth": "1961-04-12"
      |}
    """.stripMargin)

  val safeIdDetailsJson: JsValue = Json.parse("""
      |{
      | "safeId" : {
      |    "id" : "safe-id"
      |  },
      |  "address" : {
      |    "addressLine1" : "add1",
      |    "addressLine2" : "add2",
      |    "addressLine3" : "add3",
      |    "addressLine4" : "add4",
      |    "postalCode" : "postcode",
      |    "countryCode" : "GB"
      |  },
      |  "sapNumber" : {
      |    "id" : "Id"
      |  },
      |  "name" : "name"
      |}
    """.stripMargin)

  val embassyDetailsJson: JsValue = Json.parse("""
      |{
      |  "orgType": "embassy",
      |  "name": "Embassy Of Japan",
      |  "address": {
      |    "addressLine1": "101-104 Piccadilly",
      |    "addressLine2": "Greater London",
      |    "addressLine3": "London",
      |    "postalCode": "W1J 7JT",
      |    "countryCode": "GB"
      |  },
      |  "customsId": null,
      |  "safeId": {
      |    "id": ""
      |  }
      |}
      |""".stripMargin)

  val embassyDetails: RegistrationDetailsEmbassy = RegistrationDetailsEmbassy.apply(
    "Embassy Of Japan",
    Address("101-104 Piccadilly", Some("Greater London"), Some("London"), None, Some("W1J 7JT"), "GB"),
    None,
    SafeId("")
  )

  "RegistrationDetails formats" should {

    "marshall organisation details" in {
      Json.prettyPrint(marshall(organisationDetails)) shouldBe Json.prettyPrint(organisationJson)
    }

    "unmarshall organisation details" in {
      unmarshall(organisationJson) shouldBe JsSuccess(organisationDetails)
    }

    "marshall organisation details with establishment date" in {
      Json.prettyPrint(marshall(organisationDetailsWithDate)) shouldBe Json.prettyPrint(organisationJsonWithDate)
    }

    "unmarshall organisation details with establishment date" in {
      unmarshall(organisationJsonWithDate) shouldBe JsSuccess(organisationDetailsWithDate)
    }

    "marshall individual details" in {
      Json.prettyPrint(marshall(individualDetails)) shouldBe Json.prettyPrint(individualJson)
    }

    "unmarshall individual details" in {
      unmarshall(individualJson) shouldBe JsSuccess(individualDetails)
    }

    "marshall embassy details" in {
      Json.prettyPrint(marshall(embassyDetails)) shouldBe Json.prettyPrint(embassyDetailsJson)
    }

    "unmarshall embassy details" in {
      unmarshall(embassyDetailsJson) shouldBe JsSuccess(embassyDetails)
    }
  }

  private def marshall(data: RegistrationDetails): JsValue = Json.toJson(data)

  private def unmarshall(js: JsValue): JsResult[RegistrationDetails] = Json.fromJson(js)
}
