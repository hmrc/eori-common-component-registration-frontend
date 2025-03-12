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
import org.mockito.Mockito.when
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.EnrolmentPendingViewModel
import util.ControllerSpec

class EnrolmentPendingViewSpec extends UnitSpec with ControllerSpec {
  val mockMessages: Messages                    = mock[Messages]
  val viewModel: EnrolmentPendingViewModel.type = EnrolmentPendingViewModel

  "EnrolmentPendingViewModel" should {

    "return the appropriate title when the other service is the same as the current service" in {
      val someOtherService = Some(atarService)
      val service          = otherService

      when(mockMessages(messages("cds.enrolment.pending.title.user.sameService", otherService))).thenReturn(
        "Some Service"
      )

      val result = viewModel.title(someOtherService, service)
      result shouldEqual messages("cds.enrolment.pending.user.title.other-service")
    }

    "return the appropriate title when the other service is the same as the other services" in {
      val someOtherService = Some(otherService)
      val service          = atarService
      when(mockMessages(messages("cds.enrolment.pending.title.user.processingService", atarService))).thenReturn(
        "Other Service"
      )
      val result           = viewModel.title(someOtherService, service)

      result shouldEqual messages("cds.enrolment.pending.user.title.other-service")
    }

    "return the appropriate title when the service is the same as processingService" in {
      val someOtherService = Some(atarService)
      val service          = atarService
      when(mockMessages(messages("cds.enrolment.pending.title.user.processingService", atarService))).thenReturn(
        "Other Service"
      )
      val result           = viewModel.title(someOtherService, service)

      result shouldEqual messages("cds.enrolment.pending.user.title")
    }

    "return the correct service name for paragraph when the other service is the same as the other services" in {
      val someOtherService = Some(otherService)
      val viewModel        = EnrolmentPendingViewModel
      val result           = viewModel.otherServiceParagraph(someOtherService)

      result shouldEqual "Other Service"
    }

    "return the correct service name for paragraph when processingService is 'None'" in {
      val viewModel = EnrolmentPendingViewModel
      val result    = viewModel.otherServiceParagraph(None)

      result shouldEqual "cds.enrolment.pending.otherService"
    }
  }
}
