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

package unit.controllers

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.FeatureFlags
import util.ControllerSpec

class FeatureFlagsSpec extends ControllerSpec {

  private val configuration: Config = ConfigFactory.parseString("""
      |features.sub02UseServiceName=true
      |features.arsNewJourney=false
      |features.useNewVATJourney=true
      |features.edgeCaseJourney=true
      |features.useNewCharityEdgeCaseJourney=true
      """.stripMargin)

  private val featureFlags = new FeatureFlags(Configuration(configuration))

  "FeatureFlags" should {
    "retrieve values for feature flags from application conf" in {

      featureFlags.sub02UseServiceName shouldBe true
      featureFlags.arsNewJourney shouldBe false
      featureFlags.useNewVATJourney shouldBe true
      featureFlags.useNewCharityEdgeCaseJourney shouldBe true
    }
  }
}
