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

import base.UnitSpec
import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{MatchingServiceConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  MatchingRequestHolder,
  MatchingResponse,
  Organisation
}
import util.builders.matching.NinoFormBuilder
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import play.mvc.Http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{MatchingService, RequestCommonGenerator}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class MatchingServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with MatchingServiceTestData {

  private val mockMatchingServiceConnector = mock[MatchingServiceConnector]
  private val mockDetailsCreator           = mock[RegistrationDetailsCreator]
  private val mockRequestSessionData       = mock[RequestSessionData]
  private val mockDetails                  = mock[RegistrationDetails]

  private val mockRequest = mock[Request[AnyContent]]

  private val mockHeaderCarrier          = mock[HeaderCarrier]
  private val mockRequestCommonGenerator = mock[RequestCommonGenerator]
  private val mockCache                  = mock[SessionCache]
  private val loggedInCtUser             = mock[LoggedInUserWithEnrolments]
  private val mockGroupId                = mock[GroupId]
  implicit val request: Request[Any]     = mock[Request[Any]]

  private val service = new MatchingService(
    mockMatchingServiceConnector,
    mockRequestCommonGenerator,
    mockDetailsCreator,
    mockCache,
    mockRequestSessionData
  )(global)

  override def beforeEach(): Unit = {
    Mockito.reset(mockMatchingServiceConnector)
    Mockito.reset(mockDetailsCreator)
    Mockito.reset(mockCache)
    Mockito.reset(loggedInCtUser)
    when(mockGroupId.id).thenReturn("mockedGroupId")
    when(loggedInCtUser.groupId).thenReturn(Some("mockedGroupId"))

    when(mockRequestCommonGenerator.generate())
      .thenReturn(ExpectedRequestCommon)

    when(
      mockCache.saveRegistrationDetails(any[RegistrationDetails], any[GroupId], any[Option[CdsOrganisationType]])(
        any[HeaderCarrier],
        any[Request[_]]
      )
    ).thenReturn(Future.successful(true))

    when(mockCache.saveRegistrationDetails(any[RegistrationDetails])(any[Request[_]])).thenReturn(
      Future.successful(true)
    )
    when(loggedInCtUser.isAgent).thenReturn(false)
  }

  "matching an organisation with id and name" should {

    "return failed future for matchBusinessWithOrganisationName when connector fails to return result" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT[MatchingResponse](ResponseError(INTERNAL_SERVER_ERROR, "failure")))

      val expected = Left(ResponseError(INTERNAL_SERVER_ERROR, "failure"))
      val result = service.matchBusiness(
        Utr("some-utr"),
        Organisation("name", CorporateBody),
        establishmentDate = None,
        mockGroupId
      )(mockRequest, mockHeaderCarrier)

      result.value.futureValue shouldBe expected
    }

    "for UTR and name match, call matching api with correct values" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT(matchSuccessResponse))

      val expected = Right(())
      val result =
        service.matchBusiness(utr, Organisation("someOrg", Partnership), establishmentDate = None, mockGroupId)(
          mockRequest,
          mockHeaderCarrier
        )

      result.value.futureValue shouldBe expected

      val matchBusinessDataCaptor =
        ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
      verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())

      Json.toJson(matchBusinessDataCaptor.getValue) shouldBe utrAndNameRequestJson
    }

    "for UTR with a K, call matching api without the K" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT(matchSuccessResponse))

      val expected = Right(())
      val result = service.matchBusiness(
        Utr(utrId + "K"),
        Organisation("someOrg", Partnership),
        establishmentDate = None,
        mockGroupId
      )(mockRequest, mockHeaderCarrier)

      result.value.futureValue shouldBe expected

      val matchBusinessDataCaptor =
        ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
      verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())

      Json.toJson(matchBusinessDataCaptor.getValue) shouldBe utrAndNameRequestJson
    }

    "for UTR with a k, call matching api without the k" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT(matchSuccessResponse))

      val expected = Right(())
      val result = service.matchBusiness(
        Utr(utrId + "k"),
        Organisation("someOrg", Partnership),
        establishmentDate = None,
        mockGroupId
      )(mockRequest, mockHeaderCarrier)

      result.value.futureValue shouldBe expected

      val matchBusinessDataCaptor =
        ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
      verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())

      Json.toJson(matchBusinessDataCaptor.getValue) shouldBe utrAndNameRequestJson
    }

    "for EORI and name match, call matching api with correct values" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT(matchSuccessResponse))

      val expected = Right(())
      val result =
        service.matchBusiness(eori, Organisation("someOrg", UnincorporatedBody), someEstablishmentDate, mockGroupId)(
          mockRequest,
          mockHeaderCarrier
        )

      result.value.futureValue shouldBe expected

      val matchBusinessDataCaptor =
        ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
      verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())
      Json.toJson(matchBusinessDataCaptor.getValue) shouldBe eoriAndNameRequestJson
    }

    "store registration details in cache when found a match" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT(matchSuccessResponse))

      when(
        mockDetailsCreator.registrationDetails(
          ArgumentMatchers.eq(matchSuccessResponse.registerWithIDResponse),
          ArgumentMatchers.eq(utr),
          ArgumentMatchers.eq(None)
        )
      ).thenReturn(mockDetails)

      val expected = Right(())
      val result =
        service.matchBusiness(utr, Organisation("someOrg", Partnership), establishmentDate = None, mockGroupId)(
          mockRequest,
          mockHeaderCarrier
        )
      result.value.futureValue shouldBe expected

      verify(mockCache).saveRegistrationDetails(
        ArgumentMatchers.eq(mockDetails),
        ArgumentMatchers.eq(mockGroupId),
        ArgumentMatchers.any()
      )(ArgumentMatchers.eq(mockHeaderCarrier), ArgumentMatchers.eq(mockRequest))
    }
  }

  private def assertMatchIndividualWithUtr(
    connectorResponse: EitherT[Future, ResponseError, MatchingResponse],
    expectedServiceCallResult: EitherT[Future, ResponseError, MatchingResponse]
  ): Unit = {
    when(
      mockMatchingServiceConnector
        .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
    ).thenReturn(connectorResponse)

    val result = service.matchIndividualWithId(utr, individual, mockGroupId)(mockHeaderCarrier, request)
    result.value.futureValue shouldBe expectedServiceCallResult.value.futureValue

    val matchBusinessDataCaptor =
      ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
    verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())

    Json.toJson(matchBusinessDataCaptor.getValue) shouldBe utrIndividualRequestJson
  }

  "matching an individual with a utr" should {

    "call matching api with matched values" in
      assertMatchIndividualWithUtr(
        connectorResponse = eitherT(matchIndividualSuccessResponse),
        expectedServiceCallResult = eitherT(matchIndividualSuccessResponse)
      )

    "call matching api with unmatched values" in
      assertMatchIndividualWithUtr(
        connectorResponse = eitherT[MatchingResponse](MatchingServiceConnector.matchFailureResponse),
        expectedServiceCallResult = eitherT[MatchingResponse](MatchingServiceConnector.matchFailureResponse)
      )

    "store match details in cache when found a match" in {
      when(
        mockDetailsCreator.registrationDetails(
          ArgumentMatchers
            .eq(matchIndividualSuccessResponse.registerWithIDResponse),
          ArgumentMatchers.eq(utr),
          ArgumentMatchers.eq(Some(individualLocalDateOfBirth))
        )
      ).thenReturn(mockDetails)

      assertMatchIndividualWithUtr(
        connectorResponse = eitherT(matchIndividualSuccessResponse),
        expectedServiceCallResult = eitherT(matchIndividualSuccessResponse)
      )
      verify(mockCache).saveRegistrationDetails(
        ArgumentMatchers.eq(mockDetails),
        ArgumentMatchers.eq(mockGroupId),
        ArgumentMatchers.any()
      )(ArgumentMatchers.eq(mockHeaderCarrier), ArgumentMatchers.eq(request))

    }
  }

  private def assertMatchIndividualWithEori(
    individual: Individual = individualWithMiddleName,
    expectedRequestJson: JsValue = eoriIndividualRequestJson,
    connectorResponse: EitherT[Future, ResponseError, MatchingResponse],
    expectedServiceCallResult: EitherT[Future, ResponseError, MatchingResponse]
  ): Unit = {
    when(
      mockMatchingServiceConnector
        .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
    ).thenReturn(connectorResponse)

    val result = service.matchIndividualWithId(eori, individual, mockGroupId)(mockHeaderCarrier, request)
    result.value.futureValue shouldBe expectedServiceCallResult.value.futureValue

    val matchBusinessDataCaptor =
      ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
    verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())

    Json.toJson(matchBusinessDataCaptor.getValue) shouldBe expectedRequestJson
  }

  "matching an individual with an EORI number" should {

    "call matching api with matched values" in
      assertMatchIndividualWithEori(
        connectorResponse = eitherT(matchIndividualSuccessResponse),
        expectedServiceCallResult = eitherT(matchIndividualSuccessResponse)
      )

    "call matching api with unmatched values" in
      assertMatchIndividualWithEori(
        connectorResponse = eitherT[MatchingResponse](MatchingServiceConnector.matchFailureResponse),
        expectedServiceCallResult = eitherT[MatchingResponse](MatchingServiceConnector.matchFailureResponse)
      )

    "store match details in cache when found a match" in {
      when(
        mockDetailsCreator.registrationDetails(
          ArgumentMatchers
            .eq(matchIndividualSuccessResponse.registerWithIDResponse),
          ArgumentMatchers.eq(eori),
          ArgumentMatchers.eq(Some(individualLocalDateOfBirth))
        )
      ).thenReturn(mockDetails)

      assertMatchIndividualWithEori(
        connectorResponse = eitherT(matchIndividualSuccessResponse),
        expectedServiceCallResult = eitherT(matchIndividualSuccessResponse)
      )
      verify(mockCache).saveRegistrationDetails(
        ArgumentMatchers.eq(mockDetails),
        ArgumentMatchers.eq(mockGroupId),
        ArgumentMatchers.any()
      )(ArgumentMatchers.eq(mockHeaderCarrier), ArgumentMatchers.eq(request))

    }

  }

  private def assertMatchIndividualWithNino(
    connectorResponse: EitherT[Future, ResponseError, MatchingResponse],
    serviceCallResult: EitherT[Future, ResponseError, MatchingResponse]
  ): Unit = {
    when(
      mockMatchingServiceConnector
        .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
    ).thenReturn(connectorResponse)

    val result =
      service.matchIndividualWithNino(ninoId, NinoFormBuilder.asIndividual, mockGroupId)(mockHeaderCarrier, request)
    result.value.futureValue shouldBe serviceCallResult.value.futureValue

    val matchBusinessDataCaptor =
      ArgumentCaptor.forClass(classOf[MatchingRequestHolder])
    verify(mockMatchingServiceConnector).lookup(matchBusinessDataCaptor.capture())(ArgumentMatchers.any())

    Json.toJson(matchBusinessDataCaptor.getValue) shouldBe ninoIndividualRequestJson
  }

  "matching an individual with a nino" should {

    "call matching api with matched values" in {
      assertMatchIndividualWithNino(
        connectorResponse = eitherT(matchIndividualSuccessResponse),
        serviceCallResult = eitherT(matchIndividualSuccessResponse)
      )
    }

    "call matching api with unmatched values" in {
      assertMatchIndividualWithNino(
        connectorResponse = eitherT(MatchingServiceConnector.matchFailureResponse),
        serviceCallResult = eitherT[MatchingResponse](MatchingServiceConnector.matchFailureResponse)
      )
    }

    "store match details in cache" in {
      when(
        mockMatchingServiceConnector
          .lookup(ArgumentMatchers.any())(ArgumentMatchers.any())
      ).thenReturn(eitherT(matchIndividualSuccessResponse))
      when(
        mockDetailsCreator.registrationDetails(
          ArgumentMatchers
            .eq(matchIndividualSuccessResponse.registerWithIDResponse),
          ArgumentMatchers.eq(nino),
          ArgumentMatchers.eq(Some(NinoFormBuilder.DateOfBirth))
        )
      ).thenReturn(mockDetails)

      val expected = Right(matchIndividualSuccessResponse)
      val result =
        service.matchIndividualWithNino(ninoId, NinoFormBuilder.asIndividual, mockGroupId)(mockHeaderCarrier, request)
      result.value.futureValue shouldBe expected

      verify(mockCache).saveRegistrationDetails(
        ArgumentMatchers.eq(mockDetails),
        ArgumentMatchers.eq(mockGroupId),
        ArgumentMatchers.any()
      )(ArgumentMatchers.eq(mockHeaderCarrier), ArgumentMatchers.eq(request))

    }
  }
}
