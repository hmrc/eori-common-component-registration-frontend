/*
 * Copyright 2026 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{standalone_subscription_outcome, subscription_outcome, subscription_outcome_fail}
import util.{CSRFTest, TestData}

class UserResearchBannerSpec extends PlaySpec with CSRFTest with TestData {

  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(fakeAtarRegisterRequest)

  private val bannerUrlEn = "https://research.example/en"
  private val bannerUrlCy = "https://research.example/cy"

  private def appWith(bannerEnabled: Boolean): Application =
    new GuiceApplicationBuilder()
      .configure(
        "features.user-research-banner"        -> bannerEnabled,
        "external-url.user-research-banner-en" -> bannerUrlEn,
        "external-url.user-research-banner-cy" -> bannerUrlCy
      )
      .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
      .build()

  private lazy val bannerOn = appWith(bannerEnabled = true)
  private lazy val bannerOff = appWith(bannerEnabled = false)

  private def messagesFor(app: Application, lang: String): Messages =
    MessagesImpl(Lang(lang), app.injector.instanceOf[MessagesApi])

  private def subscriptionOutcome(app: Application, service: Service)(implicit messages: Messages): HtmlFormat.Appendable =
    app.injector
      .instanceOf[subscription_outcome]
      .apply(service, "GB123456789012", "01 Jan 2019", s"ecc.start-page.para1.bullet2.new.${service.code}", "")

  private def optedInPages(app: Application)(implicit messages: Messages): Seq[(String, HtmlFormat.Appendable)] =
    Seq(
      "standalone_subscription_outcome (eori-only)" -> app.injector
        .instanceOf[standalone_subscription_outcome]
        .apply("GB123456789012", "01 Jan 2019", eoriOnlyService),
      "subscription_outcome (gagmr)"                -> subscriptionOutcome(app, Service.gagmr),
      "subscription_outcome (atar)"                 -> subscriptionOutcome(app, atarService)
    )

  private def doc(html: HtmlFormat.Appendable): Document = Jsoup.parse(contentAsString(html))

  private def banners(html: HtmlFormat.Appendable) = doc(html).body.getElementsByClass("hmrc-user-research-banner")

  "User research banner" should {

    "be displayed on every opted-in page with the English link" in {
      implicit val messages: Messages = messagesFor(bannerOn, "en")
      optedInPages(bannerOn).foreach { case (name, page) =>
        withClue(name) {
          banners(page).size mustBe 1
          val link = doc(page).body.getElementsByClass("hmrc-user-research-banner__link").first()
          link.attr("href") mustBe bannerUrlEn
          link.text mustBe "Join our research panel (opens in new tab)"
          doc(page).body.getElementsByClass("hmrc-user-research-banner__close").size mustBe 0
        }
      }
    }

    "use the Welsh link and Welsh copy when the page is in Welsh" in {
      implicit val messages: Messages = messagesFor(bannerOn, "cy")
      optedInPages(bannerOn).foreach { case (name, page) =>
        withClue(name) {
          val link = doc(page).body.getElementsByClass("hmrc-user-research-banner__link").first()
          link.attr("href") mustBe bannerUrlCy
          link.text mustBe "Ymunwch â’n panel ymchwil (yn agor tab newydd)"
        }
      }
    }

    "not be displayed on any opted-in page when the feature is switched off" in {
      implicit val messages: Messages = messagesFor(bannerOff, "en")
      optedInPages(bannerOff).foreach { case (name, page) =>
        withClue(name)(banners(page).size mustBe 0)
      }
    }

    "not be displayed on a page that does not opt in" in {
      implicit val messages: Messages = messagesFor(bannerOn, "en")
      val page = bannerOn.injector.instanceOf[subscription_outcome_fail].apply("01 Jan 2019", "Org Name", atarService)
      banners(page).size mustBe 0
    }
  }
}
