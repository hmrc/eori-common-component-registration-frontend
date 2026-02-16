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

import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{optional, text}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.oneOf

import javax.inject.Singleton

@Singleton
class OrganisationTypeDetailsFormProvider extends Logging {

  def form(): Form[CdsOrganisationType] = {
    Form(
      "organisation-type" -> optional(text)
        .verifying(
          "cds.matching.organisation-type.page-error.organisation-type-field.error.required",
          x => x.fold(false)(oneOf(CdsOrganisationType.validOrganisationTypes.keySet).apply(_))
        )
        .transform[CdsOrganisationType](
          o =>
            CdsOrganisationType(
              CdsOrganisationType
                .forId(o.getOrElse {
                  val error = "Could not create CdsOrganisationType for empty ID."
                  // $COVERAGE-OFF$Loggers
                  logger.warn(error)
                  // $COVERAGE-ON
                  throw new IllegalArgumentException(error)
                })
                .id
            ),
          x => Some(x.id)
        )
    )
  }
}
