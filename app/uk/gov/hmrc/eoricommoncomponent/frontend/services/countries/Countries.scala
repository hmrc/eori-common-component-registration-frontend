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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.countries

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Countries extends Logging {

  private def mdgCountryCodes(fileName: String): List[String] =
    Source
      .fromInputStream(getClass.getResourceAsStream(fileName))
      .getLines()
      .mkString
      .split(',')
      .map(_.replace("\"", ""))
      .toList

  private val countries: List[Country] = {
    def fromJsonFile: List[Country] =
      Json.parse(getClass.getResourceAsStream("/location-autocomplete-canonical-list.json")) match {
        case JsArray(cs) =>
          cs.toList.collect { case JsArray(ArrayBuffer(c: JsString, cc: JsString)) =>
            Country(c.value, countryCode(cc.value))
          }
        case _ =>
          val error = "Could not read JSON array of countries from : location-autocomplete-canonical-list.json"
          // $COVERAGE-OFF$Loggers
          logger.warn(error)
          // $COVERAGE-ON
          throw new IllegalArgumentException(error)
      }

    fromJsonFile.sortBy(_.countryName)
  }

  private def countryCode: String => String = cc => cc.split(":")(1).trim

  val all: List[Country] = countries filter (c => mdgCountryCodes("/mdg-country-codes.csv") contains c.countryCode)

  val allExceptIom: List[Country] =
    countries filter (c => mdgCountryCodes("/mdg-country-codes-not-iom.csv") contains c.countryCode)

  val eu: List[Country] = countries filter (c => mdgCountryCodes("/mdg-country-codes-eu.csv") contains c.countryCode)

  val third: List[Country] =
    countries filter (c => mdgCountryCodes("/mdg-country-codes-third-countries.csv") contains c.countryCode)

  val thirdIncEu: List[Country] =
    countries filter (c => mdgCountryCodes("/mdg-country-codes-third-countries-inc-eu.csv") contains c.countryCode)

  val islands: List[Country] =
    countries filter (c => mdgCountryCodes("/mdg-country-codes-islands.csv") contains c.countryCode)

  def getCountryParameters(location: Option[UserLocation]): (List[Country], CountriesInCountryPicker) = location match {
    case Some(UserLocation.ThirdCountry) =>
      (third, ThirdCountriesInCountryPicker)
    case Some(UserLocation.ThirdCountryIncEU) =>
      (thirdIncEu, ThirdCountriesIncEuInCountryPicker)
    case Some(UserLocation.Islands) => (islands, IslandsInCountryPicker)
    case _ => (allExceptIom, AllCountriesExceptIomInCountryPicker)
  }

}

sealed trait CountriesInCountryPicker

case object AllCountriesInCountryPicker extends CountriesInCountryPicker
case object AllCountriesExceptIomInCountryPicker extends CountriesInCountryPicker
case object ThirdCountriesInCountryPicker extends CountriesInCountryPicker
case object ThirdCountriesIncEuInCountryPicker extends CountriesInCountryPicker
case object IslandsInCountryPicker extends CountriesInCountryPicker
case object NoCountriesInCountryPicker extends CountriesInCountryPicker
