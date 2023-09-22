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

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionBusinessService @Inject() (sessionCache: SessionCache)(implicit ec: ExecutionContext)
    extends Logging {

  def cachedContactDetailsModel(implicit request: Request[_]): Future[Option[ContactDetailsModel]] =
    sessionCache.subscriptionDetails map (_.contactDetails)

  def getCachedDateEstablished(implicit request: Request[_]): Future[LocalDate] =
    sessionCache.subscriptionDetails map {
      _.dateEstablished.getOrElse {
        val error = "No Date Of Establishment Cached"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
      }
    }

  def maybeCachedDateEstablished(implicit request: Request[_]): Future[Option[LocalDate]] =
    sessionCache.subscriptionDetails map (_.dateEstablished)

  def getCachedSicCode(implicit request: Request[_]): Future[String] =
    sessionCache.subscriptionDetails map {
      _.sicCode.getOrElse {
        val error = "No SIC Code Cached"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
      }
    }

  def cachedSicCode(implicit request: Request[_]): Future[Option[String]] =
    sessionCache.subscriptionDetails map (_.sicCode)

  def getCachedPersonalDataDisclosureConsent(implicit request: Request[_]): Future[Boolean] =
    sessionCache.subscriptionDetails map {
      _.personalDataDisclosureConsent.getOrElse {
        val error = "No Personal Data Disclosure Consent Cached"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
      }
    }

  def getCachedVatRegisteredUk(implicit request: Request[_]): Future[Boolean] =
    sessionCache.subscriptionDetails map {
      _.vatRegisteredUk.getOrElse {
        val error = "Whether the business is VAT registered in the UK has not been Cached"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
      }
    }

  def getCachedCustomsId(implicit request: Request[_]): Future[Option[CustomsId]] =
    sessionCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.customsId
    }

  def getCachedNinoOrUtrChoice(implicit request: Request[_]): Future[Option[String]] =
    sessionCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.formData.ninoOrUtrChoice
    }

  def getCachedUkVatDetails(implicit request: Request[_]): Future[Option[VatDetails]] =
    sessionCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.ukVatDetails
    }

  def getCachedVatControlListResponse(implicit request: Request[_]): Future[Option[VatControlListResponse]] =
    sessionCache.subscriptionDetails map {
      subscriptionDetails => subscriptionDetails.vatControlListResponse
    }

  def retrieveSubscriptionDetailsHolder(implicit request: Request[_]): Future[SubscriptionDetails] =
    sessionCache.subscriptionDetails

}
