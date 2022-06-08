/*
 * Copyright 2022 HM Revenue & Customs
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

package common.pages.matching

trait AddressPageFactoring {
  val PageLevelErrorSummaryListXPath = "//ul[@class='govuk-list govuk-error-summary__list']"

  val fieldLevelErrorAddressLineOne   = "//p[@id='line-1-error' and @class='govuk-error-message']"
  val fieldLevelErrorAddressLineTwo   = "//p[@id='line-2-error' and @class='govuk-error-message']"
  val fieldLevelErrorAddressLineThree = "//p[@id='line-3-error' and @class='govuk-error-message']"
  val fieldLevelErrorAddressLineFour  = "//p[@id='line-4-error' and @class='govuk-error-message']"
  val fieldLevelErrorPostcode         = "//p[@id='postcode-error' and @class='govuk-error-message']"
  val fieldLevelErrorCountry          = "//p[@id='countryCode-error' and @class='govuk-error-message']"
}

object AddressPageFactoring extends AddressPageFactoring
