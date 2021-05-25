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

package common.pages

trait HowCanWeIdentifyYouPage extends WebPage {
  override val title = "What information can we use to confirm your identity?"

  val fieldLevelErrorNino = "//*[@id='nino-outer']//span[@class='error-message']"
  val fieldLevelErrorUtr  = "//*[@id='utr-outer']//span[@class='error-message']"

}

object SubscribeHowCanWeIdentifyYouPage extends HowCanWeIdentifyYouPage {}

object RegisterHowCanWeIdentifyYouPage extends HowCanWeIdentifyYouPage {}
