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

package uk.gov.hmrc.eoricommoncomponent.frontend.views

import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

object ServiceName {

  def longName(service: Service)(implicit messages: Messages): String =
    if (isWelsh) service.friendlyNameWelsh else service.friendlyName

  def longName(implicit messages: Messages, request: Request[_]): String =
    longName(service)

  def shortName(service: Service): String = service.shortName

  private def isWelsh(implicit messages: Messages) = messages.lang.code == "cy"

  def shortName(implicit request: Request[_]): String =
    shortName(service)

  def service(implicit request: Request[_]): Service =
    // TODO - investigate why service is sometimes missing from url
    Service.serviceFromRequest.getOrElse(empty)

  private val empty = Service("", "", "", None, "", "", "", None)
}
