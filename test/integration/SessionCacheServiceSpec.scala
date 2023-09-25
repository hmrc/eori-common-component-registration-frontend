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

package integration

import common.support.testdata.registration.RegistrationInfoGenerator._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json.toJson
import play.api.mvc.{Request, Session}
import play.libs.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.ResponseCommon
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{CachedData, DataUnavailableException, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.test.MongoSupport
import util.builders.RegistrationDetailsBuilder._

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionCacheSpec extends IntegrationTestsSpec with MockitoSugar with MongoSupport {

  lazy val appConfig = app.injector.instanceOf[AppConfig]

  val mockTimeStampSupport = new CurrentTimestampSupport()

  private val mockSave4LaterService  = mock[Save4LaterService]
  implicit val request: Request[Any] = mock[Request[Any]]
  val hc: HeaderCarrier              = mock[HeaderCarrier]
  val sessionCache                   = new SessionCache(appConfig, mongoComponent, mockSave4LaterService, mockTimeStampSupport)

  "Session cache" should {

    "store, fetch and update Subscription details handler correctly" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val holder = SubscriptionDetails(businessShortName = Some(BusinessShortName("some business name")))

      await(sessionCache.saveSubscriptionDetails(holder)(request))

      val cacheItem = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(subDetails = Some(holder)))

      cacheItem.data mustBe expectedJson

      await(sessionCache.subscriptionDetails(request)) mustBe holder

      val updatedHolder = SubscriptionDetails(
        businessShortName = Some(BusinessShortName("different business name")),
        sicCode = Some("sic")
      )

      await(sessionCache.saveSubscriptionDetails(updatedHolder)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedUpdateJson = toJson(CachedData(subDetails = Some(updatedHolder)))

      cacheUpdate.data mustBe expectedUpdateJson
    }

    "provide default when subscription details holder not in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      await(sessionCache.putSession(DataKey("regDetails"), data = Json.toJson(individualRegistrationDetails)))

      await(sessionCache.subscriptionDetails(request)) mustBe SubscriptionDetails()
    }

    "store, fetch and update Registration details correctly" in {
      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))

      val cacheItem = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))

      cacheItem.data mustBe expectedJson

      await(sessionCache.registrationDetails(request)) mustBe organisationRegistrationDetails
      await(sessionCache.saveRegistrationDetails(individualRegistrationDetails)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedUpdatedJson = toJson(CachedData(regDetails = Some(individualRegistrationDetails)))

      cacheUpdate.data mustBe expectedUpdatedJson
    }

    "store, fetch and update Registration details with groupId correctly" in {

      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val groupId = GroupId("123456")

      when(mockSave4LaterService.saveOrgType(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))

      await(
        sessionCache.saveRegistrationDetails(
          organisationRegistrationDetails,
          groupId = groupId,
          Some(CdsOrganisationType.Company)
        )(hc, request)
      )

      val cacheItem = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))

      cacheItem.data mustBe expectedJson

      await(sessionCache.registrationDetails(request)) mustBe organisationRegistrationDetails
      await(sessionCache.saveRegistrationDetails(individualRegistrationDetails)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedUpdatedJson = toJson(CachedData(regDetails = Some(individualRegistrationDetails)))

      cacheUpdate.data mustBe expectedUpdatedJson
    }

    "store, fetch and update Registration details without id correctly" in {

      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val groupId = GroupId("123456")

      when(mockSave4LaterService.saveOrgType(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))
      when(mockSave4LaterService.saveSafeId(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))

      await(
        sessionCache.saveRegistrationDetailsWithoutId(
          organisationRegistrationDetails,
          groupId = groupId,
          Some(CdsOrganisationType.Company)
        )(hc, request)
      )

      val cacheItem = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))

      cacheItem.data mustBe expectedJson

      await(sessionCache.registrationDetails(request)) mustBe organisationRegistrationDetails
      await(sessionCache.saveRegistrationDetails(individualRegistrationDetails)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedUpdatedJson = toJson(CachedData(regDetails = Some(individualRegistrationDetails)))

      cacheUpdate.data mustBe expectedUpdatedJson
    }

    "throw exception when registration Details requested and not available in cache" in {
      val sessionId = "sessionId-" + UUID.randomUUID()
      val caught = intercept[DataUnavailableException] {
        when(request.session).thenReturn(Session(Map(("sessionId", sessionId))))
        await(sessionCache.registrationDetails(request))
      }
      caught.getMessage mustBe s"regDetails is not cached in data for the sessionId: $sessionId"
    }

    "store, fetch and update Registration Info correctly" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      await(sessionCache.saveRegistrationInfo(organisationRegistrationInfoWithAllOptionalValues)(request))

      val cacheItem = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(regInfo = Some(organisationRegistrationInfoWithAllOptionalValues)))

      cacheItem.data mustBe expectedJson

      await(sessionCache.registrationInfo(request)) mustBe organisationRegistrationInfoWithAllOptionalValues
      await(sessionCache.saveRegistrationInfo(individualRegistrationInfoWithAllOptionalValues)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedUpdatedJson = toJson(CachedData(regInfo = Some(individualRegistrationInfoWithAllOptionalValues)))

      cacheUpdate.data mustBe expectedUpdatedJson
    }

    "throw exception when registration info requested and not available in cache" in {
      val s: SessionId = setupSession
      when(request.session).thenReturn(Session(Map(("sessionId", s.value))))

      val caught = intercept[DataUnavailableException] {
        await(sessionCache.registrationInfo(request))
      }
      caught.getMessage mustBe s"regInfo is not cached in data for the sessionId: ${s.value}"
    }

    "store Registration Details, Info and Subscription Details Holder correctly" in {

      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))
      val holder = SubscriptionDetails()
      await(sessionCache.saveSubscriptionDetails(holder)(request))
      await(sessionCache.saveRegistrationInfo(organisationRegistrationInfoWithAllOptionalValues)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(
        CachedData(
          Some(organisationRegistrationDetails),
          Some(holder),
          Some(organisationRegistrationInfoWithAllOptionalValues)
        )
      )
      cacheUpdate.data mustBe expectedJson
    }

    "remove from the cache" in {

      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))

      await(sessionCache.remove(request))

      val cached = await(sessionCache.cacheRepo.findById(request))
      cached mustBe None
    }

    "throw DataUnavailableException when groupEnrolment is not present in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))
      val caught = intercept[DataUnavailableException] {
        await(sessionCache.groupEnrolment(request))
      }
      caught.getMessage startsWith s"${CachedData.groupEnrolmentKey} is not cached in data for the sessionId: sessionId-123"
    }

    "throw IllegalStateException when registerWithEoriAndIdResponse is not present in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))
      val caught = intercept[DataUnavailableException] {
        await(sessionCache.registerWithEoriAndIdResponse(request))
      }
      caught.getMessage startsWith s"${CachedData.registerWithEoriAndIdResponseKey} is not cached in data for the sessionId: sessionId-123"
    }

    "return empty subscription details if the subscription details are missing" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))

      val response = await(sessionCache.subscriptionDetails(request))
      response mustBe SubscriptionDetails()
    }

    "throw DataUnavailableException when sub02Outcome is not present in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))
      val caught = intercept[DataUnavailableException] {
        await(sessionCache.sub02Outcome(request))
      }
      caught.getMessage startsWith s"${CachedData.sub02OutcomeKey} is not cached in data for the sessionId: sessionId-123"
    }

    "throw DataUnavailableException when sub01Outcome is not present in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("regDetails"), data = Json.toJson(individualRegistrationDetails)))
      val caught = intercept[DataUnavailableException] {
        await(sessionCache.sub02Outcome(request))
      }
      caught.getMessage startsWith s"${CachedData.sub01OutcomeKey} is not cached in data for the sessionId: sessionId-123"
    }

    "throw DataUnavailableException when email is not present in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))
      val caught = intercept[DataUnavailableException] {
        await(sessionCache.email(request))
      }
      caught.getMessage startsWith s"${CachedData.emailKey} is not cached in data for the sessionId: sessionId-123"
    }

    "fetchSafeIdFromReg06Response returns None if reg06response is not present in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))
      val response = await(sessionCache.fetchSafeIdFromReg06Response(request))
      response mustBe None
    }

    "fetch safeId correctly from registration details" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))
      val registrationDetails: RegistrationDetails = RegistrationDetails.individual(
        sapNumber = "0123456789",
        safeId = SafeId("safe-id"),
        name = "John Doe",
        address = defaultAddress,
        dateOfBirth = LocalDate.parse("1980-07-23"),
        customsId = Some(Utr("123UTRNO"))
      )
      await(sessionCache.saveRegistrationDetails(registrationDetails)(request))
      await(sessionCache.safeId(request)) mustBe SafeId("safe-id")
    }

    "fetch safeId correctly from registerWithEoriAndIdResponse details" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))
      def registerWithEoriAndIdResponse = RegisterWithEoriAndIdResponse(
        ResponseCommon("OK", None, LocalDateTime.now(), None),
        Some(
          RegisterWithEoriAndIdResponseDetail(
            Some("PASS"),
            Some("C001"),
            responseData = Some(
              ResponseData(
                "someSafeId",
                Trader("John Doe", "Mr D"),
                EstablishmentAddress("Line 1", "City Name", Some("SE28 1AA"), "GB"),
                Some(
                  ContactDetail(
                    EstablishmentAddress("Line 1", "City Name", Some("SE28 1AA"), "GB"),
                    "John Contact Doe",
                    Some("1234567"),
                    Some("89067"),
                    Some("john.doe@example.com")
                  )
                ),
                VATIDs = Some(Seq(VatIds("AD", "1234"), VatIds("GB", "4567"))),
                hasInternetPublication = false,
                principalEconomicActivity = Some("P001"),
                hasEstablishmentInCustomsTerritory = Some(true),
                legalStatus = Some("Official"),
                thirdCountryIDNumber = Some(Seq("1234", "67890")),
                personType = Some(9),
                dateOfEstablishmentBirth = Some("2018-05-16"),
                startDate = "2018-05-15",
                expiryDate = Some("2018-05-16")
              )
            )
          )
        )
      )
      await(sessionCache.saveRegisterWithEoriAndIdResponse(registerWithEoriAndIdResponse)(request))

      await(sessionCache.safeId(request)) mustBe SafeId("someSafeId")
    }

    "store and fetch email correctly" in {

      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val email = "email@email.com"

      await(sessionCache.saveEmail(email)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(email = Some(email)))
      cacheUpdate.data mustBe expectedJson

      await(sessionCache.email(request)) mustBe email
    }

    "store subscription details correctly" in {

      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val subscriptionDetails = SubscriptionDetails(email = Some("email@email.com"))

      await(sessionCache.saveSubscriptionDetails(subscriptionDetails)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(subDetails = Some(subscriptionDetails)))
      cacheUpdate.data mustBe expectedJson
    }

    "store and fetch sub01Outcome details correctly with submissionCompleteDetails matching " in {

      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val processingDate = LocalDate.of(1961, 4, 12).toString

      val sub01Outcome = Sub01Outcome(processingDate)

      val submissionCompleteDetails = SubmissionCompleteDetails(processingDate = processingDate)

      await(sessionCache.saveSub01Outcome(sub01Outcome)(request))

      val cacheUpdate = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(
        CachedData(sub01Outcome = Some(sub01Outcome), submissionCompleteDetails = Some(submissionCompleteDetails))
      )
      cacheUpdate.data mustBe expectedJson

      await(sessionCache.sub01Outcome(request)) mustBe sub01Outcome
    }

    "store and fetch sub02Outcome details correctly" in {

      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val sub02Outcome = Sub02Outcome(LocalDate.of(1961, 4, 12).toString, "fullName", Some("GB123456789123"))

      await(sessionCache.saveSub02Outcome(sub02Outcome)(request))

      val cacheItem = await(sessionCache.cacheRepo.findById(request)).getOrElse(
        throw new IllegalStateException("Cache returned None")
      )
      val expectedJson = toJson(CachedData(sub02Outcome = Some(sub02Outcome)))

      cacheItem.data mustBe expectedJson
      await(sessionCache.sub02Outcome(request)) mustBe sub02Outcome
    }

    "store keepAlive details correctly" in {
      await(sessionCache.keepAlive(request)) mustBe true
    }

  }

  private def setupSession: SessionId = {
    val sessionId = SessionId("sessionId-" + UUID.randomUUID())
    when(hc.sessionId).thenReturn(Some(sessionId))
    sessionId
  }

}
