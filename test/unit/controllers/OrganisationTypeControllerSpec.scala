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

package unit.controllers

import common.pages.EuOrgOrIndividualPage
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result, Session}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{OrganisationTypeController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.OrganisationTypeDetailsFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, RequestSessionDataKeys}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RegistrationDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.organisation_type
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrganisationTypeControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  private val mockRegistrationDetailsService = mock[RegistrationDetailsService]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockAppConfig = mock[AppConfig]
  private val mockOrganisationTypeDetailsFormProviderSpec = mock[OrganisationTypeDetailsFormProvider]
  when(mockOrganisationTypeDetailsFormProviderSpec.form()).thenReturn(new OrganisationTypeDetailsFormProvider().form())

  private val organisationTypeView = inject[organisation_type]

  private val organisationTypeController = new OrganisationTypeController(
    mockAuthAction,
    mockRequestSessionData,
    mcc,
    organisationTypeView,
    mockRegistrationDetailsService,
    mockSubscriptionDetailsService,
    mockAppConfig,
    mockOrganisationTypeDetailsFormProviderSpec
  )

  private val ProblemWithSelectionError = "Select what you want to apply as"
  private val thirdCountryOrganisationXpath = "//*[@id='organisation-type-third-country-organisation']"
  private val thirdCountrySoleTraderXpath = "//*[@id='organisation-type-third-country-sole-trader']"
  private val thirdCountryIndividualXpath = "//*[@id='organisation-type-third-country-individual']"
  private val companyXpath = "//*[@id='organisation-type-company']"
  private val soleTraderXpath = "//*[@id='organisation-type-sole-trader']"
  private val individualXpath = "//*[@id='organisation-type-individual']"

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
    when(
      mockRegistrationDetailsService
        .initialiseCacheWithRegistrationDetails(any[CdsOrganisationType]())(any[Request[_]])
    ).thenReturn(Future.successful(true))
    when(mockSubscriptionDetailsService.cachedOrganisationType(any())).thenReturn(Future.successful(None))
  }

  override protected def afterEach(): Unit = {
    reset(mockRequestSessionData)
    reset(mockRegistrationDetailsService)
    reset(mockSubscriptionDetailsService)
    super.afterEach()
  }

  "Displaying the form" should {

    val userLocations =
      Table(UserLocation.Uk, UserLocation.ThirdCountry)

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, organisationTypeController.form(atarService))

    forAll(userLocations) { userLocation =>
      s"show correct options when user has selected location of $userLocation" in {
        showFormWithAuthenticatedUser(userLocation = Some(userLocation)) { result =>
          status(result) shouldBe OK
          val includeUk = userLocation.toString == UserLocation.Uk.toString
          val includeThirdCountry = userLocation.toString == UserLocation.ThirdCountry.toString
          val page = CdsPage(contentAsString(result))
          page.elementIsPresent(companyXpath) shouldBe includeUk
          page.elementIsPresent(soleTraderXpath) shouldBe includeUk
          page.elementIsPresent(individualXpath) shouldBe includeUk
          page.elementIsPresent(thirdCountryOrganisationXpath) shouldBe includeThirdCountry
          page.elementIsPresent(thirdCountrySoleTraderXpath) shouldBe includeThirdCountry
          page.elementIsPresent(thirdCountryIndividualXpath) shouldBe includeThirdCountry
        }
      }
    }

    "redirect status code when user has not selected a location" in {
      showFormWithAuthenticatedUser(userLocation = None) { result =>
        status(result) shouldBe OK
      }
    }

  }

  "Submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      organisationTypeController.submit(atarService)
    )

    "ensure an organisation type has been selected" in {
      submitForm(Map.empty) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(EuOrgOrIndividualPage.pageLevelErrorSummaryListXPath) shouldBe ProblemWithSelectionError
        page.getElementsText(
          EuOrgOrIndividualPage.fieldLevelErrorOrganisationType
        ) shouldBe s"Error: $ProblemWithSelectionError"
      }
    }

    val urlParameters =
      Table[CdsOrganisationType, String](
        ("option", "urlParameter"),
        (CdsOrganisationType.Company, "company"),
        (CdsOrganisationType.SoleTrader, "name-date-of-birth/sole-trader"),
        (CdsOrganisationType.Individual, "name-date-of-birth/individual"),
        (CdsOrganisationType.ThirdCountryOrganisation, "name/third-country-organisation"),
        (CdsOrganisationType.ThirdCountrySoleTrader, "row-name-date-of-birth/third-country-sole-trader"),
        (CdsOrganisationType.ThirdCountryIndividual, "row-name-date-of-birth/third-country-individual"),
        (CdsOrganisationType.CharityPublicBodyNotForProfit, "name/charity-public-body-not-for-profit")
      )

    val subscriptionPage: Map[CdsOrganisationType, SubscriptionPage] = Map(
      (CdsOrganisationType.Company, DateOfEstablishmentSubscriptionFlowPage),
      (CdsOrganisationType.SoleTrader, ContactDetailsSubscriptionFlowPageGetEori),
      (CdsOrganisationType.Individual, ContactDetailsSubscriptionFlowPageGetEori),
      (CdsOrganisationType.ThirdCountryOrganisation, DateOfEstablishmentSubscriptionFlowPage),
      (CdsOrganisationType.ThirdCountrySoleTrader, ContactDetailsSubscriptionFlowPageGetEori),
      (CdsOrganisationType.ThirdCountryIndividual, ContactDetailsSubscriptionFlowPageGetEori),
      (CdsOrganisationType.CharityPublicBodyNotForProfit, DateOfEstablishmentSubscriptionFlowPage)
    )

    forAll(urlParameters) { (cdsOrganisationType, urlParameter) =>
      val option: String = cdsOrganisationType.id
      val page = subscriptionPage(cdsOrganisationType)

      s"return a redirect to the matching form for the correct organisation type when '$option' is selected" in {
        val updatedMockSession =
          Session(Map()) + (RequestSessionDataKeys.selectedOrganisationType -> option)

        when(mockAppConfig.allowNoIdJourney).thenReturn(true)

        when(
          mockSubscriptionFlowManager
            .startSubscriptionFlow(any(), any(), any[Service])(any[Request[AnyContent]]())
        ).thenReturn(Future.successful((page, updatedMockSession)))

        submitForm(Map("organisation-type" -> option), organisationType = Some(cdsOrganisationType)) { result =>
          status(result) shouldBe SEE_OTHER
          header(LOCATION, result).value should endWith(
            s"/customs-registration-services/atar/register/matching/$urlParameter"
          )
        }
      }

      s"store the correct organisation type when '$option' is selected" in {
        submitForm(Map("organisation-type" -> option), organisationType = Some(cdsOrganisationType)) { result =>
          await(result) // this is needed to ensure the future is completed before the verify is called
          verify(mockRequestSessionData).sessionWithOrganisationTypeAdded(ArgumentMatchers.eq(cdsOrganisationType))(
            any[Request[AnyContent]]
          )
        }
      }
    }

    s"throw an exception when initialiseCacheWithRegistrationDetails returns `false`" in {
      when(
        mockRegistrationDetailsService
          .initialiseCacheWithRegistrationDetails(any[CdsOrganisationType]())(any[Request[_]])
      ).thenReturn(Future.successful(false))
      intercept[IllegalStateException](
        submitForm(
          Map("organisation-type" -> CdsOrganisationType.Company.id),
          organisationType = Some(CdsOrganisationType.Company)
        ) { result =>
          await(result) // this is needed to ensure the future is completed before the verify is called
        }
      )
    }
  }

  def showFormWithAuthenticatedUser(
    userId: String = defaultUserId,
    userLocation: Option[UserLocation] = Some(UserLocation.Uk)
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(userLocation)

    test(organisationTypeController.form(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  def showFormWithUnauthenticatedUser(test: Future[Result] => Any): Unit = {
    withNotLoggedInUser(mockAuthConnector)

    test(organisationTypeController.form(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser))
  }

  def submitForm(
    form: Map[String, String],
    userId: String = defaultUserId,
    organisationType: Option[CdsOrganisationType] = None,
    userLocation: Option[UserLocation] = Some(UserLocation.Uk)
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    organisationType foreach { o =>
      when(mockRequestSessionData.sessionWithOrganisationTypeAdded(ArgumentMatchers.eq(o))(any[Request[AnyContent]]))
        .thenReturn(Session())
    }
    when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(userLocation)

    test(
      organisationTypeController
        .submit(atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

}
