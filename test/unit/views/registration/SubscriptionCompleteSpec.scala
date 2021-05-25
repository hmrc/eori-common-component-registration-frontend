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

package unit.views.registration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.subscription_outcome
import util.ViewSpec

class SubscriptionCompleteSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarSubscribeRequest)

  val eori       = "GB123445562"
  val orgName    = "Test Organisation Name"
  val issuedDate = "01 Jan 2019"

  private val view = instanceOf[subscription_outcome]

  private val doc: Document = Jsoup.parse(contentAsString(view(eori, orgName, issuedDate)))

  "'Subscription Rejected' Page with name" should {

    "display correct heading" in {
      doc.body.getElementsByTag("h1").text() must startWith(s"The EORI number for $orgName is $eori")
    }
    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").hasClass("heading-xlarge") mustBe true
    }
    "have the correct processing date and text" in {
      doc.body.getElementById("issued-date").text mustBe s"issued by HMRC on 1 January 2019"
    }
  }

  "Subscription outcome page" should {

    "display XI EORI paragraph" in {

      val xiParagraph = doc.body().getElementById("xi-eori")

      xiParagraph.getElementsByTag("h2").get(0).text() mustBe "If you move goods to or from Northern Ireland"
      xiParagraph.getElementsByTag("p").get(0).text() mustBe "you will need an EORI number starting with XI if you:"

      val xiBulletList = xiParagraph.getElementsByTag("ul").get(0)

      xiBulletList.getElementsByTag("li").get(
        0
      ).text() mustBe "move goods between Northern Ireland and non-EU countries"
      xiBulletList.getElementsByTag("li").get(1).text() mustBe "make a declaration in Northern Ireland"
      xiBulletList.getElementsByTag("li").get(2).text() mustBe "get a customs decision in Northern Ireland"

      xiParagraph.getElementsByTag("p").get(1).text() mustBe "Apply for an EORI number that starts with XI."
      xiParagraph.getElementsByTag("p").get(
        2
      ).text() mustBe "If you have an EORI number issued by an EU country, you do not need to obtain an EORI starting with XI."
    }

    "display GG paragraph" in {

      val ggParagraph = doc.body().getElementById("eori-gg")

      ggParagraph.getElementsByTag("h2").get(0).text() mustBe "Your EORI number and Government Gateway"
      ggParagraph.getElementsByTag("p").get(
        0
      ).text() mustBe "Your EORI number is linked to the Government Gateway account you have used for this application and you will have access to Advance Tariff Rulings within two hours."
      ggParagraph.getElementsByTag("p").get(
        1
      ).text() mustBe "You can't apply for another EORI number using this Government Gateway."
    }
  }
}
