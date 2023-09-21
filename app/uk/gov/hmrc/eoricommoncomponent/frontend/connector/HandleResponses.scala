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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.mvc.Http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpResponse

trait HandleResponses extends Logging {

  def handleResponse[A](response: HttpResponse)(implicit reads: Reads[A]): Either[ResponseError, A] =
    response.json.validate[A] match {
      case JsSuccess(a, _) => Right(a)
      case JsError(_) =>
        val error = s"Invalid JSON returned: ${response.body}"
        logger.error(error)
        Left(ResponseError(INTERNAL_SERVER_ERROR, error))
    }

}

final case class ResponseError(status: Int, description: String)
