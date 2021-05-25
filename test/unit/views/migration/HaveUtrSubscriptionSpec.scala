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

package unit.views.migration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, UtrMatchModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.utrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.match_utr_subscription
import util.ViewSpec

class HaveUtrSubscriptionSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val standardForm: Form[UtrMatchModel] = utrForm
  private val noOptionSelectedForm              = utrForm.bind(Map.empty[String, String])

  private val view = instanceOf[match_utr_subscription]

  "Fresh Subscription Have Utr Page for Company" should {
    "display correct heading" in {
      companyDoc.body
        .getElementsByTag("h1")
        .text mustBe "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) issued in the UK?"
    }

    "display correct title" in {
      companyDoc.title must startWith(
        "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) issued in the UK?"
      )
    }

    "have correct intro" in {
      companyDoc.body
        .getElementById("have-utr-hintHtml")
        .text must include("You will have a UTR number if your organisation pays corporation tax in the UK.")
    }

    "have correct hint" in {
      companyDoc.body
        .getElementById("have-utr-hintHtml")
        .text must include("It will be on tax returns and other letters about Corporation Tax.")
    }
  }

  "Fresh Subscription Have Utr Page for Individual" should {
    "display correct heading" in {
      individualDoc.body
        .getElementsByTag("h1")
        .text mustBe "Do you have a Self Assessment Unique Taxpayer Reference (UTR) issued in the UK?"
    }

    "display correct title" in {
      individualDoc.title must startWith(
        "Do you have a Self Assessment Unique Taxpayer Reference (UTR) issued in the UK?"
      )
    }
    "have correct intro" in {
      individualDoc.body
        .getElementById("have-utr-hintHtml")
        .text must include(
        "You will have a self assessment UTR number if you registered for Self Assessment in the UK."
      )
    }
    "have correct hint" in {
      individualDoc.body
        .getElementById("have-utr-hintHtml")
        .text must include("It will be on tax returns and other letters about Self Assessment.")
    }
  }

  "Subscription Have Utr Page" should {
    "radio button yes with correct label" in {
      companyDoc.body.getElementById("have-utr-true").attr("value") mustBe "true"
      companyDoc.body.getElementsByAttributeValue("for", "have-utr-true").text must include("Yes")
    }

    "radio button no with correct label" in {
      companyDoc.body.getElementById("have-utr-false").attr("value") mustBe "false"
      companyDoc.body.getElementsByAttributeValue("for", "have-utr-false").text must include("No")
    }

  }

  "Form with no option selected" should {
    "display proper message page level" in {
      notSelectedIndividualDoc.body.getElementById("form-error-heading").text mustBe "There is a problem"
      notSelectedCompanyDoc.body.getElementById("form-error-heading").text mustBe "There is a problem"

      notSelectedIndividualDoc.body
        .getElementsByAttributeValue("href", "#have-utr-true")
        .text mustBe "Select yes if you have a UTR number"
      notSelectedCompanyDoc.body
        .getElementsByAttributeValue("href", "#have-utr-true")
        .text mustBe "Select yes if you have a UTR number"
    }
  }

  lazy val companyDoc: Document =
    Jsoup.parse(
      contentAsString(
        view(standardForm, CdsOrganisationType.CompanyId, isInReviewMode = false, atarService, Journey.Subscribe)
      )
    )

  lazy val notSelectedCompanyDoc: Document =
    Jsoup.parse(
      contentAsString(
        view(
          noOptionSelectedForm,
          CdsOrganisationType.CompanyId,
          isInReviewMode = false,
          atarService,
          Journey.Subscribe
        )
      )
    )

  lazy val individualDoc: Document =
    Jsoup.parse(
      contentAsString(
        view(standardForm, CdsOrganisationType.SoleTraderId, isInReviewMode = false, atarService, Journey.Subscribe)
      )
    )

  lazy val notSelectedIndividualDoc: Document =
    Jsoup.parse(
      contentAsString(
        view(
          noOptionSelectedForm,
          CdsOrganisationType.SoleTraderId,
          isInReviewMode = false,
          atarService,
          Journey.Subscribe
        )
      )
    )

}
