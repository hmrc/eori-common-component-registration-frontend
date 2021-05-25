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

import common.pages.matching.OrganisationUtrPage._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.DoYouHaveAUtrNumberController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{MatchingRequestHolder, MatchingResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.match_organisation_utr
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.OrganisationUtrFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DoYouHaveAUtrNumberControllerSpec
    extends ControllerSpec with MockitoSugar with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockMatchingConnector          = mock[MatchingServiceConnector]
  private val mockMatchingRequestHolder      = mock[MatchingRequestHolder]
  private val mockMatchingResponse           = mock[MatchingResponse]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val matchOrganisationUtrView       = instanceOf[match_organisation_utr]

  implicit val hc = mock[HeaderCarrier]

  private val controller =
    new DoYouHaveAUtrNumberController(mockAuthAction, mcc, matchOrganisationUtrView, mockSubscriptionDetailsService)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsService.cacheUtrMatch(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
  }

  "Viewing the Utr Organisation Matching form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(CdsOrganisationType.CharityPublicBodyNotForProfitId, atarService, Journey.Register)
    )

    "display the form" in {

      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))

      showForm(CdsOrganisationType.CharityPublicBodyNotForProfitId) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(fieldLevelErrorUtr) shouldBe empty

      }
    }
  }

  "Submitting the form for Organisation Types that have a UTR" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(CdsOrganisationType.CharityPublicBodyNotForProfitId, atarService, Journey.Register)
    )
  }

  "submitting an invalid form" should {

    "display error when form empty" in {

      submitForm(Map("have-utr" -> ""), CdsOrganisationType.ThirdCountryOrganisationId) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText("//*[@id='errors']") should include("Select yes if you have a UTR number")
      }
    }

    "display 'use different service' when org type is not valid page based on NO answer" in {

      when(mockSubscriptionDetailsService.updateSubscriptionDetails(any())).thenReturn(Future.successful(true))
      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cacheUtrMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(form = NoUtrRequest, CdsOrganisationType.CompanyId) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register/you-need-a-different-service")
      }
    }
  }

  "display the form for ROW organisation" should {

    "when ThirdCountryOrganisationId is passed" in {

      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))

      showForm(CdsOrganisationType.ThirdCountryOrganisationId) { result =>
        val page = CdsPage(contentAsString(result))
        page.title should startWith(
          "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) issued in the UK?"
        )
        page.h1 shouldBe "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) issued in the UK?"

        page.getElementsText(
          "//*[@id='have-utr-hintHtml']"
        ) shouldBe "You will have a UTR number if your organisation pays corporation tax in the UK. It is 10 numbers, for example 1234567890, which may be followed by a K. It will be on tax returns and other letters about Corporation Tax. It may be called ‘reference’, ‘UTR’ or ‘official use’."
      }
    }
  }

  "submitting the form for ROW organisation" should {

    "redirect to Get UTR page based on YES answer" in {

      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))

      submitForm(form = ValidUtrRequest, CdsOrganisationType.ThirdCountryOrganisationId) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register/matching/get-utr/third-country-organisation")
      }
    }

    "redirect to Confirm Details page based on NO answer" in {

      when(mockSubscriptionDetailsService.updateSubscriptionDetails(any())).thenReturn(Future.successful(true))
      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cacheUtrMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(form = NoUtrRequest, CdsOrganisationType.ThirdCountryOrganisationId) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(
          s"register/matching/address/${CdsOrganisationType.ThirdCountryOrganisationId}"
        )
      }
    }

    "redirect to Review page while on review mode" in {

      when(mockSubscriptionDetailsService.updateSubscriptionDetails(any())).thenReturn(Future.successful(true))
      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cacheUtrMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(form = NoUtrRequest, CdsOrganisationType.ThirdCountryOrganisationId, isInReviewMode = true) { result =>
        status(await(result)) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register/matching/review-determine")
      }
    }
  }

  "display the form for ROW" should {

    "contain a proper content for sole traders" in {

      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))

      showForm(CdsOrganisationType.ThirdCountrySoleTraderId, defaultUserId) { result =>
        val page = CdsPage(contentAsString(result))
        page.title should startWith(
          "Do you have a Self Assessment Unique Taxpayer Reference (UTR) number issued in the UK?"
        )
        page.h1 shouldBe "Do you have a Self Assessment Unique Taxpayer Reference (UTR) number issued in the UK?"
        page.getElementsText(
          "//*[@id='have-utr-hintHtml']"
        ) shouldBe "You will have a UTR number if you pay tax in the UK."
      }
    }
    "contain a proper content for individuals" in {

      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))

      showForm(CdsOrganisationType.ThirdCountryIndividualId, defaultUserId) { result =>
        val page = CdsPage(contentAsString(result))
        page.title should startWith(
          "Do you have a Self Assessment Unique Taxpayer Reference (UTR) number issued in the UK?"
        )
        page.h1 shouldBe "Do you have a Self Assessment Unique Taxpayer Reference (UTR) number issued in the UK?"
        page.getElementsText(
          "//*[@id='have-utr-hintHtml']"
        ) shouldBe "You will have a UTR number if you pay tax in the UK."
      }
    }
  }

  "submitting the form for ROW" should {

    "redirect to Get UTR page based on YES answer and organisation type sole trader" in {

      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(NameDobMatchModel("", None, "", LocalDate.now()))))
      when(mockMatchingConnector.lookup(mockMatchingRequestHolder))
        .thenReturn(Future.successful(Option(mockMatchingResponse)))
      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))
      submitForm(form = ValidUtrRequest, CdsOrganisationType.ThirdCountrySoleTraderId) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register/matching/get-utr/third-country-sole-trader")
      }
    }

    "redirect to Nino page based on NO answer" in {

      when(mockSubscriptionDetailsService.updateSubscriptionDetails(any())).thenReturn(Future.successful(true))
      when(mockSubscriptionDetailsService.cachedUtrMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cacheUtrMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(form = NoUtrRequest, CdsOrganisationType.ThirdCountrySoleTraderId) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register/matching/row/nino")
      }
    }

  }

  def showForm(organisationType: String, userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    val result =
      controller.form(organisationType, atarService, Journey.Register).apply(
        SessionBuilder.buildRequestWithSession(userId)
      )
    test(result)
  }

  def submitForm(
    form: Map[String, String],
    organisationType: String,
    userId: String = defaultUserId,
    isInReviewMode: Boolean = false
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .submit(organisationType, atarService, Journey.Register, isInReviewMode)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

}
