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

package unit.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.WhatIsYourContactAddressController
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactAddressFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_contact_address
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatIsYourContactAddressControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val whatIsYourContactAddressView = inject[what_is_your_contact_address]
  private val mockContactAddressFormProvider = mock[ContactAddressFormProvider]
  when(mockContactAddressFormProvider.contactAddressForm).thenReturn(new ContactAddressFormProvider().contactAddressForm)

  private val controller = new WhatIsYourContactAddressController(
    mockAuthAction,
    mcc,
    mockRequestSessionData,
    mockSubscriptionDetailsService,
    whatIsYourContactAddressView,
    mockContactAddressFormProvider
  )

  private val fieldLevelErrorAddress = "//p[@id='address-error' and @class='govuk-error-message']"
  private val pageLevelErrorSummaryListXPath = "//ul[@class='govuk-list govuk-error-summary__list']"
  private val lineOneFieldError = "//p[@id='line-1-error' and @class='govuk-error-message']"
  private val lineTwoFieldError = "//p[@id='line-2-error' and @class='govuk-error-message']"
  private val townCityFieldError = "//p[@id='townCity-error' and @class='govuk-error-message']"
  private val postcodeFieldError = "//p[@id='postcode-error' and @class='govuk-error-message']"

  "showForm" should {
    "render empty form with no data" in {
      // Given
      val userId = UUID.randomUUID().toString
      withAuthorisedUser(userId, mockAuthConnector)

      // When
      val result = controller
        .showForm(isInReviewMode = false, eoriOnlyService)
        .apply(SessionBuilder.buildRequestWithSession(userId))

      // Then
      status(result) shouldBe OK
      val page: CdsPage = CdsPage(contentAsString(result))
      page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
      page.getElementsText(fieldLevelErrorAddress) shouldBe empty
      page.h1() shouldBe "What is your contact address?"
      page.title() shouldBe "What is your contact address? - Get an EORI number - GOV.UK"
      page.getElementById("line-1").attr("value") shouldBe empty
      page.getElementById("line-2").attr("value") shouldBe empty
      page.getElementById("townCity").attr("value") shouldBe empty
      page.getElementById("postcode").attr("value") shouldBe empty
      page.getElementById("countryCode").attr("value") shouldBe empty
    }
  }

  "submit" should {
    "show errors" when {
      "no data entered" in {
        // Given
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)

        // When
        val result = controller
          .submit(isInReviewMode = false, eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              userId,
              Map("line-1" -> "", "line-2" -> "", "townCity" -> "", "postcode" -> "", "countryCode" -> "")
            )
          )

        // Then
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          pageLevelErrorSummaryListXPath
        ) shouldBe "Enter the first line of your address Enter your town or city Enter a valid postcode"
        page.getElementsText(lineOneFieldError) shouldBe "Error: Enter the first line of your address"
        page.getElementsText(townCityFieldError) shouldBe "Error: Enter your town or city"
        page.getElementsText(postcodeFieldError) shouldBe "Error: Enter a valid postcode"
        page.getElementsText("title") should startWith("Error: ")
      }

      "invalid data entered" in {
        // Given
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)

        // When
        val result = controller
          .submit(isInReviewMode = false, eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              userId,
              Map(
                "line-1"      -> "<somewhere>",
                "line-2"      -> "<someplace>",
                "townCity"    -> "a really really long town city name somewhere",
                "postcode"    -> "FakePostcode",
                "countryCode" -> "GB"
              )
            )
          )

        // Then
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          pageLevelErrorSummaryListXPath
        ) shouldBe "The first line of the address must only include letters a to z, numbers, apostrophes, full stops, ampersands, hyphens and spaces The second line of the address must only include letters a to z, numbers, apostrophes, full stops, ampersands, hyphens and spaces The town or city must be 35 characters or less Enter a valid postcode"
        page.getElementsText(
          lineOneFieldError
        ) shouldBe "Error: The first line of the address must only include letters a to z, numbers, apostrophes, full stops, ampersands, hyphens and spaces"
        page.getElementsText(
          lineTwoFieldError
        ) shouldBe "Error: The second line of the address must only include letters a to z, numbers, apostrophes, full stops, ampersands, hyphens and spaces"
        page.getElementsText(townCityFieldError) shouldBe "Error: The town or city must be 35 characters or less"
        page.getElementsText(postcodeFieldError) shouldBe "Error: Enter a valid postcode"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect" when {
      "in review mode" in {
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.unit)

        val result = controller
          .submit(isInReviewMode = true, eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              userId,
              Map(
                "line-1"      -> "101-104 Piccadilly",
                "line-2"      -> "Greater London",
                "townCity"    -> "London",
                "postcode"    -> "SW3 5DA",
                "countryCode" -> "GB"
              )
            )
          )

        status(result) shouldBe SEE_OTHER
        header(
          LOCATION,
          result
        ).value shouldBe "/customs-registration-services/eori-only/register/contact-address/review"
      }

      "not in review mode" in {
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.unit)

        val result = controller
          .submit(isInReviewMode = false, eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              userId,
              Map(
                "line-1"      -> "101-104 Piccadilly",
                "line-2"      -> "Greater London",
                "townCity"    -> "London",
                "postcode"    -> "SW3 5DA",
                "countryCode" -> "GB"
              )
            )
          )

        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe "/customs-registration-services/eori-only/register/contact-address"
      }
    }
  }

}
