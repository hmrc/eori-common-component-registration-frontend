/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.viewModels

import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatRegisteredUkController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Iom
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

object VatRegisteredUkViewModel {

  def formAction(isInReviewMode: Boolean, service: Service): Call =
    VatRegisteredUkController.submit(isInReviewMode, service)

  def titleAndHeadingLabel(isIndividualSubscriptionFlow: Boolean, isPartnership: Boolean, userLocation: UserLocation)(implicit
    messages: Messages
  ): String = {
    if (isIndividualSubscriptionFlow && userLocation == Iom)
      messages("cds.subscription.vat-registered.individual.title-and-heading")
    else if (isIndividualSubscriptionFlow && userLocation != Iom)
      messages("cds.subscription.vat-question-uk.individual")
    else if (isPartnership && userLocation != Iom)
      messages("cds.subscription.vat-registered-uk.partnership.title-and-heading")
    else if (isPartnership && userLocation == Iom)
      messages("cds.subscription.vat-registered.partnership.title-and-heading")
    else if (userLocation == Iom) messages("cds.subscription.vat-registered.title-and-heading")
    else messages("cds.subscription.vat-registered-uk.title-and-heading")
  }

}
