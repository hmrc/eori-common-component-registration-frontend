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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.email

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time._

// $COVERAGE-OFF$
object DateTimeUtil {

  def dateTime: DateTime =
    new DateTime(Clock.systemUTC().instant.toEpochMilli, DateTimeZone.UTC)

  private def dateTimeWritesIsoUtc: Writes[DateTime] = new Writes[DateTime] {

    def writes(d: org.joda.time.DateTime): JsValue =
      JsString(d.toString(ISODateTimeFormat.dateTimeNoMillis().withZoneUTC()))

  }

  private def dateTimeReadsIso: Reads[DateTime] = new Reads[DateTime] {

    def reads(value: JsValue): JsResult[DateTime] =
      JsSuccess(ISODateTimeFormat.dateTimeParser.parseDateTime(value.as[String]))

  }

  implicit val dateTimeReads: Reads[DateTime]   = dateTimeReadsIso
  implicit val dateTimeWrites: Writes[DateTime] = dateTimeWritesIsoUtc

}

// $COVERAGE-ON$
