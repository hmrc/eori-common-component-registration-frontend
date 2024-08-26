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

import common.pages.matching.AddressPageFactoring._
import common.pages.subscription.AddressPage
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{SixLineAddressController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionPage
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Country
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RegistrationDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.six_line_address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder.defaultAddress
import util.builders.matching._
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SixLineAddressControllerSpec
    extends ControllerSpec with BeforeAndAfter with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                   = mock[AuthConnector]
  private val mockAuthAction                      = authAction(mockAuthConnector)
  private val mockRegistrationDetailsCreator      = mock[RegistrationDetailsCreator]
  private val mockSubscriptionFlowManager         = mock[SubscriptionFlowManager]
  private val mockSessionCache                    = mock[SessionCache]
  private val mockRequestSessionData              = mock[RequestSessionData]
  private val mockRegistrationDetailsOrganisation = mock[RegistrationDetailsOrganisation]
  private val mockRegistrationDetailsIndividual   = mock[RegistrationDetailsIndividual]
  private val mockRegistrationDetailsService      = mock[RegistrationDetailsService]
  private val mockSubscriptionDetailsService      = mock[SubscriptionDetailsService]
  private val sixLineAddressView                  = instanceOf[six_line_address]
  private val mockSessionCacheService             = instanceOf[SessionCacheService]

  private val controller = new SixLineAddressController(
    mockAuthAction,
    mockRegistrationDetailsCreator,
    mockSubscriptionFlowManager,
    mockSessionCache,
    mockRequestSessionData,
    mcc,
    sixLineAddressView,
    mockRegistrationDetailsService,
    mockSessionCacheService
  )(global)

  private val mockSubscriptionPage         = mock[SubscriptionPage]
  private val mockSubscriptionStartSession = mock[Session]
  private val mockFlowStart                = (mockSubscriptionPage, mockSubscriptionStartSession)

  private val testSessionData              = Map[String, String]("some_session_key" -> "some_session_value")
  private val testSubscriptionStartPageUrl = "some_page_url"

  private val LineOne     = "Address line 1"
  private val LineTwo     = "Address line 2 (optional)"
  private val LineThree   = "Town or city"
  private val LineFour    = "Region or state (optional)"
  private val Postcode    = "Postcode"
  private val testAddress = Address(LineOne, Some(LineTwo), Some(LineThree), Some(LineFour), Some(Postcode), "FR")

  val organisationTypesData = Table(
    ("Organisation Type", "Form Builder", "Form", "reviewMode", "expectedRedirectURL"),
    ("third-country-organisation", RowFormBuilder, thirdCountrySixLineAddressForm, false, testSubscriptionStartPageUrl),
    ("third-country-individual", RowFormBuilder, thirdCountrySixLineAddressForm, false, testSubscriptionStartPageUrl),
    ("third-country-sole-trader", RowFormBuilder, thirdCountrySixLineAddressForm, false, testSubscriptionStartPageUrl),
    (
      "third-country-organisation",
      RowFormBuilder,
      thirdCountrySixLineAddressForm,
      true,
      "/customs-registration-services/atar/register/matching/review-determine"
    ),
    (
      "charity-public-body-not-for-profit",
      RowFormBuilder,
      ukSixLineAddressForm,
      true,
      "/customs-registration-services/atar/register/matching/review-determine"
    )
  )

  val aFewCountries = List(
    Country("France", "country:FR"),
    Country("Germany", "country:DE"),
    Country("Italy", "country:IT"),
    Country("Japan", "country:JP")
  )

  override def beforeEach(): Unit = {
    when(mockSubscriptionPage.url(atarService)).thenReturn(testSubscriptionStartPageUrl)
    when(mockSubscriptionStartSession.data).thenReturn(testSessionData)
    when(
      mockSubscriptionFlowManager
        .startSubscriptionFlow(any[Service])(any[Request[AnyContent]])
    ).thenReturn(Future.successful(mockFlowStart))
    when(mockRegistrationDetailsCreator.registrationAddress(any())).thenReturn(testAddress)
    when(mockSessionCache.saveRegistrationDetails(any[RegistrationDetails]())(any[Request[_]]()))
      .thenReturn(Future.successful(true))
    when(mockRegistrationDetailsService.cacheAddress(any())(any[Request[_]])).thenReturn(Future.successful(true))
    when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(
      Some(UserLocation.ThirdCountry)
    )
  }

  forAll(organisationTypesData) { (organisationType, formBuilder, form, reviewMode, expectedRedirectURL) =>
    val formValues = formBuilder.asForm(form)

    organisationType match {
      case "third-country-organisation" | "charity-public-body-not-for-profit" =>
        when(mockSessionCache.registrationDetails(any[Request[_]]))
          .thenReturn(Future.successful(mockRegistrationDetailsOrganisation))
        when(mockRegistrationDetailsOrganisation.address).thenReturn(testAddress)
      case _ =>
        when(mockSessionCache.registrationDetails(any[Request[_]]))
          .thenReturn(Future.successful(mockRegistrationDetailsIndividual))
        when(mockRegistrationDetailsIndividual.name).thenReturn("Test individual name")
    }

    info(s"General checks for [$organisationType]")

    s"loading the page for [$organisationType] and reviewMode is [$reviewMode]" should {

      assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
        mockAuthConnector,
        controller.showForm(reviewMode, organisationType, atarService),
        s", for reviewMode [$reviewMode] and organisationType $organisationType"
      )

      s"show the form without errors when user hasn't been registered yet and organisationType is [$organisationType], and reviewMode is [$reviewMode]" in {
        showForm(organisationType)(Map.empty) { result =>
          status(result) shouldBe OK
          val page = CdsPage(contentAsString(result))
          page.getElementsText(PageLevelErrorSummaryListXPath) shouldBe empty
        }
      }
    }

    if (!reviewMode) {
      s"address Line 1 for [$organisationType]" should {

        "be mandatory" in {
          assertInvalidField(organisationType)(formValues + ("line-1" -> ""))(
            fieldLevelErrorAddressLineOne,
            "Enter the first line of your address"
          )
        }

        "be restricted to 35 characters" in {
          assertInvalidField(organisationType)(formValues + ("line-1" -> oversizedString(35)))(
            fieldLevelErrorAddressLineOne,
            "The first line of the address must be 35 characters or less"
          )
        }
      }

      s"address line 2 for [$organisationType]" should {

        "be optional with empty string submitted" in {
          assertValidFormSubmit(organisationType)(formValues + ("line-2" -> ""))
        }

        "be optional without input field submitted" in {
          assertValidFormSubmit(organisationType)(formValues - "line-2")
        }

        "be restricted to 34 characters" in {
          assertInvalidField(organisationType)(formValues + ("line-2" -> oversizedString(34)))(
            fieldLevelErrorAddressLineTwo,
            "The second line of the address must be 34 characters or less"
          )
        }
      }

      s"address line 3 for [$organisationType]" should {

        "be mandatory" in {
          assertInvalidField(organisationType)(formValues + ("line-3" -> ""))(
            fieldLevelErrorAddressLineThree,
            "Enter your town or city"
          )
        }

        "be restricted to 35 characters" in {
          assertInvalidField(organisationType)(formValues + ("line-3" -> oversizedString(35)))(
            fieldLevelErrorAddressLineThree,
            "The town or city must be 35 characters or less"
          )
        }
      }

      s"address line 4 for [$organisationType]" should {

        "be optional with empty string submitted" in {
          assertValidFormSubmit(organisationType)(formValues + ("line-4" -> ""))
        }

        "be optional without input field submitted" in {
          assertValidFormSubmit(organisationType)(formValues - "line-4")
        }

        "be restricted to 35 characters" in {
          assertInvalidField(organisationType)(formValues + ("line-4" -> oversizedString(35)))(
            fieldLevelErrorAddressLineFour,
            "The Region or state must be 35 characters or less"
          )
        }
      }

      s"country for [$organisationType]" should {

        "be mandatory" in {
          assertInvalidField(organisationType)(formValues - "countryCode")(
            fieldLevelErrorCountry,
            messages("cds.matching-error.country.invalid")
          )
        }

        "be non-empty" in {
          assertInvalidField(organisationType)(formValues + ("countryCode" -> ""))(
            fieldLevelErrorCountry,
            messages("cds.matching-error.country.invalid")
          )
        }

        "be at least 2 characters long" in {
          assertInvalidField(organisationType)(formValues + ("countryCode" -> undersizedString(2)))(
            fieldLevelErrorCountry,
            messages("cds.matching-error.country.invalid")
          )
        }

        "be at most 2 characters long" in {
          assertInvalidField(organisationType)(formValues + ("countryCode" -> oversizedString(2)))(
            fieldLevelErrorCountry,
            messages("cds.matching-error.country.invalid")
          )
        }
      }
    }

    s"submitting a valid form for [$organisationType], and reviewMode is [$reviewMode]" should {

      assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
        mockAuthConnector,
        controller.submit(reviewMode, organisationType, atarService),
        s", for reviewMode [$reviewMode] and organisationType $organisationType"
      )

      s"redirect to the next page when organisationType is [$organisationType], and reviewMode is [$reviewMode]" in {
        submitForm(organisationType, reviewMode)(formValues) { result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(PageLevelErrorSummaryListXPath) shouldBe empty

          status(result) shouldBe SEE_OTHER
          header(LOCATION, result).value shouldBe expectedRedirectURL
        }
      }
    }
  }

  "postcode for third-country-organisation" should {
    "be accepted when no value provided" in {
      val formValues = EUOrganisationFormBuilder.asForm(thirdCountrySixLineAddressForm)
      assertValidFormSubmit("third-country-organisation")(formValues + ("postcode" -> ""))
    }
  }

  "postcode for third-country-organisation" should {
    "be restricted to 9 characters" in {
      val formValues = RowFormBuilder.asForm(thirdCountrySixLineAddressForm)
      assertInvalidField("third-country-organisation")(formValues + ("postcode" -> oversizedString(9)))(
        fieldLevelErrorPostcode,
        "The postcode must be 9 characters or less"
      )
    }
  }

  "postcode for third-country-organisation from the Channel Islands" should {
    "be mandatory for Jersey" in {
      val formValues = RowFormBuilder.asForm(thirdCountrySixLineAddressForm)
      assertInvalidField("third-country-organisation")(formValues ++ Map("postcode" -> "", "countryCode" -> "JE"))(
        fieldLevelErrorPostcode,
        "Enter a valid postcode"
      )
    }
    "be mandatory for Guernsey" in {
      val formValues = RowFormBuilder.asForm(thirdCountrySixLineAddressForm)
      assertInvalidField("third-country-organisation")(formValues ++ Map("postcode" -> "", "countryCode" -> "GG"))(
        fieldLevelErrorPostcode,
        "Enter a valid postcode"
      )
    }

  }

  "country for third-country-organisation" should {
    "reject country code GB" in {
      val formValues = RowFormBuilder.asForm(thirdCountrySixLineAddressForm)
      assertInvalidField("third-country-organisation")(formValues + ("countryCode" -> "GB"))(
        fieldLevelErrorCountry,
        "The entered country is not acceptable"
      )
    }
  }

  "submitting the form for Registration journey" should {

    "throw an error when UTR is provided and GB is entered" in {
      submitFormInCreateModeForIndividualRegistration(
        Map(
          "line-1"      -> "My street",
          "line-2"      -> "My street 2",
          "line-3"      -> "My city",
          "line-4"      -> "My region",
          "postcode"    -> "SE28 1BG",
          "countryCode" -> "GB"
        )
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          AddressPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The entered country is not acceptable"

        page.getElementsText(
          AddressPage.countryFieldLevelErrorXPath
        ) shouldBe "Error: The entered country is not acceptable"
      }
    }
  }

  private def submitFormInCreateModeForIndividualRegistration(
    form: Map[String, String],
    userId: String = defaultUserId
  )(test: Future[Result] => Any): Unit = {
    val individualRegistrationDetails = RegistrationDetails.individual(
      sapNumber = "0123456789",
      safeId = SafeId("safe-id"),
      name = "John Doe",
      address = defaultAddress,
      dateOfBirth = LocalDate.parse("1980-07-23"),
      customsId = None
    )

    withAuthorisedUser(userId, mockAuthConnector)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(CdsOrganisationType.ThirdCountryIndividual))
    when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(
      Future.successful(individualRegistrationDetails)
    )
    when(mockSubscriptionDetailsService.cachedCustomsId(any[Request[_]])).thenReturn(Future.successful(None))

    test(
      controller.submit(false, CdsOrganisationType.ThirdCountryIndividualId, atarService)(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  def assertValidFormSubmit(cdsOrgType: String)(formValues: Map[String, String]): Unit =
    submitForm(cdsOrgType)(formValues) { result =>
      status(result) shouldBe SEE_OTHER
      val page = CdsPage(contentAsString(result))
      page.getElementsText(PageLevelErrorSummaryListXPath) shouldBe empty
    }

  def assertInvalidField(
    cdsOrgType: String
  )(formValues: Map[String, String])(fieldLevelErrorXPath: String, errorMessage: String): Result =
    submitForm(cdsOrgType)(formValues) { result =>
      status(result) shouldBe BAD_REQUEST
      val page = CdsPage(contentAsString(result))
      page.getElementsText(PageLevelErrorSummaryListXPath) shouldBe errorMessage
      page.getElementsText(fieldLevelErrorXPath) shouldBe s"Error: $errorMessage"
      page.getElementsText("title") should startWith("Error: ")
      await(result)
    }

  def showForm[T](
    cdsOrgType: String,
    reviewMode: Boolean = false
  )(formValues: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => T): T = {
    withAuthorisedUser(userId, mockAuthConnector)

    test(
      controller
        .showForm(reviewMode, cdsOrgType, atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, formValues))
    )
  }

  def submitForm[T](
    cdsOrgType: String,
    reviewMode: Boolean = false
  )(formValues: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => T): T = {
    withAuthorisedUser(userId, mockAuthConnector)

    test(
      controller
        .submit(reviewMode, cdsOrgType, atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, formValues))
    )
  }

}
