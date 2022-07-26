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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{AddressController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Country
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{address, error_template}
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.{
  CdsPage,
  SubscriptionFlowCreateModeTestSupport,
  SubscriptionFlowReviewModeTestSupport,
  SubscriptionFlowTestSupport
}
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder._
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressControllerSpec
    extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport
    with SubscriptionFlowReviewModeTestSupport {

  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockCdsFrontendDataCache       = mock[SessionCache]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlow           = mock[SubscriptionFlowManager]
  private val mockSubscriptionFlowInfo       = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage           = mock[SubscriptionPage]

  private val viewAddress       = instanceOf[address]
  private val viewErrorTemplate = instanceOf[error_template]

  private val controller = new AddressController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionDetailsService,
    mockSubscriptionFlow,
    mcc,
    viewAddress,
    viewErrorTemplate
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

  val contactDetailsModel = ContactDetailsModel(
    fullName = "John Doe",
    emailAddress = "john.doe@example.com",
    telephone = "234234",
    None,
    false,
    Some("streetName"),
    Some("cityName"),
    Some("SE281AA"),
    Some("GB")
  )

  private val validRequest =
    Map("street" -> "streetName", "city" -> "cityName", "postcode" -> "SE281AA", "countryCode" -> "GB")

  private val inValidRequest = Map("city" -> "cityName", "postcode" -> "SE281AA", "countryCode" -> "GB")

  override def beforeEach: Unit = {
    super.beforeEach()

    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)
    when(mockSubscriptionFlow.stepInformation(any())(any[Request[AnyContent]]))
      .thenReturn(mockSubscriptionFlowInfo)
    when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
  }

  private val problemWithSelectionError = "Error: This field is required"

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionBusinessService, mockRequestSessionData, mockSubscriptionDetailsService)

    super.afterEach()
  }

  "Subscription Address Controller form in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display title as 'EORI application contact address'" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("EORI application contact address")
      }
    }

  }
  "Subscription Address Controller form in  review mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewForm(atarService))

    "populate form if address is in the cache" in {
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
        .thenReturn(Some(contactDetailsModel))
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("EORI application contact address")
        page.getElementValue(AddressPage.streetFieldXPath) shouldBe "streetName"
        page.getElementValue(AddressPage.cityFieldXPath) shouldBe "cityName"
        page.getElementValue(AddressPage.postcodeFieldXPath) shouldBe "SE281AA"
      }
    }

    "display the correct text for the continue button" in {
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
        .thenReturn(Some(contactDetailsModel))
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(AddressPage.continueButtonXpath) shouldBe ContinueButtonTextInReviewMode
      }
    }
  }
  "Submitting the form in review mode" should {
    "redirect to review page when details are valid" in {
      when(mockSubscriptionPage.url(any())).thenReturn(
        "/customs-registration-services/atar/register/matching/review-determine"
      )
      submitFormInReviewMode(validRequest)(verifyRedirectToReviewPage())
    }
  }
  "Submitting the form in create mode" should {

    "display a relevant error if street is chosen" in {
      submitFormInCreateMode(inValidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(AddressPage.streetFieldLevelErrorXPath) shouldBe problemWithSelectionError
      }
    }

  }

  private def submitFormInReviewMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
      .thenReturn(Some(contactDetailsModel))
    when(mockSubscriptionDetailsService.cacheContactAddressDetails(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    test(
      controller.submit(isInReviewMode = true, atarService)(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  private def submitFormInCreateMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
      .thenReturn(Some(contactDetailsModel))
    when(mockSubscriptionDetailsService.cacheContactAddressDetails(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    test(
      controller.submit(isInReviewMode = false, atarService)(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  protected override val formId: String = "addressDetailsForm"

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressController
      .submit(isInReviewMode = false, atarService)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressController
      .submit(isInReviewMode = true, atarService)
      .url

  private def showCreateForm(userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(organisationRegistrationDetails)

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def showReviewForm(userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
      .thenReturn(Some(contactDetailsModel))

    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
