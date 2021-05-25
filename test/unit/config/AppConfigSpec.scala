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

package unit.config

import java.util.concurrent.TimeUnit

import org.mockito.Mockito
import org.mockito.Mockito.{spy, when}
import org.scalatest.BeforeAndAfterEach
import play.api.Configuration
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.ControllerSpec

import scala.concurrent.duration.Duration

class AppConfigSpec extends ControllerSpec with BeforeAndAfterEach {

  private val mockConfig: Configuration = spy(config)
  private val mockServiceConfig         = mock[ServicesConfig]

  override def beforeEach() {
    super.beforeEach()
    Mockito.reset(mockConfig, mockServiceConfig)
  }

  "AppConfig" should {

    "have blockedRoutesRegex defined" in {
      appConfig.blockedRoutesRegex.map(_.pattern.pattern()).mkString(":") shouldBe ""
    }

    "have ttl defined" in {
      appConfig.ttl shouldBe Duration(40, TimeUnit.MINUTES)
    }

    "have allowlistReferrers defined" in {
      appConfig.allowlistReferrers shouldBe Array.empty
    }

    "have emailVerificationBaseUrl defined" in {
      appConfig.emailVerificationBaseUrl shouldBe "http://localhost:6754"
    }

    "have emailVerificationServiceContext defined" in {
      appConfig.emailVerificationServiceContext shouldBe "email-verification"
    }

    "have verifyEmailAddress defined" in {
      appConfig.emailVerificationTemplateId shouldBe "verifyEmailAddress"
    }

    "have emailVerificationLinkExpiryDuration defined" in {
      appConfig.emailVerificationLinkExpiryDuration shouldBe "P1D"
    }

    "have handleSubscriptionBaseUrl defined" in {
      appConfig.handleSubscriptionBaseUrl shouldBe "http://localhost:6752"
    }

    "have handleSubscriptionServiceContext defined" in {
      appConfig.handleSubscriptionServiceContext shouldBe "handle-subscription"
    }

    "have pdfGeneratorBaseUrl defined" in {
      appConfig.pdfGeneratorBaseUrl shouldBe "http://localhost:9852"
    }

    "have taxEnrolmentsBaseUrl defined" in {
      appConfig.taxEnrolmentsBaseUrl shouldBe "http://localhost:6754"
    }

    "have taxEnrolmentsServiceContext defined" in {
      appConfig.taxEnrolmentsServiceContext shouldBe "tax-enrolments"
    }

    "have enrolmentStoreProxyBaseUrl defined" in {
      appConfig.enrolmentStoreProxyBaseUrl shouldBe "http://localhost:6754"
    }

    "have enrolmentStoreProxyServiceContext defined" in {
      appConfig.enrolmentStoreProxyServiceContext shouldBe "enrolment-store-proxy"
    }

    "have feedbackLink defined for register" in {
      appConfig.feedbackUrl(
        atarService,
        Journey.Register
      ) shouldBe "http://localhost:9514/feedback/eori-common-component-register-atar"
    }

    "have feedbackLink defined for subscribe" in {
      appConfig.feedbackUrl(
        atarService,
        Journey.Subscribe
      ) shouldBe "http://localhost:9514/feedback/eori-common-component-subscribe-atar"
    }

    "have reportAProblemPartialUrl defined for register" in {
      appConfig.reportAProblemPartialUrlRegister(
        atarService
      ) shouldBe "http://localhost:9250/contact/problem_reports_ajax?service=eori-common-component-register-atar"
    }

    "have reportAProblemNonJSUrl defined for register" in {
      appConfig.reportAProblemNonJSUrlRegister(
        atarService
      ) shouldBe "http://localhost:9250/contact/problem_reports_nonjs?service=eori-common-component-register-atar"
    }

    "have reportAProblemPartialUrl defined for subscribe" in {
      appConfig.reportAProblemPartialUrlSubscribe(
        atarService
      ) shouldBe "http://localhost:9250/contact/problem_reports_ajax?service=eori-common-component-subscribe-atar"
    }

    "have reportAProblemNonJSUrl defined for subscribe" in {
      appConfig.reportAProblemNonJSUrlSubscribe(
        atarService
      ) shouldBe "http://localhost:9250/contact/problem_reports_nonjs?service=eori-common-component-subscribe-atar"
    }
  }

  "using getServiceUrl" should {
    "return service url for register-with-id" in {
      appConfig.getServiceUrl("register-with-id") shouldBe "http://localhost:6753/register-with-id"
    }
    "return service url for register-without-id" in {
      appConfig.getServiceUrl("register-without-id") shouldBe "http://localhost:6753/register-without-id"
    }
    "return service url for register-with-eori-and-id" in {
      appConfig.getServiceUrl("register-with-eori-and-id") shouldBe "http://localhost:6753/register-with-eori-and-id"
    }
    "return service url for subscription-status" in {
      appConfig.getServiceUrl("subscription-status") shouldBe "http://localhost:6753/subscription-status"
    }
    "return service url for subscription-display" in {
      appConfig.getServiceUrl("subscription-display") shouldBe "http://localhost:6753/subscription-display"
    }
    "return service url for registration-display" in {
      appConfig.getServiceUrl("registration-display") shouldBe "http://localhost:6753/registration-display"
    }
    "return service url for subscribe" in {
      appConfig.getServiceUrl("subscribe") shouldBe "http://localhost:6753/subscribe"
    }
    "return service url for vat-known-facts-control-list" in {
      appConfig.getServiceUrl(
        "vat-known-facts-control-list"
      ) shouldBe "http://localhost:6753/vat-known-facts-control-list"
    }

    "return address lookup url" in {

      appConfig.addressLookup shouldBe "http://localhost:6754/v2/uk/addresses"
    }

    "return url for 'get EORI" when {

      "register is blocked" in {
        when(mockConfig.getOptional[String]("routes-to-block")).thenReturn(Some("register"))
        when(mockConfig.get[String]("external-url.get-cds-eori")).thenReturn("/config-url")

        val testAppConfig = new AppConfig(mockConfig, mockServiceConfig, "appName")

        testAppConfig.externalGetEORILink(atarService) shouldBe "/config-url"
      }

      "register is un-blocked" in {
        when(mockConfig.getOptional[String]("routes-to-block")).thenReturn(None)

        val testAppConfig = new AppConfig(mockConfig, mockServiceConfig, "appName")

        testAppConfig.externalGetEORILink(atarService) shouldBe ApplicationController.startRegister(atarService).url
      }
    }
  }
}
