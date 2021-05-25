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

package unit.domain.subscription

import base.UnitSpec
import org.joda.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  NameDobMatchModel,
  NameIdOrganisationMatchModel,
  NameMatchModel,
  NameOrganisationMatchModel
}

class SubscriptionDetailsSpec extends UnitSpec {

  "Subscription details" should {

    "use name from nameIdOrganisationDetails when present" in {
      val sd = SubscriptionDetails(
        nameIdOrganisationDetails = Some(NameIdOrganisationMatchModel("John Doe", "")),
        nameOrganisationDetails = Some(NameOrganisationMatchModel("other name"))
      )
      sd.name shouldBe "John Doe"
    }

    "use name from nameOrganisationDetails when nameIdOrganisationDetails not present" in {
      val sd = SubscriptionDetails(
        nameIdOrganisationDetails = None,
        nameOrganisationDetails = Some(NameOrganisationMatchModel("John Doe"))
      )
      sd.name shouldBe "John Doe"
    }

    "use name from nameDobDetails when nameOrganisationDetails not present" in {
      val sd = SubscriptionDetails(
        nameOrganisationDetails = None,
        nameDobDetails = Some(NameDobMatchModel("John", Some("Middle"), "Doe", LocalDate.now))
      )
      sd.name shouldBe "John Middle Doe"
    }

    "use name from nameDetails when nameDobDetails not present" in {
      val sd = SubscriptionDetails(nameDobDetails = None, nameDetails = Some(NameMatchModel("John Doe")))
      sd.name shouldBe "John Doe"
    }

    "throw exception when no name is present" in {
      val sd = SubscriptionDetails()
      the[IllegalArgumentException] thrownBy {
        sd.name
      } should have message "Name is missing"
    }
  }

}
