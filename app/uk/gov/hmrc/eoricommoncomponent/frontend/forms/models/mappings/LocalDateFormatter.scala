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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings

import play.api.data.FormError
import play.api.data.format.Formatter

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

class LocalDateFormatter(emptyKey: String, invalidKey: String, args: Seq[String] = Seq.empty) extends Formatter[LocalDate] with Formatters {

  private val dayKey = "day"
  private val monthKey = "month"
  private val yearKey = "year"
  private val fieldKeys: List[String] = List(dayKey, monthKey, yearKey)

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_) =>
        Left(Seq(FormError(key, invalidKey, args)))
    }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val int = intFormatter(requiredKey = invalidKey, wholeNumberKey = invalidKey, nonNumericKey = invalidKey, args)

    for {
      day   <- int.bind(s"$key.day", data)
      month <- int.bind(s"$key.month", data)
      year  <- int.bind(s"$key.year", data)
      _     <- validateFields(key, day, month, year)
      date  <- toDate(key, day, month, year)
    } yield date
  }

  private def validateFields(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], Unit] = {

    val errors: List[FormError] = List(
      if (day > 31) Some(FormError(s"$key.$dayKey", "date.day.error", args)) else None,
      if (month > 12) Some(FormError(s"$key.$monthKey", "date.month.error", args)) else None,
      if (year < 1000) Some(FormError(s"$key.$yearKey", "date-invalid-year-too-short", args)) else None,
      if (year > LocalDate.now().getYear) Some(FormError(s"$key.$yearKey", "date.year.error", args)) else None
    ).flatten

    errors match {
      case Nil => Right(())
      case _ => Left(errors)
    }

  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val trimmedData = data.map(d => d._1 -> d._2.trim)

    val fields = fieldKeys.map { field =>
      field -> trimmedData.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    lazy val missingFields = fields
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList

    fields.count(_._2.isDefined) match {
      case 3 =>
        formatDate(key, trimmedData).left.map { leftValue =>
          leftValue.map(formError => formError)
        }
      case 2 =>
        Left(List(FormError(s"$key.${missingFields.head}", s"$key.${missingFields.head}.empty", args)))
      case 1 =>
        Left(
          List(
            FormError(
              s"$key.${missingFields.head}",
              s"$key.${missingFields.head}-$key.${missingFields.last}.empty",
              args
            ),
            FormError(s"$key.${missingFields.last}", "", args)
          )
        )
      case _ =>
        Left(List(FormError(key, emptyKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )

}
