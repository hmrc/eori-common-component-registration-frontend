/*
 * Copyright 2022 HM Revenue & Customs
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

package common.support

case class GGCredentials(userId: String, password: String, oid: Option[String])

object Env {

  lazy val port: Int = Option(System.getProperty("port")).fold(6751)(_.toInt)

  lazy val frontendHost: String = Option(System.getProperty("host")).getOrElse("http://localhost:" + port)

}
