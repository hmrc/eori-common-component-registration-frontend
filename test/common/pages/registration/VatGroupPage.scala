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

package common.pages.registration

import common.pages.WebPage

trait VatGroupPage extends WebPage {

  val fieldLevelErrorYesNoAnswer: String =
    "//p[contains(@id, 'yes-no-answer-error') and contains(@class, 'govuk-error-message']"

  override val title            = "Is your organisation part of a VAT group in the UK?"
  val problemWithSelectionError = "Select yes if your organisation is part of a VAT group in the UK"
}

object VatGroupPage extends VatGroupPage
