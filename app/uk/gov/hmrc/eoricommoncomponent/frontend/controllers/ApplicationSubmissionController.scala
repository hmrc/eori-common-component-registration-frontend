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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{
  CharityPublicBodyNotForProfit,
  Company,
  Embassy,
  Individual,
  LimitedLiabilityPartnership,
  Partnership,
  SoleTrader
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.application_processing

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ofPattern
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ApplicationSubmissionController @Inject() (
  authorise: AuthAction,
  mcc: MessagesControllerComponents,
  sessionCache: SessionCache,
  application_processing_view: application_processing
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def processing(service: Service): Action[AnyContent] = authorise.ggAuthorisedUserAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.subscriptionDetails.flatMap { sd =>
        sessionCache.txe13ProcessingDate.map { txe13ProcessedDate =>
          val name = sd.formData.organisationType.flatMap {
            case SoleTrader | Individual => sd.nameDobDetails.map(model => s"${model.firstName} ${model.lastName}")
            case Company | LimitedLiabilityPartnership | Partnership | CharityPublicBodyNotForProfit =>
              sd.nameOrganisationDetails.map(_.name)
            case Embassy => sd.embassyName
            case _       => Some("")
          }
            .getOrElse("")

          val email = sd.contactDetails.map(_.emailAddress).getOrElse("")

          Ok(
            application_processing_view(
              name,
              email,
              LocalDateTime.parse(txe13ProcessedDate).atOffset(UTC).format(ofPattern("dd MMMM yyyy")),
              service
            )
          )
        }
      }
  }

}
