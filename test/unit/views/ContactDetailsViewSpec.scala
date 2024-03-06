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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactDetailsForm.contactDetailsCreateForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.contact_details
import util.ViewSpec

class ContactDetailsViewSpec extends ViewSpec {

  private val form: Form[ContactDetailsViewModel]               = contactDetailsCreateForm()
  private implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())
  private val view                                              = instanceOf[contact_details]
  private val fullNameLabel                                     = "label[for=full-name]"
  private val telephoneLabel                                    = "label[for=telephone]"

  "Contact Details" should {
    "display correct title" in {
      doc.title() must startWith("EORI number application contact details")
    }
    "have the correct h1 text" in {
      doc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "EORI number application contact details"
    }
    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("govuk-fieldset__heading") mustBe true
    }

    "have the right legend" in {
      doc
        .body()
        .getElementsByTag("legend")
        .text() mustBe "EORI number application contact details"
    }

    "have full Name" in {
      doc.select(fullNameLabel).text() must include("Full name")
      doc.getElementById("full-name").attr("autocomplete") must include("name")
    }

    "have telephone" in {
      doc.select(telephoneLabel).text() must include("Telephone")
      doc.getElementById("telephone").attr("autocomplete") must include("tel")
    }
  }

  private lazy val doc: Document = {
    val result = view(form, Some("email@email.com"), isInReviewMode = false, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
