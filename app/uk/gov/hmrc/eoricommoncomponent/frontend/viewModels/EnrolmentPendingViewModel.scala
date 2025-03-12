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

package uk.gov.hmrc.eoricommoncomponent.frontend.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName

object EnrolmentPendingViewModel {

  private def processingServiceName(processingService: Option[Service])(implicit messages: Messages) =
    processingService
      .map(other => ServiceName.longName(other))
      .getOrElse(
        messages("cds.enrolment.pending.otherService")
      )

  def title(processingService: Option[Service], service: Service)(implicit messages: Messages): String =
    if (processingService.contains(service))
      messages("cds.enrolment.pending.user.title")
    else messages("cds.enrolment.pending.user.title.other-service")

  def otherServiceParagraph(processingService: Option[Service])(implicit messages: Messages): String =
    messages(processingServiceName(processingService))

}
