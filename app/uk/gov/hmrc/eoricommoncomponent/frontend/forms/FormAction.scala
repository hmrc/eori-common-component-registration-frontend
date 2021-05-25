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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

sealed trait FormAction

object FormAction {
  private val changeLabel   = Change.toString
  private val continueLabel = Continue.toString
  private val removeLabel   = Remove.toString

  def fromUrlEncoded(input: Map[String, Seq[String]]): FormAction =
    input.flatMap {
      case (`changeLabel`, _)      => Some(Change)
      case (`continueLabel`, _)    => Some(Continue)
      case (`removeLabel`, values) => Some(Remove(values))
      case _                       => None
    }.headOption.getOrElse(Unknown)

}

case object Unknown  extends FormAction
case object Continue extends FormAction
case object Change   extends FormAction

case class Remove(keys: Seq[String]) extends FormAction
