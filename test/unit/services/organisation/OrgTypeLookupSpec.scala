/*
 * Copyright 2026 HM Revenue & Customs
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

package unit.services.organisation

import base.UnitSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, CorporateBody, Partnership}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import util.builders.{RegistrationDetailsBuilder, SubscriptionFormBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class OrgTypeLookupSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar {

  private val mockCache = mock[SessionCache]
  private val mockReqSessionData = mock[RequestSessionData]
  private val req = mock[Request[AnyContent]]

  override def beforeEach(): Unit = {
    reset(mockCache)
    reset(mockReqSessionData)
  }

  val lookup = new OrgTypeLookup(mockReqSessionData, mockCache)(global)

  "Org type lookup" should {

    "give org type from request session" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
        .thenReturn(Some(CdsOrganisationType.Company))

      val orgType = await(lookup.etmpOrgType(req))

      orgType shouldBe CorporateBody
    }

    "give org type from cache" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.partnershipRegistrationDetails))

      val orgType = await(lookup.etmpOrgType(req))

      orgType shouldBe Partnership
    }

    "throw an exception when neither the request session or cache contains the org type" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.emptyETMPOrgTypeRegistrationDetails))

      when(mockCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionFormBuilder.detailsHolderWithAllFields))

      val thrown = intercept[IllegalStateException] {
        await(lookup.etmpOrgType(req))
      }

      thrown.getMessage shouldBe "Unable to retrieve Org Type from the cache"
    }

    "get details from subscription instead of registration and save them" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.emptyETMPOrgTypeRegistrationDetails))

      when(mockCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionFormBuilder.detailsWithOrgTypeField))

      when(mockCache.saveRegistrationDetails(any())(any[Request[_]]))
        .thenReturn(Future.successful(true))

      val orgType = await(lookup.etmpOrgType(req))

      orgType shouldBe CorporateBody
    }

    "throw an exception when details are not saved from subscription and are not retrieved from registration" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.emptyETMPOrgTypeRegistrationDetails))

      when(mockCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionFormBuilder.detailsWithOrgTypeField))

      when(mockCache.saveRegistrationDetails(any())(any[Request[_]]))
        .thenReturn(Future.successful(false))

      val thrown = intercept[RuntimeException] {
        await(lookup.etmpOrgType(req))
      }

      thrown.getMessage shouldBe "Unable to save updated Registration Details with ETMP Org Type"
    }

    "throw an exception when different type of registration details is retrieved" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails))

      val thrown = intercept[IllegalStateException] {
        await(lookup.etmpOrgType(req))
      }

      thrown.getMessage shouldBe "No Registration details in cache."
    }
  }

  "etmpOrgTypeOpt" should {

    "give org type from request session" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
        .thenReturn(Some(CdsOrganisationType.Company))

      val orgType = await(lookup.etmpOrgTypeOpt(req))

      orgType shouldBe Some(CorporateBody)
    }

    "give org type from cache" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.partnershipRegistrationDetails))

      val orgType = await(lookup.etmpOrgTypeOpt(req))

      orgType shouldBe Some(Partnership)
    }

    "throw an exception when different type of registration details is retrieved" in {
      when(mockReqSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      when(mockCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(RegistrationDetailsBuilder.individualRegistrationDetails))

      val thrown = intercept[DataUnavailableException] {
        await(lookup.etmpOrgTypeOpt(req))
      }

      thrown.getMessage shouldBe "No Registration details in cache."
    }
  }
}
