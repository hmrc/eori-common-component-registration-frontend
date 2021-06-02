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

package unit.controllers.subscription

import common.pages.subscription.SubscriptionContactDetailsPage
import play.api.mvc.Result
import play.api.test.Helpers._
import unit.controllers.CdsPage

import scala.concurrent.Future

trait SubscriptionFlowReviewModeTestSupport extends SubscriptionFlowTestSupport {

  protected def submitInReviewModeUrl: String

  protected val ContinueButtonTextInReviewMode = "Save and review"

  def verifyFormSubmitsInReviewMode: Future[Result] => Any = { result =>
    val page = CdsPage(contentAsString(result))
    page.formAction(formId) shouldBe submitInReviewModeUrl
  }

  def verifyNoStepsAndBackLinkInReviewMode: Future[Result] => Any = { result =>
    val page = CdsPage(contentAsString(result))
    page.elementIsPresent(SubscriptionContactDetailsPage.stepsXpath) shouldBe false
    page.getElementAttributeHref(SubscriptionContactDetailsPage.backLinkXPath) shouldBe previousPageUrl
  }

  def verifyBackLinkInReviewMode: Future[Result] => Any = { result =>
    val page = CdsPage(contentAsString(result))
    page.getElementAttributeHref(SubscriptionContactDetailsPage.backLinkXPath) shouldBe previousPageUrl
  }

  def verifyRedirectToReviewPage(): Future[Result] => Any = { result =>
    status(result) shouldBe SEE_OTHER
    result.header.headers(
      LOCATION
    ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
      .determineRoute(atarService)
      .url
  }

}
