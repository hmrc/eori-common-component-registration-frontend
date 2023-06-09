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

package uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers

import javax.inject.Inject

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils

class DateFormatter @Inject() (languageUtils: LanguageUtils) {

  val dateFormatter: DateTimeFormatter     = DateTimeFormatter.ofPattern("d MMM yyyy")
  val longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def format(dateString: String)(implicit messages: Messages): String = {

    def tryConvert =
      try Some(languageUtils.Dates.formatDate(LocalDate.parse(dateString, dateFormatter)))
      catch {
        case e: Exception => None
      }

    tryConvert.getOrElse(dateString)
  }

  def formatLocalDate(date: LocalDate): String =
    Some(date.format(longDateFormatter)).getOrElse(throw new Exception("Cannot convert date"))

}
