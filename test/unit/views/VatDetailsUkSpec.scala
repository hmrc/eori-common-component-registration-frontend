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

import common.pages.subscription.SubscriptionVatDetailsPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsForm.vatDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_details
import util.ViewSpec
import java.time.LocalDate

class VatDetailsUkSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[vat_details]

  val form: Form[VatDetails] = vatDetailsForm

  "Vat Details UK Page" should {

    "display correct title" in {
      doc.title must startWith(SubscriptionVatDetailsPage.title)
    }

    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").attr("class") mustBe "govuk-heading-l"
    }

    "should display correct text" in {
      doc.body.getElementById("intro-text").text() must include(
        "You can find your VAT details on your VAT registration certificate. To view this login to your tax account and select 'More VAT details'. Select the 'View VAT certificate' link under the heading 'Help with tax'."
      )

      doc.body.getElementById("intro-link").text() must include("Find your VAT details (opens in new tab).")

      doc.body.getElementsByTag("label").text() must include("VAT registration address postcode")

    }

  }

  private lazy val doc: Document =
    Jsoup.parse(contentAsString(view(form, false, atarService)))

}
