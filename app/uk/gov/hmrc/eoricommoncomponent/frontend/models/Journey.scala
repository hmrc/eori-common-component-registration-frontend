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

package uk.gov.hmrc.eoricommoncomponent.frontend.models

import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.Constants

object Journey extends Enumeration {

  val Register, Subscribe = Value

  implicit val reads: Reads[Journey.Value]   = Reads.enumNameReads(Journey)
  implicit val writes: Writes[Journey.Value] = Writes.enumNameWrites

  implicit lazy val pathBindable: PathBindable[Journey.Value] = new PathBindable[Journey.Value] {

    override def bind(key: String, value: String): Either[String, Journey.Value] =
      value match {
        case "subscribe" => Right(Subscribe)
        case "register"  => Right(Register)
        case _           => Left(Constants.INVALID_PATH_PARAM)
      }

    override def unbind(key: String, value: Journey.Value): String =
      value match {
        case Subscribe => "subscribe"
        case Register  => "register"
      }

  }

  def apply(journey: String): Journey.Value = journey match {
    case "subscribe" => Subscribe
    case "register"  => Register
  }

  implicit def queryBindable(implicit pathBindable: PathBindable[Journey.Value]): QueryStringBindable[Journey.Value] =
    new QueryStringBindable[Journey.Value] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Journey.Value]] =
        params.get(key).map(seq => pathBindable.bind(key, seq.headOption.getOrElse("")))

      override def unbind(key: String, value: Journey.Value): String = pathBindable.unbind(key, value)
    }

  def journeyFromRequest(implicit request: Request[_]): Journey.Value = {
    val path = request.path
    if (path.contains("/subscribe/") || path.endsWith("/subscribe"))
      Journey.Subscribe
    else
      Journey.Register

  }

}
