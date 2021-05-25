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

package util

import akka.util.Timeout
import base.Injector
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Lang.defaultLang
import play.api.i18n._
import play.api.mvc.Request
import play.api.test.CSRFTokenHelper

import scala.concurrent.duration._

trait ViewSpec extends PlaySpec with CSRFTest with Injector with TestData {

  private val messageApi: MessagesApi = instanceOf[MessagesApi]

  implicit val messages: Messages = MessagesImpl(defaultLang, messageApi)

  implicit val timeout: Timeout = 30.seconds
  val userId: String            = "someUserId"
}

import play.api.test.FakeRequest

trait CSRFTest {

  def withFakeCSRF[T](fakeRequest: FakeRequest[T]): Request[T] =
    CSRFTokenHelper.addCSRFToken(fakeRequest)

  val fakeAtarSubscribeRequest = FakeRequest("GET", "/atar/subscribe")
}
