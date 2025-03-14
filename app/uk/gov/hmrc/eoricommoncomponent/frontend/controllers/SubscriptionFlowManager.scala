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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.Logger
import play.api.mvc.{AnyContent, Request, Session}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Iom
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError.FlowNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError.DataNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.Constants.ONE
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class SubscriptionFlowConfig(
  pageBeforeFirstFlowPage: SubscriptionPage,
  pagesInOrder: List[SubscriptionPage],
  pageAfterLastFlowPage: SubscriptionPage
) {

  private def lastPageInTheFlow(currentPos: Int): Boolean = currentPos == pagesInOrder.size - ONE

  def determinePageBeforeSubscriptionFlow(uriBeforeSubscriptionFlow: Option[String]): SubscriptionPage =
    uriBeforeSubscriptionFlow.fold(pageBeforeFirstFlowPage)(url => PreviousPage(url))

  def stepInformation(currentPage: SubscriptionPage): SubscriptionFlowInfo = {

    val currentPos = pagesInOrder.indexOf(currentPage)

    val nextPage = if (lastPageInTheFlow(currentPos)) pageAfterLastFlowPage else pagesInOrder(currentPos + ONE)

    SubscriptionFlowInfo(stepNumber = currentPos + ONE, totalSteps = pagesInOrder.size, nextPage = nextPage)
  }

}

@Singleton
class SubscriptionFlowManager @Inject() (
  requestSessionData: RequestSessionData,
  cdsFrontendDataCache: SessionCache,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def currentSubscriptionFlow(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Either[FlowError, SubscriptionFlow] =
    requestSessionData.userSubscriptionFlow match {
      case Left(_: DataNotFound) => Left(FlowNotFound())
      case Right(flow) => Right(flow)
    }

  def stepInformation(
    currentPage: SubscriptionPage
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Either[FlowError, SubscriptionFlowInfo] =
    currentSubscriptionFlow map { flow =>
      SubscriptionFlows(flow).stepInformation(currentPage)
    }

  def startSubscriptionFlow(
    service: Service
  )(implicit request: Request[AnyContent]): Future[(SubscriptionPage, Session)] =
    startSubscriptionFlow(None, requestSessionData.userSelectedOrganisationType, service)

  def startSubscriptionFlow(
    previousPage: Option[SubscriptionPage] = None,
    cdsOrganisationType: CdsOrganisationType,
    service: Service
  )(implicit request: Request[AnyContent]): Future[(SubscriptionPage, Session)] =
    startSubscriptionFlow(previousPage, Some(cdsOrganisationType), service)

  def startSubscriptionFlow(previousPage: Option[SubscriptionPage], service: Service)(implicit
    request: Request[AnyContent]
  ): Future[(SubscriptionPage, Session)] =
    startSubscriptionFlow(previousPage, requestSessionData.userSelectedOrganisationType, service)

  def startSubscriptionFlowWithPage(
    previousPage: Option[SubscriptionPage] = None,
    service: Service,
    flow: SubscriptionFlow
  )(implicit request: Request[AnyContent]): Future[(SubscriptionPage, Session)] = {
    val maybePreviousPageUrl = previousPage.map(page => page.url(service))
    // $COVERAGE-OFF$Loggers
    logger.info(s"select Subscription flow: ${flow.name}")
    // $COVERAGE-ON
    Future.successful(
      (
        SubscriptionFlows(flow).pagesInOrder.head,
        requestSessionData.storeUserSubscriptionFlow(
          flow,
          SubscriptionFlows(flow).determinePageBeforeSubscriptionFlow(maybePreviousPageUrl).url(service)
        )
      )
    )
  }

  private def startSubscriptionFlow(
    previousPage: Option[SubscriptionPage],
    orgType: => Option[CdsOrganisationType],
    service: Service
  )(implicit request: Request[AnyContent]): Future[(SubscriptionPage, Session)] = {
    val maybePreviousPageUrl = previousPage.map(page => page.url(service))
    cdsFrontendDataCache.registrationDetails map { registrationDetails =>
      val userLocation = requestSessionData.selectedUserLocation.getOrElse(
        throw new Exception("unable to start flow without user's location")
      )

      val flow = if (appConfig.allowNoIdJourney) {
        selectFlow(registrationDetails, orgType, userLocation)
      } else {
        oldFlow(registrationDetails, orgType)
      }

      // $COVERAGE-OFF$Loggers
      logger.info(s"select Subscription flow: ${flow.name}")
      // $COVERAGE-ON
      (
        SubscriptionFlows(flow).pagesInOrder.head,
        requestSessionData.storeUserSubscriptionFlow(
          flow,
          SubscriptionFlows(flow).determinePageBeforeSubscriptionFlow(maybePreviousPageUrl).url(service)
        )
      )
    }
  }

  private def selectFlow(
    registrationDetails: RegistrationDetails,
    maybeOrgType: => Option[CdsOrganisationType],
    userLocation: UserLocation
  ): SubscriptionFlow = {
    val selectedFlow: SubscriptionFlow = {
      (registrationDetails, userLocation) match {
        case (rdo: RegistrationDetailsOrganisation, loc) =>
          (rdo.etmpOrganisationType, loc) match {
            case (Some(UnincorporatedBody), Iom) => SubscriptionFlow(CharityPublicBodySubscriptionFlowIom.name)
            case (Some(UnincorporatedBody), _) => SubscriptionFlow(CharityPublicBodySubscriptionFlow.name)
            case (Some(Partnership), Iom) => SubscriptionFlow(PartnershipSubscriptionFlowIom.name)
            case (Some(Partnership), _) => SubscriptionFlow(PartnershipSubscriptionFlow.name)
            case (_, Iom) => throw new Exception("to be done in DDCYLS-6242")
            case _ => SubscriptionFlow(OrganisationSubscriptionFlow.name)
          }
        case (_: RegistrationDetailsIndividual, Iom) =>
          SubscriptionFlow(IndividualSubscriptionFlow.name)
        case (_: RegistrationDetailsIndividual, _) =>
          SubscriptionFlow(IndividualSubscriptionFlow.name)
        case (_: RegistrationDetailsEmbassy, _) =>
          SubscriptionFlow(EmbassySubscriptionFlow.name)
        case _ => throw DataUnavailableException("Incomplete cache cannot complete journey")
      }
    }

    maybeOrgType.fold(selectedFlow)(orgType => SubscriptionFlows.flows.keys.find(_.name == orgType.id).getOrElse(selectedFlow))
  }

  private def oldFlow(
    registrationDetails: RegistrationDetails,
    maybeOrgType: => Option[CdsOrganisationType]
  ): SubscriptionFlow = {
    val selectedFlow: SubscriptionFlow =
      registrationDetails match {
        case _: RegistrationDetailsOrganisation =>
          SubscriptionFlow(OrganisationSubscriptionFlow.name)
        case _: RegistrationDetailsIndividual =>
          SubscriptionFlow(IndividualSubscriptionFlow.name)
        case _ => throw new IllegalStateException("Incomplete cache cannot complete journey")
      }

    maybeOrgType.fold(selectedFlow)(orgType => SubscriptionFlows.flows.keys.find(_.name == orgType.id).getOrElse(selectedFlow))
  }

}
