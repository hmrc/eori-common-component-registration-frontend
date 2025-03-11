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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.cache

import cats.data.EitherT
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{MatchingServiceConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  ApplicationController,
  IndStCannotRegisterUsingThisServiceController,
  YouCannotChangeAddressController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.MatchingResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.isRow
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, Nino, NinoOrUtr, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionCacheService @Inject() (
  cache: SessionCache,
  requestSessionData: RequestSessionData,
  matchingService: MatchingService,
  errorView: error_template
)(implicit ec: ExecutionContext)
    extends Logging {

  def retrieveNameDobFromCache()(implicit request: Request[_], ec: ExecutionContext): Future[Individual] =
    cache.subscriptionDetails.map(
      _.nameDobDetails.getOrElse(throw DataUnavailableException(s"NameDob is not cached in data"))
    ).map { nameDobDetails =>
      Individual.withLocalDate(
        firstName = nameDobDetails.firstName,
        lastName = nameDobDetails.lastName,
        dateOfBirth = nameDobDetails.dateOfBirth
      )
    }

  def individualAndSoleTraderRouter(groupId: String, service: Service, result: Result)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Result] =
    if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
      Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
    else if (
      requestSessionData.isIndividualOrSoleTrader && requestSessionData.selectedUserLocation.contains(UserLocation.Uk)
    )
      cache.getNinoOrUtrDetails flatMap {
        case Some(ninoOrUtr) =>
          cache.getPostcodeAndLine1Details flatMap {
            case Some(postcodeViewModel) =>
              matchOnId(ninoOrUtr, GroupId(groupId), postcodeViewModel.postcode).fold(
                {
                  case MatchingServiceConnector.matchFailureResponse =>
                    logger.warn("Matching service returned Match Failure Response, cannot change address")
                    Redirect(YouCannotChangeAddressController.page(service))
                  case MatchingServiceConnector.downstreamFailureResponse => Ok(errorView(service))
                  case _                                                  => InternalServerError(errorView(service))
                },
                matchResult =>
                  if (matchResult)
                    result
                  else {
                    logger.warn("Matching service returned False on match, cannot change address")
                    Redirect(YouCannotChangeAddressController.page(service))
                  }
              )
            case _ => Future.successful(Redirect(ApplicationController.startRegister(service)))
          }
        case _ => Future.successful(Redirect(ApplicationController.startRegister(service)))
      }
    else
      Future.successful(result)

  private def matchOnId(ninoOrUtr: NinoOrUtr, groupId: GroupId, postcode: String)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): EitherT[Future, ResponseError, Boolean] = EitherT {
    retrieveNameDobFromCache().flatMap(
      ind =>
        ninoOrUtr.ninoOrUtrRadio match {
          case Some(Nino(id)) =>
            matchingService.matchIndividualWithNino(id, ind, groupId).value.flatMap {
              case Right(matchingResponse) =>
                val matchResult = matchIndDobAndPostCode(matchingResponse, ind, postcode)
                Future.successful(Right(matchResult))
              case Left(errorResponse) => Future.successful(Left(errorResponse))
              case _                   => Future.successful(Left(ResponseError(500, "Unexpected response from Matching service")))
            }
          case Some(Utr(id)) =>
            matchingService.matchIndividualWithId(Utr(id), ind, groupId).value.flatMap {
              case Right(matchingResponse) =>
                val matchResult = matchIndDobAndPostCode(matchingResponse, ind, postcode)
                Future.successful(Right(matchResult))
              case Left(errorResponse) => Future.successful(Left(errorResponse))
              case _                   => Future.successful(Left(ResponseError(500, "Unexpected response from Matching service")))
            }
          case _ => Future.successful(Left(ResponseError(500, "Nino or UTR does not exists")))
        }
    )
  }

  private def matchIndDobAndPostCode(
    matchingResponse: MatchingResponse,
    individual: Individual,
    postcode: String
  ): Boolean =
    matchingResponse.registerWithIDResponse.responseDetail.exists(
      detail =>
        detail.isAnIndividual &&
          (detail.address.postalCode.exists(_.equalsIgnoreCase(postcode))
            || detail.address.postalCode.exists(_.replaceAll(" ", "").equalsIgnoreCase(postcode)))
          && detail.individual.exists(_.dateOfBirth.contains(individual.dateOfBirth))
    )

}
