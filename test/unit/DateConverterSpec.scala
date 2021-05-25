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

package unit

import base.UnitSpec
import org.joda.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter

class DateConverterSpec extends UnitSpec {

  "Date converter" should {

    "convert string date to local date" in {
      val dt = LocalDate.parse("2010-04-28")
      DateConverter.toLocalDate("2010-04-28") shouldBe Some(dt)
    }

    "return None when an invalid date string is provided" in {
      DateConverter.toLocalDate("2010-04-38") shouldBe None
    }
  }
}
