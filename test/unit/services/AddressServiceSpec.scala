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

package unit.services

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import base.UnitSpec
import common.pages.NinoMatchPage.convertToAnyMustWrapper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{ApplicationController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm.addressDetailsCreateForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{
  AddressService,
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{address, error_template}
import uk.gov.hmrc.http.HeaderCarrier
import util.builders.SessionBuilder
import util.builders.SubscriptionContactDetailsFormBuilder.contactDetailsModel

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with MatchingServiceTestData {

  private val mockSubscriptionDetailsService   = mock[SubscriptionDetailsService]
  private val mockSubscriptionBusinessService  = mock[SubscriptionBusinessService]
  private val mockSubscriptionFlowManager      = mock[SubscriptionFlowManager]
  private val mockSubscriptionPage             = mock[SubscriptionPage]
  private val mockFlowError                    = mock[FlowError]
  private val mockApplicationController        = mock[ApplicationController]
  private val mockAddress                      = mock[address]
  private val mockErrorTemplate                = mock[error_template]
  private val mockMessagesControllerComponents = mock[MessagesControllerComponents]

  private val defaultUserId: String = s"user-${UUID.randomUUID}"

  private val subscriptionToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val formMappings =
    Map("street" -> "street", "city" -> "city", "postcode" -> "SE28 1AA", "countryCode" -> "GB")

  val service = new AddressService(
    mockSubscriptionDetailsService,
    mockSubscriptionBusinessService,
    mockSubscriptionFlowManager,
    mockAddress,
    mockErrorTemplate,
    mockMessagesControllerComponents
  )

  "populateViewIfContactDetailsCached" should {
    "Successfully populate view when details are cached" in subscriptionToTest.foreach { subscription =>
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
        Future.successful(Some(contactDetailsModel))
      )
      service.populateViewIfContactDetailsCached(subscription)(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
      ) flatMap (result => {
        status(result) mustBe Ok
      })
    }
  }

  "Should 'successfully' display InternalServerError when data not found in cache" in subscriptionToTest.foreach {
    subscription =>
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(None)
      service.populateViewIfContactDetailsCached(subscription)(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
      ) flatMap (result => {
        status(result) mustBe Ok
      })
  }

  "handleFormDataAndRedirect" should {
    "successfully redirect when correct input provided" in subscriptionToTest.foreach { subscription =>
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
        Future.successful(Some(contactDetailsModel))
      )
      val subscriptionFlowInfo = SubscriptionFlowInfo(1, 2, mockSubscriptionPage)

      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenReturn(Right(subscriptionFlowInfo))
      when(subscriptionFlowInfo.nextPage.url(any[Service])).thenReturn("/nextPage")

      await(
        service.handleFormDataAndRedirect(addressDetailsCreateForm(), isInReviewMode = false, subscription)(
          SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
        )
      ).flatMap { result =>
        result.header.headers("Location").endsWith("/nextPage")
      }
    }

    "redirect to start page when subscriptionFlow unavailable" in subscriptionToTest.foreach { subscription =>
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
        Future.successful(Some(contactDetailsModel))
      )

      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenReturn(Left(mockFlowError))

      await(
        service.handleFormDataAndRedirect(addressDetailsCreateForm(), isInReviewMode = false, subscription)(
          SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
        )
      ).flatMap { _ =>
        verify(mockApplicationController.startRegister(any()))
      }
    }
  }
}
