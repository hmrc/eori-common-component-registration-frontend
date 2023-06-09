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

import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationKeys
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.CachedData._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.{DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import java.time.{LocalDateTime, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.{NoStackTrace, NonFatal}

sealed case class CachedData(
  regDetails: Option[RegistrationDetails] = None,
  subDetails: Option[SubscriptionDetails] = None,
  regInfo: Option[RegistrationInfo] = None,
  sub02Outcome: Option[Sub02Outcome] = None,
  sub01Outcome: Option[Sub01Outcome] = None,
  registerWithEoriAndIdResponse: Option[RegisterWithEoriAndIdResponse] = None,
  email: Option[String] = None,
  groupEnrolment: Option[EnrolmentResponse] = None,
  keepAlive: Option[String] = None,
  eori: Option[String] = None
)

object CachedData {
  val regDetailsKey                    = "regDetails"
  val regInfoKey                       = "regInfo"
  val subDetailsKey                    = "subDetails"
  val sub01OutcomeKey                  = "sub01Outcome"
  val sub02OutcomeKey                  = "sub02Outcome"
  val registerWithEoriAndIdResponseKey = "registerWithEoriAndIdResponse"
  val emailKey                         = "email"
  val keepAliveKey                     = "keepAlive"
  val safeIdKey                        = "safeId"
  val groupIdKey                       = "cachedGroupId"
  val groupEnrolmentKey                = "groupEnrolment"
  val eoriKey                          = "eori"
  implicit val format                  = Json.format[CachedData]
}

@Singleton
class SessionCache @Inject() (
  appConfig: AppConfig,
  mongo: MongoComponent,
  save4LaterService: Save4LaterService,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends SessionCacheRepository(
      mongo,
      "session-cache",
      ttl = appConfig.ttl,
      timestampSupport = timestampSupport,
      sessionIdKey = SessionKeys.sessionId
    )(ec) {

  val now = LocalDateTime.now(ZoneId.of("Europe/London"))

  def sessionId(implicit request: Request[_]): String =
    request.session.get("sessionId") match {
      case None =>
        throw new IllegalStateException("Session id is not available")
      case Some(sessionId) => sessionId
    }

  def putData[A: Writes](key: String, data: A)(implicit request: Request[_]): Future[A] =
    preservingMdc {
      putSession[A](DataKey(key), data).map(_ => data)
    }

  def getData[A: Reads](key: String)(implicit request: Request[_]): Future[Option[A]] =
    preservingMdc {
      getFromSession[A](DataKey(key))
    }

  def saveRegistrationDetails(rd: RegistrationDetails)(implicit request: Request[_]): Future[Boolean] =
    putData(regDetailsKey, Json.toJson(rd)) map (_ => true)

  def saveRegistrationDetails(
    rd: RegistrationDetails,
    groupId: GroupId,
    orgType: Option[CdsOrganisationType] = None
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] =
    for {
      _                <- save4LaterService.saveOrgType(groupId, orgType)
      createdOrUpdated <- putData(regDetailsKey, Json.toJson(rd)) map (_ => true)
    } yield createdOrUpdated

  def saveRegistrationDetailsWithoutId(
    rd: RegistrationDetails,
    groupId: GroupId,
    orgType: Option[CdsOrganisationType] = None
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] =
    for {
      _                <- save4LaterService.saveSafeId(groupId, rd.safeId)
      _                <- save4LaterService.saveOrgType(groupId, orgType)
      createdOrUpdated <- putData(regDetailsKey, Json.toJson(rd)) map (_ => true)
    } yield createdOrUpdated

  def saveSub02Outcome(subscribeOutcome: Sub02Outcome)(implicit request: Request[_]): Future[Boolean] =
    putData(sub02OutcomeKey, Json.toJson(subscribeOutcome)) map (_ => true)

  def saveSub01Outcome(sub01Outcome: Sub01Outcome)(implicit request: Request[_]): Future[Boolean] =
    putData(sub01OutcomeKey, Json.toJson(sub01Outcome)) map (_ => true)

  def saveRegistrationInfo(rd: RegistrationInfo)(implicit request: Request[_]): Future[Boolean] =
    putData(regInfoKey, Json.toJson(rd)) map (_ => true)

  def saveRegisterWithEoriAndIdResponse(
    rd: RegisterWithEoriAndIdResponse
  )(implicit request: Request[_]): Future[Boolean] =
    putData(registerWithEoriAndIdResponseKey, Json.toJson(rd)) map (_ => true)

  def saveSubscriptionDetails(rdh: SubscriptionDetails)(implicit request: Request[_]): Future[Boolean] =
    putData(subDetailsKey, Json.toJson(rdh)) map (_ => true)

  def saveEmail(email: String)(implicit request: Request[_]): Future[Boolean] =
    putData(emailKey, Json.toJson(email)) map (_ => true)

  def saveEori(eori: Eori)(implicit request: Request[_]): Future[Boolean] =
    putData(eoriKey, Json.toJson(eori.id)) map (_ => true)

  def keepAlive(implicit request: Request[_]): Future[Boolean] =
    putData(keepAliveKey, Json.toJson(LocalDateTime.now().toString)) map (_ => true)

  def subscriptionDetails(implicit request: Request[_]): Future[SubscriptionDetails] =
    getData[SubscriptionDetails](subDetailsKey).map(_.getOrElse(SubscriptionDetails()))

  def email(implicit request: Request[_]): Future[String] =
    getData[String](emailKey).map(_.getOrElse(throwException(emailKey)))

  def eori(implicit request: Request[_]): Future[Option[String]] =
    getData[String](eoriKey)

  def safeId(implicit request: Request[_]): Future[SafeId] = fetchSafeIdFromRegDetails.flatMap {
    case Some(value) => Future.successful(value)
    case None =>
      fetchSafeIdFromReg06Response.map(
        _.getOrElse(throw new IllegalStateException(s"$safeIdKey is not cached in data for the sessionId: $sessionId"))
      )
  }

  def fetchSafeIdFromReg06Response(implicit request: Request[_]): Future[Option[SafeId]] =
    registerWithEoriAndIdResponse.map(
      response =>
        response.responseDetail.flatMap(_.responseData.map(_.SAFEID))
          .map(SafeId(_))
    ).recoverWith {
      case NonFatal(_) => Future.successful(None)
    }

  def fetchSafeIdFromRegDetails(implicit request: Request[_]): Future[Option[SafeId]] =
    registrationDetails.map(response => if (response.safeId.id.nonEmpty) Some(response.safeId) else None)
      .recoverWith {
        case NonFatal(_) => Future.successful(None)
      }

  def registrationDetails(implicit request: Request[_]): Future[RegistrationDetails] =
    getData[RegistrationDetails](regDetailsKey).map(_.getOrElse(throwException(regDetailsKey)))

  def registerWithEoriAndIdResponse(implicit request: Request[_]): Future[RegisterWithEoriAndIdResponse] =
    getData[RegisterWithEoriAndIdResponse](registerWithEoriAndIdResponseKey).map(
      _.getOrElse(throwException(registerWithEoriAndIdResponseKey))
    )

  def sub01Outcome(implicit request: Request[_]): Future[Sub01Outcome] =
    getData[Sub01Outcome](sub01OutcomeKey).map(_.getOrElse(throwException(sub01OutcomeKey)))

  def sub02Outcome(implicit request: Request[_]): Future[Sub02Outcome] =
    getData[Sub02Outcome](sub02OutcomeKey).map(_.getOrElse(throwException(sub02OutcomeKey)))

  def registrationInfo(implicit request: Request[_]): Future[RegistrationInfo] =
    getData[RegistrationInfo](regInfoKey).map(_.getOrElse(throwException(regInfoKey)))

  def groupEnrolment(implicit request: Request[_]): Future[EnrolmentResponse] =
    getData[EnrolmentResponse](groupEnrolmentKey).map(_.getOrElse(throwException(groupEnrolmentKey)))

  def remove(implicit request: Request[_]): Future[Boolean] =
    preservingMdc {
      cacheRepo.deleteEntity(request).map(_ => true).recoverWith {
        case _ => Future.successful(false)
      }
    }

  private def throwException(name: String)(implicit request: Request[_]) =
    throw DataUnavailableException(s"$name is not cached in data for the sessionId: $sessionId")

}

case class SessionTimeOutException(errorMessage: String) extends NoStackTrace
case class DataUnavailableException(message: String)     extends RuntimeException(message)
