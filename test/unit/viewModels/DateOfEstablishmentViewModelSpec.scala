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

package unit.viewModels

import base.UnitSpec
import play.api.data.Form
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, Partnership, UnincorporatedBody}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.DateOfEstablishmentViewModel
import util.ControllerSpec

import java.time

class DateOfEstablishmentViewModelSpec extends UnitSpec with ControllerSpec {

  "DateOfEstablishmentViewModel" should {

    "return the correct header and title for Partnership and isRestOfWorldJourney true" in {
      val orgType = Partnership
      val isRestOfWorldJourney = true
      val result = DateOfEstablishmentViewModel.headerAndTitle(orgType, isRestOfWorldJourney)
      result shouldBe "cds.subscription.partnership.date-of-establishment.title-and-heading"
    }
    "return the correct header and title for CorporateBody and isRestOfWorldJourney is false" in {
      val orgType = CorporateBody
      val isRestOfWorldJourney = false
      val result = DateOfEstablishmentViewModel.headerAndTitle(orgType, isRestOfWorldJourney)
      result shouldBe "cds.subscription.date-of-establishment.company.title-and-heading"
    }
    "return the correct header and title for any other cases" in {
      val orgType = UnincorporatedBody
      val isRestOfWorldJourney = true
      val result = DateOfEstablishmentViewModel.headerAndTitle(orgType, isRestOfWorldJourney)
      result shouldBe "cds.subscription.date-of-establishment.title-and-heading"
    }

    "should update and bring correct form with errors " in {
      val form: Form[time.LocalDate] = SubscriptionForm.subscriptionDateOfEstablishmentForm
      val updatedForm = DateOfEstablishmentViewModel.updateFormErrors(form)
      updatedForm.errors shouldBe DateConverter.updateDateOfEstablishmentErrors(form.errors)
    }

  }

}
