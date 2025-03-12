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
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.you_need_different_service_iom

import javax.inject.{Inject, Singleton}

@Singleton
class YouNeedADifferentServiceIomController @Inject() (
  youNeedADifferentService: you_need_different_service_iom,
  mcc: MessagesControllerComponents
) extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] = Action { implicit request =>
    Ok(youNeedADifferentService(service))
  }

}
