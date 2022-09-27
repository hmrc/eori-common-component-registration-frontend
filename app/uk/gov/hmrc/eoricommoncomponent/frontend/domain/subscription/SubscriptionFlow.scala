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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription

import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

object SubscriptionFlows {

  private val individualFlowConfig =
    createFlowConfig(
      List(
        EoriConsentSubscriptionFlowPage,
        ContactDetailsSubscriptionFlowPageGetEori,
        ContactAddressSubscriptionFlowPageGetEori
      )
    )

  private val soleTraderFlowConfig = createFlowConfig(
    List(
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val corporateFlowConfig = createFlowConfig(
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      //VatGroupFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori,
      BusinessShortNameSubscriptionFlowPage
    )
  )

  private val partnershipFlowConfig = createFlowConfig(
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori,
      BusinessShortNameSubscriptionFlowPage
    )
  )

  private val thirdCountryIndividualFlowConfig =
    createFlowConfig(
      List(
        ContactDetailsSubscriptionFlowPageGetEori,
        ContactAddressSubscriptionFlowPageGetEori,
        EoriConsentSubscriptionFlowPage
      )
    )

  private val thirdCountrySoleTraderFlowConfig = createFlowConfig(
    List(
      SicCodeSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  private val thirdCountryCorporateFlowConfig = createFlowConfig(
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori,
      BusinessShortNameSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      //VatGroupFlowPage,
      VatDetailsSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  val flows: Map[SubscriptionFlow, SubscriptionFlowConfig] = Map(
    OrganisationSubscriptionFlow             -> corporateFlowConfig,
    PartnershipSubscriptionFlow              -> partnershipFlowConfig,
    SoleTraderSubscriptionFlow               -> soleTraderFlowConfig,
    IndividualSubscriptionFlow               -> individualFlowConfig,
    ThirdCountryOrganisationSubscriptionFlow -> thirdCountryCorporateFlowConfig,
    ThirdCountrySoleTraderSubscriptionFlow   -> thirdCountrySoleTraderFlowConfig,
    ThirdCountryIndividualSubscriptionFlow   -> thirdCountryIndividualFlowConfig
  )

  private def createFlowConfig(flowStepList: List[SubscriptionPage]): SubscriptionFlowConfig =
    SubscriptionFlowConfig(
      pageBeforeFirstFlowPage = RegistrationConfirmPage,
      flowStepList,
      pageAfterLastFlowPage = ReviewDetailsPageGetYourEORI
    )

  def apply(subscriptionFlow: SubscriptionFlow): SubscriptionFlowConfig = flows(subscriptionFlow)
}

case class SubscriptionFlowInfo(stepNumber: Int, totalSteps: Int, nextPage: SubscriptionPage)

sealed abstract class SubscriptionFlow(val name: String, val isIndividualFlow: Boolean)

case object OrganisationSubscriptionFlow extends SubscriptionFlow("Organisation", isIndividualFlow = false)

case object PartnershipSubscriptionFlow extends SubscriptionFlow("Partnership", isIndividualFlow = false)

case object IndividualSubscriptionFlow extends SubscriptionFlow("Individual", isIndividualFlow = true)

case object ThirdCountryOrganisationSubscriptionFlow
    extends SubscriptionFlow(ThirdCountryOrganisation.id, isIndividualFlow = false)

case object ThirdCountrySoleTraderSubscriptionFlow
    extends SubscriptionFlow(ThirdCountrySoleTrader.id, isIndividualFlow = true)

case object ThirdCountryIndividualSubscriptionFlow
    extends SubscriptionFlow(ThirdCountryIndividual.id, isIndividualFlow = true)

case object SoleTraderSubscriptionFlow extends SubscriptionFlow(SoleTrader.id, isIndividualFlow = true)

object SubscriptionFlow {

  def apply(flowName: String): SubscriptionFlow =
    SubscriptionFlows.flows.keys
      .find(_.name == flowName)
      .fold(throw new IllegalStateException(s"Incorrect Subscription flowname $flowName"))(identity)

}

sealed abstract class SubscriptionPage() {
  def url(service: Service): String
}

case object ContactDetailsSubscriptionFlowPageGetEori extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactDetailsController
      .createForm(service)
      .url

}

case object ContactAddressSubscriptionFlowPageGetEori extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactAddressController
      .createForm(service)
      .url

}

case object DateOfEstablishmentSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DateOfEstablishmentController
      .createForm(service)
      .url

}

case object VatRegisteredUkSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatRegisteredUkController
      .createForm(service)
      .url

}

case object BusinessShortNameSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.BusinessShortNameYesNoController
      .displayPage(service)
      .url

}

case object VatDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
      .createForm(service)
      .url

}

case object EoriConsentSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DisclosePersonalDetailsConsentController
      .createForm(service)
      .url

}

case object SicCodeSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.SicCodeController
      .createForm(service)
      .url

}

case object ReviewDetailsPageGetYourEORI extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
      .determineRoute(service)
      .url

}

case object ReviewDetailsPageSubscription extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
      .determineRoute(service)
      .url

}

case object RegistrationConfirmPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
      .form(service, isInReviewMode = false)
      .url

}

case object ConfirmIndividualTypePage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmIndividualTypeController
      .form(service)
      .url

}

case object BusinessDetailsRecoveryPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.BusinessDetailsRecoveryController
      .form(service)
      .url

}

case class PreviousPage(someUrl: String) extends SubscriptionPage() {
  override def url(service: Service): String = someUrl
}
