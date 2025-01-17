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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.EmbassyNameController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.EmbassyId
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.embassy.EmbassyNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_embassy_name
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmbassyNameControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val form                           = new EmbassyNameForm()
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockEmbassyNameView            = instanceOf[what_is_your_embassy_name]

  private val embassyNameController =
    new EmbassyNameController(mockAuthAction, mcc, form, mockEmbassyNameView, mockSubscriptionDetailsService)

  private val fieldLevelErrorName            = "//p[@id='name-error' and @class='govuk-error-message']"
  private val pageLevelErrorSummaryListXPath = "//ul[@class='govuk-list govuk-error-summary__list']"
  private val nameFieldError                 = "//p[@id='name-error' and @class='govuk-error-message']"

  "showForm" should {
    "show the embassy name form" in {
      // Given
      val userId = UUID.randomUUID().toString
      withAuthorisedUser(userId, mockAuthConnector)
      when(mockSubscriptionDetailsService.cachedEmbassyName(any())).thenReturn(Future.successful(None))

      // When
      val result = embassyNameController.showForm(isInReviewMode = false, EmbassyId, eoriOnlyService)
        .apply(SessionBuilder.buildRequestWithSession(userId))

      // Then
      status(result) shouldBe OK
      val page: CdsPage = CdsPage(contentAsString(result))
      page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
      page.getElementsText(fieldLevelErrorName) shouldBe empty
      page.title() shouldBe "What is the embassy’s name? - Get an EORI number - GOV.UK"
      page.h1() shouldBe "What is the embassy’s name?"
      page.getElementById("name").text() shouldBe empty
    }

    "show the embassy name form with embassy name populated in input text" in {
      // Given
      val userId = UUID.randomUUID().toString
      withAuthorisedUser(userId, mockAuthConnector)
      when(mockSubscriptionDetailsService.cachedEmbassyName(any())).thenReturn(Future.successful(Some("U.S. Embassy")))

      // When
      val result = embassyNameController.showForm(isInReviewMode = false, EmbassyId, eoriOnlyService)
        .apply(SessionBuilder.buildRequestWithSession(userId))

      // Then
      status(result) shouldBe OK
      val page: CdsPage = CdsPage(contentAsString(result))
      page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
      page.getElementsText(fieldLevelErrorName) shouldBe empty
      page.title() shouldBe "What is the embassy’s name? - Get an EORI number - GOV.UK"
      page.h1() shouldBe "What is the embassy’s name?"
      page.getElementById("name").attr("value") shouldBe "U.S. Embassy"
    }
  }

  "submit" should {
    "show errors" when {
      "invalid data entered" in {
        // Given
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockSubscriptionDetailsService.cachedEmbassyName(any())).thenReturn(Future.successful(None))

        // When
        val result = embassyNameController
          .submit(isInReviewMode = false, EmbassyId, eoriOnlyService)
          .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, Map("name" -> "<Embassy Of Japan>")))

        // Then
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Embassy name cannot contain '<' or '>'"
        page.getElementsText(nameFieldError) shouldBe "Error: Embassy name cannot contain '<' or '>'"
        page.getElementsText("title") should startWith("Error: ")
      }

      "no data entered" in {
        // Given
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockSubscriptionDetailsService.cachedEmbassyName(any())).thenReturn(Future.successful(None))

        // When
        val result = embassyNameController
          .submit(isInReviewMode = false, EmbassyId, eoriOnlyService)
          .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, Map("name" -> "")))

        // Then
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your registered embassy name"
        page.getElementsText(nameFieldError) shouldBe "Error: Enter your registered embassy name"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect" when {
      "in review mode to embassy address page" in {
        // Given
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockSubscriptionDetailsService.cachedEmbassyName(any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDetailsService.cacheEmbassyName(any())(any())).thenReturn(Future.unit)

        // When
        val result = embassyNameController
          .submit(isInReviewMode = true, EmbassyId, eoriOnlyService)
          .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, Map("name" -> "Embassy Of Japan")))

        status(result) shouldBe SEE_OTHER
        header(
          LOCATION,
          result
        ).value shouldBe "/customs-registration-services/eori-only/register/matching/review-determine"
      }

      "not in review mode to error page" in {
        // Given
        val userId = UUID.randomUUID().toString
        withAuthorisedUser(userId, mockAuthConnector)
        when(mockSubscriptionDetailsService.cachedEmbassyName(any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDetailsService.cacheEmbassyName(any())(any())).thenReturn(Future.unit)
        when(mockSubscriptionDetailsService.updateSubscriptionDetailsEmbassyName(any())(any())).thenReturn(Future.unit)

        // When
        val result = embassyNameController
          .submit(isInReviewMode = false, EmbassyId, eoriOnlyService)
          .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, Map("name" -> "Embassy Of Japan")))

        status(result) shouldBe SEE_OTHER
        header(
          LOCATION,
          result
        ).value shouldBe "/customs-registration-services/eori-only/register/matching/embassy-address"
      }
    }
  }

}
