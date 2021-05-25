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

package uk.gov.hmrc.eoricommoncomponent.frontend

import java.time.Year

import org.joda.time.LocalDate
import play.api.Logger
import play.api.data.FormError

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

object DateConverter {

  private val logger = Logger(this.getClass)

  val earliestYearDateOfBirth         = 1900
  val earliestYearEffectiveVatDate    = 1970
  val earliestYearDateOfEstablishment = 1000

  def toLocalDate(dateStr: String): Option[LocalDate] =
    Try(LocalDate.parse(dateStr)).recoverWith {
      case NonFatal(e) =>
        logger.warn(s"Could not parse the LocalDate '$dateStr': ${e.getMessage}", e)
        Failure(e)
    }.toOption

  def updateDateOfBirthErrors(errors: Seq[FormError]): Seq[FormError] =
    updateYearErrors(errors, earliestYearDateOfBirth)

  def updateDateOfEstablishmentErrors(errors: Seq[FormError]): Seq[FormError] =
    updateYearErrors(errors, earliestYearDateOfEstablishment)

  def updateEffectiveVatDateErrors(errors: Seq[FormError]): Seq[FormError] =
    updateYearErrors(errors, earliestYearEffectiveVatDate)

  private def updateYearErrors(errors: Seq[FormError], minYear: Int): Seq[FormError] = errors.map(
    err =>
      if (err.messages.contains("date.year.error")) err.copy(args = Seq(minYear.toString, Year.now.getValue.toString))
      else err
  )

}
