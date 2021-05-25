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

package uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers

import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.JourneyExtractor.journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName._

object JourneyName {

  def journeyName(implicit messages: Messages, request: Request[_]): String =
    journey match {
      case Journey.Register  => messages("cds.banner.registration")
      case Journey.Subscribe => messages("cds.banner.subscription", longName)
    }

}
