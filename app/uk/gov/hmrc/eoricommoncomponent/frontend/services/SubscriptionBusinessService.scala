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

import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionBusinessService @Inject() (cdsFrontendDataCache: SessionCache)(implicit ec: ExecutionContext) {

  def cachedContactDetailsModel(implicit hc: HeaderCarrier): Future[Option[ContactDetailsModel]] =
    cdsFrontendDataCache.subscriptionDetails map (_.contactDetails)

  def getCachedDateEstablished(implicit hc: HeaderCarrier): Future[LocalDate] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.dateEstablished.getOrElse(throw new IllegalStateException("No Date Of Establishment Cached"))
    }

  def maybeCachedDateEstablished(implicit hc: HeaderCarrier): Future[Option[LocalDate]] =
    cdsFrontendDataCache.subscriptionDetails map (_.dateEstablished)

  def getCachedSicCode(implicit hc: HeaderCarrier): Future[String] = cdsFrontendDataCache.subscriptionDetails map {
    _.sicCode.getOrElse(throw new IllegalStateException("No SIC Code Cached"))
  }

  def cachedSicCode(implicit hc: HeaderCarrier): Future[Option[String]] =
    cdsFrontendDataCache.subscriptionDetails map (_.sicCode)

  def getCachedPersonalDataDisclosureConsent(implicit hc: HeaderCarrier): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.personalDataDisclosureConsent.getOrElse(
        throw new IllegalStateException("No Personal Data Disclosure Consent Cached")
      )
    }

  def getCachedContactAddressConfirmation(implicit hc: HeaderCarrier): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.contactAddressConfirmation.getOrElse(throw new IllegalStateException("No Contact Address Confirmation Cached"))
    }

  def getCachedVatRegisteredUk(implicit hc: HeaderCarrier): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.vatRegisteredUk.getOrElse(
        throw new IllegalStateException("Whether the business is VAT registered in the UK has not been Cached")
      )
    }

  def addressOrException(implicit hc: HeaderCarrier): Future[AddressViewModel] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.addressDetails.getOrElse(throw new IllegalStateException("No Address Details Cached"))
    }

  def address(implicit hc: HeaderCarrier): Future[Option[AddressViewModel]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.addressDetails
    }

  def getCachedCustomsId(implicit hc: HeaderCarrier): Future[Option[CustomsId]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.customsId
    }

  def getCachedNinoOrUtrChoice(implicit hc: HeaderCarrier): Future[Option[String]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.formData.ninoOrUtrChoice
    }

  def getCachedUkVatDetails(implicit hc: HeaderCarrier): Future[Option[VatDetails]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.ukVatDetails
    }

  def retrieveSubscriptionDetailsHolder(implicit hc: HeaderCarrier): Future[SubscriptionDetails] =
    cdsFrontendDataCache.subscriptionDetails

}
