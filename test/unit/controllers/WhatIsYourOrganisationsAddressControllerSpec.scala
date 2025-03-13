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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.Session
import play.api.test.Helpers.{LOCATION, contentAsString, defaultAwaitTimeout, header, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{SubscriptionFlowManager, WhatIsYourOrganisationsAddressController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Uk
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{EoriConsentSubscriptionFlowPage, SicCodeSubscriptionFlowPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, UtrMatchModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RegistrationDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_organisations_address
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatIsYourOrganisationsAddressControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  private val mockRegistrationDetailsService = mock[RegistrationDetailsService]
  private val what_is__your_organisation_address_view = inject[what_is_your_organisations_address]

  private val controller = new WhatIsYourOrganisationsAddressController(
    mockAuthAction,
    mcc,
    mockRequestSessionData,
    mockSubscriptionDetailsService,
    mockSubscriptionFlowManager,
    mockRegistrationDetailsService,
    what_is__your_organisation_address_view
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
      when(mockSubscriptionDetailsService.cachedOrganisationType(any())).thenReturn(
        Future.successful(Some(CdsOrganisationType.Company))
      )

      // When
      val result = controller
        .showForm(isInReviewMode = false, eoriOnlyService)
        .apply(SessionBuilder.buildRequestWithSession(userId))

      // Then
      status(result) shouldBe OK
      val page: CdsPage = CdsPage(contentAsString(result))
      page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
      page.getElementsText(fieldLevelErrorAddress) shouldBe empty
      page.h1() shouldBe "What is your organisation’s address?"
      page.title() shouldBe "What is your organisation’s address? - Get an EORI number - GOV.UK"
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
        ) shouldBe "Address line cannot contain '<' or '>' Address line cannot contain '<' or '>' The town or city must be 35 characters or less Enter a valid postcode"
        page.getElementsText(lineOneFieldError) shouldBe "Error: Address line cannot contain '<' or '>'"
        page.getElementsText(lineTwoFieldError) shouldBe "Error: Address line cannot contain '<' or '>'"
        page.getElementsText(townCityFieldError) shouldBe "Error: The town or city must be 35 characters or less"
        page.getElementsText(postcodeFieldError) shouldBe "Error: Enter a valid postcode"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect" when {
      "in review mode" in {
        val expectedUrl =
          uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DisclosePersonalDetailsConsentController
            .createForm(eoriOnlyService)
            .url
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockRegistrationDetailsService.cacheAddress(any())(any())).thenReturn(Future.successful(true))
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.unit)
        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(Uk))
        when(mockSubscriptionDetailsService.cachedOrganisationType(any())).thenReturn(
          Future.successful(Some(CdsOrganisationType.Company))
        )
        when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))

        when(mockSubscriptionFlowManager.startSubscriptionFlowWithPage(any(), any(), any())(any())).thenReturn(
          Future.successful((EoriConsentSubscriptionFlowPage, Session(Map.empty[String, String])))
        )

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
        header(LOCATION, result).value shouldBe expectedUrl
      }

      "not in review mode" in {
        val expectedUrl =
          uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DisclosePersonalDetailsConsentController
            .createForm(eoriOnlyService)
            .url
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockRegistrationDetailsService.cacheAddress(any())(any())).thenReturn(Future.successful(true))
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.unit)
        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(Uk))
        when(mockSubscriptionDetailsService.cachedOrganisationType(any())).thenReturn(
          Future.successful(Some(CdsOrganisationType.Company))
        )
        when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))

        when(mockSubscriptionFlowManager.startSubscriptionFlowWithPage(any(), any(), any())(any())).thenReturn(
          Future.successful((EoriConsentSubscriptionFlowPage, Session(Map.empty[String, String])))
        )

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
        header(LOCATION, result).value shouldBe expectedUrl
      }
    }

    "use correct redirect location" when {
      "Charity Public Body with UTR" in {
        val expectedUrl =
          uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.SicCodeController
            .createForm(eoriOnlyService)
            .url
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockRegistrationDetailsService.cacheAddress(any())(any())).thenReturn(Future.successful(true))
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.unit)
        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(Uk))
        when(mockSubscriptionDetailsService.cachedOrganisationType(any())).thenReturn(
          Future.successful(Some(CdsOrganisationType.CharityPublicBodyNotForProfit))
        )
        when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(
          Future.successful(Some(UtrMatchModel(Some(true))))
        )

        when(mockSubscriptionFlowManager.startSubscriptionFlowWithPage(any(), any(), any())(any())).thenReturn(
          Future.successful((SicCodeSubscriptionFlowPage, Session(Map.empty[String, String])))
        )

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
        header(LOCATION, result).value shouldBe expectedUrl
      }

      "Charity Public Body with no UTR" in {
        val expectedUrl =
          uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DisclosePersonalDetailsConsentController
            .createForm(eoriOnlyService)
            .url
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockRegistrationDetailsService.cacheAddress(any())(any())).thenReturn(Future.successful(true))
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.unit)
        when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(Uk))
        when(mockSubscriptionDetailsService.cachedOrganisationType(any())).thenReturn(
          Future.successful(Some(CdsOrganisationType.CharityPublicBodyNotForProfit))
        )
        when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(
          Future.successful(Some(UtrMatchModel(Some(true))))
        )

        when(mockSubscriptionFlowManager.startSubscriptionFlowWithPage(any(), any(), any())(any())).thenReturn(
          Future.successful((EoriConsentSubscriptionFlowPage, Session(Map.empty[String, String])))
        )

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
        header(LOCATION, result).value shouldBe expectedUrl
      }
    }
  }
}
