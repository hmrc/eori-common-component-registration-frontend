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

package unit.services.cache

import base.UnitSpec
import org.joda.time.DateTime
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.cache.model.Id
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.CachedData

class CachedDataSpec extends UnitSpec with MockitoSugar {

  val sessionId: Id = Id("1234567")

  def errorMsg(name: String) = s"$name is not cached in data for the sessionId: ${sessionId.id}"

  "CachedData" should {

    "throw IllegalStateException" when {

      "registrationDetails missing " in {
        intercept[Exception](CachedData().registrationDetails(sessionId)).getMessage shouldBe errorMsg(
          CachedData.regDetailsKey
        )
      }

      "registerWithEoriAndIdResponse missing " in {
        intercept[Exception](CachedData().registerWithEoriAndIdResponse(sessionId)).getMessage shouldBe errorMsg(
          CachedData.registerWithEoriAndIdResponseKey
        )
      }

      "sub01Outcome missing " in {
        intercept[Exception](CachedData().sub01Outcome(sessionId)).getMessage shouldBe errorMsg(
          CachedData.sub01OutcomeKey
        )
      }

      "sub02Outcome missing " in {
        intercept[Exception](CachedData().sub02Outcome(sessionId)).getMessage shouldBe errorMsg(
          CachedData.sub02OutcomeKey
        )
      }

      "registrationInfo missing " in {
        intercept[Exception](CachedData().registrationInfo(sessionId)).getMessage shouldBe errorMsg(
          CachedData.regInfoKey
        )
      }

      "email missing " in {
        intercept[Exception](CachedData().email(sessionId)).getMessage shouldBe errorMsg(CachedData.emailKey)
      }

      "safeId missing " in {
        intercept[Exception](CachedData().safeId(sessionId)).getMessage shouldBe errorMsg(CachedData.safeIdKey)
      }
    }

    "return default" when {

      "subscriptionDetails missing " in {
        CachedData().subscriptionDetails(sessionId) shouldBe SubscriptionDetails()
      }
    }

    "return SAFEID" when {

      "registerWithEoriAndIdResponse" in {
        val safeId = "someSafeId"
        CachedData(registerWithEoriAndIdResponse = Some(registerWithEoriAndIdResponse(safeId))).safeId(
          sessionId
        ) shouldBe SafeId(safeId)
      }

      "registrationDetails" in {
        val safeId = "anotherSafeId"
        CachedData(regDetails = Some(registrationDetails(safeId))).safeId(sessionId) shouldBe SafeId(safeId)
      }
    }
  }

  def registrationDetails(safeId: String) = RegistrationDetailsSafeId(
    SafeId(safeId),
    Address("", Some(""), Some(""), Some(""), Some(""), ""),
    TaxPayerId(""),
    None,
    ""
  )

  def registerWithEoriAndIdResponse(safeId: String) = RegisterWithEoriAndIdResponse(
    ResponseCommon("OK", None, new DateTime, None),
    Some(
      RegisterWithEoriAndIdResponseDetail(
        Some("PASS"),
        Some("C001"),
        responseData = Some(
          ResponseData(
            safeId,
            Trader("John Doe", "Mr D"),
            EstablishmentAddress("Line 1", "City Name", Some("SE28 1AA"), "GB"),
            Some(
              ContactDetail(
                EstablishmentAddress("Line 1", "City Name", Some("SE28 1AA"), "GB"),
                "John Contact Doe",
                Some("1234567"),
                Some("89067"),
                Some("john.doe@example.com")
              )
            ),
            VATIDs = Some(Seq(VatIds("AD", "1234"), VatIds("GB", "4567"))),
            hasInternetPublication = false,
            principalEconomicActivity = Some("P001"),
            hasEstablishmentInCustomsTerritory = Some(true),
            legalStatus = Some("Official"),
            thirdCountryIDNumber = Some(Seq("1234", "67890")),
            personType = Some(9),
            dateOfEstablishmentBirth = Some("2018-05-16"),
            startDate = "2018-05-15",
            expiryDate = Some("2018-05-16")
          )
        )
      )
    )
  )

}
