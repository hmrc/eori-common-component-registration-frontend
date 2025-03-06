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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_registered_uk

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class VatRegisteredController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  vatRegisteredUkView: vat_registered_uk,
  sessionCacheService: SessionCacheService
)(implicit ec: ExecutionContext)
    extends VatRegisteredUkController(
      authAction,
      subscriptionBusinessService,
      subscriptionDetailsService,
      requestSessionData,
      mcc,
      vatRegisteredUkView,
      sessionCacheService
    ) {

  override def createForm(service: Service): Action[AnyContent] = super.createForm(service)

  override def reviewForm(service: Service): Action[AnyContent] = super.reviewForm(service)

  override def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    super.submit(isInReviewMode, service)

}
