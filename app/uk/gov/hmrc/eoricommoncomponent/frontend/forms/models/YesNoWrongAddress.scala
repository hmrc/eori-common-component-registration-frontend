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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.oneOf

object YesNoWrongAddress {

  val yesAnswered = "yes"
  val wrongAddress = "wrong-address"
  val noAnswered = "no"

  private val validYesNoWrongAddress = Set(yesAnswered, noAnswered, wrongAddress)

  def createForm(): Form[YesNoWrongAddress] =
    Form(
      mapping(
        "yes-no-wrong-address" ->
          optional(text).verifying("yes-no-wrong-address.error", x => x.fold(false)(oneOf(validYesNoWrongAddress)))
      )(YesNoWrongAddress.apply)(yesNoWrongAddress => Some(yesNoWrongAddress.yesNoWrongAddress))
    )
}

case class YesNoWrongAddress(yesNoWrongAddress: Option[String]) {

  def areDetailsCorrect: YesNoWrong = yesNoWrongAddress match {
    case Some("yes") => Yes
    case Some("no") => No
    case Some("wrong-address") => WrongAddress
    case _ => InvalidAddress
  }

}

sealed trait YesNoWrong

case object Yes extends YesNoWrong

case object No extends YesNoWrong

case object WrongAddress extends YesNoWrong

case object InvalidAddress extends YesNoWrong
