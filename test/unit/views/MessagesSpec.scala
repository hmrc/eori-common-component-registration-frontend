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

import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import play.i18n.Lang
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}

class MessagesSpec extends PlaySpec with Injecting {

  implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  private val messageApi: MessagesApi = inject[MessagesApi]

  private val messagesEn: Messages = MessagesImpl(Lang.forCode("en"), messageApi)
  private val messagesCy: Messages = MessagesImpl(Lang.forCode("cy"), messageApi)

  val keysEn: Set[String] =
    messageApi.messages.get("en").map(_.keySet).getOrElse(throw new RuntimeException("no message keys"))

  val keysCy: Map[String, String] =
    messageApi.messages.get("cy").value

  val sameTranslation: Set[String] = Set(
    "cds.subscription.outcomes.rejected.vat-registered2",
    "cds.not-applicable.label",
    "cds.subscription-details.tab.data",
    "cds.subscription.outcomes.eori-already-associated.vat-registered2",
    "cds.subscription-details.tab.title.data"
  )

  def ignoreKey(key: String): Boolean = key.startsWith("cds.country.") || sameTranslation.contains(key)

  "Messages" should {

    "contain English keys" in {
      keysEn must not be Set.empty
    }

    "contain a Welsh definition for every key" ignore {
      val missingCy: Set[String] = keysEn.flatMap(key => if (messagesCy.isDefinedAt(key)) None else Some(key))
      missingCy mustBe Set.empty
    }

    "welsh key must contain value" in {
      val welshValueForKey = keysCy.flatMap(key => if (key._2.isBlank) Some(key) else None)
      welshValueForKey mustBe Map.empty
    }

    "contain a different Welsh translation for every key" in {

      val sameTranslation: Set[String] = keysEn.flatMap(key => if (!ignoreKey(key) && (messagesEn.apply(key) == messagesCy.apply(key))) Some(key) else None)
      sameTranslation mustBe Set.empty
    }

  }
}
