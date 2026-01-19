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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NinoOrUtrChoice

import javax.inject.Singleton

@Singleton
class NinoOrUtrChoiceFormProvider() {

  def ninoOrUtrChoiceForm: Form[NinoOrUtrChoice] = {
    Form(
      mapping(
        "ninoOrUtrRadio" -> optional(text)
          .verifying("cds.subscription.nino.utr.invalid", _.fold(false)(x => x.trim.nonEmpty))
      )(NinoOrUtrChoice.apply)(ninoUtrChoice => Some(ninoUtrChoice.ninoOrUtrRadio))
    )
  }
}
