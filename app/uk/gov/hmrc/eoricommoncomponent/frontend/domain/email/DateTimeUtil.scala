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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.email

import play.api.libs.json._

import java.time._
import java.time.format.DateTimeFormatter

object DateTimeUtil {

  def dateTime: LocalDateTime = LocalDateTime.ofInstant(
    Clock.systemUTC().instant,
    ZoneId.of("Europe/London")
  ).truncatedTo(java.time.temporal.ChronoUnit.MILLIS)

  private def dateTimeWritesIsoUtc: Writes[LocalDateTime] = new Writes[LocalDateTime] {

    def writes(d: LocalDateTime): JsValue =
      JsString(
        ZonedDateTime.of(d, ZoneId.of("Europe/London")).withNano(0).withZoneSameInstant(ZoneOffset.UTC).format(
          DateTimeFormatter.ISO_DATE_TIME
        )
      )

  }

  private def dateTimeReadsIso: Reads[LocalDateTime] = new Reads[LocalDateTime] {

    def reads(value: JsValue): JsResult[LocalDateTime] =
      try JsSuccess(
        ZonedDateTime.parse(value.as[String], DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(
          ZoneId.of("Europe/London")
        ).toLocalDateTime
      )
      catch {
        case e: Exception => JsError(s"Could not parse '${value.toString()}' as an ISO date. Reason: ${e.getMessage}")
      }

  }

  implicit val dateTimeReads: Reads[LocalDateTime]   = dateTimeReadsIso
  implicit val dateTimeWrites: Writes[LocalDateTime] = dateTimeWritesIsoUtc

}
