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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.NonUKIdentification

sealed trait RegistrationInfo {
  val taxPayerId: TaxPayerId
  val lineOne: String
  val lineTwo: Option[String]
  val lineThree: Option[String]
  val lineFour: Option[String]
  val postcode: Option[String]
  val country: String
  val email: Option[String]
  val phoneNumber: Option[String]
  val nonUKIdentification: Option[NonUKIdentification]
  val isAnAgent: Boolean
}

case class IndividualRegistrationInfo(
  firstName: String,
  middleName: Option[String],
  lastName: String,
  dateOfBirth: Option[LocalDate],
  override val taxPayerId: TaxPayerId,
  override val lineOne: String,
  override val lineTwo: Option[String],
  override val lineThree: Option[String],
  override val lineFour: Option[String],
  override val postcode: Option[String],
  override val country: String,
  override val email: Option[String],
  override val phoneNumber: Option[String],
  override val nonUKIdentification: Option[NonUKIdentification],
  override val isAnAgent: Boolean
) extends RegistrationInfo

case class OrgRegistrationInfo(
  name: String,
  organisationType: Option[String],
  isAGroup: Boolean,
  override val taxPayerId: TaxPayerId,
  override val lineOne: String,
  override val lineTwo: Option[String],
  override val lineThree: Option[String],
  override val lineFour: Option[String],
  override val postcode: Option[String],
  override val country: String,
  override val email: Option[String],
  override val phoneNumber: Option[String],
  override val nonUKIdentification: Option[NonUKIdentification],
  override val isAnAgent: Boolean
) extends RegistrationInfo

object RegistrationInfo {
  private val formatsOrganisation = Json.format[OrgRegistrationInfo]
  private val formatsIndividual   = Json.format[IndividualRegistrationInfo]

  implicit val formats = Format[RegistrationInfo](
    Reads { js =>
      formatsIndividual.reads(js) match {
        case individual: JsSuccess[IndividualRegistrationInfo] => individual
        case _                                                 => formatsOrganisation.reads(js)
      }
    },
    Writes {
      case individual: IndividualRegistrationInfo => formatsIndividual.writes(individual)
      case organisation: OrgRegistrationInfo      => formatsOrganisation.writes(organisation)
    }
  )

}

object IndividualRegistrationInfo {

  def apply(
    firstName: String,
    middleName: Option[String],
    lastName: String,
    dateOfBirth: Option[LocalDate]
  ): IndividualRegistrationInfo =
    new IndividualRegistrationInfo(
      firstName,
      middleName,
      lastName,
      dateOfBirth,
      TaxPayerId(""),
      "",
      None,
      None,
      None,
      None,
      "",
      None,
      None,
      None,
      false
    )

}
