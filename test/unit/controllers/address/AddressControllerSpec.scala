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

package unit.controllers.address

import common.pages.subscription.AddressPage
import common.support.testdata.subscription.BusinessDatesOrganisationTypeTables
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.AddressController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, RegistrationDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Country
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.address
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.StringThings._
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressControllerSpec
    extends ControllerSpec with BusinessDatesOrganisationTypeTables with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector               = mock[AuthConnector]
  private val mockAuthAction                  = authAction(mockAuthConnector)
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockRequestSessionData          = mock[RequestSessionData]
  private val mockCdsFrontendDataCache        = mock[SessionCache]
  private val mockSubscriptionDetailsService  = mock[SubscriptionDetailsService]
  private val emulatedFailure                 = new UnsupportedOperationException("Emulation of service call failure")
  private val mockOrganisationType            = mock[CdsOrganisationType]

  private val viewAddress = instanceOf[address]

  private val controller = new AddressController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockCdsFrontendDataCache,
    mockRequestSessionData,
    mockSubscriptionDetailsService,
    mcc,
    viewAddress
  )

  def stringOfLengthXGen(minLength: Int): Gen[String] =
    for {
      single: Char       <- Gen.alphaNumChar
      baseString: String <- Gen.listOfN(minLength, Gen.alphaNumChar).map(c => c.mkString)
      additionalEnding   <- Gen.alphaStr
    } yield single + baseString + additionalEnding

  val mandatoryFields      = Map("city" -> "city", "street" -> "street", "postcode" -> "SE28 1AA", "countryCode" -> "GB")
  val mandatoryFieldsEmpty = Map("city" -> "", "street" -> "", "postcode" -> "", "countryCode" -> "")

  val aFewCountries = List(
    Country("France", "country:FR"),
    Country("Germany", "country:DE"),
    Country("Italy", "country:IT"),
    Country("Japan", "country:JP")
  )

  override def beforeEach: Unit = {
    super.beforeEach()

    when(mockSubscriptionBusinessService.address(any[HeaderCarrier])).thenReturn(None)
    when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier])).thenReturn(None)
    when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.successful((): Unit))
    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)
    when(mockCdsFrontendDataCache.saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))
    when(mockRequestSessionData.mayBeUnMatchedUser(any[Request[AnyContent]])).thenReturn(None)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(mockOrganisationType))
    registerSaveDetailsMockSuccess()
  }

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionBusinessService, mockRequestSessionData, mockSubscriptionDetailsService)

    super.afterEach()
  }

  "Subscription Address Controller form in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display title as 'Enter your business address'" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Enter your organisation address")
      }
    }

    "have Address input field without data if not cached previously" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        verifyAddressFieldExistsWithNoData(page)
      }
    }

    "have Address input field prepopulated if cached previously" in {
      when(mockSubscriptionBusinessService.address(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(AddressPage.filledValues)))
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        verifyAddressFieldExistsAndPopulatedCorrectly(page)
      }
    }

  }

  "Subscription Address Controller form in create mode for Individual" should {

    "display title as 'Enter your address'" in {
      showCreateForm(userSelectedOrganisationType = Some(CdsOrganisationType.Individual)) { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Enter your address")
        page.h1() shouldBe "Enter your address"
      }
    }
  }

  "submitting the form with all mandatory fields filled when in create mode for organisation type" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService)
    )

    "wait until the saveSubscriptionDetailsHolder is completed before progressing" in {
      registerSaveDetailsMockFailure(emulatedFailure)

      val caught = intercept[RuntimeException] {
        submitFormInCreateModeForOrganisation(mandatoryFields) { result =>
          await(result)
        }
      }

      caught shouldEqual emulatedFailure
    }

    "redirect to next screen" in {
      submitFormInCreateModeForOrganisation(mandatoryFields) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/customs-registration-services/atar/register/matching/confirm")
      }
    }
  }

  "submitting the form with all mandatory fields filled for individual type" should {

    "redirect to next screen" in {
      submitFormInCreateModeForIndividual(mandatoryFields) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/customs-registration-services/atar/register/matching/confirm")
      }
    }
  }

  "submitting the form with all mandatory fields filled when in review mode for all organisation types" should {

    "wait until the saveSubscriptionDetailsHolder is completed before progressing" in {

      registerSaveDetailsMockFailure(emulatedFailure)
      the[UnsupportedOperationException] thrownBy {
        submitFormInReviewMode(mandatoryFields)(await)
      } should have message emulatedFailure.getMessage
    }

    "redirect to review screen" in {
      submitFormInReviewMode(mandatoryFields, userSelectedOrgType = Some(mockOrganisationType)) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
          .determineRoute(atarService)
          .url
      }
    }
  }

  "Address Detail form" should {
    "be mandatory" in {
      submitFormInCreateModeForOrganisation(mandatoryFieldsEmpty) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) should include(
          "Enter the first line of your address"
        )
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) should include("Enter your town or city")
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) should include("Enter your country")

        page.getElementsText(
          AddressPage.streetFieldLevelErrorXPath
        ) shouldBe "Error: Enter the first line of your address"
        page.getElementsText(AddressPage.cityFieldLevelErrorXPath) shouldBe "Error: Enter your town or city"
        page.getElementsText(AddressPage.countryFieldLevelErrorXPath) shouldBe "Error: Enter your country"
      }
    }

    "have postcode mandatory when country is GB" in {
      submitFormInCreateModeForOrganisation(
        Map("street" -> "My street", "city" -> "My city", "postcode" -> "", "countryCode" -> "GB")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) shouldBe "Enter a valid postcode"

        page.getElementsText(AddressPage.postcodeFieldLevelErrorXPath) shouldBe "Error: Enter a valid postcode"
      }
    }

    "have postcode mandatory when country is a channel island" in {
      submitFormInCreateModeForOrganisation(
        Map("street" -> "My street", "city" -> "My city", "postcode" -> "", "countryCode" -> "JE")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) shouldBe "Enter a valid postcode"
        page.getElementsText(AddressPage.postcodeFieldLevelErrorXPath) shouldBe "Error: Enter a valid postcode"
      }
    }

    "not allow spaces to satisfy minimum length requirements" in {
      submitFormInCreateModeForOrganisation(
        Map(
          "city"   -> 10.spaces,
          "street" -> 7.spaces // we allow spaces for postcode
        )
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) should include(
          "Enter the first line of your address"
        )
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) should include("Enter your town or city")

        page.getElementsText(
          AddressPage.streetFieldLevelErrorXPath
        ) shouldBe "Error: Enter the first line of your address"
        page.getElementsText(AddressPage.cityFieldLevelErrorXPath) shouldBe "Error: Enter your town or city"
      }
    }

    "be restricted to 70 character for street validation only" in {
      val streetLine = stringOfLengthXGen(70)
      submitFormInCreateModeForOrganisation(mandatoryFields ++ Map("street" -> streetLine.sample.get)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          AddressPage.streetFieldLevelErrorXPath
        ) shouldBe "Error: The first line of the address must be 70 characters or less"
      }
    }

    "be restricted to 35 character for city validation only" in {
      val city = stringOfLengthXGen(35)
      submitFormInCreateModeForOrganisation(mandatoryFields ++ Map("city" -> city.sample.get)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          AddressPage.cityFieldLevelErrorXPath
        ) shouldBe "Error: The town or city must be 35 characters or less"

      }
    }

    "be validating postcode length to 8 when country is a channel island" in {
      submitFormInCreateModeForOrganisation(
        Map("street" -> "My street", "city" -> "City", "postcode" -> "123456789", "countryCode" -> "JE")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(AddressPage.pageLevelErrorSummaryListXPath) shouldBe "Enter a valid postcode"
        page.getElementsText(AddressPage.postcodeFieldLevelErrorXPath) shouldBe "Error: Enter a valid postcode"
      }
    }

    "be validating postcode length to 9 when country is not GB" in {
      submitFormInCreateModeForOrganisation(
        Map("street" -> "My street", "city" -> "City", "postcode" -> "1234567890", "countryCode" -> "FR")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          AddressPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The postcode must be 9 characters or less"
        page.getElementsText(
          AddressPage.postcodeFieldLevelErrorXPath
        ) shouldBe "Error: The postcode must be 9 characters or less"
      }
    }
  }

  private def submitFormInCreateModeForOrganisation(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)

    test(
      controller.submit(isInReviewMode = false, atarService)(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  private def submitFormInCreateModeForIndividual(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(individualRegistrationDetails)

    test(
      controller.submit(isInReviewMode = false, atarService)(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  private def submitFormInReviewMode(
    form: Map[String, String],
    userId: String = defaultUserId,
    userSelectedOrgType: Option[CdsOrganisationType] = None
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(CdsOrganisationType("company")))

    test(
      controller.submit(isInReviewMode = true, atarService)(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  private def registerSaveDetailsMockSuccess() {
    when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
  }

  private def registerSaveDetailsMockFailure(exception: Throwable) {
    when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(exception))
  }

  private def showCreateForm(
    userId: String = defaultUserId,
    userSelectedOrganisationType: Option[CdsOrganisationType] = None
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSessionData.isIndividualOrSoleTrader(any[Request[AnyContent]]))
      .thenReturn(isIndividual(userSelectedOrganisationType))
    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def verifyAddressFieldExistsAndPopulatedCorrectly(page: CdsPage): Unit = {
    page.getElementValue(AddressPage.cityFieldXPath) shouldBe "city name"
    page.getElementValue(AddressPage.streetFieldXPath) shouldBe "Line 1"
    page.getElementValue(AddressPage.postcodeFieldXPath) shouldBe "SE28 1AA"
  }

  private def verifyAddressFieldExistsWithNoData(page: CdsPage): Unit = {
    page.getElementValue(AddressPage.cityFieldXPath) shouldBe empty
    page.getElementValue(AddressPage.streetFieldXPath) shouldBe empty
    page.getElementValue(AddressPage.postcodeFieldXPath) shouldBe empty
    page.getElementValue(AddressPage.countryCodeFieldXPath) shouldBe empty
  }

  private def isIndividual(userSelectedOrganisationType: Option[CdsOrganisationType]) =
    userSelectedOrganisationType.contains(CdsOrganisationType.Individual)

}
