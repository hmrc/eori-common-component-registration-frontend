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

package unit.controllers.migration

import common.support.testdata.subscription.SubscriptionContactDetailsModelBuilder._
import common.support.testdata.subscription.{BusinessDatesOrganisationTypeTables, ReviewPageOrganisationTypeTables}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.CheckYourDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionDetails, SubscriptionFlow}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{IdMatchModel, NameDobMatchModel, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{AddressViewModel, CompanyRegisteredCountry}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.check_your_details
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder.{existingOrganisationRegistrationDetails, individualRegistrationDetails}
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.SubscriptionContactDetailsFormBuilder.Email

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

//TODO: We need to simplify the reduce no of tests in the class. Review page should be simple, if value is available in holder then display otherwise not.
class CheckYourDetailsControllerSpec
    extends ControllerSpec with BusinessDatesOrganisationTypeTables with ReviewPageOrganisationTypeTables
    with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector      = mock[AuthConnector]
  private val mockAuthAction         = authAction(mockAuthConnector)
  private val mockCdsDataCache       = mock[SessionCache]
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSubscriptionFlow   = mock[SubscriptionFlow]

  private val checkYourDetailsView = instanceOf[check_your_details]

  val controller =
    new CheckYourDetailsController(mockAuthAction, mockCdsDataCache, mcc, checkYourDetailsView, mockRequestSessionData)

  override def beforeEach: Unit = {
    reset(mockCdsDataCache, mockSubscriptionFlow)
    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(mockSubscriptionFlow)

    val subscriptionDetailsHolderForCompany = SubscriptionDetails(
      personalDataDisclosureConsent = Some(true),
      contactDetails = Some(contactUkDetailsModelWithMandatoryValuesOnly),
      nameDobDetails = Some(NameDobMatchModel("John", None, "Doe", LocalDate.parse("2003-04-08"))),
      idDetails = Some(IdMatchModel(id = "AB123456C")),
      eoriNumber = Some("SOMEEORINUMBER"),
      dateEstablished = Some(LocalDate.parse("2003-04-08")),
      nameIdOrganisationDetails = Some(NameIdOrganisationMatchModel(name = "Company UTR number", id = "UTRNUMBER")),
      addressDetails =
        Some(AddressViewModel(street = "street", city = "city", postcode = Some("postcode"), countryCode = "GB")),
      email = Some("john.doe@example.com"),
      registeredCompany = Some(CompanyRegisteredCountry("GB"))
    )
    when(mockCdsDataCache.email(any[HeaderCarrier])).thenReturn(Future.successful(Email))

    when(mockCdsDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(subscriptionDetailsHolderForCompany)
    when(mockCdsDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(individualRegistrationDetails)
    when(mockCdsDataCache.addressLookupParams(any[HeaderCarrier])).thenReturn(Future.successful(None))
  }

  "Reviewing the details" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.reviewDetails(atarService, Journey.Subscribe)
    )

    "return ok when data has been provided" in {
      when(mockCdsDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(existingOrganisationRegistrationDetails)

      showForm() { result =>
        status(result) shouldBe OK
      }
    }
  }

  def showForm(
    userSelectedOrgType: Option[CdsOrganisationType] = None,
    userId: String = defaultUserId,
    isIndividualSubscriptionFlow: Boolean = false
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(userSelectedOrgType)
    when(mockSubscriptionFlow.isIndividualFlow).thenReturn(isIndividualSubscriptionFlow)

    test(controller.reviewDetails(atarService, Journey.Subscribe).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
