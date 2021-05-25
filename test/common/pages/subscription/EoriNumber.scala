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

package common.pages.subscription

import common.pages.WebPage

sealed trait EoriNumberPageCommon extends WebPage {

  override val title = "Your details"

  val reviewUrl = "/customs-enrolment-services/subscribe/matching/what-is-your-eori/review"

  val formId: String = "eoriNumberForm"

  val continueButtonXpath = "//*[@class='button']"
}

trait EoriNumberPage extends EoriNumberPageCommon {

  override val title = "What is your EORI number?"
}

object EoriNumberPage extends EoriNumberPage
