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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription

import akka.routing.MurmurHash
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.VatIdentification

case class SubscriptionVatEUDetailsFormModel(hasOtherVats: Boolean, vatIds: Option[List[VatIdentification]])

object SubscriptionVatEUDetailsFormModel {

  def stringListsToVats(vatCountryCodes: List[String], vatNumbers: List[String]): List[VatIdentification] =
    vatCountryCodes.map(Some(_)).zipAll(vatNumbers.map(Some(_)), None, None).map {
      case (country, vatId) => VatIdentification(country, vatId)
    }

  def vatsToStringLists(vatIds: List[VatIdentification]): Option[(List[String], List[String])] =
    vatIds match {
      case Nil => None
      case _ =>
        val (countryCodes, numbers) = vatIds.map(id => id.countryCode.getOrElse("") -> id.number.getOrElse("")).unzip
        Some(countryCodes -> numbers)
    }

}

case class VatEUDetailsModel(vatCountry: String, vatNumber: String) {
  val index = MurmurHash.stringHash(s"$vatNumber:$vatCountry").abs
}

object VatEUDetailsModel {
  implicit val format: Format[VatEUDetailsModel] = Json.format[VatEUDetailsModel]
}
