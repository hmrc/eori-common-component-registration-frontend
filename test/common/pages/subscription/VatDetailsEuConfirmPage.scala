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
import common.support.Env
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

trait VatDetailsEuConfirmPage extends WebPage {

  val formId: String = "vatDetailsEuConfirmForm"

  override val title = "You have added VAT details for 1 EU member country"

  def url(service: Service): String =
    Env.frontendHost + s"/customs-enrolment-services/${service.code}/register/vat-details-eu-confirm"

}

object VatDetailsEuConfirmPage extends VatDetailsEuConfirmPage {

  def apply(numberOfEuCountries: String): VatDetailsEuConfirmPage = new VatDetailsEuConfirmPage {

    override val title =
      if (numberOfEuCountries.toInt <= 1)
        s"You have added VAT details for $numberOfEuCountries EU member country"
      else
        s"You have added VAT details for $numberOfEuCountries EU member countries"

  }

}
