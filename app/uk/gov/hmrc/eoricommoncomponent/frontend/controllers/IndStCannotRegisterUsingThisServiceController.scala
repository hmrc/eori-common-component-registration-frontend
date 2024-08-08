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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.ind_st_cannot_register_using_this_service

import javax.inject.{Inject, Singleton}

@Singleton
class IndStCannotRegisterUsingThisServiceController @Inject() (
  view: ind_st_cannot_register_using_this_service,
  mcc: MessagesControllerComponents
) extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] = Action { implicit request =>
    Ok(view(service))
  }

}
