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

package unit.controllers

import common.pages.migration.SubscriptionExistingDetailsReviewPage
import common.pages.registration.RegistrationReviewPage
import common.support.testdata.subscription.SubscriptionContactDetailsModelBuilder._
import common.support.testdata.subscription.{BusinessDatesOrganisationTypeTables, ReviewPageOrganisationTypeTables}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CheckYourDetailsRegisterController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Partnership, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  BusinessShortName,
  SubscriptionDetails,
  SubscriptionFlow
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegisterWithoutIdWithSubscriptionService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.check_your_details_register
import uk.gov.hmrc.http.HeaderCarrier
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

import java.time.LocalDate
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
    when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(organisationRegistrationDetails)
    when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(mockSubscriptionFlow)
    when(mockSubscriptionDetailsHolder.ukVatDetails).thenReturn(None)
    when(mockSubscriptionDetailsHolder.ukVatDetails).thenReturn(None)
    when(mockSubscriptionDetailsHolder.businessShortName).thenReturn(None)
    when(mockSubscriptionDetailsHolder.dateEstablished).thenReturn(None)
    when(mockSubscriptionDetailsHolder.sicCode).thenReturn(None)
    when(mockSubscriptionDetailsHolder.nameDobDetails).thenReturn(None)
    when(mockSubscriptionDetailsHolder.addressDetails).thenReturn(Some(addressDetails))
    when(mockSubscriptionDetailsHolder.personalDataDisclosureConsent).thenReturn(Some(true))
    when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(Some(contactUkDetailsModelWithMandatoryValuesOnly))
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(mockSubscriptionDetailsHolder)
    when(mockRequestSession.isPartnership(any[Request[AnyContent]])).thenReturn(false)
  }

  "Reviewing the details" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewDetails(atarService))

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
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Full name") shouldBe
          strim("""
                |John
                |Doe
              """)

        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Date of birth") shouldBe
          strim("""
                |23 July 1980
              """)

        page.summaryListElementPresent(
          RegistrationReviewPage.SummaryListRowXPath,
          "Date of establishment"
        ) shouldBe false
      }
    }

    "display the sole trader name and dob from the cache when user has NOT been identified" in {
      when(mockSubscriptionDetailsHolder.name).thenReturn("John Doe")
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(individualRegistrationDetailsNotIdentifiedByReg01)

      showForm(userSelectedOrgType = SoleTrader) { result =>
        val page = CdsPage(contentAsString(result))
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Full name") shouldBe
          strim("""
                |John
                |Doe
              """)

        page.getSummaryListLink(
          RegistrationReviewPage.SummaryListRowXPath,
          "Full name",
          "Change"
        ) shouldBe RegistrationReviewPage.changeAnswerText("Full name")
        page.getSummaryListHref(
          RegistrationReviewPage.SummaryListRowXPath,
          "Full name",
          "Change"
        ) shouldBe "/customs-registration-services/atar/register/matching/row-name-date-of-birth/sole-trader/review"

        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Date of birth") shouldBe
          strim("""
                |23 July 1980
              """)

        page.getSummaryListLink(
          RegistrationReviewPage.SummaryListRowXPath,
          "Date of birth",
          "Change"
        ) shouldBe RegistrationReviewPage
          .changeAnswerText("Date of birth")
        page.getSummaryListHref(
          RegistrationReviewPage.SummaryListRowXPath,
          "Date of birth",
          "Change"
        ) shouldBe "/customs-registration-services/atar/register/matching/row-name-date-of-birth/sole-trader/review"

        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentLabelXPath) shouldBe false
        page.elementIsPresent(RegistrationReviewPage.DateOfEstablishmentXPath) shouldBe false
      }
    }

    "display the business name and address from the cache" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Registered company address") shouldBe
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
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(organisationRegistrationDetailsWithEmptySafeId)
      showForm(CdsOrganisationType.ThirdCountryOrganisation) { result =>
        val page = CdsPage(contentAsString(result))
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Organisation name") shouldBe "orgName"
        page.getSummaryListHref(
          RegistrationReviewPage.SummaryListRowXPath,
          "Organisation name",
          "Change"
        ) shouldBe "/customs-registration-services/atar/register/matching/name/third-country-organisation/review"

        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Organisation address") shouldBe
          strim("""
                |Line 1
                |line 2
                |line 3
                |line 4
                |SE28 1AA
                |United Kingdom
              """)

        page.getSummaryListLink(
          RegistrationReviewPage.SummaryListRowXPath,
          "Organisation address",
          "Change"
        ) shouldBe RegistrationReviewPage
          .changeAnswerText("Organisation address")
      }
    }

    "display the business name and four line address from the cache when user was registered, and translate EU country to full country name" in {
      when(mockSubscriptionDetailsHolder.name).thenReturn("orgName")
      when(mockSubscriptionDetailsHolder.addressDetails)
        .thenReturn(Some(AddressViewModel("street", "city", Some("322811"), "PL")))
      showForm(CdsOrganisationType.ThirdCountryOrganisation) { result =>
        val page = CdsPage(contentAsString(result))
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Organisation name") shouldBe "orgName"
        page.getSummaryListLink(
          RegistrationReviewPage.SummaryListRowXPath,
          "Organisation name",
          "Change"
        ) shouldBe empty
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Organisation address") shouldBe
          strim("""|street
                 |city
                 |322811
                 |Poland
              """)

      }
    }

    "not translate country code if it is third country" in {
      when(mockSubscriptionDetailsHolder.addressDetails)
        .thenReturn(Some(AddressViewModel("street", "city", None, "IN")))

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Registered company address") shouldBe
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
        page.getSummaryListValue(
          RegistrationReviewPage.SummaryListRowXPath,
          "Contact name"
        ) shouldBe contactUkDetailsModelWithMandatoryValuesOnly.fullName
        page.getSummaryListValue(
          RegistrationReviewPage.SummaryListRowXPath,
          "Contact telephone"
        ) shouldBe contactUkDetailsModelWithMandatoryValuesOnly.telephone
        page.getSummaryListValue(
          RegistrationReviewPage.SummaryListRowXPath,
          "Email address"
        ) shouldBe contactUkDetailsModelWithMandatoryValuesOnly.emailAddress
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe NotEntered
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe
          strim(s"""                 
                 |${contactUkDetailsModelWithMandatoryValuesOnly.street.get}
                 |${contactUkDetailsModelWithMandatoryValuesOnly.city.get}
                 |United Kingdom
              """)
      }
    }

    "display all fields including date of establishment when all are provided" in {
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(
        detailsHolderWithAllFields.copy(
          contactDetails = Some(contactDetailsModelWithAllValues),
          addressDetails = Some(addressDetails),
          nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
        )
      )

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        testCommonReviewPageFields(page)

        page.getSummaryListValue(
          RegistrationReviewPage.SummaryListRowXPath,
          "Standard Industrial Classification (SIC) code"
        ) shouldBe "9999"
        page.getSummaryListValue(
          RegistrationReviewPage.SummaryListRowXPath,
          "Date of establishment"
        ) shouldBe "11 November 1900"
        page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Date of birth") shouldBe false
      }
    }

    forAll(businessDetailsOrganisationTypes) { organisationType =>
      val labelText = organisationType match {
        case LimitedLiabilityPartnership =>
          "Registered partnership name"
        case Partnership =>
          "Registered partnership name"
        case CharityPublicBodyNotForProfit | ThirdCountryOrganisation =>
          "Organisation name"
        case _ =>
          "Registered company name"
      }

      s"display $labelText label for ${organisationType.id}" in {
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Organisation"))

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, labelText) shouldBe true
        }
      }

      val UtrLabelText = organisationType match {
        case LimitedLiabilityPartnership | Partnership =>
          "Partnership Self Assessment UTR number"
        case _ =>
          "Corporation Tax UTR"
      }
      s"display $UtrLabelText label for ${organisationType.id}" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]]))
          .thenReturn(SubscriptionFlow("Organisation"))
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, UtrLabelText) shouldBe true
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

          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Your address") shouldBe true
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
          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe true
          page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe shortName
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
          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe true
          page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe "Not entered"
        }
      }
    }

    "display all fields when all are provided for an individual" in {
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(individualRegistrationDetails)
      val holder = detailsHolderWithAllFields.copy(
        contactDetails = Some(contactDetailsModelWithAllValues),
        dateEstablished = None,
        businessShortName = None,
        addressDetails = Some(addressDetails),
        nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
      )
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(holder)

      showForm(isIndividualSubscriptionFlow = true) { result =>
        val page = CdsPage(contentAsString(result))

        page.getSummaryListValue(
          RegistrationReviewPage.SummaryListRowXPath,
          "Email address"
        ) shouldBe contactDetailsModelWithAllValues.emailAddress
        page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe
          strim(s"""             
                 |${contactDetailsModelWithAllValues.street.get}
                 |${contactDetailsModelWithAllValues.city.get}
                 |${contactDetailsModelWithAllValues.postcode.get} France
              """)

        page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe false
        page.summaryListElementPresent(
          RegistrationReviewPage.SummaryListRowXPath,
          "Date of establishment"
        ) shouldBe false
      }
    }
  }

  "display the review page check-your-details for Company" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(incorporatedRegistrationDetails.copy(customsId = Some(Utr("7280616009"))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameOrganisationDetails = Some(NameOrganisationMatchModel("orgName"))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(holder)

    showForm(userSelectedOrgType = Company) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))
      page.title should startWith("Check your answers")

      page.h2() should startWith("Company details VAT details Contact details Declaration")

      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Email address"
      ) shouldBe contactDetailsModelWithAllValues.emailAddress

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company name"
      ) shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Registered company name") shouldBe "orgName"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Corporation Tax UTR") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Corporation Tax UTR") shouldBe "7280616009"

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company address"
      ) shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Registered company address") shouldBe
        strim("""
            |street
            |city
            |SE28 1AA
            |United Kingdom
          """)
      page.summaryListHrefPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company address",
        "Change"
      ) shouldBe true
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company address",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/matching/confirm/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Date of establishment") shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of establishment"
      ) shouldBe "23 July 1980"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of establishment",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Date of establishment")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of establishment",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/date-established/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Contact name") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Contact name") shouldBe "John Doe"
      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe
        strim("""
            |Line 1
            |city name
            |SE28 1AA
            |France
          """)
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Contact address",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Contact address")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Contact address",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/contact-address/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe "Short Name"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Shortened name",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Shortened name")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Shortened name",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/company-short-name-yes-no/review"

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code"
      ) shouldBe "9999"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Standard Industrial Classification (SIC) code")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/sic-code/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe "123456789"
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Postcode of your VAT registration address"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Postcode of your VAT registration address"
      ) shouldBe "SE28 1AA"
      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "VAT effective date") shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT effective date"
      ) shouldBe "1 January 2017"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT number",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("VAT number")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT number",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/vat-registered-uk/review"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Postcode of your VAT registration address",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Postcode of your VAT registration address")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Postcode of your VAT registration address",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/vat-registered-uk/review"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT effective date",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("VAT effective date")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT effective date",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/vat-registered-uk/review"

      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company details included on the EORI checker",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Registered company details included on the EORI checker")
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company details included on the EORI checker"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company details included on the EORI checker"
      ) shouldBe "Yes I want my organisation name and address on the EORI checker"
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company details included on the EORI checker",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/disclose-personal-details-consent/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ConfirmAndRegisterInfoXpath
      ) shouldBe "You confirm that, to the best of your knowledge, the details you are providing are correct."
    }
  }

  "display the review page check-your-details for LLP" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(incorporatedRegistrationDetails.copy(customsId = Some(Utr("7280616009"))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameOrganisationDetails = Some(NameOrganisationMatchModel("orgName"))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(holder)

    showForm(userSelectedOrgType = LimitedLiabilityPartnership) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))
      page.title should startWith("Check your answers")

      page.h2() should startWith("Partnership details VAT details Contact details Declaration")

      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Email address"
      ) shouldBe contactDetailsModelWithAllValues.emailAddress

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered partnership name"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered partnership name"
      ) shouldBe "orgName"

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership Self Assessment UTR number"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership Self Assessment UTR number"
      ) shouldBe "7280616009"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Partnership address") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Partnership address") shouldBe
        strim("""
            |street
            |city
            |SE28 1AA
            |United Kingdom
          """)
      page.summaryListHrefPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership address",
        "Change"
      ) shouldBe true

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Date of establishment") shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of establishment"
      ) shouldBe "23 July 1980"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of establishment",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Date of establishment")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of establishment",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/date-established/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe
        strim("""           
            |Line 1
            |city name
            |SE28 1AA
            |France
          """)
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Contact address",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Contact address")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Contact address",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/contact-address/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Shortened name") shouldBe "Short Name"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Shortened name",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Shortened name")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Shortened name",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/company-short-name-yes-no/review"

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code"
      ) shouldBe "9999"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Standard Industrial Classification (SIC) code")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Standard Industrial Classification (SIC) code",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/sic-code/review"

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe "123456789"
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Postcode of your VAT registration address"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Postcode of your VAT registration address"
      ) shouldBe "SE28 1AA"
      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "VAT effective date") shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT effective date"
      ) shouldBe "1 January 2017"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT number",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("VAT number")
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "VAT number",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/vat-registered-uk/review"

      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership details included on the EORI checker",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Partnership details included on the EORI checker")
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership details included on the EORI checker"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership details included on the EORI checker"
      ) shouldBe "Yes I want my partnership name and address on the EORI checker"
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Partnership details included on the EORI checker",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/disclose-personal-details-consent/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ConfirmAndRegisterInfoXpath
      ) shouldBe "You confirm that, to the best of your knowledge, the details you are providing are correct."
    }
  }

  "display the review page check-your-details for an individual with nino" in {
    val expectedNino = "someNino"

    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(individualRegistrationDetails.copy(customsId = Some(Nino(expectedNino))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(holder)

    showForm(userSelectedOrgType = Individual, isIndividualSubscriptionFlow = true) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.h2() should startWith("Your details Contact details Declaration")

      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "National Insurance number"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "National Insurance number"
      ) shouldBe expectedNino

    }
  }

  "display the review page check-your-details with option to change address for UK entities" in {
    showForm() { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.summaryListHrefPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Registered company address",
        "Change"
      ) shouldBe true
    }
  }

  "display the form with 'UTR Not entered'" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(organisationRegistrationDetailsWithEmptySafeId)

    showForm(userSelectedOrgType = CdsOrganisationType.ThirdCountryOrganisation) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Corporation Tax UTR") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Corporation Tax UTR") shouldBe NotEntered
    }
  }

  "display the review page check-your-details for an individual with UTR" in {
    val expectedUtr = "someUTR"

    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(individualRegistrationDetails.copy(customsId = Some(Utr(expectedUtr))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("1980-07-23")))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(holder)

    showForm(userSelectedOrgType = Individual, isIndividualSubscriptionFlow = true) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "UTR number") shouldBe true
    }
  }

  "VAT details" should {
    "display only UK vat details when found in cache" in {
      when(mockSubscriptionDetailsHolder.ukVatDetails).thenReturn(gbVatDetails)
      mockRegistrationDetailsBasedOnOrganisationType(Individual)

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        assertUkVatDetailsShowValues(page)
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

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submitDetails(atarService))

    "redirect to next screen" in {
      when(
        mockRegisterWithoutIdWithSubscription
          .rowRegisterWithoutIdWithSubscription(any(), any())(any[HeaderCarrier], any())
      ).thenReturn(Future.successful(Results.Ok))
      submitForm(Map.empty)(verifyRedirectToNextPageIn(_))
      verify(mockRegisterWithoutIdWithSubscription, times(1))
        .rowRegisterWithoutIdWithSubscription(any(), any())(any[HeaderCarrier], any())
    }
  }

  private def testCommonReviewPageFields(page: CdsPage, expectedCountry: Option[String] = Option("France")): Unit = {
    assertUkVatDetailsShowValues(page)

    val countryString = expectedCountry match {
      case None    => ""
      case Some(x) => x
    }

    page.getSummaryListValue(
      RegistrationReviewPage.SummaryListRowXPath,
      "Email address"
    ) shouldBe contactDetailsModelWithAllValues.emailAddress

    page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Contact address") shouldBe
      strim(s"""
           |${contactDetailsModelWithAllValues.street.get}
           |${contactDetailsModelWithAllValues.city.get}
           |${contactDetailsModelWithAllValues.postcode.get} $countryString
              """)
  }

  private def assertUkVatDetailsShowValues(page: CdsPage) {
    //VAT number
    page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe "123456789"
    page.getSummaryListValue(
      RegistrationReviewPage.SummaryListRowXPath,
      "Postcode of your VAT registration address"
    ) shouldBe "SE28 1AA"
    page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "VAT effective date") shouldBe "1 January 2017"
    page.getSummaryListLink(
      RegistrationReviewPage.SummaryListRowXPath,
      "VAT number",
      "Change"
    ) shouldBe RegistrationReviewPage.changeAnswerText("VAT number")
    page.getSummaryListHref(
      RegistrationReviewPage.SummaryListRowXPath,
      "VAT number",
      "Change"
    ) shouldBe VatRegisteredUkController
      .reviewForm(atarService)
      .url
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

    test(controller.reviewDetails(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitForm(
    form: Map[String, String],
    userId: String = defaultUserId,
    userSelectedOrgType: Option[CdsOrganisationType] = None
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSession.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(userSelectedOrgType)

    test(controller.submitDetails(atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

  private def verifyRedirectToNextPageIn(result: Result) =
    status(result) shouldBe OK

  private def mockRegistrationDetailsBasedOnOrganisationType(orgType: CdsOrganisationType) =
    orgType match {
      case SoleTrader | Individual =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(individualRegistrationDetails)
      case Partnership =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(partnershipRegistrationDetails)
      case _ =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(organisationRegistrationDetails)
    }

}
