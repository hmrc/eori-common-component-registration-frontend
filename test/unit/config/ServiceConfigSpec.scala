/*
 * Copyright 2022 HM Revenue & Customs
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

package unit.config

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import play.api.Configuration
import uk.gov.hmrc.eoricommoncomponent.frontend.config.ServiceConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import util.ControllerSpec

class ServiceConfigSpec extends ControllerSpec {

  private val configurationFull: Config =
    ConfigFactory.parseString("""
      |services-config.list="atar,gvms"
      |services-config.atar.enrolment=HMRC-ATAR-ORG
      |services-config.atar.shortName=ATaR
      |services-config.atar.callBack=/advance-tariff-application
      |services-config.atar.accessibility=/advance-tariff-application/accessibility
      |services-config.atar.friendlyName=Advance_Tariff_Rulings
      |services-config.atar.friendlyNameWelsh=Dyfarniadau_Tariffau_Uwch_(ATaR)
      |services-config.gvms.enrolment=HMRC-GVMS-ORG
      |services-config.gvms.shortName=GVMS
      |services-config.gvms.callBack=/goods-movement
      |services-config.gvms.accessibility=/goods-movement/accessibility
      |services-config.gvms.friendlyName=Goods_Movement
      |services-config.gvms.friendlyNameWelsh=Symud_Cerbydau_Nwyddau_(GVMS)
      """.stripMargin)

  private val configurationMissingWelsh: Config =
    ConfigFactory.parseString("""
                                |services-config.list=atar
                                |services-config.atar.enrolment=HMRC-ATAR-ORG
                                |services-config.atar.shortName=ATaR
                                |services-config.atar.callBack=/advance-tariff-application
                                |services-config.atar.accessibility=/advance-tariff-application
                                |services-config.atar.friendlyName=Advance_Tariff_Rulings
      """.stripMargin)

  private val configurationEmptyWelsh: Config =
    ConfigFactory.parseString("""
                                |services-config.list=atar
                                |services-config.atar.enrolment=HMRC-ATAR-ORG
                                |services-config.atar.shortName=ATaR
                                |services-config.atar.callBack=/advance-tariff-application
                                |services-config.atar.accessibility=/advance-tariff-application
                                |services-config.atar.friendlyName=Binding_Tariffs
                                |services-config.atar.friendlyNameWelsh=""
      """.stripMargin)

  private val configurationMissingServiceParameter: Config =
    ConfigFactory.parseString("""
                                |services-config.list=atar
                                |services-config.atar.enrolment=HMRC-ATAR-ORG
                                |services-config.atar.shortName=ATaR
      """.stripMargin)

  private val configurationMissingServiceDefinition: Config =
    ConfigFactory.parseString("""
                                |services-config.list=abcd
      """.stripMargin)

  "ServiceConfig" should {
    "retrieve values for full config" in {

      val config = new ServiceConfig(Configuration(configurationFull))

      config.supportedServicesMap.size shouldBe 2
      config.supportedServicesMap("atar") shouldBe Service(
        "atar",
        "HMRC-ATAR-ORG",
        "ATaR",
        "/advance-tariff-application",
        "/advance-tariff-application/accessibility",
        "Advance Tariff Rulings",
        "Dyfarniadau Tariffau Uwch (ATaR)",
        None
      )
    }

    "default welsh name when missing" in {

      val config = new ServiceConfig(Configuration(configurationMissingWelsh))
      config.supportedServicesMap("atar").friendlyNameWelsh shouldBe "Advance Tariff Rulings"
    }

    "default welsh name when empty" in {

      val config = new ServiceConfig(Configuration(configurationEmptyWelsh))
      config.supportedServicesMap("atar").friendlyNameWelsh shouldBe "Binding Tariffs"
    }

    "error when config missing" in {

      val configException = intercept[ConfigException] {
        new ServiceConfig(Configuration(configurationMissingServiceParameter))
      }

      configException.getMessage should include(
        "No configuration setting found for key 'services-config.atar.friendlyName'"
      )

    }

    "error when service definition missing" in {

      val configException = intercept[ConfigException] {
        new ServiceConfig(Configuration(configurationMissingServiceDefinition))
      }

      configException.getMessage should include("No configuration setting found for key 'services-config.abcd'")

    }
  }
}
