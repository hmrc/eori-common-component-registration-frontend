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

package unit.views

import base.Injector
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.i18n.Lang

class MessagesSpec extends PlaySpec with Injector {

  private val messageApi: MessagesApi = instanceOf[MessagesApi]

  private val messagesEn: Messages = MessagesImpl(Lang.forCode("en"), messageApi)
  private val messagesCy: Messages = MessagesImpl(Lang.forCode("cy"), messageApi)

  val keysEn: Set[String] =
    messageApi.messages.get("en").map(_.keySet).getOrElse(throw new RuntimeException("no message keys"))

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

    "contain a Welsh definition for every key" in {
      val missingCy: Set[String] = keysEn.flatMap(key => if (messagesCy.isDefinedAt(key)) None else Some(key))
      missingCy mustBe Set.empty
    }

    "contain a different Welsh translation for every key" in {

      val sameTranslation: Set[String] = keysEn.flatMap(
        key => if (!ignoreKey(key) && (messagesEn.apply(key) == messagesCy.apply(key))) Some(key) else None
      )
      sameTranslation mustBe Set.empty
    }

    "print out any untranslated Welsh messages (not a test)" in {
      val welshMessages = messageApi.messages.get("cy").map(_.values.toList).getOrElse(List.empty)
      welshMessages must not be List.empty

      val untranslated = welshMessages.filter(_.startsWith("TRANSLATE")).map(_.substring(9))
      println("Untranslated messages:")
      untranslated.foreach(m => println(m))
    }

  }
}
