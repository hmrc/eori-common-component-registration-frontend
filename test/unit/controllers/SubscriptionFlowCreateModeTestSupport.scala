/*
 * Copyright 2023 HM Revenue & Customs
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

import common.pages.subscription.SubscriptionContactDetailsPage
import play.api.mvc.Result
import play.api.test.Helpers._

import scala.concurrent.Future

trait SubscriptionFlowCreateModeTestSupport extends SubscriptionFlowTestSupport {

  protected def submitInCreateModeUrl: String

  protected val ContinueButtonTextInCreateMode = "Continue"

  def verifyFormActionInCreateMode: Future[Result] => Any = { result =>
    val page = CdsPage(contentAsString(result))
    page.formAction(formId) shouldBe submitInCreateModeUrl
  }

  def verifyBackLinkInCreateModeRegister: Future[Result] => Any = { result =>
    val page = CdsPage(contentAsString(result))
    page.getElementAttributeHref(SubscriptionContactDetailsPage.backLinkXPath) shouldBe previousPageUrl
  }

  def verifyBackLinkInCreateModeSubscribe: Future[Result] => Any = { result =>
    val page = CdsPage(contentAsString(result))
    page.getElementAttributeHref(SubscriptionContactDetailsPage.backLinkXPath) shouldBe previousPageUrl
  }

  def verifyRedirectToNextPageInCreateMode: Future[Result] => Any = { result =>
    status(result) shouldBe SEE_OTHER
    header(LOCATION, result).value should endWith(nextPageUrl)
  }

}
