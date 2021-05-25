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

package common.pages.migration

import common.pages.WebPage

class SubscriptionExistingDetailsReviewPage extends WebPage {

  val BusinessNameLabelXpath = "//dt[@id='review-tbl__business-name_heading']"
  val BusinessNameValueXpath = "//dd[@id='review-tbl__business-name']"

  val UKVatIdentificationNumbersXpathLabel = "//dt[@id='review-tbl__gb-vat-number_heading']"
  val UKVatIdentificationNumbersXpath      = "//dd[@id='review-tbl__gb-vat-number']"

  val UKVatIdentificationPostcodeXpathLabel = "//dt[@id='review-tbl__gb-vat-postcode_heading']"
  val UKVatIdentificationPostcodeXpath      = "//dd[@id='review-tbl__gb-vat-postcode']"

  val UKVatIdentificationDateXpathLabel = "//dt[@id='review-tbl__gb-vat-date_heading']"
  val UKVatIdentificationDateXpath      = "//dd[@id='review-tbl__gb-vat-date']"

  val UKVatIdentificationNumbersReviewLinkXpath  = "//a[@id='review-tbl__gb-vat-number_change']"
  val UKVatIdentificationPostcodeReviewLinkXpath = "//a[@id='review-tbl__gb-vat-postcode_change']"
  val UKVatIdentificationDateReviewLinkXpath     = "//a[@id='review-tbl__gb-vat-date_change']"

  val EUVatIdentificationNumbersXpathLabel      = "//dt[@id='review-tbl__eu-vat_heading']"
  val EUVatIdentificationNumbersXpath           = "//dd[@id='review-tbl__eu-vat']"
  val EUVatIdentificationNumbersReviewLinkXpath = "//a[@id='review-tbl__eu-vat_change']"

  val ContactDetailsXPathLabel = "//dt[@id='review-tbl__contact_heading']"
  val ContactDetailsXPath      = "//dd[@id='review-tbl__contact']"

  val ContactDetailsReviewLinkXPath = "//a[@id='review-tbl__contact_change']"

  val ConfirmAndRegisterInfoXpath = "//p[@id='disclaimer-content']"

  val EUDisclosureConsentXPathLabel = "//dt[@id='review-tbl__disclosure_heading']"
  val EUDisclosureConsentXPath      = "//dd[@id='review-tbl__disclosure']"
  val EUDisclosureReviewLinkXpath   = "//a[@id='review-tbl__disclosure_change']"

  val ShortNameXPathLabel      = "//dt[@id='review-tbl__short-name_heading']"
  val ShortNameXPath           = "//dd[@id='review-tbl__short-name']"
  val ShortNameReviewLinkXPath = "//a[@id='review-tbl__short-name_change']"

  val NatureOfBusinessXPathLabel      = "//dt[@id='review-tbl__activity_heading']"
  val NatureOfBusinessXPath           = "//dd[@id='review-tbl__activity']"
  val NatureOfBusinessReviewLinkXPath = "//a[@id='review-tbl__activity_change']"

  val startAgainLinkXPath = "//a[@id='review-tbl__start-again']"

  val UtrNoLabelXPath      = "//dt[@id='review-tbl__nino-or-utr_heading']"
  val UtrNoLabelValueXPath = "//dd[@id='review-tbl__nino-or-utr']"

  val DateOfEstablishmentLabelXPath      = "//dt[@id='review-tbl__doe_heading']"
  val DateOfEstablishmentXPath           = "//dd[@id='review-tbl__doe']"
  val DateOfEstablishmentReviewLinkXPath = "//a[@id='review-tbl__doe_change']"

  val LimitedAddressLabelXpath = "//dt[@id='review-tbl__address_heading']"
  val LimitedAddressValueXpath = "//dd[@id='review-tbl__address']"
  val LimitedAddressReviewLink = "//a[@id='review-tbl__address_change']"

  def changeAnswerText(heading: String): String = s"ChangeChange $heading"

  override val title = "Check your details"

}

object SubscriptionExistingDetailsReviewPage extends SubscriptionExistingDetailsReviewPage
