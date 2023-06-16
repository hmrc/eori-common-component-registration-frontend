package unit.viewModels

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RegistrationInfoRequest.UTR
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, CustomsId}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.{CheckYourDetailsRegisterConstructor, CheckYourDetailsRegisterViewModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers.DateFormatter
import unit.services.SubscriptionServiceTestData
import util.ControllerSpec

import scala.concurrent.ExecutionContext.Implicits.global




class CheckYourDetailsRegisterViewModelSpec extends UnitSpec with ControllerSpec with SubscriptionServiceTestData {

  val mockDateFormatter: DateFormatter = mock[DateFormatter]

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val organisationToTest = Seq(
    CdsOrganisationType.Company,
    CdsOrganisationType.EUOrganisation,
    CdsOrganisationType.ThirdCountryOrganisation,

  )

  private val partnershipToTest = Seq(
    CdsOrganisationType.Partnership,
    CdsOrganisationType.LimitedLiabilityPartnership
  )

  private val individualToTest = Seq(
    CdsOrganisationType.Individual,
    CdsOrganisationType.EUIndividual,
    CdsOrganisationType.ThirdCountryIndividual
  )

  private val soleTraderToTest = Seq(
    CdsOrganisationType.SoleTrader,
    CdsOrganisationType.ThirdCountrySoleTrader
  )

  val constructorInstance = new CheckYourDetailsRegisterConstructor(
    mockDateFormatter
  )


  "generateViewModel" when {

    "generateViewModel with isUserIdentifiedByRegService true" should {

      "Populate CheckYourDetailsRegisterViewModel correctly for organisation non GB" in servicesToTest.foreach({ service =>

        organisationToTest.foreach(orgType => {

         val result: CheckYourDetailsRegisterViewModel =  constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(addressDetails = Some(AddressViewModel("Line 1 line 2", "city name", Some("SE28 1AA"), "DE"))),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        )

          result shouldBe ""

        })




      })
      "Populate CheckYourDetailsRegisterViewModel correctly for organisation for unknown country code" in servicesToTest.foreach({ service =>

        val result: Unit = organisationToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(addressDetails = Some(AddressViewModel("Line 1 line 2", "city name", Some("SE28 1AA"), ""))),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for individual" in servicesToTest.foreach({ service =>

        val result: Unit = individualToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(customsId = Some(CustomsId(UTR, "1111111111"))),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for individual without DoB" in servicesToTest.foreach({ service =>

        val result: Unit = individualToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(nameDobDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for individual DoB and registrationDetails incorrectly set" in servicesToTest.foreach({ service =>

        val result: Unit = individualToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(nameDobDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for organisation without org name" in servicesToTest.foreach({ service =>

        val result: Unit = organisationToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(nameOrganisationDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for organisation none details" in servicesToTest.foreach({ service =>

        val result: Unit = organisationToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(nameOrganisationDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for organisation customId is defined " in servicesToTest.foreach({ service =>

        val result: Unit = organisationToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails.copy(customsId = Some(CustomsId(UTR, "1111111111"))),
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(addressDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for organisation is not verified " in servicesToTest.foreach({ service =>

        val result: Unit = organisationToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails.copy(customsId = Some(CustomsId(UTR, "1111111111"))),
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(vatVerificationOption = Some(false)),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for soleTrader" in servicesToTest.foreach({ service =>

        val result: Unit = soleTraderToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(customsId = Some(CustomsId(UTR, "1111111111"))),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for partnership if PartnershipOrLLP is false " in servicesToTest.foreach({ service =>

        val result: Unit = partnershipToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for partnership if PartnershipOrLLP is true " in servicesToTest.foreach({ service =>

        val result: Unit = partnershipToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = true,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for CharityPublicBodyNotForProfit" in servicesToTest.foreach({ service =>

        val result: Unit = constructorInstance.generateViewModel(
          Some(CdsOrganisationType.CharityPublicBodyNotForProfit),
          isPartnership = true,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = true,
        )
      })
    }

    "generateViewModel with isUserIdentifiedByRegService false" should {

      "Populate CheckYourDetailsRegisterViewModel correctly for organisation" in servicesToTest.foreach({ service =>

        val result: Unit = organisationToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for individual" in servicesToTest.foreach({ service =>

        val result: Unit = individualToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for individual without DoB" in servicesToTest.foreach({ service =>

        val result: Unit = individualToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(nameDobDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for individual DoB and registrationDetails incorrectly set" in servicesToTest.foreach({ service =>

        val result: Unit = individualToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes.copy(nameDobDetails = None),
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for soleTrader" in servicesToTest.foreach({ service =>

        val result: Unit = soleTraderToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          individualRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for partnership if PartnershipOrLLP is false " in servicesToTest.foreach({ service =>

        val result: Unit = partnershipToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = false,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for partnership if PartnershipOrLLP is true " in servicesToTest.foreach({ service =>

        val result: Unit = partnershipToTest.foreach(orgType => constructorInstance.generateViewModel(
          Some(orgType),
          isPartnership = true,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        ))
      })

      "Populate CheckYourDetailsRegisterViewModel correctly for CharityPublicBodyNotForProfit" in servicesToTest.foreach({ service =>

        val result: Unit = constructorInstance.generateViewModel(
          Some(CdsOrganisationType.CharityPublicBodyNotForProfit),
          isPartnership = true,
          organisationRegistrationDetails,
          fullyPopulatedSubscriptionDetailsAllOrgTypes,
          personalDataDisclosureConsent = fullyPopulatedSubscriptionDetailsAllOrgTypes.personalDataDisclosureConsent.getOrElse(false),
          service,
          isUserIdentifiedByRegService = false,
        )
      })

    }
  }
}
