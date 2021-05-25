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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

sealed abstract class ES1QueryType(val value: String)

object ES1QueryType {
  case object All extends ES1QueryType("all")

  case object Principal extends ES1QueryType("principal")

  case object Delegated extends ES1QueryType("delegated")
}

case class ES1Request(enrolment: String, queryType: ES1QueryType)

object ES1Request {

  def apply(service: Service, eori: String, queryType: ES1QueryType = ES1QueryType.All): ES1Request = {
    val enrolment = service.enrolmentKey + "~EORINumber~" + eori

    ES1Request(enrolment, queryType)
  }

}

case class ES1Response(principalGroupIds: Option[Seq[String]], delegatedGroupIds: Option[Seq[String]]) {

  val isEnrolmentInUse: Boolean = principalGroupIds.exists(_.nonEmpty) || delegatedGroupIds.exists(_.nonEmpty)
}

object ES1Response {
  implicit val format = Json.format[ES1Response]
}
