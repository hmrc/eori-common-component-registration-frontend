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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.RegisterWithoutIdConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, Individual}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterWithoutIdService @Inject() (
  connector: RegisterWithoutIdConnector,
  requestCommonGenerator: RequestCommonGenerator,
  detailsCreator: RegistrationDetailsCreator,
  sessionCache: SessionCache
)(implicit ec: ExecutionContext)
    extends Logging {

  def registerOrganisation(
    orgName: String,
    address: Address,
    contactDetail: Option[ContactDetailsModel],
    loggedInUser: LoggedInUserWithEnrolments,
    orgType: Option[CdsOrganisationType] = None
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[RegisterWithoutIDResponse] = {

    val requestWithoutId = RegisterWithoutIDRequest(
      requestCommonGenerator.generate(),
      RegisterWithoutIdReqDetails.organisation(
        OrganisationName(orgName),
        address,
        contactDetail.getOrElse {
          val error = "registerOrganisation: No contact details in cache"
          // $COVERAGE-OFF$Loggers
          logger.warn(error)
          // $COVERAGE-ON
          throw DataUnavailableException(error)
        }
      )
    )

    for {
      response <- connector.register(requestWithoutId)
      registrationDetails = detailsCreator.registrationDetails(response, orgName, createSixLineAddress(address))
      _        <- save(registrationDetails, loggedInUser, orgType)
    } yield response
  }

  def registerIndividual(
    individualNameAndDateOfBirth: IndividualNameAndDateOfBirth,
    address: Address,
    contactDetail: Option[ContactDetailsModel],
    loggedInUser: LoggedInUserWithEnrolments,
    orgType: Option[CdsOrganisationType] = None
  )(implicit request: Request[_], hc: HeaderCarrier): Future[RegisterWithoutIDResponse] = {
    import individualNameAndDateOfBirth._
    val individual =
      Individual.withLocalDate(firstName, lastName, dateOfBirth)
    val reqDetails = RegisterWithoutIdReqDetails.individual(
      address = address,
      individual = individual,
      contactDetail = contactDetail.getOrElse {
        val error = "registerIndividual: No contact details in cache"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw DataUnavailableException(error)
      }
    )
    val requestWithoutId =
      RegisterWithoutIDRequest(requestCommonGenerator.generate(), reqDetails)
    for {
      response <- connector.register(requestWithoutId)
      registrationDetails = detailsCreator.registrationDetails(
                              response,
                              individualNameAndDateOfBirth,
                              createSixLineAddress(address)
                            )
      _        <- save(registrationDetails, loggedInUser, orgType)

    } yield response

  }

  private def save(
    registrationDetails: RegistrationDetails,
    loggedInUser: LoggedInUserWithEnrolments,
    orgType: Option[CdsOrganisationType]
  )(implicit hc: HeaderCarrier, request: Request[_]) =
    if (registrationDetails.safeId.id.nonEmpty)
      sessionCache.saveRegistrationDetailsWithoutId(
        registrationDetails: RegistrationDetails,
        GroupId(loggedInUser.groupId),
        orgType
      )
    else
      sessionCache.saveRegistrationDetails(registrationDetails: RegistrationDetails)

  private def createSixLineAddress(addr: Address): SixLineAddressMatchModel = {
    SixLineAddressMatchModel(
      addr.addressLine1,
      addr.addressLine2,
      addr.addressLine3.getOrElse(""),
      addr.addressLine4,
      addr.postalCode,
      addr.countryCode
    )
  }
}
