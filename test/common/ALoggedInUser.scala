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

package common

import org.scalacheck.Gen
import uk.gov.hmrc.auth.core
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Nino, Utr}

case class User(
  internalId: String,
  ctUtr: Option[Utr] = None,
  saUtr: Option[Utr] = None,
  nino: Option[Nino] = None,
  email: Option[String] = None,
  affinityGroup: AffinityGroup = AffinityGroup.Organisation,
  bearerToken: String = Users.bearerToken,
  credentials: Option[Credentials] = Some(Credentials("SomeCredId", "GovernmentGateway")),
  credRole: CredentialRole = core.User,
  groupId: Option[String] = Some("groupId-abcd-1234")
)

object Users {

  val bearerToken =
    "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI"

  def ASampleUser = User(Gen.uuid.sample.get.toString, email = Some("sample@user.com"))

  def ASampleGroupIdUserWithEnrolment =
    User(Gen.uuid.sample.get.toString, email = Some("sample@user.com"), groupId = Some("gg-id-rcm-cases"))

  def ASampleUserWithoutEmail = User(Gen.uuid.sample.get.toString)

  def ACtOrgUser = User(Gen.uuid.sample.get.toString, ctUtr = Some(Utr("1234567")), email = Some("ctorg@user.com"))

  def IrSaUtrUser = User(Gen.uuid.sample.get.toString, saUtr = Some(Utr("1234567")), email = Some("ctorg@user.com"))

  def ASampleUserWithRandomInternalId(internalId: String) =
    User(internalId = internalId, email = Some("sample@user.com"), groupId = Some("gg-id-rcm-cases"))

}
