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

package util.builders

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

object AuthBuilder {

  lazy val notLoggedInException = new NoActiveSession("A user is not logged in") {}

  def withAuthorisedUser(
    userId: String,
    mockAuthConnector: AuthConnector,
    ctUtrId: Option[String] = None,
    saUtrId: Option[String] = None,
    payeNinoId: Option[String] = None,
    cdsEnrolmentId: Option[String] = None,
    otherEnrolments: Set[Enrolment] = Set.empty,
    userAffinityGroup: AffinityGroup = AffinityGroup.Organisation,
    userEmail: Option[String] = Some("testuser@hmrc.gov.uk"),
    userCredentials: Option[Credentials] = Some(Credentials("SomeCredId", "GovernmentGateway")),
    userCredentialRole: Option[CredentialRole] = Some(User),
    groupId: Option[String] = Some("groupId-abcd-1234")
  ) {

    when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(EmptyRetrieval))(any(), any()))
      .thenReturn(Future.successful(()))

    val idsRetrievalResult = new ~(new ~(Option(userAffinityGroup), Option(userId)), groupId)

    when(
      mockAuthConnector.authorise(
        any(),
        retrieval = ArgumentMatchers.eq(affinityGroup and internalId and groupIdentifier)
      )(any(), any())
    ).thenReturn(Future.successful(idsRetrievalResult))

    val userEnrolments = Set(
      ctUtrId.map(ctUtr => Enrolment("IR-CT").withIdentifier("UTR", ctUtr)),
      saUtrId.map(saUtr => Enrolment("IR-SA").withIdentifier("UTR", saUtr)),
      payeNinoId.map(nino => Enrolment("HMRC-NI").withIdentifier("NINO", nino)),
      cdsEnrolmentId.map(eoriNumber => Enrolment("HMRC-CUS-ORG").withIdentifier("EORINumber", eoriNumber))
    ).flatten ++ otherEnrolments

    when(
      mockAuthConnector.authorise(
        any(),
        retrieval = ArgumentMatchers
          .eq(email and credentialRole and affinityGroup and internalId and allEnrolments and groupIdentifier)
      )(any(), any())
    ).thenReturn(
      Future.successful(
        new ~(
          new ~(
            new ~(new ~(new ~(userEmail, userCredentialRole), Option(userAffinityGroup)), Option(userId)),
            Enrolments(userEnrolments)
          ),
          groupId
        )
      )
    )

    when(
      mockAuthConnector.authorise(
        any(),
        retrieval = ArgumentMatchers.eq(
          email and credentialRole and affinityGroup and internalId and allEnrolments and credentials and groupIdentifier
        )
      )(any(), any())
    ).thenReturn(
      Future.successful(
        new ~(
          new ~(
            new ~(
              new ~(new ~(new ~(userEmail, userCredentialRole), Option(userAffinityGroup)), Option(userId)),
              Enrolments(userEnrolments)
            ),
            userCredentials
          ),
          groupId
        )
      )
    )

    when(mockAuthConnector.authorise(any(), retrieval = ArgumentMatchers.eq(allEnrolments))(any(), any()))
      .thenReturn(Future.successful(Enrolments(userEnrolments)))

    when(
      mockAuthConnector
        .authorise(any(), retrieval = ArgumentMatchers.eq(email and credentialRole and affinityGroup))(any(), any())
    ).thenReturn(Future.successful(new ~(new ~(userEmail, userCredentialRole), Some(userAffinityGroup))))

    when(mockAuthConnector.authorise(any(), retrieval = ArgumentMatchers.eq(credentials))(any(), any()))
      .thenReturn(Future.successful(userCredentials))

    when(mockAuthConnector.authorise(any(), retrieval = ArgumentMatchers.eq(affinityGroup))(any(), any()))
      .thenReturn(Future.successful(Some(userAffinityGroup)))
  }

  def withNotLoggedInUser(mockAuthConnector: AuthConnector): Unit = {
    val noBearerTokenMatcher: ArgumentMatcher[HeaderCarrier] = new ArgumentMatcher[HeaderCarrier] {
      def matches(item: HeaderCarrier): Boolean = item match {
        case hc: HeaderCarrier if hc.authorization.isEmpty => true
        case _                                             => false
      }
    }

    when(mockAuthConnector.authorise(any(), any[Retrieval[_]])(ArgumentMatchers.argThat(noBearerTokenMatcher), any()))
      .thenReturn(Future.failed(notLoggedInException))
  }

}
