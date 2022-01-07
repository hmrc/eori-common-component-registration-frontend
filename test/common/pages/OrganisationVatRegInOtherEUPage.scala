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

package common.pages

class VatRegInOtherEUPage extends WebPage {

  override val title = "Is your organisation VAT registered in other EU member countries?"
}

object VatDetailsEuPage extends VatRegInOtherEUPage {
  override val title = "Enter the VAT details for an EU country"
}

trait RemoveEUVatDetails extends WebPage {

  override val title = "Are you sure you want to remove these VAT details?"

  val pageLevelErrorMessage = "Tell us if you want to remove these VAT details"
}

object RemoveVatDetails extends RemoveEUVatDetails {}
