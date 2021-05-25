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

package unit.controllers.registration

import common.pages.migration.SubscriptionExistingDetailsReviewPage
import common.pages.registration.RegistrationReviewPage
import common.support.testdata.subscription.SubscriptionContactDetailsModelBuilder._
import common.support.testdata.subscription.{BusinessDatesOrganisationTypeTables, ReviewPageOrganisationTypeTables}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.CheckYourDetailsRegisterController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Partnership, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  BusinessShortName,
  SubscriptionDetails,
  SubscriptionFlow
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{AddressViewModel, VatEUDetailsModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegisterWithoutIdWithSubscriptionService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.check_your_details_register
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder.{
  incorporatedRegistrationDetails,
  individualRegistrationDetails,
  individualRegistrationDetailsNotIdentifiedByReg01,
  organisationRegistrationDetails,
  partnershipRegistrationDetails
}
import util.builders.SubscriptionFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

//TODO: We need to simplify the reduce no of tests in the class. Review page should be simple, if value is available in holder then display otherwise not.
class CheckYourDetailsRegisterControllerSpec
    extends ControllerSpec with BusinessDatesOrganisationTypeTables with ReviewPageOrganisationTypeTables
    with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                     = mock[AuthConnector]
  private val mockAuthAction                        = authAction(mockAuthConnector)
  private val mockSessionCache                      = mock[SessionCache]
  private val mockSubscriptionDetailsHolder         = mock[SubscriptionDetails]
  private val mockRegisterWithoutIdWithSubscription = mock[RegisterWithoutIdWithSubscriptionService]
  private val mockSubscriptionFlow                  = mock[SubscriptionFlow]
  private val mockRequestSession                    = mock[RequestSessionData]
  private val checkYourDetailsRegisterView          = instanceOf[check_your_details_register]

  val controller = new CheckYourDetailsRegisterController(
    mockAuthAction,
    mockSessionCache,
    mockRequestSession,
    mcc,
    checkYourDetailsRegisterView,
    mockRegisterWithoutIdWithSubscription
  )

  private val organisationRegistrationDetailsWithEmptySafeId = organisationRegistrationDetails.copy(safeId = SafeId(""))

  private val addressDetails =
    AddressViewModel(street = "street", city = "city", postcode = Some("SE28 1AA"), countryCode = "GB")

  private val shortName = "Company Details Short name"

  private val NotEntered: String = "Not entered"

  override def beforeEach: Unit = {
    reset(mockSessionCache, mockSubscriptionDetailsHolder, mockSubscriptionFlow)
    when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)
    when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(mockSubscriptionFlow)
    when(mockSubscriptionDetailsHolder.ukVatDetails).thenReturn(None)
    when(mockSubscriptionDetailsHolder.vatEUDetails).thenReturn(Nil)
    when(mockSubscriptionDetailsHolder.ukVatDetails).thenReturn(None)
    when(mockSubscriptionDetailsHolder.businessShortName).thenReturn(None)
    when(mockSubscriptionDetailsHolder.dateEstablished).thenReturn(None)
    when(mockSubscriptionDetailsHolder.sicCode).thenReturn(None)
    when(mockSubscriptionDetailsHolder.nameDobDetails).thenReturn(None)
    when(mockSubscriptionDetailsHolder.addressDetails).thenReturn(Some(addressDetails))
    when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(Some(true))
    when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(Some(contactUkDetailsModelWithMandatoryValuesOnly))
    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(mockSubscriptionDetailsHolder)
    when(mockRequestSession.isPartnership(any[Request[AnyContent]])).thenReturn(false)
  }

  "Reviewing the details" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.reviewDetails(atarService, Journey.Register)
    )

    "return ok when data has been provided" in {
      showForm() { result =>
        status(result) shouldBe OK
      }
    }

    "display the sole trader name and dob from the cache when user has been identified by REG01" in {
      when(mockSubscriptionDetailsHolder.nameDobDetails)
        .thenReturn(Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23"))))
      when(mockSubscriptionDetailsHolder.name).thenReturn("John Doe")

      showForm(userSelectedOrgType = SoleTrader) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.FullNameXPath) shouldBe
          strim("""
                |John
                |Doe
              """)

        page.getElementsText(RegistrationReviewPage.IndividualDateOfBirthXPath) shouldBe
          strim("""
                |23 July 1980
              """)

        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentLabelXPath) shouldBe false
        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentXPath) shouldBe false

      }
    }

    "display the sole trader name and dob from the cache when user has NOT been identified" in {
      when(mockSubscriptionDetailsHolder.name).thenReturn("John Doe")
      when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
        .thenReturn(individualRegistrationDetailsNotIdentifiedByReg01)

      showForm(userSelectedOrgType = SoleTrader) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.FullNameXPath) shouldBe
          strim("""
                |John
                |Doe
              """)

        page.getElementsText(RegistrationReviewPage.FullNameReviewLinkXPath) shouldBe RegistrationReviewPage
          .changeAnswerText("Full name")
        page.getElementsHref(
          RegistrationReviewPage.FullNameReviewLinkXPath
        ) shouldBe "/customs-enrolment-services/atar/register/matching/row-name-date-of-birth/sole-trader/review"

        page.getElementsText(RegistrationReviewPage.IndividualDateOfBirthXPath) shouldBe
          strim("""
                |23 July 1980
              """)

        page.getElementsText(
          RegistrationReviewPage.IndividualDateOfBirthReviewLinkXPath
        ) shouldBe RegistrationReviewPage
          .changeAnswerText("Date of birth")
        page.getElementsHref(
          RegistrationReviewPage.IndividualDateOfBirthReviewLinkXPath
        ) shouldBe "/customs-enrolment-services/atar/register/matching/row-name-date-of-birth/sole-trader/review"

        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentLabelXPath) shouldBe false
        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentXPath) shouldBe false
      }
    }

    "display the business name and address from the cache" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.AddressXPath) shouldBe
          strim("""
                |street
                |city
                |SE28 1AA
                |United Kingdom
              """)

      }
    }

    "display the business name and six line address from the cache when user wasnt registered" in {
      when(mockSubscriptionDetailsHolder.name).thenReturn("orgName")
      when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
        .thenReturn(organisationRegistrationDetailsWithEmptySafeId)
      showForm(CdsOrganisationType.ThirdCountryOrganisation) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.BusinessNameXPath) shouldBe "orgName"
        page.getElementsText(RegistrationReviewPage.BusinessNameReviewLinkXPath) shouldBe RegistrationReviewPage
          .changeAnswerText("Organisation name")

        page.getElementsText(RegistrationReviewPage.SixLineAddressXPath) shouldBe
          strim("""
                |Line 1
                |line 2
                |line 3
                |line 4
                |SE28 1AA
                |United Kingdom
              """)
        page.getElementsText(RegistrationReviewPage.AddressXPath) shouldBe empty
        page.getElementsText(RegistrationReviewPage.SixLineAddressReviewLinkXPath) shouldBe RegistrationReviewPage
          .changeAnswerText("Organisation address")
        page.getElementsText(RegistrationReviewPage.AddressReviewLinkXPath) shouldBe empty
      }
    }

    "display the business name and four line address from the cache when user was registered, and translate EU country to full country name" in {
      when(mockSubscriptionDetailsHolder.name).thenReturn("orgName")
      when(mockSubscriptionDetailsHolder.addressDetails)
        .thenReturn(Some(AddressViewModel("street", "city", Some("322811"), "PL")))
      showForm(CdsOrganisationType.ThirdCountryOrganisation) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.BusinessNameXPath) shouldBe "orgName"
        page.getElementsText(RegistrationReviewPage.BusinessNameReviewLinkXPath) shouldBe empty
        page.getElementsText(RegistrationReviewPage.AddressXPath) shouldBe
          strim("""|street
                 |city
                 |322811
                 |Poland
              """)

        page.getElementsText(RegistrationReviewPage.SixLineAddressXPath) shouldBe empty
        page.getElementsText(RegistrationReviewPage.SixLineAddressReviewLinkXPath) shouldBe empty
      }
    }

    "not translate country code if it is third country" in {
      when(mockSubscriptionDetailsHolder.addressDetails)
        .thenReturn(Some(AddressViewModel("street", "city", None, "IN")))

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.AddressXPath) shouldBe
          strim("""
                |street
                |city
                |India
              """)
      }
    }

    "display all mandatory fields for an organisation" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          RegistrationReviewPage.EmailXPath
        ) shouldBe contactUkDetailsModelWithMandatoryValuesOnly.emailAddress
        page.getElementsText(RegistrationReviewPage.UKVatIdentificationNumberXpath) shouldBe NotEntered
        page.getElementsText(RegistrationReviewPage.EUVatDetailsXpath) shouldBe NotEntered
        page.getElementsText(RegistrationReviewPage.ContactDetailsXPath) shouldBe
          strim(s"""
                 |${contactUkDetailsModelWithMandatoryValuesOnly.fullName}
                 |${contactUkDetailsModelWithMandatoryValuesOnly.telephone}
                 |${contactUkDetailsModelWithMandatoryValuesOnly.street.get}
                 |${contactUkDetailsModelWithMandatoryValuesOnly.city.get}
                 |United Kingdom
              """)
      }
    }

    "display all fields including date of establishment when all are provided" in {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(
        detailsHolderWithAllFields.copy(
          contactDetails = Some(contactDetailsModelWithAllValues),
          addressDetails = Some(addressDetails),
          nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
        )
      )

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        testCommonReviewPageFields(page)

        page.getElementsText(RegistrationReviewPage.PrincipalEconomicActivityXPath) shouldBe "9999"
        page.getElementsText(RegistrationReviewPage.DateOfEstablishmentLabelXPath) shouldBe "Date of establishment"
        page.getElementsText(RegistrationReviewPage.DateOfEstablishmentXPath) shouldBe "11 November 1900"
        page.elementIsPresent(RegistrationReviewPage.IndividualDateOfBirthLabelXPath) shouldBe false
        page.elementIsPresent(RegistrationReviewPage.IndividualDateOfBirthXPath) shouldBe false
      }
    }

    forAll(businessDetailsOrganisationTypes) { organisationType =>
      val labelText = organisationType match {
        case LimitedLiabilityPartnership =>
          "Registered partnership name"
        case Partnership =>
          "Registered partnership name"
        case _ =>
          "Organisation name"
      }

      s"display $labelText label for ${organisationType.id}" in {
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Organisation"))

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(RegistrationReviewPage.BusinessNameLabelXPath) shouldBe labelText
        }
      }

      val UtrLabelText = organisationType match {
        case LimitedLiabilityPartnership | Partnership =>
          "Partnership Self Assessment UTR number"
        case _ =>
          "Corporation Tax UTR number"
      }
      s"display $UtrLabelText label for ${organisationType.id}" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Organisation"))
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(RegistrationReviewPage.UtrLabelXPath) shouldBe UtrLabelText
        }
      }
    }

    forAll(contactDetailsOrganisationTypes) { organisationType =>
      s"contact details label displayed for ${organisationType.id}" in {
        val subscriptionFlow = organisationType match {
          case SoleTrader => SubscriptionFlow("Organisation")
          case _          => SubscriptionFlow("Individual")
        }
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))

          page.getElementsText(RegistrationReviewPage.AddressHeadingXPath) shouldBe "Your address"
        }
      }
    }

    forAll(individualsOnlyOrganisationTypes) { organisationType =>
      s"should not display shortened name for ${organisationType.id}" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Individual"))
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.elementIsPresent(RegistrationReviewPage.ShortNameXPath) shouldBe false
        }
      }
    }

    forAll(shortenedNameOrganisationTypes) { organisationType =>
      s"display shortened name label and value for ${organisationType.id}" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Organisation"))
        when(mockSubscriptionDetailsHolder.businessShortName).thenReturn(Some(BusinessShortName(shortName)))
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.elementIsPresent(RegistrationReviewPage.ShortNameXPath) shouldBe true
          page.getElementsText(RegistrationReviewPage.ShortNameXPath) shouldBe shortName
        }
      }
    }

    forAll(shortenedNameOrganisationTypes) { organisationType =>
      s"display shortened name and 'Not entered' for ${organisationType.id} if alternative name wasn't defined" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Organisation"))
        when(mockSubscriptionDetailsHolder.businessShortName).thenReturn(Some(BusinessShortName(false, None)))
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.elementIsPresent(RegistrationReviewPage.ShortNameXPath) shouldBe true
          page.getElementsText(RegistrationReviewPage.ShortNameXPath) shouldBe "Not entered"
        }
      }
    }

    "display all fields when all are provided for an individual" in {
      when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(individualRegistrationDetails)
      val holder = detailsHolderWithAllFields.copy(
        contactDetails = Some(contactDetailsModelWithAllValues),
        dateEstablished = None,
        businessShortName = None,
        addressDetails = Some(addressDetails),
        nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
      )
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(holder)

      showForm(isIndividualSubscriptionFlow = true) { result =>
        val page = CdsPage(contentAsString(result))

        page.getElementsText(RegistrationReviewPage.EmailXPath) shouldBe contactDetailsModelWithAllValues.emailAddress
        page.getElementsText(RegistrationReviewPage.ContactDetailsXPath) shouldBe
          strim(s"""
                 |${contactDetailsModelWithAllValues.fullName}
                 |${contactDetailsModelWithAllValues.telephone}
                 |${messages("cds.review-page.fax-prefix")} ${contactDetailsModelWithAllValues.fax.get}
                 |${contactDetailsModelWithAllValues.street.get}
                 |${contactDetailsModelWithAllValues.city.get}
                 |${contactDetailsModelWithAllValues.postcode.get} France
              """)

        page.elementIsPresent(RegistrationReviewPage.ShortNameXPath) shouldBe false
        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentXPath) shouldBe false
        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentLabelXPath) shouldBe false
      }
    }
  }

  "display the review page check-your-details for Company" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(incorporatedRegistrationDetails.copy(customsId = Some(Utr("7280616009"))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameOrganisationDetails = Some(NameOrganisationMatchModel("orgName"))
    )
    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(holder)

    showForm(userSelectedOrgType = Company) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))
      page.title should startWith("Check your answers")

      page.getElementsText(RegistrationReviewPage.EmailXPath) shouldBe contactDetailsModelWithAllValues.emailAddress

      page.getElementsText(SubscriptionExistingDetailsReviewPage.BusinessNameLabelXpath) shouldBe "Organisation name"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.BusinessNameValueXpath) shouldBe "orgName"

      page.getElementsText(SubscriptionExistingDetailsReviewPage.UtrNoLabelXPath) shouldBe "Corporation Tax UTR number"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UtrNoLabelValueXPath) shouldBe "7280616009"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.LimitedAddressLabelXpath
      ) shouldBe "Organisation address"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.LimitedAddressValueXpath) shouldBe
        strim("""
            |street
            |city
            |SE28 1AA
            |United Kingdom
          """)
      page.elementIsPresent(SubscriptionExistingDetailsReviewPage.LimitedAddressReviewLink) shouldBe true

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.DateOfEstablishmentLabelXPath
      ) shouldBe "Date of establishment"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.DateOfEstablishmentXPath) shouldBe "23 July 1980"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.DateOfEstablishmentReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Date of establishment")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.DateOfEstablishmentReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/date-established/review"

      page.getElementsText(SubscriptionExistingDetailsReviewPage.ContactDetailsXPathLabel) shouldBe "Contact"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.ContactDetailsXPath) shouldBe
        strim("""
            |John Doe
            |01632961234
            |fax: 01632961234
            |Line 1
            |city name
            |SE28 1AA
            |France
          """)
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ContactDetailsReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Contact")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.ContactDetailsReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/contact-details/review"

      page.getElementsText(SubscriptionExistingDetailsReviewPage.ShortNameXPathLabel) shouldBe "Shortened name"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.ShortNameXPath) shouldBe "Short Name"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ShortNameReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Shortened name")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.ShortNameReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/company-short-name-yes-no/review"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.NatureOfBusinessXPathLabel
      ) shouldBe "Standard Industrial Classification (SIC) code"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.NatureOfBusinessXPath) shouldBe "9999"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.NatureOfBusinessReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Standard Industrial Classification (SIC) code")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.NatureOfBusinessReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/sic-code/review"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersXpathLabel
      ) shouldBe "UK VAT Number"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersXpath) shouldBe "123456789"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationPostcodeXpathLabel
      ) shouldBe "Postcode of your UK VAT registration address"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UKVatIdentificationPostcodeXpath) shouldBe "SE28 1AA"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationDateXpathLabel
      ) shouldBe "UK VAT effective date"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UKVatIdentificationDateXpath) shouldBe "1 January 2017"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("UK VAT Number")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/vat-registered-uk/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationPostcodeReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Postcode of your UK VAT registration address")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationPostcodeReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/vat-registered-uk/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationDateReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("UK VAT effective date")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationDateReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/vat-registered-uk/review"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersXpathLabel
      ) shouldBe "EU VAT numbers"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersXpath) shouldBe
        strim("""
            |VAT-2 - France
            |VAT-3 - Poland
          """)
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("EU VAT numbers")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/vat-details-eu-confirm/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUDisclosureReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Organisation details included on the EORI checker")
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUDisclosureConsentXPathLabel
      ) shouldBe "Organisation details included on the EORI checker"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUDisclosureConsentXPath
      ) shouldBe "Yes - I want my organisation name and address on the EORI checker"
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.EUDisclosureReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/disclose-personal-details-consent/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ConfirmAndRegisterInfoXpath
      ) shouldBe "By sending this application you confirm that the information you are providing is correct and complete."
    }
  }

  "display the review page check-your-details for LLP" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(incorporatedRegistrationDetails.copy(customsId = Some(Utr("7280616009"))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameOrganisationDetails = Some(NameOrganisationMatchModel("orgName"))
    )
    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(holder)

    showForm(userSelectedOrgType = LimitedLiabilityPartnership) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))
      page.title should startWith("Check your answers")

      page.getElementsText(RegistrationReviewPage.EmailXPath) shouldBe contactDetailsModelWithAllValues.emailAddress

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.BusinessNameLabelXpath
      ) shouldBe "Registered partnership name"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.BusinessNameValueXpath) shouldBe "orgName"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UtrNoLabelXPath
      ) shouldBe "Partnership Self Assessment UTR number"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UtrNoLabelValueXPath) shouldBe "7280616009"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.LimitedAddressLabelXpath
      ) shouldBe "Partnership address"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.LimitedAddressValueXpath) shouldBe
        strim("""
            |street
            |city
            |SE28 1AA
            |United Kingdom
          """)
      page.elementIsPresent(SubscriptionExistingDetailsReviewPage.LimitedAddressReviewLink) shouldBe true

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.DateOfEstablishmentLabelXPath
      ) shouldBe "Date of establishment"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.DateOfEstablishmentXPath) shouldBe "23 July 1980"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.DateOfEstablishmentReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Date of establishment")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.DateOfEstablishmentReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/date-established/review"

      page.getElementsText(SubscriptionExistingDetailsReviewPage.ContactDetailsXPathLabel) shouldBe "Contact"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.ContactDetailsXPath) shouldBe
        strim("""
            |John Doe
            |01632961234
            |fax: 01632961234
            |Line 1
            |city name
            |SE28 1AA
            |France
          """)
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ContactDetailsReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Contact")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.ContactDetailsReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/contact-details/review"

      page.getElementsText(SubscriptionExistingDetailsReviewPage.ShortNameXPathLabel) shouldBe "Shortened name"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.ShortNameXPath) shouldBe "Short Name"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ShortNameReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Shortened name")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.ShortNameReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/company-short-name-yes-no/review"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.NatureOfBusinessXPathLabel
      ) shouldBe "Standard Industrial Classification (SIC) code"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.NatureOfBusinessXPath) shouldBe "9999"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.NatureOfBusinessReviewLinkXPath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Standard Industrial Classification (SIC) code")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.NatureOfBusinessReviewLinkXPath
      ) shouldBe "/customs-enrolment-services/atar/register/sic-code/review"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersXpathLabel
      ) shouldBe "UK VAT Number"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersXpath) shouldBe "123456789"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationPostcodeXpathLabel
      ) shouldBe "Postcode of your UK VAT registration address"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UKVatIdentificationPostcodeXpath) shouldBe "SE28 1AA"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationDateXpathLabel
      ) shouldBe "UK VAT effective date"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UKVatIdentificationDateXpath) shouldBe "1 January 2017"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("UK VAT Number")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.UKVatIdentificationNumbersReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/vat-registered-uk/review"

      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersXpathLabel
      ) shouldBe "EU VAT numbers"

      page.getElementsText(SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersXpath) shouldBe
        strim("""
            |VAT-2 - France
            |VAT-3 - Poland
          """)
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("EU VAT numbers")
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.EUVatIdentificationNumbersReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/vat-details-eu-confirm/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUDisclosureReviewLinkXpath
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Partnership details included on the EORI checker")
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUDisclosureConsentXPathLabel
      ) shouldBe "Partnership details included on the EORI checker"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.EUDisclosureConsentXPath
      ) shouldBe "Yes - I want my partnership name and address on the EORI checker"
      page.getElementsHref(
        SubscriptionExistingDetailsReviewPage.EUDisclosureReviewLinkXpath
      ) shouldBe "/customs-enrolment-services/atar/register/disclose-personal-details-consent/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ConfirmAndRegisterInfoXpath
      ) shouldBe "By sending this application you confirm that the information you are providing is correct and complete."
    }
  }

  "display the review page check-your-details for an individual with nino" in {
    val expectedNino = "someNino"

    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(individualRegistrationDetails.copy(customsId = Some(Nino(expectedNino))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
    )
    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(holder)

    showForm(userSelectedOrgType = Individual, isIndividualSubscriptionFlow = true) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.getElementsText(SubscriptionExistingDetailsReviewPage.UtrNoLabelXPath) shouldBe "National Insurance number"
      page.getElementsText(SubscriptionExistingDetailsReviewPage.UtrNoLabelValueXPath) shouldBe expectedNino

    }
  }

  "display the review page check-your-details with option to change address for UK entities" in {
    showForm() { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.elementIsPresent("//*[@id='review-tbl__address_change']") shouldBe true
    }
  }

  "display the form with 'UTR Not entered'" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(organisationRegistrationDetailsWithEmptySafeId)

    showForm(userSelectedOrgType = CdsOrganisationType.ThirdCountryOrganisation) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.elementIsPresent("//*[@id='review-tbl__have-utr']") shouldBe true
      page.getElementsText("//*[@id='review-tbl__have-utr']") shouldBe NotEntered
    }
  }

  "display the review page check-your-details for an individual with UTR" in {
    val expectedUtr = "someUTR"

    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(individualRegistrationDetails.copy(customsId = Some(Utr(expectedUtr))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
    )
    when(mockSessionCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(holder)

    showForm(userSelectedOrgType = Individual, isIndividualSubscriptionFlow = true) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.getElementsText(SubscriptionExistingDetailsReviewPage.UtrNoLabelXPath) shouldBe "UTR number"
    }
  }

  "VAT details" should {
    "display only UK ones when only for UK found in cache" in {
      when(mockSubscriptionDetailsHolder.ukVatDetails).thenReturn(gbVatDetails)
      mockRegistrationDetailsBasedOnOrganisationType(Individual)

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        assertUkVatDetailsShowValues(page)
        page.getElementsText(RegistrationReviewPage.EUVatDetailsXpath) shouldBe NotEntered
      }
    }

    "display only EU ones when only for EU found in cache" in {
      when(mockSubscriptionDetailsHolder.vatEUDetails).thenReturn(euVats)
      mockRegistrationDetailsBasedOnOrganisationType(Individual)

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.UKVatIdentificationNumberXpath) shouldBe NotEntered
        assertEuVatDetailsShowValues(page)
      }
    }

    "display country in correct form" in {

      val euVatsCaseInsensitive: Seq[VatEUDetailsModel] =
        Seq(VatEUDetailsModel("FR", "VAT-2"), VatEUDetailsModel("PL", "VAT-3"))

      when(mockSubscriptionDetailsHolder.vatEUDetails).thenReturn(euVatsCaseInsensitive)
      mockRegistrationDetailsBasedOnOrganisationType(Individual)

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(RegistrationReviewPage.EUVatDetailsXpath) shouldBe "VAT-2 - France VAT-3 - Poland"
        assertEuVatDetailsShowValues(page)
      }
    }
  }

  "failure" should {

    "throw an expected exception when cache does not contain consent to disclose personal data" in {
      when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(None)
      mockRegistrationDetailsBasedOnOrganisationType(Individual)

      val caught = intercept[IllegalStateException] {
        showForm() { result =>
          await(result)
        }
      }
      caught.getMessage shouldBe "Consent to disclose personal data is missing"
    }
  }

  "submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.submitDetails(atarService, Journey.Register)
    )

    "redirect to next screen" in {
      when(
        mockRegisterWithoutIdWithSubscription
          .rowRegisterWithoutIdWithSubscription(any(), any(), any())(any[HeaderCarrier], any())
      ).thenReturn(Future.successful(Results.Ok))
      submitForm(Map.empty, journey = Journey.Register)(verifyRedirectToNextPageIn(_))
      verify(mockRegisterWithoutIdWithSubscription, times(1))
        .rowRegisterWithoutIdWithSubscription(any(), any(), any())(any[HeaderCarrier], any())
    }
  }

  private def testCommonReviewPageFields(page: CdsPage, expectedCountry: Option[String] = Option("France")): Unit = {
    assertUkVatDetailsShowValues(page)
    assertEuVatDetailsShowValues(page)

    val countryString = expectedCountry match {
      case None    => ""
      case Some(x) => x
    }

    page.getElementsText(RegistrationReviewPage.EmailXPath) shouldBe contactDetailsModelWithAllValues.emailAddress

    page.getElementsText(RegistrationReviewPage.ContactDetailsXPath) shouldBe
      strim(s"""
           |${contactDetailsModelWithAllValues.fullName}
           |${contactDetailsModelWithAllValues.telephone}
           |${messages("cds.review-page.fax-prefix")} ${contactDetailsModelWithAllValues.fax.get}
           |${contactDetailsModelWithAllValues.street.get}
           |${contactDetailsModelWithAllValues.city.get}
           |${contactDetailsModelWithAllValues.postcode.get} $countryString
              """)
  }

  private def assertUkVatDetailsShowValues(page: CdsPage) {
    page.getElementsText(RegistrationReviewPage.UKVatIdentificationNumberXpath) shouldBe "123456789"
    page.getElementsText(RegistrationReviewPage.UKVatIdentificationPostcodeXpath) shouldBe "SE28 1AA"
    page.getElementsText(RegistrationReviewPage.UKVatIdentificationDateXpath) shouldBe "1 January 2017"
    page.getElementText(
      RegistrationReviewPage.UKVatIdentificationNumbersReviewLinkXpath
    ) shouldBe RegistrationReviewPage
      .changeAnswerText("UK VAT Number")
    page.getElementsHref(
      RegistrationReviewPage.UKVatIdentificationNumbersReviewLinkXpath
    ) shouldBe VatRegisteredUkController
      .reviewForm(atarService, Journey.Register)
      .url
  }

  private def assertEuVatDetailsShowValues(page: CdsPage) {
    page.getElementsText(RegistrationReviewPage.EUVatDetailsXpath) shouldBe
      strim("""
          |VAT-2 - France
          |VAT-3 - Poland
        """)

    page.getElementText(
      RegistrationReviewPage.EUVatIdentificationNumbersReviewLinkXpath
    ) shouldBe RegistrationReviewPage
      .changeAnswerText("EU VAT numbers")
  }

  def showForm(
    userSelectedOrgType: CdsOrganisationType = CdsOrganisationType.Company,
    userId: String = defaultUserId,
    isIndividualSubscriptionFlow: Boolean = false
  )(test: Future[Result] => Any) {
    val controller = new CheckYourDetailsRegisterController(
      mockAuthAction,
      mockSessionCache,
      mockRequestSession,
      mcc,
      checkYourDetailsRegisterView,
      mockRegisterWithoutIdWithSubscription
    )

    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSession.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(userSelectedOrgType))
    if (
      userSelectedOrgType.id == CdsOrganisationType.PartnershipId || userSelectedOrgType.id == CdsOrganisationType.LimitedLiabilityPartnershipId
    )
      when(mockRequestSession.isPartnership(any[Request[AnyContent]])).thenReturn(true)

    when(mockSubscriptionFlow.isIndividualFlow).thenReturn(isIndividualSubscriptionFlow)

    test(controller.reviewDetails(atarService, Journey.Register).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitForm(
    form: Map[String, String],
    userId: String = defaultUserId,
    userSelectedOrgType: Option[CdsOrganisationType] = None,
    journey: Journey.Value
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSession.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(userSelectedOrgType)

    test(
      controller.submitDetails(atarService, journey)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def verifyRedirectToNextPageIn(result: Result) =
    status(result) shouldBe OK

  private def mockRegistrationDetailsBasedOnOrganisationType(orgType: CdsOrganisationType) =
    orgType match {
      case SoleTrader | Individual =>
        when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(individualRegistrationDetails)
      case Partnership =>
        when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(partnershipRegistrationDetails)
      case _ =>
        when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)
    }

}
