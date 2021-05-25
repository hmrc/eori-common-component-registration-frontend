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

package unit.controllers.auth

import base.UnitSpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.EnrolmentExtractor
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

class EnrolmentExtractorSpec extends UnitSpec {

  private val eori = Eori("GB123456789012")
  private val utr  = Utr("1111111111K")
  private val nino = Nino("NINO")

  private def loggedInUser(enrolments: Set[Enrolment]) =
    LoggedInUserWithEnrolments(None, None, Enrolments(enrolments), None, None)

  val enrolmentExtractor = new EnrolmentExtractor {}

  "Enrolment Extractor" should {

    "return EORI" when {

      "user is enrolled for ATaR" in {

        val atarEnrolment = Enrolment("HMRC-ATAR-ORG").withIdentifier("EORINumber", eori.id)

        val result = enrolmentExtractor.enrolledForService(loggedInUser(Set(atarEnrolment)), atarService)

        result shouldBe Some(eori)
      }

      "user is enrolled for CDS" in {

        val cdsEnrolment = Enrolment("HMRC-CUS-ORG").withIdentifier("EORINumber", eori.id)

        val result = enrolmentExtractor.enrolledForService(loggedInUser(Set(cdsEnrolment)), Service.cds)

        result shouldBe Some(eori)
      }
    }

    "doesn't return EORI" when {

      "user is not enrolled for ATaR" in {

        enrolmentExtractor.enrolledForService(loggedInUser(Set.empty), atarService) shouldBe None
      }

      "user is not enrolled for CDS" in {

        enrolmentExtractor.enrolledForService(loggedInUser(Set.empty), Service.cds) shouldBe None
      }
    }

    "return EORI" when {

      "user has activated enrolment" in {

        val atarEnrolment = Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", eori.id)), "Activated")

        val result = enrolmentExtractor.activatedEnrolmentForService(loggedInUser(Set(atarEnrolment)), atarService)

        result shouldBe Some(eori)
      }
    }

    "doesn't return EORI" when {

      "user has not activated enrolment" in {

        val atarEnrolment = Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", eori.id)), "Pending")

        val result = enrolmentExtractor.activatedEnrolmentForService(loggedInUser(Set(atarEnrolment)), atarService)

        result shouldBe None
      }

      "user doesn't have enrolment" in {

        val result = enrolmentExtractor.activatedEnrolmentForService(loggedInUser(Set.empty), atarService)

        result shouldBe None
      }
    }

    "return Self Assessment UTR" when {

      "user has correct enrolment" in {

        val selfAssessmentEnrolment = Enrolment("IR-SA").withIdentifier("UTR", utr.id)

        val result = enrolmentExtractor.enrolledSaUtr(loggedInUser(Set(selfAssessmentEnrolment)))

        result shouldBe Some(utr)
      }
    }

    "doesn't return Self Assessment UTR" when {

      "user doesn't have correct enrolment" in {

        enrolmentExtractor.enrolledSaUtr(loggedInUser(Set.empty)) shouldBe None
      }
    }

    "return Corporation Tax UTR" when {

      "user has correct enrolment" in {

        val corporationTaxEnrolment = Enrolment("IR-CT").withIdentifier("UTR", utr.id)

        val result = enrolmentExtractor.enrolledCtUtr(loggedInUser(Set(corporationTaxEnrolment)))

        result shouldBe Some(utr)
      }
    }

    "doesn't return Corporation Tax UTR" when {

      "user doesn't have correct enrolment" in {

        enrolmentExtractor.enrolledCtUtr(loggedInUser(Set.empty)) shouldBe None
      }
    }

    "return Nino" when {

      "user has correct enrolment" in {

        val ninoEnrolment = Enrolment("HMRC-NI").withIdentifier("NINO", nino.id)

        val result = enrolmentExtractor.enrolledNino(loggedInUser(Set(ninoEnrolment)))

        result shouldBe Some(nino)
      }
    }

    "doesn't return Nino" when {

      "user doesn't have correct enrolment" in {

        enrolmentExtractor.enrolledNino(loggedInUser(Set.empty)) shouldBe None
      }
    }

    "return existing EORI for user and/or group" when {

      "user has enrolment with an EORI" in {

        val userEnrolments                           = Set(Enrolment("HMRC-TEST-ORG").withIdentifier("EORINumber", eori.id))
        val groupEnrolments: List[EnrolmentResponse] = List.empty

        val result = enrolmentExtractor.existingEoriForUserOrGroup(loggedInUser(userEnrolments), groupEnrolments)

        result shouldBe Some(ExistingEori(eori.id, "HMRC-TEST-ORG"))
      }

      "user's group has enrolment with an EORI" in {

        val userEnrolments = Set(Enrolment("HMRC-NI").withIdentifier("NINO", nino.id))
        val groupEnrolments: List[EnrolmentResponse] =
          List(EnrolmentResponse("HMRC-GROUP-ORG", "Active", List(KeyValue("EORINumber", eori.id))))

        val result = enrolmentExtractor.existingEoriForUserOrGroup(loggedInUser(userEnrolments), groupEnrolments)

        result shouldBe Some(ExistingEori(eori.id, "HMRC-GROUP-ORG"))
      }

      "user has no enrolment with EORI nor group enrolment with EORI" in {

        val userEnrolments = Set(Enrolment("HMRC-NI").withIdentifier("NINO", nino.id))
        val groupEnrolments =
          List(EnrolmentResponse("HMRC-GROUP-ORG", "Active", List(KeyValue("OtherIdentifierKey", "SomeValue"))))

        val result = enrolmentExtractor.existingEoriForUserOrGroup(loggedInUser(userEnrolments), groupEnrolments)

        result shouldBe None
      }

      "user enrolments or group enrolments" in {

        val userEnrolments: Set[Enrolment]           = Set.empty
        val groupEnrolments: List[EnrolmentResponse] = List.empty

        val result = enrolmentExtractor.existingEoriForUserOrGroup(loggedInUser(userEnrolments), groupEnrolments)

        result shouldBe None
      }
    }
  }
}
