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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails

case class VatControlListResponse(
  postcode: Option[String] = None,
  dateOfReg: Option[String] = None,
  lastNetDue: Option[Double] = None,
  lastReturnMonthPeriod: Option[String] = None
) {

  private def stripSpaces: String => String = s => s.filterNot(_.isSpaceChar)

  def isPostcodeAssociatedWithVrn(vatDetails: VatDetails, isRestOfTheWorld: Boolean): Boolean = {
    postcode match {
      case Some(pc) => stripSpaces(pc) equalsIgnoreCase stripSpaces(vatDetails.postcode)
      case None     => isRestOfTheWorld
    }
  }

}

object VatControlListResponse {
  implicit val jsonFormat: OFormat[VatControlListResponse] = Json.format[VatControlListResponse]
}
