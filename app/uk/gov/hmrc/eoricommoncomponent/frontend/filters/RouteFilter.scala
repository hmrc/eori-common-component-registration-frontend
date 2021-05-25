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

package uk.gov.hmrc.eoricommoncomponent.frontend.filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}
import play.mvc.Http.Status.NOT_FOUND
import uk.gov.hmrc.eoricommoncomponent.frontend.CdsErrorHandler
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig

import scala.concurrent.Future

class RouteFilter @Inject() (appConfig: AppConfig, errorHandler: CdsErrorHandler)(implicit val mat: Materializer)
    extends Filter {

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    appConfig.blockedRoutesRegex.exists(_.findFirstIn(rh.uri).isDefined) match {
      case false => next(rh)
      case true  => errorHandler.onClientError(rh, NOT_FOUND, "Blocked routes")
    }

}
