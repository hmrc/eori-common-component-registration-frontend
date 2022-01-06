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

package unit.views

import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.i18n.Lang
import uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers.DateFormatter
import util.ViewSpec

class DateFormatterSpec extends ViewSpec {

  private val dateFormatter = instanceOf[DateFormatter]

  private val messageApi: MessagesApi = instanceOf[MessagesApi]

  "DateFormatter" should {

    "convert string with expected format" in {
      dateFormatter.format("1 Feb 2020") mustBe "1 February 2020"
    }

    "convert string with expected format to Welsh" in {
      implicit val welshMessages: Messages = MessagesImpl(Lang.forCode("cy"), messageApi)

      dateFormatter.format("28 Aug 2020")(welshMessages) mustBe "28 Awst 2020"
    }

    "return invalid date string unchanged" in {
      dateFormatter.format("invalid date format") mustBe "invalid date format"
    }

  }
}
