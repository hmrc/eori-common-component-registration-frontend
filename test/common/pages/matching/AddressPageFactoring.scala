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

package common.pages.matching

trait AddressPageFactoring {
  val PageLevelErrorSummaryListXPath = "//ul[@class='error-summary-list']"

  val fieldLevelErrorAddressLineOne   = "//*[@id='line-1-outer']//span[@class='error-message']"
  val fieldLevelErrorAddressLineTwo   = "//*[@id='line-2-outer']//span[@class='error-message']"
  val fieldLevelErrorAddressLineThree = "//*[@id='line-3-outer']//span[@class='error-message']"
  val fieldLevelErrorAddressLineFour  = "//*[@id='line-4-outer']//span[@class='error-message']"
  val fieldLevelErrorPostcode         = "//*[@id='postcode-outer']//span[@class='error-message']"
  val fieldLevelErrorCountry          = "//*[@id='country-outer']//span[@class='error-message']"
}

object AddressPageFactoring extends AddressPageFactoring
