/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionBusinessService @Inject() (cdsFrontendDataCache: SessionCache)(implicit ec: ExecutionContext) {

  def cachedContactDetailsModel(implicit request: Request[_]): Future[Option[ContactDetailsModel]] =
    cdsFrontendDataCache.subscriptionDetails map (_.contactDetails)

  def getCachedDateEstablished(implicit request: Request[_]): Future[LocalDate] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.dateEstablished.getOrElse(throw new IllegalStateException("No Date Of Establishment Cached"))
    }

  def maybeCachedDateEstablished(implicit request: Request[_]): Future[Option[LocalDate]] =
    cdsFrontendDataCache.subscriptionDetails map (_.dateEstablished)

  def getCachedSicCode(implicit request: Request[_]): Future[String] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.sicCode.getOrElse(throw new IllegalStateException("No SIC Code Cached"))
    }

  def cachedSicCode(implicit request: Request[_]): Future[Option[String]] =
    cdsFrontendDataCache.subscriptionDetails map (_.sicCode)

  def getCachedPersonalDataDisclosureConsent(implicit request: Request[_]): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.personalDataDisclosureConsent.getOrElse(
        throw new IllegalStateException("No Personal Data Disclosure Consent Cached")
      )
    }

  def getCachedVatRegisteredUk(implicit request: Request[_]): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.vatRegisteredUk.getOrElse(
        throw new IllegalStateException("Whether the business is VAT registered in the UK has not been Cached")
      )
    }

  def getCachedCustomsId(implicit request: Request[_]): Future[Option[CustomsId]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.customsId
    }

  def getCachedNinoOrUtrChoice(implicit request: Request[_]): Future[Option[String]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.formData.ninoOrUtrChoice
    }

  def getCachedUkVatDetails(implicit request: Request[_]): Future[Option[VatDetails]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.ukVatDetails
    }

  def retrieveSubscriptionDetailsHolder(implicit request: Request[_]): Future[SubscriptionDetails] =
    cdsFrontendDataCache.subscriptionDetails

}
