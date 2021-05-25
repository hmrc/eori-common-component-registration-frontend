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

package unit.playext.mappers

import base.UnitSpec
import org.joda.time.LocalDate
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import play.api.data.{FormError, Mapping}
import uk.gov.hmrc.eoricommoncomponent.frontend.playext.mappers.DateTuple
import uk.gov.hmrc.play.mappers.DateFields._

class DateTupleSpec extends UnitSpec {

  private val dateField        = "some-date"
  private val customError      = "some.custom.error.key"
  private val defaultDateTuple = DateTuple.dateTuple(minYear = 1999)
  private val customDateTuple  = DateTuple.dateTuple(customError, minYear = 1999)

  private val d = "15"
  private val m = "7"
  private val y = "2010"

  private val defaultDate = LocalDate.parse("2010-7-15")

  private def request(dayValue: String = d, monthValue: String = m, yearValue: String = y) = Map(
    s"$dateField.$day"   -> dayValue,
    day                  -> dayValue,
    s"$dateField.$month" -> monthValue,
    month                -> monthValue,
    s"$dateField.$year"  -> yearValue,
    year                 -> yearValue
  )

  private val allMappings = Table(
    ("date mapping label", "date mapping instance"),
    ("default date mapping", defaultDateTuple),
    ("date mapping with custom error message", customDateTuple)
  )

  private val validatingMappings = Table(
    ("date mapping label", "date mapping instance", "error message"),
    ("default date mapping", defaultDateTuple, "cds.error.invalid.date.format"),
    ("date mapping with custom error message", customDateTuple, customError)
  )

  forAll(allMappings) { (label, dateMapping) =>
    implicit val dm = dateMapping

    label should {
      "accept a valid date" in {
        assertSuccessfulBinding(request(), Some(defaultDate))
      }

      "trim date elements" in {
        assertSuccessfulBinding(request(s" $d ", s" $m ", s" $y "), Some(defaultDate))
      }

      "return None when all the fields are empty" in {
        assertSuccessfulBinding(request("", "", ""), None)
      }
    }
  }

  forAll(validatingMappings) { (label, dateMapping, errorMessage) =>
    implicit val dm = dateMapping

    s"validating $label" should {

      "reject invalid date" in {
        assertErrorTriggered(request(dayValue = "30", monthValue = "2"), "", errorMessage)
      }

      "reject invalid day" in {
        assertErrorTriggered(request(dayValue = "32"), day, "date.day.error")
      }

      "reject day with characters instead of numbers" in {
        assertErrorTriggered(request(dayValue = "foo"), day, "date.day.error")
      }

      "reject month with characters instead of numbers" in {
        assertErrorTriggered(request(monthValue = "foo"), month, "date.month.error")
      }

      "reject year with characters instead of numbers" in {
        assertErrorTriggered(request(yearValue = "foo"), year, "date.year.error")
      }

      "reject invalid month" in {
        assertErrorTriggered(request(monthValue = "13"), month, "date.month.error")
      }

      "reject invalid year" in {
        assertErrorTriggered(request(yearValue = "0"), year, "date.year.error")
      }

      "return no date without day" in {
        assertNoDateReturned(request(dayValue = ""))
      }

      "return no date without month" in {
        assertNoDateReturned(request(monthValue = ""))
      }

      "return no date without year" in {
        assertNoDateReturned(request(yearValue = ""))
      }
    }
  }

  private def assertSuccessfulBinding(request: Map[String, String], expectedResult: Option[LocalDate])(implicit
    dateMapping: Mapping[Option[LocalDate]]
  ) {
    dateMapping.bind(request) shouldBe Right(expectedResult)
  }

  private def assertErrorTriggered(request: Map[String, String], field: String, errorMessage: String)(implicit
    dateMapping: Mapping[Option[LocalDate]]
  ) {
    dateMapping.bind(request) shouldBe Left(Seq(FormError(field, errorMessage)))
  }

  private def assertNoDateReturned(request: Map[String, String])(implicit dateMapping: Mapping[Option[LocalDate]]) {
    dateMapping.bind(request) shouldBe Right(None)
  }

}
