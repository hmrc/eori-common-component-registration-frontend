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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsForm.VatDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_details
import util.ViewSpec

class VatDetailsViewSpec extends ViewSpec {
  implicit private val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  val mockAuditable: Auditable = mock[Auditable]

  val mockRequestSessionData: RequestSessionData = new RequestSessionData(mockAuditable) {
    def isRestOfTheWorld: Boolean = false
  }

  private val form: Form[VatDetails] = new VatDetailsForm(mockRequestSessionData).vatDetailsForm

  private val view           = inject[vat_details]
  private val vatNumberLabel = "label[for=vat-number]"
  private val postcodeLabel  = "label[for=postcode]"

  "VAT Details" should {
    "have the correct title for UK" in {
      doc.title() must startWith(messages("cds.subscription.uk.vat-details.heading"))
    }
    "have the correct h1 text" in {
      doc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "Your UK VAT details"
    }
    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }

    "have correct first paragraph" in {
      doc.body().getElementById("intro-text").text must startWith(messages("cds.subscription.vat-details.intro-text"))
    }

    "have the correct class on the paragraph" in {
      doc.body.getElementsByTag("p").hasClass("govuk-body") mustBe true
    }

    "contain a link to VAT login page" in {
      val link = doc.body.getElementById("vat-link")
      link.text mustBe messages("cds.subscription.vat-details.intro-text.link")
      link.attr("href") mustBe "https://www.gov.uk/sign-in-vat-account"
    }

    "have a question asking for VAT Registration number " in {
      doc.select(vatNumberLabel).text() must startWith(messages("cds.subscription.vat-details.vat-number"))
    }

    "have the correct title for Iom" in {
      iomDoc.title() must startWith(messages("cds.subscription.vat-details.heading"))
    }
    "have the correct h1 text for Iom" in {
      iomDoc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "Your VAT details"
    }
    "have the correct postcode label for Iom" in {
      iomDoc.select(postcodeLabel).text() must startWith(messages("cds.subscription.vat-details.postcode"))
    }
  }

  private lazy val doc: Document = {
    val result =
      view(form, isInReviewMode = false, UserLocation.Uk, isIndividualOrSoleTrader = false, isRow = false, atarService)
    Jsoup.parse(contentAsString(result))
  }

  private lazy val iomDoc: Document = {
    val result =
      view(form, isInReviewMode = false, UserLocation.Iom, isIndividualOrSoleTrader = false, isRow = false, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
