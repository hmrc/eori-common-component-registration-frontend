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

package unit.domain

import base.UnitSpec
import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address

class RegistrationDetailsFormatSpec extends UnitSpec {

  import RegistrationDetails.formats

  val organisationDetails = RegistrationDetailsOrganisation(
    customsId = Some(Eori("ZZZ1ZZZZ23ZZZZZZZ")),
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfEstablishment = None,
    etmpOrganisationType = Some(CorporateBody)
  )

  val organisationJson = Json.parse("""
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

  val organisationDetailsWithDate = RegistrationDetailsOrganisation(
    customsId = None,
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfEstablishment = Some(LocalDate.parse("1961-04-12")),
    etmpOrganisationType = Some(CorporateBody)
  )

  val organisationJsonWithDate = Json.parse("""
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

  val dateOfBirth = {
    val year  = 1961
    val month = 4
    val day   = 12
    new LocalDate(year, month, day)
  }

  val individualDetails = RegistrationDetailsIndividual(
    customsId = None,
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfBirth
  )

  val individualJson = Json.parse("""
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
  }

  private def marshall(data: RegistrationDetails): JsValue = Json.toJson(data)

  private def unmarshall(js: JsValue): JsResult[RegistrationDetails] = Json.fromJson(js)
}
