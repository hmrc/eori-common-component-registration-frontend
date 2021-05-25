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

package unit.views.email

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.check_your_email
import util.ViewSpec

class CheckYourEmailSpec extends ViewSpec {
  val isInReviewMode    = false
  val previousPageUrl   = "/"
  val form: Form[YesNo] = EmailForm.confirmEmailYesNoAnswerForm()

  val view = instanceOf[check_your_email]

  implicit val request = withFakeCSRF(FakeRequest())

  "What Is Your Email Address page" should {
    "display correct title" in {
      doc.title must startWith("Is this the email address you want to use?")
    }
    "have the correct h1 text" in {
      doc.body.getElementsByTag("h1").text() mustBe "Is this the email address you want to use?"
    }
    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have an input of type 'text'" in {
      doc.body.getElementById("email").text() mustBe "test@example.com"
    }
  }

  lazy val doc: Document = {
    val email  = "test@example.com"
    val result = view(Some(email), form, isInReviewMode, atarService, Journey.Subscribe)
    Jsoup.parse(contentAsString(result))
  }

}
