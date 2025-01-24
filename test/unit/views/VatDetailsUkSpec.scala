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

import common.pages.subscription.SubscriptionVatDetailsPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsForm.vatDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_details
import util.ViewSpec

class VatDetailsUkSpec extends ViewSpec {

  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  private val view = instanceOf[vat_details]

  val form: Form[VatDetails] = vatDetailsForm

  "Vat Details UK Page" should {

    "display correct title" in {
      doc.title must startWith(SubscriptionVatDetailsPage.title)
    }

    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").attr("class") mustBe "govuk-heading-l"
    }
  }

  private lazy val doc: Document =
    Jsoup.parse(contentAsString(view(form, isInReviewMode = false, UserLocation.Uk, atarService)))

}
