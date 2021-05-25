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

package unit.services.registration

import base.UnitSpec
import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.RegisterWithoutIdConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.Sub02Controller
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RequestCommonGenerator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegisterWithoutIdService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RegisterWithoutIdServiceSpec
    extends UnitSpec with ScalaFutures with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {
  private val mockConnector          = mock[RegisterWithoutIdConnector]
  private val mockReqCommonGen       = mock[RequestCommonGenerator]
  private val mockDetailsCreator     = mock[RegistrationDetailsCreator]
  private val mockRequestCommon      = mock[RequestCommon]
  private val mockSessionCache       = mock[SessionCache]
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSub02Controller    = mock[Sub02Controller]
  private val mockOrgTypeLookup      = mock[OrgTypeLookup]

  implicit val hc: HeaderCarrier       = mock[HeaderCarrier]
  implicit val rq: Request[AnyContent] = mock[Request[AnyContent]]

  private val loggedInUserId   = java.util.UUID.randomUUID.toString
  private val mockLoggedInUser = mock[LoggedInUserWithEnrolments]

  val Failure = new RuntimeException("something bad has happened")

  val service = new RegisterWithoutIdService(mockConnector, mockReqCommonGen, mockDetailsCreator, mockSessionCache)(
    global
  )

  private val dateOfBirth = {
    val year        = 1980
    val monthOfYear = 3
    val dayOfMonth  = 31
    new LocalDate(year, monthOfYear, dayOfMonth)
  }

  private val individualNameAndDateOfBirth =
    IndividualNameAndDateOfBirth("indName", Some("indMidName"), "indLastName", dateOfBirth)

  private val orgName = "orgName"

  private val organisationAddress =
    SixLineAddressMatchModel("add1", Some("add2"), "add3", Some("add4"), Some("postcode"), "COUNTRY")

  private val organisationAddressWithEmptyPostcode =
    SixLineAddressMatchModel("add1", Some("add2"), "add3", Some("add4"), None, "COUNTRY")

  private val address = Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "country")

  private val contactDetails = Some(
    ContactDetailsModel("John Doe", "john@example.com", "441234987654", None, true, None, None, None, None)
  )

  private val addressWithEmptyPostcode = Address("add1", Some("add2"), Some("add3"), Some("add4"), Some(""), "country")

  private val SAFEID    = java.util.UUID.randomUUID.toString
  private val sapNumber = "sapNumber-123"

  private val registrationResponse = RegisterWithoutIdResponseHolder(
    RegisterWithoutIDResponse(
      ResponseCommon(
        "status",
        Some("Status text"),
        DateTime.now(),
        Some(List(MessagingServiceParam("SAP_NUMBER", sapNumber)))
      ),
      Some(RegisterWithoutIdResponseDetail(SAFEID, ARN = None))
    )
  )

  private val mockDetailsOrganisation = mock[RegistrationDetailsOrganisation]
  private val mockDetailsIndividual   = mock[RegistrationDetailsIndividual]

  override protected def beforeAll(): Unit = {
    when(mockLoggedInUser.userId()).thenReturn(loggedInUserId)
    when(mockDetailsOrganisation.sapNumber).thenReturn(TaxPayerId(sapNumber))
    when(mockDetailsIndividual.sapNumber).thenReturn(TaxPayerId(sapNumber))
  }

  override protected def beforeEach(): Unit = {
    reset(
      mockConnector,
      mockDetailsCreator,
      mockSessionCache,
      mockRequestSessionData,
      mockOrgTypeLookup,
      mockSub02Controller
    )
    when(mockDetailsOrganisation.safeId).thenReturn(SafeId(""))
    when(
      mockSessionCache
        .saveRegistrationDetails(any[RegistrationDetails], any[GroupId], any[Option[CdsOrganisationType]])(
          any[HeaderCarrier]
        )
    ).thenReturn(Future.successful(true))
    when(
      mockSessionCache
        .saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier])
    ).thenReturn(Future.successful(true))

    when(mockReqCommonGen.generate()).thenReturn(mockRequestCommon)
    when(
      mockSessionCache
        .saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier])
    ).thenReturn(Future.successful(true))
  }

  private val emulatedFailure = new RuntimeException("something bad happened")

  private def mockRegistrationFailure() =
    when(mockConnector.register(any[RegisterWithoutIDRequest])(any[HeaderCarrier])) thenReturn Future.failed(
      emulatedFailure
    )

  private def mockRegistrationSuccess() =
    when(mockConnector.register(any[RegisterWithoutIDRequest])(any[HeaderCarrier])) thenReturn Future.successful(
      registrationResponse.registerWithoutIDResponse
    )

  private def mockOrganisationRegistrationSuccess() = {
    mockRegistrationSuccess()
    when(mockDetailsOrganisation.safeId).thenReturn(SafeId(""))
    when(
      mockDetailsCreator
        .registrationDetails(registrationResponse.registerWithoutIDResponse, orgName, organisationAddress)
    ).thenReturn(mockDetailsOrganisation)
    when(
      mockDetailsCreator.registrationDetails(
        registrationResponse.registerWithoutIDResponse,
        orgName,
        organisationAddressWithEmptyPostcode
      )
    ).thenReturn(mockDetailsOrganisation)
  }

  private def mockIndividualRegistrationSuccess() = {
    mockRegistrationSuccess()
    when(mockDetailsIndividual.safeId).thenReturn(SafeId(""))

    when(
      mockDetailsCreator.registrationDetails(
        registrationResponse.registerWithoutIDResponse,
        individualNameAndDateOfBirth,
        organisationAddress
      )
    ).thenReturn(mockDetailsIndividual)
    when(
      mockDetailsCreator.registrationDetails(
        registrationResponse.registerWithoutIDResponse,
        individualNameAndDateOfBirth,
        organisationAddressWithEmptyPostcode
      )
    ).thenReturn(mockDetailsIndividual)
  }

  "RegisterWithoutIdService" should {

    "send correct organisation registration request" in {
      mockOrganisationRegistrationSuccess()

      await(service.registerOrganisation(orgName, address, contactDetails, mockLoggedInUser))

      val captor = ArgumentCaptor.forClass(classOf[RegisterWithoutIDRequest])
      verify(mockConnector).register(captor.capture())(ArgumentMatchers.eq(hc))

      val registrationRequest: RegisterWithoutIDRequest = captor.getValue

      registrationRequest.requestCommon should be(mockRequestCommon)
      registrationRequest.requestDetail.organisation shouldBe Some(OrganisationName("orgName"))
      registrationRequest.requestDetail.address shouldBe Address(
        "add1",
        Some("add2"),
        Some("add3"),
        Some("add4"),
        Some("postcode"),
        "country"
      )
      registrationRequest.requestDetail.contactDetails shouldBe RegisterWithoutIdContactDetails(
        Some("441234987654"),
        None,
        None,
        Some("john@example.com")
      )
    }

    "send organisation registration request with postcode as None when postcode is empty" in {
      mockOrganisationRegistrationSuccess()

      await(service.registerOrganisation(orgName, addressWithEmptyPostcode, contactDetails, mockLoggedInUser))

      val captor = ArgumentCaptor.forClass(classOf[RegisterWithoutIDRequest])
      verify(mockConnector).register(captor.capture())(ArgumentMatchers.eq(hc))

      val registrationRequest: RegisterWithoutIDRequest = captor.getValue
      registrationRequest.requestDetail.address.postalCode shouldBe None
    }

    "should throw exception when request for organisation fails" in {
      mockRegistrationFailure()
      val caught = intercept[RuntimeException](
        await(service.registerOrganisation(orgName, address, contactDetails, mockLoggedInUser))
      )
      caught shouldBe emulatedFailure
    }

    "store registration details in cache and DB when found a match for organisation" in {

      mockOrganisationRegistrationSuccess()

      await(service.registerOrganisation(orgName, address, contactDetails, mockLoggedInUser))

      verify(mockSessionCache).saveRegistrationDetails(ArgumentMatchers.eq(mockDetailsOrganisation))(
        ArgumentMatchers.eq(hc)
      )
    }

    "not proceed/return until organisation details are saved in cache" in {
      mockOrganisationRegistrationSuccess()

      when(
        mockSessionCache
          .saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier])
      ).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[RuntimeException] {
        await(service.registerOrganisation(orgName, address, contactDetails, mockLoggedInUser))
      }
      caught shouldBe emulatedFailure
    }

    "send correct individual registration request" in {
      mockIndividualRegistrationSuccess()
      when(
        mockSessionCache
          .saveRegistrationDetails(any[RegistrationDetails], any[GroupId], any[Option[CdsOrganisationType]])(
            any[HeaderCarrier]
          )
      ).thenReturn(Future.successful(true))
      service
        .registerIndividual(individualNameAndDateOfBirth, address, contactDetails, mockLoggedInUser)
        .futureValue shouldBe registrationResponse.registerWithoutIDResponse

      val captor = ArgumentCaptor.forClass(classOf[RegisterWithoutIDRequest])
      verify(mockConnector).register(captor.capture())(ArgumentMatchers.eq(hc))

      val registrationRequest: RegisterWithoutIDRequest = captor.getValue

      registrationRequest.requestCommon shouldBe mockRequestCommon
      registrationRequest.requestDetail.individual shouldBe Some(
        Individual("indName", Some("indMidName"), "indLastName", dateOfBirth.toString)
      )
      registrationRequest.requestDetail.address shouldBe Address(
        "add1",
        Some("add2"),
        Some("add3"),
        Some("add4"),
        Some("postcode"),
        "country"
      )
      registrationRequest.requestDetail.contactDetails shouldBe RegisterWithoutIdContactDetails(
        Some("441234987654"),
        None,
        None,
        Some("john@example.com")
      )
    }

    "send individual registration request with postcode as None when postcode is empty" in {
      mockIndividualRegistrationSuccess()

      await(
        service
          .registerIndividual(individualNameAndDateOfBirth, addressWithEmptyPostcode, contactDetails, mockLoggedInUser)
      ) should be((registrationResponse.registerWithoutIDResponse))

      val captor = ArgumentCaptor.forClass(classOf[RegisterWithoutIDRequest])
      verify(mockConnector).register(captor.capture())(ArgumentMatchers.eq(hc))

      val registrationRequest: RegisterWithoutIDRequest = captor.getValue
      registrationRequest.requestDetail.address.postalCode shouldBe None
    }

    "should throw exception when request for individual fails" in {
      mockRegistrationFailure()
      val caught = intercept[RuntimeException](
        await(service.registerIndividual(individualNameAndDateOfBirth, address, contactDetails, mockLoggedInUser))
      )
      caught shouldBe emulatedFailure
    }

    "store registration details in cache and DB when found a match for individual" in {

      mockIndividualRegistrationSuccess()

      await(
        service.registerIndividual(individualNameAndDateOfBirth, address, contactDetails, mockLoggedInUser)
      ) should be(registrationResponse.registerWithoutIDResponse)

      verify(mockSessionCache).saveRegistrationDetails(ArgumentMatchers.eq(mockDetailsIndividual))(
        ArgumentMatchers.eq(hc)
      )
    }

    "not proceed/return until individual details are saved in cache" in {
      mockIndividualRegistrationSuccess()

      when(
        mockSessionCache
          .saveRegistrationDetails(any[RegistrationDetails])(any[HeaderCarrier])
      ).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[RuntimeException] {
        await(service.registerIndividual(individualNameAndDateOfBirth, address, contactDetails, mockLoggedInUser))
      }
      caught shouldBe emulatedFailure
    }
  }
}
