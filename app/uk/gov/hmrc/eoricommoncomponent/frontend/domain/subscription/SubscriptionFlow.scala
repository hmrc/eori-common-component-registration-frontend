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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription

import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}

object SubscriptionFlows {

  private val individualFlowConfig =
    createFlowConfig(Journey.Register, List(ContactDetailsSubscriptionFlowPageGetEori, EoriConsentSubscriptionFlowPage))

  private val soleTraderFlowConfig = createFlowConfig(
    Journey.Register,
    List(
      ContactDetailsSubscriptionFlowPageGetEori,
      SicCodeSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      VatRegisteredEuSubscriptionFlowPage,
      VatEUIdsSubscriptionFlowPage,
      VatEUConfirmSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  private val corporateFlowConfig = createFlowConfig(
    Journey.Register,
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      BusinessShortNameSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      //VatGroupFlowPage,
      VatDetailsSubscriptionFlowPage,
      VatRegisteredEuSubscriptionFlowPage,
      VatEUIdsSubscriptionFlowPage,
      VatEUConfirmSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  private val partnershipFlowConfig = createFlowConfig(
    Journey.Register,
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      BusinessShortNameSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      VatRegisteredEuSubscriptionFlowPage,
      VatEUIdsSubscriptionFlowPage,
      VatEUConfirmSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  private val thirdCountryIndividualFlowConfig =
    createFlowConfig(Journey.Register, List(ContactDetailsSubscriptionFlowPageGetEori, EoriConsentSubscriptionFlowPage))

  private val thirdCountrySoleTraderFlowConfig = createFlowConfig(
    Journey.Register,
    List(
      ContactDetailsSubscriptionFlowPageGetEori,
      SicCodeSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      VatDetailsSubscriptionFlowPage,
      VatRegisteredEuSubscriptionFlowPage,
      VatEUIdsSubscriptionFlowPage,
      VatEUConfirmSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  private val thirdCountryCorporateFlowConfig = createFlowConfig(
    Journey.Register,
    List(
      DateOfEstablishmentSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageGetEori,
      BusinessShortNameSubscriptionFlowPage,
      SicCodeSubscriptionFlowPage,
      VatRegisteredUkSubscriptionFlowPage,
      //VatGroupFlowPage,
      VatDetailsSubscriptionFlowPage,
      VatRegisteredEuSubscriptionFlowPage,
      VatEUIdsSubscriptionFlowPage,
      VatEUConfirmSubscriptionFlowPage,
      EoriConsentSubscriptionFlowPage
    )
  )

  private val soleTraderRegExistingEoriFlowConfig = createFlowConfig(
    Journey.Subscribe,
    List(
      NameDobDetailsSubscriptionFlowPage,
      HowCanWeIdentifyYouSubscriptionFlowPage,
      AddressDetailsSubscriptionFlowPage
    )
  )

  private val corporateRegExistingEoriFlowConfig = createFlowConfig(
    Journey.Subscribe,
    List(
      NameUtrDetailsSubscriptionFlowPage,
      DateOfEstablishmentSubscriptionFlowPageMigrate,
      AddressDetailsSubscriptionFlowPage
    )
  )

  private val rowIndividualFlowConfig = createFlowConfig(
    Journey.Subscribe,
    List(
      NameDobDetailsSubscriptionFlowPage,
      UtrSubscriptionFlowPage,
      NinoSubscriptionFlowPage,
      AddressDetailsSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageMigrate
    )
  )

  private val rowOrganisationFlowConfig = createFlowConfig(
    Journey.Subscribe,
    List(
      NameDetailsSubscriptionFlowPage,
      UtrSubscriptionFlowPage,
      AddressDetailsSubscriptionFlowPage,
      RowDateOfEstablishmentSubscriptionFlowPage,
      ContactDetailsSubscriptionFlowPageMigrate
    )
  )

  val flows: Map[SubscriptionFlow, SubscriptionFlowConfig] = Map(
    OrganisationSubscriptionFlow             -> corporateFlowConfig,
    PartnershipSubscriptionFlow              -> partnershipFlowConfig,
    SoleTraderSubscriptionFlow               -> soleTraderFlowConfig,
    IndividualSubscriptionFlow               -> individualFlowConfig,
    ThirdCountryOrganisationSubscriptionFlow -> thirdCountryCorporateFlowConfig,
    ThirdCountrySoleTraderSubscriptionFlow   -> thirdCountrySoleTraderFlowConfig,
    ThirdCountryIndividualSubscriptionFlow   -> thirdCountryIndividualFlowConfig,
    OrganisationFlow                         -> corporateRegExistingEoriFlowConfig,
    SoleTraderFlow                           -> soleTraderRegExistingEoriFlowConfig,
    IndividualFlow                           -> soleTraderRegExistingEoriFlowConfig,
    RowOrganisationFlow                      -> rowOrganisationFlowConfig,
    RowIndividualFlow                        -> rowIndividualFlowConfig
  )

  private def createFlowConfig(journey: Journey.Value, flowStepList: List[SubscriptionPage]): SubscriptionFlowConfig =
    journey match {
      case Journey.Subscribe =>
        SubscriptionFlowConfig(
          pageBeforeFirstFlowPage = RegistrationConfirmPage,
          flowStepList,
          pageAfterLastFlowPage = ReviewDetailsPageSubscription
        )
      case _ =>
        SubscriptionFlowConfig(
          pageBeforeFirstFlowPage = RegistrationConfirmPage,
          flowStepList,
          pageAfterLastFlowPage = ReviewDetailsPageGetYourEORI
        )
    }

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

case object OrganisationFlow extends SubscriptionFlow("migration-eori-Organisation", isIndividualFlow = false)

case object IndividualFlow extends SubscriptionFlow("migration-eori-Individual", isIndividualFlow = true)

case object SoleTraderFlow extends SubscriptionFlow("migration-eori-sole-trader", isIndividualFlow = true)

case object RowOrganisationFlow
    extends SubscriptionFlow("migration-eori-row-utrNino-enabled-Organisation", isIndividualFlow = false)

case object RowIndividualFlow
    extends SubscriptionFlow("migration-eori-row-utrNino-enabled-Individual", isIndividualFlow = true)

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
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.ContactDetailsController
      .createForm(service)
      .url

}

case object ContactDetailsSubscriptionFlowPageMigrate extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.ContactDetailsController
      .createForm(service)
      .url

}

case object UtrSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.HaveUtrSubscriptionController
      .createForm(service, Journey.Subscribe)
      .url

}

case object NinoSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.HaveNinoSubscriptionController
      .createForm(service, Journey.Subscribe)
      .url

}

case object AddressDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressController.createForm(
      service,
      Journey.Subscribe
    ).url

}

case object NameUtrDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameIDOrgController
      .createForm(service, Journey.Subscribe)
      .url

}

case object NameDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameOrgController
      .createForm(service, Journey.Subscribe)
      .url

}

case object NameDobDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameDobSoleTraderController
      .createForm(service, Journey.Subscribe)
      .url

}

case object HowCanWeIdentifyYouSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.HowCanWeIdentifyYouController
      .createForm(service, Journey.Subscribe)
      .url

}

case object RowDateOfEstablishmentSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.DateOfEstablishmentController
      .createForm(service, Journey.Subscribe)
      .url

}

case object DateOfEstablishmentSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.DateOfEstablishmentController
      .createForm(service, Journey.Register)
      .url

}

case object DateOfEstablishmentSubscriptionFlowPageMigrate extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.DateOfEstablishmentController
      .createForm(service, Journey.Subscribe)
      .url

}

case object VatRegisteredUkSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatRegisteredUkController
      .createForm(service, Journey.Register)
      .url

}

case object BusinessShortNameSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.BusinessShortNameYesNoController
      .displayPage(service)
      .url

}

case object VatDetailsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatDetailsController
      .createForm(service, Journey.Register)
      .url

}

case object VatRegisteredEuSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatRegisteredEuController
      .createForm(service, Journey.Register)
      .url

}

case object VatEUIdsSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatDetailsEuController
      .createForm(service, Journey.Register)
      .url

}

case object VatEUConfirmSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatDetailsEuConfirmController
      .createForm(service, Journey.Register)
      .url

}

case object EoriConsentSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.DisclosePersonalDetailsConsentController
      .createForm(service, Journey.Register)
      .url

}

case object SicCodeSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.SicCodeController
      .createForm(service, Journey.Register)
      .url

}

case object EmailSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.routes.WhatIsYourEmailController
      .createForm(service, Journey.Subscribe)
      .url

}

case object CheckYourEmailSubscriptionFlowPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.routes.CheckYourEmailController
      .createForm(service, Journey.Subscribe)
      .url

}

case object ReviewDetailsPageGetYourEORI extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
      .determineRoute(service, Journey.Register)
      .url

}

case object ReviewDetailsPageSubscription extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
      .determineRoute(service, Journey.Subscribe)
      .url

}

case object RegistrationConfirmPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.ConfirmContactDetailsController
      .form(service, Journey.Register)
      .url

}

case object ConfirmIndividualTypePage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.ConfirmIndividualTypeController
      .form(service, Journey.Register)
      .url

}

case object UserLocationPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.UserLocationController
      .form(service, Journey.Subscribe)
      .url

}

case object BusinessDetailsRecoveryPage extends SubscriptionPage {

  override def url(service: Service): String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.BusinessDetailsRecoveryController
      .form(service, Journey.Register)
      .url

}

case class PreviousPage(someUrl: String) extends SubscriptionPage() {
  override def url(service: Service): String = someUrl
}
