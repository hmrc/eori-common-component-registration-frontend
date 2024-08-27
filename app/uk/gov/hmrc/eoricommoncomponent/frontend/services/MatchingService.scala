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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import cats.data.EitherT
import play.api.Logging
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{MatchingServiceConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Individual, RegistrationInfoRequest}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingService @Inject() (
  matchingConnector: MatchingServiceConnector,
  requestCommonGenerator: RequestCommonGenerator,
  detailsCreator: RegistrationDetailsCreator,
  cache: SessionCache,
  requestSessionData: RequestSessionData
)(implicit ec: ExecutionContext)
    extends Logging {

  private val CustomsIdsMap: Map[Class[_ <: CustomsId], String] =
    Map(
      classOf[Utr]  -> RegistrationInfoRequest.UTR,
      classOf[Eori] -> RegistrationInfoRequest.EORI,
      classOf[Nino] -> RegistrationInfoRequest.NINO
    )

  private def convert(customsId: CustomsId, capturedDate: Option[LocalDate])(
    response: MatchingResponse
  ): RegistrationDetails =
    detailsCreator.registrationDetails(response.registerWithIDResponse, customsId, capturedDate)

  def matchBusiness(customsId: CustomsId, org: Organisation, establishmentDate: Option[LocalDate], groupId: GroupId)(
    implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): EitherT[Future, ResponseError, Unit] = {
    def stripKFromUtr: CustomsId => CustomsId = {
      case Utr(id) => Utr(id.stripSuffix("k").stripSuffix("K"))
      case other   => other
    }

    val orgWithCode = org.copy(organisationType = EtmpOrganisationType.orgTypeToEtmpOrgCode(org.organisationType))
    for {
      response <- matchingConnector.lookup(idAndNameMatchRequest(stripKFromUtr(customsId), orgWithCode))
      details = convert(customsId, establishmentDate)(response)
      _ <- EitherT[Future, ResponseError, Unit](
        cache.saveRegistrationDetails(details, groupId, requestSessionData.userSelectedOrganisationType).map(
          _ => Right(())
        )
      )
    } yield ()

  }

  def matchIndividualWithId(customsId: CustomsId, individual: Individual, groupId: GroupId)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): EitherT[Future, ResponseError, MatchingResponse] =
    for {
      response <- matchingConnector.lookup(individualIdMatchRequest(customsId, individual))
      details = convert(customsId, toLocalDate(individual.dateOfBirth))(response)
      resp <- EitherT[Future, ResponseError, MatchingResponse](
        cache.saveRegistrationDetails(details, groupId).map(_ => Right(response))
      )
    } yield resp

  def matchIndividualWithNino(nino: String, individual: Individual, groupId: GroupId)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): EitherT[Future, ResponseError, MatchingResponse] =
    for {
      response <- matchingConnector.lookup(individualNinoMatchRequest(nino, individual))
      details = convert(customsId = Nino(nino), capturedDate = toLocalDate(individual.dateOfBirth))(response)
      resp <- EitherT[Future, ResponseError, MatchingResponse](
        cache.saveRegistrationDetails(details, groupId).map(_ => Right(response))
      )
    } yield resp

  private def idAndNameMatchRequest(customsId: CustomsId, org: Organisation): MatchingRequestHolder =
    MatchingRequestHolder(
      MatchingRequest(
        requestCommonGenerator.generate(),
        RequestDetail(
          nameOfCustomsIdType(customsId),
          customsId.id,
          requiresNameMatch = true,
          isAnAgent = false,
          Some(org)
        )
      )
    )

  private def nameOfCustomsIdType(customsId: CustomsId): String =
    CustomsIdsMap.getOrElse(
      customsId.getClass, {
        val error = s"Invalid matching id $customsId"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalArgumentException(error)
      }
    )

  private def individualIdMatchRequest(customsId: CustomsId, individual: Individual): MatchingRequestHolder =
    MatchingRequestHolder(
      MatchingRequest(
        requestCommonGenerator.generate(),
        RequestDetail(
          nameOfCustomsIdType(customsId),
          customsId.id,
          requiresNameMatch = true,
          isAnAgent = false,
          individual = Some(individual)
        )
      )
    )

  private def individualNinoMatchRequest(nino: String, individual: Individual): MatchingRequestHolder =
    MatchingRequestHolder(
      MatchingRequest(
        requestCommonGenerator.generate(),
        RequestDetail(
          RegistrationInfoRequest.NINO,
          nino,
          requiresNameMatch = true,
          isAnAgent = false,
          individual = Some(individual)
        )
      )
    )

}
