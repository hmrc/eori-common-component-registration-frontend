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

package common.pages

import common.support.Env

trait XPathRegistrationOutcomePage {
  val pageHeadingXpath = "//*[@id='page-heading']"
  val activeFromXpath  = "//*[@id='active-from']"
  val issuedDateXpath  = "//*[@id='issued-date']"
  val eoriNumberXpath  = "//*[@id='eori-number']"

  val additionalInformationXpath = "//*[@id='additional-information']"
  val whatHappensNextXpath       = "//*[@id='what-happens-next']"
}

abstract class RegistrationOutcomePage(val registrationOutcome: String)
    extends WebPage with XPathRegistrationOutcomePage {
  protected val baseUrl: String = Env.frontendHost + s"/customs-enrolment-services/register/$registrationOutcome"
}
