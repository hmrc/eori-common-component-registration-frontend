/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.config

import play.api.Configuration
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

class ServiceConfig(configuration: Configuration) {

  val supportedServicesMap: Map[String, Service] = serviceList.map(service => service.code -> service).toMap

  private def serviceList: Set[Service] =
    serviceKeys.map { service =>
      val englishFriendlyName = configuration.get[String](s"services-config.$service.friendlyName").replace("_", " ")
      val welshFriendlyName =
        configuration.getOptional[String](s"services-config.$service.friendlyNameWelsh").map(
          _.replace("_", " ")
        ).filter(_.nonEmpty).getOrElse(englishFriendlyName)

      Service(
        code = service,
        enrolmentKey = configuration.get[String](s"services-config.$service.enrolment"),
        shortName = configuration.get[String](s"services-config.$service.shortName"),
        callBack = configuration.getOptional[String](s"services-config.$service.callBack").filter(_.nonEmpty),
        friendlyName = englishFriendlyName,
        friendlyNameWelsh = welshFriendlyName,
        feedbackUrl = configuration.getOptional[String](s"services-config.$service.feedBack").filter(_.nonEmpty)
      )
    }

  private def serviceKeys = configuration.get[String]("services-config.list").split(",").map(_.trim).toSet

}
