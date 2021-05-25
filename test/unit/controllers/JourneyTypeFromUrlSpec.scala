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

package unit.controllers

import base.UnitSpec
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.JourneyTypeFromUrl
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey

class JourneyTypeFromUrlSpec extends UnitSpec with MockitoSugar {

  private implicit val mockRequest: Request[AnyContent] = mock[Request[AnyContent]]
  private val journeyTypeTrait                          = new JourneyTypeFromUrl {}

  "Journey type" can {

    "be extracted from URL as a Journey Type" in {

      when(mockRequest.path).thenReturn("/path1/path2/path3/customs-enrolment-services/register/path4")
      journeyTypeTrait.journeyFromUrl shouldBe Journey.Register

      when(mockRequest.path).thenReturn("/customs-enrolment-services/subscribe/path1")
      journeyTypeTrait.journeyFromUrl shouldBe Journey.Subscribe

      when(mockRequest.path).thenReturn("/customs-enrolment-services/register/")
      journeyTypeTrait.journeyFromUrl shouldBe Journey.Register
    }
  }
}
