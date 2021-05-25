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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription

import play.api.data.{Form, Forms}
import play.api.data.Forms._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils._

case class EoriUnableToUse(answer: Option[String]) {

  def isAnswerChangeEori(): Boolean = answer.exists(_ == EoriUnableToUse.changeEori)
}

object EoriUnableToUse {

  val changeEori = "change"
  val signout    = "signout"

  val validAnswers: Set[String] = Set(changeEori, signout)

  def form(): Form[EoriUnableToUse] = Form(
    Forms.mapping(
      "answer" -> optional(text).verifying(
        "ecc.what-is-your-eori.unable-to-use.empty",
        answer => answer.fold(false)(oneOf(validAnswers))
      )
    )(EoriUnableToUse.apply)(EoriUnableToUse.unapply)
  )

}
