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
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.{AddressDetailsForm, PostcodeForm}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.postcode
import util.ViewSpec

class PostcodeViewSpec extends ViewSpec {

  private val view = instanceOf[postcode]

  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  private val form = PostcodeForm.postCodeCreateForm

  private val formWithError = form.bind(Map("postcode" -> "invalid"))

  val viewModel: PostcodeViewModel = PostcodeViewModel("SE88 1AA", Some("addressline1"))

  private def doc(): Document =
    Jsoup.parse(contentAsString(view(form, atarService)))

  private val docWithErrorSummary = Jsoup.parse(contentAsString(view(formWithError, atarService)))

  "Postcode Lookup page" should {

    "display title for individual" in {

      doc().title() must startWith("What is your address?")
    }

    "display header for individual" in {

      doc().body().getElementsByTag("h1").text() mustBe "What is your address?"
    }

    "display postcode input with label" in {

      doc().body().getElementsByClass("govuk-label govuk-!-font-weight-bold postcode").text() mustBe "Postcode"
    }

    "display line 1 input with label" in {

      doc().body().getElementsByClass(
        "govuk-label govuk-!-font-weight-bold"
      ).text() mustBe "House number or name (optional)"
    }

    "display Find Address button" in {

      doc().body().getElementById("continue-button").text() mustBe "Continue"
    }

    "display manual address link" in {
      val manualAddressLink = doc().body().getElementById("cannot-find-address")

      manualAddressLink.text() mustBe "Enter your address manually"
      manualAddressLink.attr("href") mustBe "/customs-registration-services/atar/register/manual/address"
    }

    "display error summary" in {

      docWithErrorSummary.getElementsByClass("govuk-error-summary__title").text() mustBe "There is a problem"
      docWithErrorSummary.getElementsByClass("govuk-error-summary__list").get(0).text() mustBe messages(
        "cds.subscription.contact-details.error.postcode"
      )
    }
  }
}
