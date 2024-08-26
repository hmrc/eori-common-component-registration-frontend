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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{CheckYourDetailsRegisterController, routes}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Partnership, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionDetails, SubscriptionFlow}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegisterWithoutIdWithSubscriptionService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.CheckYourDetailsRegisterConstructor
import uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers.DateFormatter
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.check_your_details_register
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder.{incorporatedRegistrationDetails, individualRegistrationDetails, individualRegistrationDetailsNotIdentifiedByReg01, organisationRegistrationDetails, partnershipRegistrationDetails}
import util.builders.SubscriptionFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckYourDetailsRegisterControllerSpec
    extends ControllerSpec with BusinessDatesOrganisationTypeTables with ReviewPageOrganisationTypeTables
    with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                     = mock[AuthConnector]
  private val mockAuthAction                        = authAction(mockAuthConnector)
  private val mockSessionCache                      = mock[SessionCache]
  val dateFormatter: DateFormatter                  = instanceOf[DateFormatter]
  private val mockSubscriptionDetails               = mock[SubscriptionDetails]
  private val mockRegisterWithoutIdWithSubscription = mock[RegisterWithoutIdWithSubscriptionService]
  private val mockSubscriptionFlow                  = mock[SubscriptionFlow]
  private val mockVatControlListDetails             = mock[VatControlListResponse]
  private val mockRequestSession                    = mock[RequestSessionData]
  private val checkYourDetailsRegisterView          = instanceOf[check_your_details_register]
  private val mockSessionCacheService               = instanceOf[SessionCacheService]

  private val viewModelConstructor =
    new CheckYourDetailsRegisterConstructor(dateFormatter, mockSessionCache, mockRequestSession, mockSessionCacheService)

  val controller = new CheckYourDetailsRegisterController(
    mockAuthAction,
    mockRequestSession,
    mockSessionCacheService,
    mcc,
    checkYourDetailsRegisterView,
    mockRegisterWithoutIdWithSubscription,
    viewModelConstructor
  )(global)

  private val organisationRegistrationDetailsWithEmptySafeId = organisationRegistrationDetails.copy(safeId = SafeId(""))

  private val addressDetails =
    AddressViewModel(street = "street", city = "city", postcode = Some("SE28 1AA"), countryCode = "GB")

  private val NotEntered: String = "Not entered"

  override def beforeEach(): Unit = {
    reset(mockSessionCache)
    reset(mockSubscriptionDetails)
    reset(mockSubscriptionFlow)
    when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
      Future.successful(organisationRegistrationDetails)
    )
    when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(mockSubscriptionFlow)
    )
    when(mockSubscriptionDetails.ukVatDetails).thenReturn(None)
    when(mockSubscriptionDetails.businessShortName).thenReturn(None)
    when(mockSubscriptionDetails.dateEstablished).thenReturn(None)
    when(mockSubscriptionDetails.sicCode).thenReturn(None)
    when(mockSubscriptionDetails.nameDobDetails).thenReturn(None)
    when(mockSubscriptionDetails.addressDetails).thenReturn(Some(addressDetails))
    when(mockSubscriptionDetails.personalDataDisclosureConsent).thenReturn(Some(true))
    when(mockSubscriptionDetails.contactDetails).thenReturn(Some(contactUkDetailsModelWithMandatoryValuesOnly))
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(mockSubscriptionDetails))
    when(mockRequestSession.isPartnershipOrLLP(any[Request[AnyContent]])).thenReturn(false)
    when(mockSubscriptionDetails.vatVerificationOption).thenReturn(Some(true))
    when(mockSubscriptionDetails.vatControlListResponse).thenReturn(vatControlListResponseDetails)
    when(mockVatControlListDetails.dateOfReg).thenReturn(Some("2017-01-01"))
    when(mockSubscriptionDetails.nameOrganisationDetails).thenReturn(Some(NameOrganisationMatchModel("orgName")))
  }

  "Reviewing the details" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewDetails(atarService))

    "return ok when data has been provided" in {
      showForm() { result =>
        status(result) shouldBe OK
      }
    }

    "redirect to email controller for an organisation whose name wasn't entered" in {
      when(mockSubscriptionDetails.nameOrganisationDetails).thenReturn(None)
      when(mockSubscriptionDetails.name).thenReturn(None)
      showForm() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith(routes.EmailController.form(atarService).url)
      }
    }

    "display the sole trader name and dob from the cache when user has been identified by REG01" in {
      when(mockSubscriptionDetails.nameDobDetails)
        .thenReturn(Some(NameDobMatchModel("John", "Doe", LocalDate.parse("1980-07-23"))))
      when(mockSubscriptionDetails.name).thenReturn(Some("John Doe"))

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
      when(mockSubscriptionDetails.name).thenReturn(Some("John Doe"))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(individualRegistrationDetailsNotIdentifiedByReg01))

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

    "display the business name and six line address from the cache when user wasn't registered" in {
      when(mockSubscriptionDetails.nameOrganisationDetails).thenReturn(Some(NameOrganisationMatchModel("orgName")))
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(organisationRegistrationDetailsWithEmptySafeId))
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
      when(mockSubscriptionDetails.nameOrganisationDetails).thenReturn(Some(NameOrganisationMatchModel("orgName")))
      when(mockSubscriptionDetails.addressDetails)
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
      when(mockSubscriptionDetails.addressDetails)
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
        Future.successful(
          detailsHolderWithAllFields.copy(
            contactDetails = Some(contactDetailsModelWithAllValues),
            addressDetails = Some(addressDetails),
            nameDobDetails = Some(NameDobMatchModel("John", "Doe", LocalDate.parse("1980-07-23")))
          )
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
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier]))
          .thenReturn(Right(SubscriptionFlow("Organisation")))

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, labelText) shouldBe true
        }
      }

      val UtrLabelText = organisationType match {
        case LimitedLiabilityPartnership => "Corporation Tax Unique Taxpayer Reference (UTR)"
        case Partnership                 => "Partnership Self Assessment UTR"
        case _ =>
          "Corporation Tax UTR"
      }
      s"display $UtrLabelText label for ${organisationType.id}" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier]))
          .thenReturn(Right(SubscriptionFlow("Organisation")))
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
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
          Right(subscriptionFlow)
        )
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))

          page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Your address") shouldBe true
        }
      }
    }

    forAll(individualsOnlyOrganisationTypes) { organisationType =>
      s"should not display shortened name for ${organisationType.id}" in {
        when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier]))
          .thenReturn(Right(SubscriptionFlow("Individual")))
        mockRegistrationDetailsBasedOnOrganisationType(organisationType)

        showForm(userSelectedOrgType = organisationType) { result =>
          val page = CdsPage(contentAsString(result))
          page.elementIsPresent(RegistrationReviewPage.ShortNameXPath) shouldBe false
        }
      }
    }

    "display all fields when all are provided for an individual" in {
      when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
        Future.successful(individualRegistrationDetails)
      )
      val holder = detailsHolderWithAllFields.copy(
        contactDetails = Some(contactDetailsModelWithAllValues),
        dateEstablished = None,
        businessShortName = None,
        addressDetails = Some(addressDetails),
        nameDobDetails = Some(NameDobMatchModel("John", "Doe", LocalDate.parse("1980-07-23")))
      )
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(holder))

      showForm(isIndividualSubscriptionFlow = true, userSelectedOrgType = ThirdCountrySoleTrader) { result =>
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
      .thenReturn(Future.successful(incorporatedRegistrationDetails.copy(customsId = Some(Utr("7280616009")))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameOrganisationDetails = Some(NameOrganisationMatchModel("orgName"))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(holder))

    showForm(userSelectedOrgType = Company) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))
      page.title() should startWith("Check your answers")

      page.h2() should startWith("Company details VAT details Contact details Declaration Support links")

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
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of VAT registration"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of VAT registration"
      ) shouldBe "1 January 2017"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Show name and address on the 'Check an EORI number' service")
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service"
      ) shouldBe "Yes"
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service",
        "Change"
      ) shouldBe "/customs-registration-services/atar/register/disclose-personal-details-consent/review"
      page.getElementsText(
        SubscriptionExistingDetailsReviewPage.ConfirmAndRegisterInfoXpath
      ) shouldBe "You confirm that, to the best of your knowledge, the details you are providing are correct."
    }
  }

  "display the review page check-your-details for LLP" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(incorporatedRegistrationDetails.copy(customsId = Some(Utr("7280616009")))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameOrganisationDetails = Some(NameOrganisationMatchModel("orgName"))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(holder))

    showForm(userSelectedOrgType = LimitedLiabilityPartnership) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))
      page.title() should startWith("Check your answers")

      page.h2() should startWith("Partnership details VAT details Contact details Declaration Support links")

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
        "Corporation Tax Unique Taxpayer Reference (UTR)"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Corporation Tax Unique Taxpayer Reference (UTR)"
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
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of VAT registration"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Date of VAT registration"
      ) shouldBe "1 January 2017"
      page.getSummaryListLink(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service",
        "Change"
      ) shouldBe SubscriptionExistingDetailsReviewPage
        .changeAnswerText("Show name and address on the 'Check an EORI number' service")
      page.summaryListElementPresent(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service"
      ) shouldBe true
      page.getSummaryListValue(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service"
      ) shouldBe "Yes I want my partnership name and address on the EORI checker"
      page.getSummaryListHref(
        RegistrationReviewPage.SummaryListRowXPath,
        "Show name and address on the 'Check an EORI number' service",
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
      .thenReturn(Future.successful(individualRegistrationDetails.copy(customsId = Some(Nino(expectedNino)))))
    val holder = detailsHolderWithAllFields.copy(
      contactDetails = Some(contactDetailsModelWithAllValues),
      dateEstablished = Some(LocalDate.parse("1980-07-23")),
      addressDetails = Some(addressDetails),
      nameDobDetails = Some(NameDobMatchModel("John", "Doe", LocalDate.parse("1980-07-23")))
    )
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(holder))

    showForm(userSelectedOrgType = Individual, isIndividualSubscriptionFlow = true) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.h2() should startWith("Your details Contact details Declaration Support links")

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
      .thenReturn(Future.successful(organisationRegistrationDetailsWithEmptySafeId))

    showForm(userSelectedOrgType = CdsOrganisationType.ThirdCountryOrganisation) { result =>
      val page: CdsPage = CdsPage(contentAsString(result))

      page.summaryListElementPresent(RegistrationReviewPage.SummaryListRowXPath, "Corporation Tax UTR") shouldBe true
      page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "Corporation Tax UTR") shouldBe NotEntered
    }
  }

  "VAT details" should {
    "display only UK vat details when found in cache" in {
      when(mockSubscriptionDetails.ukVatDetails).thenReturn(gbVatDetails)
      mockRegistrationDetailsBasedOnOrganisationType(Individual)

      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        assertUkVatDetailsShowValues(page)
      }
    }
  }

  "submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submitDetails(atarService))

    "redirect to next screen" in {
      when(
        mockRegisterWithoutIdWithSubscription
          .rowRegisterWithoutIdWithSubscription(any(), any())(any[HeaderCarrier], any())
      ).thenReturn(Future.successful(Results.Ok))
      submitForm(Map.empty)(res => status(res) shouldBe OK)
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

  private def assertUkVatDetailsShowValues(page: CdsPage): Unit = {
    //VAT number
    page.getSummaryListValue(RegistrationReviewPage.SummaryListRowXPath, "VAT number") shouldBe "123456789"
    page.getSummaryListValue(
      RegistrationReviewPage.SummaryListRowXPath,
      "Postcode of your VAT registration address"
    ) shouldBe "SE28 1AA"
    page.getSummaryListValue(
      RegistrationReviewPage.SummaryListRowXPath,
      "Date of VAT registration"
    ) shouldBe "1 January 2017"
  }

  def showForm(
    userSelectedOrgType: CdsOrganisationType = CdsOrganisationType.Company,
    userId: String = defaultUserId,
    isIndividualSubscriptionFlow: Boolean = false
  )(test: Future[Result] => Any): Unit = {
    val controller = new CheckYourDetailsRegisterController(
      mockAuthAction,
      mockRequestSession,
      mockSessionCacheService,
      mcc,
      checkYourDetailsRegisterView,
      mockRegisterWithoutIdWithSubscription,
      viewModelConstructor
    )(global)

    withAuthorisedUser(userId = userId, mockAuthConnector = mockAuthConnector, groupId = Some("groupId"))

    when(mockRequestSession.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(userSelectedOrgType))
    if (
      userSelectedOrgType.id == CdsOrganisationType.PartnershipId || userSelectedOrgType.id == CdsOrganisationType.LimitedLiabilityPartnershipId
    )
      when(mockRequestSession.isPartnershipOrLLP(any[Request[AnyContent]])).thenReturn(true)

    when(mockSubscriptionFlow.isIndividualFlow).thenReturn(isIndividualSubscriptionFlow)

    test(controller.reviewDetails(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitForm(
    form: Map[String, String],
    userId: String = defaultUserId,
    userSelectedOrgType: Option[CdsOrganisationType] = None
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSession.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(userSelectedOrgType)

    test(controller.submitDetails(atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

  private def mockRegistrationDetailsBasedOnOrganisationType(orgType: CdsOrganisationType) =
    orgType match {
      case SoleTrader | Individual =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(individualRegistrationDetails)
        )
      case Partnership =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(partnershipRegistrationDetails)
        )
      case _ =>
        when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
          Future.successful(organisationRegistrationDetails)
        )
    }

}
