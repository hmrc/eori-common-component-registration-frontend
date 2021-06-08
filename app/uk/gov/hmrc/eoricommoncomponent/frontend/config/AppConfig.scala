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

package uk.gov.hmrc.eoricommoncomponent.frontend.config

import javax.inject.{Inject, Named, Singleton}
import play.api.Configuration
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.Duration
import scala.util.matching.Regex

@Singleton
class AppConfig @Inject() (
  config: Configuration,
  servicesConfig: ServicesConfig,
  @Named("appName") val appName: String
) {

  val messageFiles: Seq[String] = config.get[Seq[String]]("messages.file.names")

  val ttl: Duration = Duration.create(config.get[String]("cds-frontend-cache.ttl"))

  private val contactBaseUrl = servicesConfig.baseUrl("contact-frontend")

  private val serviceIdentifierRegister =
    config.get[String]("microservice.services.contact-frontend.serviceIdentifierRegister")

  private val feedbackLink = config.get[String]("external-url.feedback-survey")

  def feedbackUrl(service: Service) = s"$feedbackLink-${service.code}"

  private def languageKey(implicit messages: Messages) = messages.lang.language match {
    case "cy" => "cy"
    case _    => "en"
  }

  def findLostUtr()(implicit messages: Messages): String =
    config.get[String](s"external-url.find-lost-utr-$languageKey")

  val traderSupportService: String                       = config.get[String]("external-url.trader-support-service")
  val getCompanyInformation: String                      = config.get[String]("external-url.get-company-information")
  val contactEORITeam: String                            = config.get[String]("external-url.contact-eori-team")
  val checkEORINumber: String                            = config.get[String]("external-url.check-eori-number")
  def callCharges()(implicit messages: Messages): String = config.get[String](s"external-url.call-charges-$languageKey")

  val blockedRoutesRegex: Seq[Regex] =
    config.getOptional[String]("routes-to-block") match {
      case Some(routes) if routes.nonEmpty => routes.split(',').map(_.r).toSeq
      case _                               => Seq.empty
    }

  //get help link feedback for Register journey
  def reportAProblemPartialUrlRegister(service: Service): String =
    s"$contactBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifierRegister-${service.code}"

  def reportAProblemNonJSUrlRegister(service: Service): String =
    s"$contactBaseUrl/contact/problem_reports_nonjs?service=$serviceIdentifierRegister-${service.code}"

  private val betafeedbackBaseUrl = s"$contactBaseUrl/contact/beta-feedback"

  def betaFeedBackRegister(service: Service) =
    s"$betafeedbackBaseUrl?service=$serviceIdentifierRegister-${service.code}"

  //email verification service
  val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")

  val emailVerificationServiceContext: String =
    config.get[String]("microservice.services.email-verification.context")

  val emailVerificationTemplateId: String =
    config.get[String]("microservice.services.email-verification.templateId")

  val emailVerificationLinkExpiryDuration: String =
    config.get[String]("microservice.services.email-verification.linkExpiryDuration")

  //handle subscription service
  val handleSubscriptionBaseUrl: String = servicesConfig.baseUrl("handle-subscription")

  val handleSubscriptionServiceContext: String =
    config.get[String]("microservice.services.handle-subscription.context")

  //pdf generation
  val pdfGeneratorBaseUrl: String = servicesConfig.baseUrl("pdf-generator")

  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")

  val enrolmentStoreProxyServiceContext: String =
    config.get[String]("microservice.services.enrolment-store-proxy.context")

  private val eoriCommonComponentFrontendBaseUrl: String = servicesConfig.baseUrl("eori-common-component-frontend")

  private val eoriCommonComponentFrontendContext: String =
    config.get[String]("microservice.services.eori-common-component-frontend.context")

  def eoriCommonComponentFrontend(serviceName: String) =
    eoriCommonComponentFrontendBaseUrl + eoriCommonComponentFrontendContext + serviceName + "/subscribe"

  def getServiceUrl(proxyServiceName: String): String = {
    val baseUrl = servicesConfig.baseUrl("eori-common-component-hods-proxy")
    val serviceContext =
      config.get[String](s"microservice.services.eori-common-component-hods-proxy.$proxyServiceName.context")
    s"$baseUrl/$serviceContext"
  }

}
