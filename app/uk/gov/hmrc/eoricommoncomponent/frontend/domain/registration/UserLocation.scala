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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData

//TODO Make these sealed trait / case object with a isROW fn
object UserLocation {

  val Uk                = "uk"
  val Iom               = "isle-of-man"
  val Eu                = "eu" // TODO Remove EU location
  val ThirdCountry      = "third-country"
  val ThirdCountryIncEU = "third-country-inc-eu"
  val Islands           = "islands"

  val validLocations: Set[String] = Set(Uk, Iom, Eu, ThirdCountry, ThirdCountryIncEU, Islands)

  private val rowLocations = Set(Eu, ThirdCountry, ThirdCountryIncEU, Islands)

  def forId(locationId: String): Boolean = validLocations(locationId)

  def isRow(requestSessionData: RequestSessionData)(implicit request: Request[AnyContent]): Boolean =
    requestSessionData.selectedUserLocation match {
      case Some(location) => isRow(location)
      case _              => false
    }

  def isRow(location: String): Boolean = rowLocations.contains(location)

  def isIom(location: String): Boolean =
    if (location == Iom) true
    else false

}
