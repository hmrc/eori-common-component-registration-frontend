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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth

import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RegistrationInfoRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

trait EnrolmentExtractor {

  private val EoriIdentifier: String = "EORINumber"

  private def identifierFor(
    enrolmentKey: String,
    identifierName: String,
    loggedInUser: LoggedInUserWithEnrolments
  ): Option[String] =
    loggedInUser.enrolments
      .getEnrolment(enrolmentKey)
      .flatMap(
        enrolment =>
          enrolment
            .getIdentifier(identifierName)
            .map(identifier => identifier.value)
      )

  def enrolledForService(loggedInUser: LoggedInUserWithEnrolments, service: Service): Option[Eori] =
    identifierFor(service.enrolmentKey, EoriIdentifier, loggedInUser).map(Eori)

  def activatedEnrolmentForService(loggedInUser: LoggedInUserWithEnrolments, service: Service): Option[Eori] =
    loggedInUser.enrolments
      .getEnrolment(service.enrolmentKey)
      .flatMap { enrolment =>
        if (enrolment.state.equalsIgnoreCase("Activated"))
          enrolment.getIdentifier(EoriIdentifier).map(identifier => Eori(identifier.value))
        else None
      }

  def enrolledCtUtr(loggedInUser: LoggedInUserWithEnrolments): Option[Utr] =
    identifierFor("IR-CT", RegistrationInfoRequest.UTR, loggedInUser).map(Utr)

  def enrolledSaUtr(loggedInUser: LoggedInUserWithEnrolments): Option[Utr] =
    identifierFor("IR-SA", RegistrationInfoRequest.UTR, loggedInUser).map(Utr)

  def enrolledNino(loggedInUser: LoggedInUserWithEnrolments): Option[Nino] =
    identifierFor("HMRC-NI", RegistrationInfoRequest.NINO, loggedInUser).map(Nino)

  def existingEoriForUserOrGroup(
    loggedInUser: LoggedInUserWithEnrolments,
    groupEnrolments: List[EnrolmentResponse]
  ): Option[ExistingEori] =
    existingEoriForUser(loggedInUser.enrolments.enrolments).orElse(existingEoriForGroup(groupEnrolments))

  def existingEoriForUser(loggedInUserEnrolments: Set[Enrolment]): Option[ExistingEori] = {
    val userEnrolmentWithEori = loggedInUserEnrolments.find(_.identifiers.exists(_.key == EoriIdentifier))
    userEnrolmentWithEori.map(
      enrolment => ExistingEori(enrolment.getIdentifier(EoriIdentifier).map(_.value), enrolment.key)
    )
  }

  def existingEoriForGroup(groupEnrolments: List[EnrolmentResponse]): Option[ExistingEori] =
    groupEnrolments.find(_.eori.exists(_.nonEmpty)).map(enrolment => ExistingEori(enrolment.eori, enrolment.service))

}
