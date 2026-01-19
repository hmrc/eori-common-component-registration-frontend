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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.siccode

import play.api.data.Forms.text
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Forms}

import javax.inject.Singleton

@Singleton
class SicCodeForm() {

  def form(): Form[SicCodeViewModel] = {
    Form(
      Forms.mapping("sic" -> text.verifying(validSicCode))(SicCodeViewModel.apply)(sicCodeViewModel => Some(sicCodeViewModel.sicCode))
    )
  }

  private def validSicCode: Constraint[String] =
    Constraint("constraints.sic") { s =>
      val sicCodeWhitespaceRemoved = s.filterNot(_.isWhitespace)

      sicCodeWhitespaceRemoved match {
        case s if s.isEmpty =>
          Invalid(ValidationError("cds.subscription.sic.error.empty"))
        case s if !s.matches("[0-9]*") =>
          Invalid(ValidationError("cds.subscription.sic.error.wrong-format"))
        case s if s.length < 4 =>
          Invalid(ValidationError("cds.subscription.sic.error.too-short"))
        case s if s.length > 5 =>
          Invalid(ValidationError("cds.subscription.sic.error.too-long"))
        case _ => Valid
      }
    }
}
