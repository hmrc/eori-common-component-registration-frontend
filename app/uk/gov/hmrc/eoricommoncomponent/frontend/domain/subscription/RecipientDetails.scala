/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription

import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.models._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName

case class RecipientDetails(
  journey: String,
  enrolmentKey: String,
  serviceName: String,
  recipientEmailAddress: String,
  recipientFullName: String,
  orgName: Option[String],
  completionDate: Option[String] = None,
  languageCode: Option[String] = None
)

object RecipientDetails {
  implicit val jsonFormat: OFormat[RecipientDetails] = Json.format[RecipientDetails]

  def apply(
    service: Service,
    recipientEmailAddress: String,
    recipientFullName: String,
    orgName: Option[String],
    completionDate: Option[String]
  )(implicit messages: Messages): RecipientDetails =
    RecipientDetails(
      RegisterJourney.value,
      service.enrolmentKey,
      ServiceName.longName(service),
      recipientEmailAddress,
      recipientFullName,
      orgName,
      completionDate,
      Some(messages.lang.code)
    )

}
