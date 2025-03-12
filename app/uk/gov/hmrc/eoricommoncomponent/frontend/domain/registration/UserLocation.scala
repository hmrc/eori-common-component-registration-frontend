/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Enumerable, WithName}

sealed trait UserLocation

object UserLocation extends Enumerable.Implicits {

  case object Uk extends WithName("uk") with UserLocation
  case object Iom extends WithName("isle-of-man") with UserLocation
  case object ThirdCountry extends WithName("third-country") with UserLocation
  case object ThirdCountryIncEU extends WithName("third-country-inc-eu") with UserLocation
  case object Islands extends WithName("islands") with UserLocation

  implicit def convert(location: UserLocation): String =
    location.toString

  val values: Seq[UserLocation] = Seq(Uk, Iom, ThirdCountry, ThirdCountryIncEU, Islands)
  val validLocations: Set[String] = values.map(_.toString).toSet

  private val rowLocations: Set[UserLocation] = Set(ThirdCountry, ThirdCountryIncEU, Islands)

  def forId(locationId: String): Boolean = validLocations(locationId)

  def isRow(requestSessionData: RequestSessionData)(implicit request: Request[AnyContent]): Boolean =
    requestSessionData.selectedUserLocation match {
      case Some(location) => isRow(location)
      case _ => false
    }

  def isRow(location: UserLocation): Boolean = rowLocations.contains(location)

  implicit val enumerable: Enumerable[UserLocation] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
