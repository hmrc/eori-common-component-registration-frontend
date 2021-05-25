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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator

class RegistrationDetailsCreatorRegistrationInfoSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val registrationDetailsCreator = new RegistrationDetailsCreator()
  private val orgRegInfo                 = mock[OrgRegistrationInfo](RETURNS_DEEP_STUBS)
  private val individualRegInfo          = mock[IndividualRegistrationInfo](RETURNS_DEEP_STUBS)
  private val orgType                    = "Partnership"
  private val sapNumber                  = "7656565646"
  private val firstName                  = "Jon"
  private val middleName                 = "middle"
  private val lastName                   = "Doe"
  private val dob                        = new LocalDate()
  private val postcode                   = "SE28 1AA"
  private val countryCode                = "ZZ"

  override def beforeEach(): Unit = {
    reset(orgRegInfo)
    when(individualRegInfo.taxPayerId).thenReturn(TaxPayerId(sapNumber))
    when(orgRegInfo.taxPayerId).thenReturn(TaxPayerId(sapNumber))
    when(orgRegInfo.organisationType).thenReturn(Some(orgType))
    when(orgRegInfo.postcode).thenReturn(Some(postcode))
    when(orgRegInfo.country).thenReturn(countryCode)
    when(individualRegInfo.firstName).thenReturn(firstName)
    when(individualRegInfo.middleName).thenReturn(Some(middleName))
    when(individualRegInfo.lastName).thenReturn(lastName)
    when(individualRegInfo.dateOfBirth).thenReturn(Some(dob))
    when(individualRegInfo.postcode).thenReturn(Some(postcode))
    when(individualRegInfo.country).thenReturn(countryCode)
  }

  "RegistrationDetailsCreator from OrgRegistrationInfo" should {

    "create organisation registration details" in {
      val actual = registrationDetailsCreator.registrationDetails()(orgRegInfo)

      actual shouldBe RegistrationDetailsOrganisation(
        customsId = None,
        sapNumber = orgRegInfo.taxPayerId,
        safeId = SafeId(""),
        name = orgRegInfo.name,
        address = Address(
          orgRegInfo.lineOne,
          orgRegInfo.lineTwo,
          orgRegInfo.lineThree,
          orgRegInfo.lineFour,
          orgRegInfo.postcode,
          orgRegInfo.country
        ),
        dateOfEstablishment = None,
        etmpOrganisationType = Some(EtmpOrganisationType.apply(orgType))
      )
    }

    "create individual registration details with missing middle name" in {
      when(individualRegInfo.middleName).thenReturn(None)

      val actual = registrationDetailsCreator.registrationDetails()(individualRegInfo)

      actual shouldBe RegistrationDetailsIndividual(
        customsId = None,
        sapNumber = individualRegInfo.taxPayerId,
        safeId = SafeId(""),
        name = firstName + " " + lastName,
        address = Address(
          individualRegInfo.lineOne,
          individualRegInfo.lineTwo,
          individualRegInfo.lineThree,
          individualRegInfo.lineFour,
          individualRegInfo.postcode,
          individualRegInfo.country
        ),
        dateOfBirth = dob
      )

    }

    "create individual registration details with middle name" in {
      when(individualRegInfo.middleName).thenReturn(Some(middleName))

      registrationDetailsCreator.registrationDetails()(individualRegInfo) shouldBe RegistrationDetailsIndividual(
        customsId = None,
        sapNumber = individualRegInfo.taxPayerId,
        safeId = SafeId(""),
        name = firstName + " " + middleName + " " + lastName,
        address = Address(
          individualRegInfo.lineOne,
          individualRegInfo.lineTwo,
          individualRegInfo.lineThree,
          individualRegInfo.lineFour,
          individualRegInfo.postcode,
          individualRegInfo.country
        ),
        dateOfBirth = dob
      )
    }

    "create individual registration details with missing dob" in {
      when(individualRegInfo.dateOfBirth).thenReturn(None)

      intercept[IllegalArgumentException] {
        registrationDetailsCreator.registrationDetails()(individualRegInfo)
      }
    }
  }
}
