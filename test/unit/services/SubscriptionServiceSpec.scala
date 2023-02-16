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
import org.mockito.Mockito.when
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalacheck.{Gen, Prop}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.Checkers
import play.api.libs.json.{JsObject, JsValue, Json}
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.SubscriptionServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.FeatureFlags
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionCreateResponse._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  SubscriptionRequest,
  SubscriptionResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel, VatDetailsOld}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.EtmpTypeOfPerson
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{
  SubscriptionFailed,
  SubscriptionResult,
  SubscriptionService,
  SubscriptionSuccessful
}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionServiceSpec
    extends UnitSpec with MockitoSugar with BeforeAndAfterAll with Checkers with SubscriptionServiceTestData {
  private val mockHeaderCarrier = mock[HeaderCarrier]
  private val mockConfig        = mock[FeatureFlags]

  override def beforeAll() = {
    super.beforeAll()
    when(mockConfig.sub02UseServiceName).thenReturn(true)
  }

  private def subscriptionSuccessResultIgnoreTimestamp(
    expected: SubscriptionSuccessful,
    request: SubscriptionRequest
  ): SubscriptionResult = {
    val timestamp =
      request.subscriptionCreateRequest.requestDetail.contactInformation.flatMap(_.emailVerificationTimestamp)
    expected.copy(emailVerificationTimestamp = timestamp)
  }

  "Calling Subscribe" should {

    "call connector with correct values when organisation type has been manually selected" in {
      val cdsOrganisationTypeGenerator             = Gen.oneOf(cdsOrganisationTypeToTypeOfPersonMap.keys.toSeq)
      val etmpOrganisationTypeGenerator            = Gen.oneOf(etmpOrganisationTypeToTypeOfPersonMap.keys.toSeq)
      val vatIdsGenerator: List[VatIdentification] = List(VatIdentification(Some("GB"), Some("123456789")))
      check(Prop.forAllNoShrink(cdsOrganisationTypeGenerator, etmpOrganisationTypeGenerator, vatIdsGenerator) {
        (cdsOrganisationType, etmpOrganisationType, vatIds) =>
          val expectedRequest = requestJson(
            businessName,
            vatIds,
            Some(EtmpOrganisationType.apply(cdsOrganisationType)),
            expectedDateEstablishedString = dateEstablishedString
          )
          assertOrganisationSubscriptionRequest(
            expectedRequest,
            subscriptionSuccessResult,
            vatIds,
            Some(cdsOrganisationType),
            Some(etmpOrganisationType)
          )
          Prop.proved
      })
    }

    "call connector with correct person type when user is an organisation and organisation type has not been manually selected" in {
      val etmpOrganisationTypeGenerator            = Gen.oneOf(etmpOrganisationTypeToTypeOfPersonMap.keys.toSeq)
      val vatIdsGenerator: List[VatIdentification] = List(VatIdentification(Some("GB"), Some("123456789")))

      check(Prop.forAllNoShrink(etmpOrganisationTypeGenerator, vatIdsGenerator) { (etmpOrganisationType, vatIds) =>
        val expectedRequest =
          requestJson(name = businessName, vatIds = vatIds, organisationType = Some(etmpOrganisationType))
        assertOrganisationSubscriptionRequest(
          expectedRequest,
          SubscriptionSuccessful(
            Eori(responseEoriNumber),
            responseFormBundleId,
            processingDateResponse,
            Some(emailVerificationTimestamp)
          ),
          vatIds,
          None,
          Some(etmpOrganisationType)
        )
        Prop.proved
      })
    }

    "call connector with correct person type when user is an organisation and organisation type has not been manually selected and organisation type is not available from matching" in {
      val vatIdsGenerator = Gen.oneOf(List(VatIdentification(Some("GB"), Some("123456789"))))

      check(Prop.forAllNoShrink(vatIdsGenerator) { vatIds =>
        val expectedRequest = requestJson(
          name = businessName,
          vatIds = List(vatIds),
          organisationType = None,
          isOrganisationEvenIfOrganisationTypeIsNone = true
        )

        assertOrganisationSubscriptionRequest(
          expectedRequest = expectedRequest,
          expectedServiceCallResult = subscriptionSuccessResult,
          vatIds = List(vatIds),
          cdsOrganisationType = None,
          etmpOrganisationType = None
        )
        Prop.proved
      })
    }

    "call connector with correct person type when user is an individual and organisation type has not been manually selected" in {
      val vatIdsGenerator                   = Gen.oneOf(List(VatIdentification(Some("GB"), Some("123456789"))))
      val vatDetails: Option[VatDetailsOld] = ukVatDetails

      check(Prop.forAllNoShrink(vatIdsGenerator) { vatIds =>
        val expectedRequest = requestJsonIndividual(
          name = individualName,
          vatIds = List(vatIds),
          organisationType = None,
          expectedDateOfBirthString = dateOfBirthString
        )
        assertIndividualSubscriptionRequest(expectedRequest, subscriptionSuccessResult, vatDetails)
        Prop.proved
      })
    }

    "call connector with date of birth captured in subscription flow when user is an individual" in {
      val capturedDateOfBirth = dateOfBirth
      val expectedRequest = requestJsonIndividual(
        name = individualName,
        vatIds = EmptyVatIds,
        organisationType = None,
        expectedDateOfBirthString = capturedDateOfBirth.toString
      )

      assertIndividualSubscriptionRequest(expectedRequest, subscriptionSuccessResult, None)
    }

    "return failed future for matchBusinessWithOrganisationName when connector fails to return result" in {
      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.failed(UpstreamErrorResponse("failure", INTERNAL_SERVER_ERROR, 1)))
      )

      val caught = intercept[UpstreamErrorResponse] {
        await(
          service
            .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
              mockHeaderCarrier
            )
        )
      }
      caught.statusCode shouldBe 500
      caught.getMessage shouldBe "failure"
    }

    "return failed future if response does not contain POSITION parameter" in {
      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(subscriptionResponseWithoutPosition))
      )
      val caught = intercept[IllegalStateException] {
        await(
          service
            .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
              mockHeaderCarrier
            )
        )
      }
      caught.getMessage shouldEqual "POSITION parameter is missing in subscription create response"
    }

    forAll(successfulPositionValues) { positionValue =>
      s"return failed future if response with POSITION $positionValue does not contain form bundle id parameter" in {
        val service = constructService(
          connectorMock =>
            when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(
                Future.successful(subscriptionResponseWithoutFormBundleIdJson(positionValue).as[SubscriptionResponse])
              )
        )
        val caught = intercept[IllegalStateException] {
          await(
            service
              .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
                mockHeaderCarrier
              )
          )
        }
        caught.getMessage shouldBe "ETMPFORMBUNDLENUMBER parameter is missing in subscription create response"
      }
    }

    "return a valid fail response when subscription fails for business reason" in {
      val errorFromEIS = "999 - Some error occurred"

      val expectedFailure =
        SubscriptionFailed(
          "Response status of FAIL returned for a SUB02: Create Subscription. " + errorFromEIS,
          "18 Aug 2016"
        )

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(subscriptionFailedResponseJson(errorFromEIS)).as[SubscriptionResponse])
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe expectedFailure
    }

    "return a valid fail response when subscription fails due to eori already exists" in {
      val failResponse = SubscriptionFailed(EoriAlreadyExists, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(subscriptionFailedResponseJson(EoriAlreadyExists)).as[SubscriptionResponse])
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to eori already exists - ignoring letter case" in {
      val failResponse = SubscriptionFailed(EoriAlreadyExists, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(
              Future.successful(subscriptionFailedResponseJson("069 - EORI Already Exists FOR the VAT Number")).as[
                SubscriptionResponse
              ]
            )
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to request could not be processed" in {
      val failResponse = SubscriptionFailed(RequestNotProcessed, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(subscriptionFailedResponseJson(RequestNotProcessed)).as[SubscriptionResponse])
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to request could not be processed - ignoring letter case" in {
      val failResponse = SubscriptionFailed(RequestNotProcessed, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(
              Future.successful(subscriptionFailedResponseJson("003 - Request Could Not Be Processed")).as[
                SubscriptionResponse
              ]
            )
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to subscription already in-progress" in {
      val failResponse = SubscriptionFailed(SubscriptionInProgress, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(
              Future.successful(subscriptionFailedResponseJson(SubscriptionInProgress)).as[SubscriptionResponse]
            )
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to subscription already in-progress - ignoring letter case" in {
      val failResponse = SubscriptionFailed(SubscriptionInProgress, "18 Aug 2016")

      val service =
        constructService(
          connectorMock =>
            when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(
                Future.successful(
                  subscriptionFailedResponseJson("068 - Subscription Already IN-Progress or active")
                ).as[SubscriptionResponse]
              )
        )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to eori already associated to different business partner record" in {
      val failResponse = SubscriptionFailed(EoriAlreadyAssociated, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(
              Future.successful(subscriptionFailedResponseJson(EoriAlreadyAssociated)).as[SubscriptionResponse]
            )
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "return a valid fail response when subscription fails due to eori already associated to different business partner record - ignoring letter case" in {
      val failResponse = SubscriptionFailed(EoriAlreadyAssociated, "18 Aug 2016")

      val service = constructService(
        connectorMock =>
          when(connectorMock.subscribe(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(
              Future.successful(
                subscriptionFailedResponseJson(
                  "070 - There IS Another EORI already associated TO this Business partner"
                )
              ).as[SubscriptionResponse]
            )
      )

      val res = await(
        service
          .subscribe(organisationRegistrationDetails, fullyPopulatedSubscriptionDetails, None, atarService)(
            mockHeaderCarrier
          )
      )

      res shouldBe failResponse
    }

    "assert that Date established is available when subscribing for organisation" in {
      val service = constructService(_ => None)

      the[IllegalStateException] thrownBy {
        val holder = fullyPopulatedSubscriptionDetails.copy(dateEstablished = None)
        service.subscribe(organisationRegistrationDetails, holder, None, atarService)(mockHeaderCarrier)
      } should have message "Date Established must be present for an organisation subscription"

    }

    "assert that Principal Economic Activity is available when subscribing for organisation" in {
      val service = constructService(_ => None)
      the[AssertionError] thrownBy {
        val holder = fullyPopulatedSubscriptionDetails.copy(sicCode = None)
        service.subscribe(organisationRegistrationDetails, holder, None, atarService)(mockHeaderCarrier)
      } should have message "assertion failed: SicCode/Principal Economic Activity must be present for an organisation subscription"
    }
  }

  "Create request" should {

    "truncate sic code to 4 numbers by removing the rightmost number" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetails.copy(sicCode = Some("12750"))
      val req     = service.createRequest(organisationRegistrationDetails, holder, None, atarService)

      req.subscriptionCreateRequest.requestDetail.principalEconomicActivity shouldBe Some("1275")
    }

    "replace empty city with a dash" in {
      val service = constructService(_ => None)
      val holder = fullyPopulatedSubscriptionDetails.copy(addressDetails =
        Some(AddressViewModel("some street", "", Some("AB99 3DW"), "GB"))
      )
      val req = service.createRequest(organisationRegistrationDetails, holder, None, atarService)

      req.subscriptionCreateRequest.requestDetail.CDSEstablishmentAddress.city shouldBe "-"
    }

    "replace empty postcode with a None" in {
      val service = constructService(_ => None)
      val holder = fullyPopulatedSubscriptionDetails.copy(addressDetails =
        Some(AddressViewModel("some street", "", Some(""), "GB"))
      )
      val req = service.createRequest(organisationRegistrationDetails, holder, None, atarService)

      req.subscriptionCreateRequest.requestDetail.CDSEstablishmentAddress.postalCode shouldBe None
    }

    "have correct person type for Individual Subscription" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetails.copy(sicCode = Some("12750"))
      val req     = service.createRequest(individualRegistrationDetails, holder, None, atarService)

      req.subscriptionCreateRequest.requestDetail.typeOfPerson shouldBe Some(EtmpTypeOfPerson.NaturalPerson)
    }

    "throw an exception when unexpected registration details received" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetails.copy(sicCode = Some("12750"))
      val thrown = intercept[IllegalStateException] {
        service.createRequest(RegistrationDetails.rdSafeId(SafeId("safeid")), holder, None, atarService)
      }
      thrown.getMessage shouldBe "Incomplete cache cannot complete journey"
    }

    "throw an exception when date of Establishment is None" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetails.copy(dateEstablished = None)
      val thrown = intercept[IllegalStateException] {
        service.createRequest(
          organisationRegistrationDetails,
          holder,
          Some(CdsOrganisationType("third-country-organisation")),
          atarService
        )
      }
      thrown.getMessage shouldBe "Date Established must be present for an organisation subscription"
    }

    "populate the SubscriptionCreate Request when there is a plus (+) sign in the request on telephone number" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetailsWithPlusSignInTelephone
      val req = service.createRequest(
        organisationRegistrationDetails,
        holder,
        Some(CdsOrganisationType("company")),
        atarService
      )
      req.subscriptionCreateRequest.requestDetail.contactInformation.flatMap(_.telephoneNumber) shouldBe Some(
        "+01632961234"
      )

    }

    "populate the SubscriptionCreate Request when there is a plus (+) sign in the request on fax number" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetailsWithPlusSignInFaxNumber
      val req = service.createRequest(
        organisationRegistrationDetails,
        holder,
        Some(CdsOrganisationType("company")),
        atarService
      )
      req.subscriptionCreateRequest.requestDetail.contactInformation.flatMap(_.faxNumber) shouldBe Some("+01632961234")
    }

    "populate the SubscriptionCreate Request when there is a plus (+) sign in the request on telephone and fax number" in {
      val service = constructService(_ => None)
      val holder  = fullyPopulatedSubscriptionDetailsWithPlusSignInTelAndFaxNumber
      val req = service.createRequest(
        organisationRegistrationDetails,
        holder,
        Some(CdsOrganisationType("company")),
        atarService
      )
      req.subscriptionCreateRequest.requestDetail.contactInformation.flatMap(_.faxNumber) shouldBe Some("+01632961235")
      req.subscriptionCreateRequest.requestDetail.contactInformation.flatMap(_.telephoneNumber) shouldBe Some(
        "+01632961234"
      )
    }
  }

  private def assertOrganisationSubscriptionRequest(
    expectedRequest: JsValue,
    expectedServiceCallResult: SubscriptionResult,
    vatIds: List[VatIdentification],
    cdsOrganisationType: Option[CdsOrganisationType],
    etmpOrganisationType: Option[EtmpOrganisationType],
    subscriptionContactDetails: ContactDetailsModel = subscriptionContactDetailsModel,
    personalDataDisclosureConsent: Boolean = false
  ): Unit = {

    val registrationDetailsWithOrgTypeAdded =
      organisationRegistrationDetails.copy(etmpOrganisationType = etmpOrganisationType)

    val subscriptionDetailsHolder = SubscriptionDetails(
      contactDetails = Some(subscriptionContactDetails),
      personalDataDisclosureConsent = Some(personalDataDisclosureConsent),
      businessShortName = None,
      dateEstablished = Some(dateOfEstablishment),
      sicCode = Some(principalEconomicActivity),
      ukVatDetailsOld = ukVatDetails
    )

    val result = makeSubscriptionRequest(
      registrationDetailsWithOrgTypeAdded,
      subscriptionDetailsHolder,
      cdsOrganisationType,
      subscriptionResponse = subscriptionGenerateResponse
    )

    result.actualServiceCallResult shouldBe subscriptionSuccessResultIgnoreTimestamp(
      subscriptionSuccessResult,
      result.actualConnectorRequest
    )
    assertSameJson(Json.toJson(result.actualConnectorRequest), expectedRequest)
  }

  private def assertIndividualSubscriptionRequest(
    expectedRequest: JsValue,
    expectedServiceCallResult: SubscriptionSuccessful,
    ukVatDetails: Option[VatDetailsOld],
    subscriptionContactDetails: ContactDetailsModel = subscriptionContactDetailsModel,
    personalDataDisclosureConsent: Boolean = false
  ): Unit = {

    val subscriptionDetailsHolder = SubscriptionDetails(
      contactDetails = Some(subscriptionContactDetails),
      personalDataDisclosureConsent = Some(personalDataDisclosureConsent),
      ukVatDetailsOld = ukVatDetails
    )

    val result = makeSubscriptionRequest(
      registrationDetails = individualRegistrationDetails,
      subscriptionDetailsHolder = subscriptionDetailsHolder,
      organisationType = None,
      subscriptionResponse = subscriptionGenerateResponse
    )

    val expectedResult =
      subscriptionSuccessResultIgnoreTimestamp(expectedServiceCallResult, result.actualConnectorRequest)

    result.actualServiceCallResult should be(expectedResult)
    assertSameJson(Json.toJson(result.actualConnectorRequest), expectedRequest)
  }

  private case class SubscriptionCallResult(
    actualServiceCallResult: SubscriptionResult,
    actualConnectorRequest: SubscriptionRequest
  )

  private def makeSubscriptionRequest(
    registrationDetails: RegistrationDetails,
    subscriptionDetailsHolder: SubscriptionDetails,
    organisationType: Option[CdsOrganisationType],
    subscriptionResponse: SubscriptionResponse
  ): SubscriptionCallResult = {

    val subscribeDataCaptor = ArgumentCaptor.forClass(classOf[SubscriptionRequest])
    val service = constructService(
      connectorMock =>
        when(connectorMock.subscribe(subscribeDataCaptor.capture())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(subscriptionResponse))
    )

    val actualServiceCallResult = await(
      service.subscribe(registrationDetails, subscriptionDetailsHolder, organisationType, atarService)(
        mockHeaderCarrier
      )
    )
    val actualConnectorRequest = subscribeDataCaptor.getValue
    SubscriptionCallResult(actualServiceCallResult, actualConnectorRequest)
  }

  private def constructService(setupServiceConnector: SubscriptionServiceConnector => Unit) = {
    val mockSubscriptionServiceConnector = mock[SubscriptionServiceConnector]
    setupServiceConnector(mockSubscriptionServiceConnector)
    new SubscriptionService(mockSubscriptionServiceConnector, mockConfig)
  }

  private def assertSameJson(json: JsValue, expectedJson: JsValue) = {
    def assertSameRequestCommon = {
      val commonJson = (json \ "subscriptionCreateRequest" \ "requestCommon")
        .as[JsObject] - "receiptDate" - "acknowledgementReference"
      val expectedCommonJson = (expectedJson \ "subscriptionCreateRequest" \ "requestCommon")
        .as[JsObject] - "receiptDate" - "acknowledgementReference"
      commonJson shouldEqual expectedCommonJson
    }

    def assertSameRequestDetail = {
      val detailJson = (json \ "subscriptionCreateRequest" \ "requestDetail").as[JsObject] - "contactInformation"
      val expectedDetailJson = (expectedJson \ "subscriptionCreateRequest" \ "requestDetail")
        .as[JsObject] - "contactInformation"
      detailJson shouldEqual expectedDetailJson
    }

    assertSameRequestCommon
    assertSameRequestDetail
  }

}
