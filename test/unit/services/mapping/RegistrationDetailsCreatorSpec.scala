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
import org.joda.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.{
  RegistrationDisplayResponse,
  ResponseCommon,
  ResponseDetail
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  ContactResponse,
  IndividualResponse,
  OrganisationResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator

class RegistrationDetailsCreatorSpec extends UnitSpec {

  val testCreator = new RegistrationDetailsCreator

  "RegistrationDetailsCreator" should {
    "return registration details individual for RegistrationDisplayResponse" in {
      val individual = IndividualResponse("fname", Some("mname"), "lname", Some("2019-01-01"))
      val responseDetail = ResponseDetail(
        "SAFEID",
        None,
        None,
        true,
        false,
        true,
        Some(individual),
        None,
        Address("Line1", None, None, None, None, "GB"),
        ContactResponse()
      )
      val responseCommon = ResponseCommon("status", None, "date", None, Some("taxPayerId"))
      val response       = RegistrationDisplayResponse(responseCommon, Some(responseDetail))
      val expectedDetails = RegistrationDetailsIndividual(
        None,
        TaxPayerId("taxPayerId"),
        SafeId("SAFEID"),
        "fname mname lname",
        Address("Line1", None, None, None, None, "GB"),
        new LocalDate("2019-01-01")
      )

      testCreator.registrationDetails(response) shouldBe expectedDetails
    }

    "return registration details organisation for RegistrationDisplayResponse" in {
      val organisation = OrganisationResponse("orgname", Some("code"), None, Some("LLP"))
      val responseDetail = ResponseDetail(
        "SAFEID",
        None,
        None,
        true,
        false,
        false,
        None,
        Some(organisation),
        Address("Line1", None, None, None, None, "GB"),
        ContactResponse()
      )
      val responseCommon = ResponseCommon("status", None, "date", None, Some("taxPayerId"))
      val response       = RegistrationDisplayResponse(responseCommon, Some(responseDetail))
      val expectedDetails = RegistrationDetailsOrganisation(
        None,
        TaxPayerId("taxPayerId"),
        SafeId("SAFEID"),
        "orgname",
        Address("Line1", None, None, None, None, "GB"),
        None,
        Some(LLP)
      )

      testCreator.registrationDetails(response) shouldBe expectedDetails
    }
  }
}
