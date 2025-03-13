/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Logging
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

  private val individualSoleTraderFlowIomConfig = createFlowConfig(
    List(
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredSubscriptionFlowPage,
      YourVatDetailsSubscriptionFlowPage,
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
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
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
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val partnershipFlowIomConfig = createFlowConfig(
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredSubscriptionFlowPage,
      YourVatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val thirdCountryIndividualFlowConfig =
    createFlowConfig(
      List(
        EoriConsentSubscriptionFlowPage,
        ContactDetailsSubscriptionFlowPageGetEori,
        ContactAddressSubscriptionFlowPageGetEori
      )
    )

  private val thirdCountrySoleTraderFlowConfig = createFlowConfig(
    List(
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val thirdCountryCorporateFlowConfig = createFlowConfig(
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val embassyFlowConfig = createFlowConfig(
    List(
      EoriConsentSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val charityPublicBodyNotForProfitFlowConfig = createFlowConfig(
    List(
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val charityPublicBodySubscriptionNoUtrFlowConfig = createFlowConfig(
    List(
      EoriConsentSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val charityPublicBodyNotForProfitFlowIomConfig = createFlowConfig(
    List(
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredSubscriptionFlowPage,
      YourVatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val charityPublicBodySubscriptionNoUtrFlowIomConfig = createFlowConfig(
    List(
      EoriConsentSubscriptionFlowPage,
      VatRegisteredSubscriptionFlowPage,
      YourVatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  private val companyLlpFlowIomConfig = createFlowConfig(
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage,
      VatRegisteredSubscriptionFlowPage,
      YourVatDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      ContactAddressSubscriptionFlowPageGetEori
    )
  )

  val flows: Map[SubscriptionFlow, SubscriptionFlowConfig] = Map(
    OrganisationSubscriptionFlow              -> corporateFlowConfig,
    PartnershipSubscriptionFlow               -> partnershipFlowConfig,
    EmbassySubscriptionFlow                   -> embassyFlowConfig,
    SoleTraderSubscriptionFlow                -> soleTraderFlowConfig,
    IndividualSubscriptionFlow                -> individualFlowConfig,
    ThirdCountryOrganisationSubscriptionFlow  -> thirdCountryCorporateFlowConfig,
    ThirdCountrySoleTraderSubscriptionFlow    -> thirdCountrySoleTraderFlowConfig,
    ThirdCountryIndividualSubscriptionFlow    -> thirdCountryIndividualFlowConfig,
    CharityPublicBodySubscriptionFlow         -> charityPublicBodyNotForProfitFlowConfig,
    CharityPublicBodySubscriptionNoUtrFlow    -> charityPublicBodySubscriptionNoUtrFlowConfig,
    CharityPublicBodySubscriptionFlowIom      -> charityPublicBodyNotForProfitFlowIomConfig,
    CharityPublicBodySubscriptionNoUtrFlowIom -> charityPublicBodySubscriptionNoUtrFlowIomConfig,
    PartnershipSubscriptionFlowIom            -> partnershipFlowIomConfig,
    IndividualSoleTraderFlowIom               -> individualSoleTraderFlowIomConfig,
    CompanyLlpFlowIom                         -> companyLlpFlowIomConfig
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

case object EmbassySubscriptionFlow extends SubscriptionFlow("Embassy", isIndividualFlow = false)

case object PartnershipSubscriptionFlow extends SubscriptionFlow("Partnership", isIndividualFlow = false)

case object PartnershipSubscriptionFlowIom extends SubscriptionFlow("PartnershipIom", isIndividualFlow = false)

case object IndividualSubscriptionFlow extends SubscriptionFlow("Individual", isIndividualFlow = true)

case object ThirdCountryOrganisationSubscriptionFlow extends SubscriptionFlow(ThirdCountryOrganisation.id, isIndividualFlow = false)

case object ThirdCountrySoleTraderSubscriptionFlow extends SubscriptionFlow(ThirdCountrySoleTrader.id, isIndividualFlow = true)

case object ThirdCountryIndividualSubscriptionFlow extends SubscriptionFlow(ThirdCountryIndividual.id, isIndividualFlow = true)

case object SoleTraderSubscriptionFlow extends SubscriptionFlow(SoleTrader.id, isIndividualFlow = true)

case object IndividualSoleTraderFlowIom extends SubscriptionFlow("IndividualSoleTraderIom", isIndividualFlow = true)

case object CharityPublicBodySubscriptionFlow extends SubscriptionFlow("CharityPublicBody", isIndividualFlow = false)

case object CharityPublicBodySubscriptionFlowIom extends SubscriptionFlow("CharityPublicBodyIom", isIndividualFlow = false)

case object CharityPublicBodySubscriptionNoUtrFlow extends SubscriptionFlow("CharityPublicBodyNoUtr", isIndividualFlow = false)

case object CharityPublicBodySubscriptionNoUtrFlowIom extends SubscriptionFlow("CharityPublicBodyNoUtrIom", isIndividualFlow = false)

case object CompanyLlpFlowIom extends SubscriptionFlow("CompanyLlpIom", isIndividualFlow = false)

object SubscriptionFlow extends Logging {

  def apply(flowName: String): SubscriptionFlow =
    SubscriptionFlows.flows.keys
      .find(_.name == flowName)
      .fold {

        val error = s"Incorrect Subscription flowname $flowName"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
      }(identity)

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

case object VatRegisteredSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatRegisteredController
      .createForm(service)
      .url

}

case object VatDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
      .createForm(service)
      .url

}

case object YourVatDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.YourVatDetailsController
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

case object WhatIsYourContactAddressPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.WhatIsYourContactAddressController.showForm(service).url

}

case class PreviousPage(someUrl: String) extends SubscriptionPage() {
  override def url(service: Service): String = someUrl
}
