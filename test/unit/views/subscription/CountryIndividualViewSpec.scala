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

package unit.views.subscription

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Request
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.CompanyRegisteredCountry
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.country_individual
import util.ViewSpec

class CountryIndividualViewSpec extends ViewSpec {

  private val view = instanceOf[country_individual]

  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val form = CompanyRegisteredCountry.form("ecc.registered-company-country.individual.error")

  private val formWithError = form.bind(Map("countryCode" -> ""))

  val (countries, picker) = Countries.getCountryParametersForAllCountries()

  private val doc: Document = Jsoup.parse(contentAsString(view(form, countries, picker, atarService, false)))

  private val docWithErrorSummary: Document =
    Jsoup.parse(contentAsString(view(formWithError, countries, picker, atarService, false)))

  "Country view" should {

    "display correct title" in {

      doc.title() must startWith("In which country is you business registered?")
    }

    "display correct header" in {

      doc.body().getElementsByTag("h1").text() mustBe "In which country is you business registered?"
    }

    "display input with Country label" in {

      val inputDiv = doc.body().getElementById("country-outer")

      inputDiv.getElementsByTag("label").get(0).text() must startWith("Country")
    }

    "display continue button" in {

      val continueButton = doc.body().getElementById("continue-button")

      continueButton.attr("value") mustBe "Continue"
    }

    "display error summary" in {

      docWithErrorSummary.getElementById("form-error-heading").text() mustBe "There is a problem"
      docWithErrorSummary.getElementsByClass("error-list").get(
        0
      ).text() mustBe "Enter the country in which your business is registered"
    }
  }
}
