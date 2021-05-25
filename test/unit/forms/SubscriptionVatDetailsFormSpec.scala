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

package unit.forms

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm
import util.builders.SubscriptionFormBuilder

class SubscriptionVatDetailsFormSpec extends UnitSpec {

  "SubscriptionVatDetailsForm" should {

    "map from request to vat form model when mandatory data is provided" in {

      val data = SubscriptionFormBuilder.vatSubscriptionMandatoryRequestMap

      val form = SubscriptionForm.subscriptionVatUKDetailsForm.bindFromRequest(data)

      val vatModelOption = form.value

      vatModelOption should not be 'empty

      val vatModel = vatModelOption.get
      vatModel.hasGbVats should be(true)

      vatModel.gbVats should not be 'empty
      val gbVats = vatModel.gbVats.get
      gbVats.size should be(2)
    }

    "map from request to vat form model when mandatory data is not provided" in {

      val data = SubscriptionFormBuilder.vatSubscriptionOptionalInvalidMap

      val form = SubscriptionForm.subscriptionVatUKDetailsForm.bindFromRequest(data)

      form.value should be('empty)
    }
  }
}
