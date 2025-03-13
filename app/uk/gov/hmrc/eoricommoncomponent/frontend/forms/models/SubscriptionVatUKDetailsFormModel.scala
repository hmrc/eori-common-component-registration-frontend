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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.VatIdentification
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms

case class SubscriptionVatUKDetailsFormModel(hasGbVats: Boolean, gbVats: Option[List[VatIdentification]]) {

  lazy val toVatIds: List[VatIdentification] = (hasGbVats, gbVats) match {
    case (true, Some(list)) => list
    case _ => Nil
  }

}

object SubscriptionVatUKDetailsFormModel {

  def convertRequestForGbVatsToModel(gbVatsRequest: Option[List[String]]): Option[List[VatIdentification]] =
    gbVatsRequest.map(l => l.map(id => VatIdentification.apply(Some(MatchingForms.countryCodeGB), Some(id))))

  def convertModelForGbVatsToRequest(gbVatsModel: Option[List[VatIdentification]]): Option[List[String]] =
    gbVatsModel.map(list => list.map(id => id.number.getOrElse("")))

}
