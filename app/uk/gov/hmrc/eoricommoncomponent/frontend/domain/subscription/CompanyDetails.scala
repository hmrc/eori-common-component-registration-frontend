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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

trait CompanyDetailsViewModel {
  def useShortName: Boolean
  def shortName: Option[String]
  def principalEconomicActivity: String
}

case class CompanyDetailsWithDateOfEstablishmentViewModel(
  useShortName: Boolean,
  shortName: Option[String],
  dateEstablished: LocalDate,
  principalEconomicActivity: String
) extends CompanyDetailsViewModel

object CompanyDetailsWithDateOfEstablishmentViewModel {

  implicit val jsonFormat = Json.format[CompanyDetailsWithDateOfEstablishmentViewModel]
}

case class CompanyDetailsWithoutDateOfEstablishmentViewModel(
  useShortName: Boolean,
  shortName: Option[String],
  principalEconomicActivity: String
) extends CompanyDetailsViewModel

object CompanyDetailsWithoutDateOfEstablishmentViewModel {
  implicit val jsonFormat = Json.format[CompanyDetailsWithoutDateOfEstablishmentViewModel]
}

case class BusinessShortName(shortNameProvided: Boolean, shortName: Option[String])

object BusinessShortName {
  implicit val jsonFormat = Json.format[BusinessShortName]

  def apply(shortName: String): BusinessShortName = BusinessShortName(true, Some(shortName))
}

case class CompanyShortNameViewModel(useShortName: Option[Boolean], shortName: Option[String])
