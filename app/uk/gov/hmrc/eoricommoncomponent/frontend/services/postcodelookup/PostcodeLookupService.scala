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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.postcodelookup

import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector.AddressLookupException
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.AddressLookupSuccess
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostcodeLookupService @Inject() (sessionCache: SessionCache, addressLookupConnector: AddressLookupConnector)(implicit ec: ExecutionContext) {

  def lookup()(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[(AddressLookupSuccess, PostcodeViewModel)]] = {
    sessionCache.getPostcodeAndLine1Details.flatMap {
      case None => Future.successful(None)
      case Some(postcodeViewModel) => {
        addressLookupConnector
          .lookup(postcodeViewModel.sanitisedPostcode, postcodeViewModel.addressLine1)
          .flatMap {
            case addressesResponse @ AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.lookupFieldsDefined) =>
              Future.successful(Some((addressesResponse, postcodeViewModel)))
            case AddressLookupSuccess(_) if postcodeViewModel.containsLineOne => {
              addressLookupConnector.lookup(postcodeViewModel.sanitisedPostcode, None).flatMap {
                case addressesResponse @ AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.lookupFieldsDefined) =>
                  sessionCache
                    .savePostcodeAndLine1Details(postcodeViewModel.copy(addressLine1 = None))
                    .flatMap(_ => Future.successful(Some((addressesResponse, postcodeViewModel))))
                case _ => Future.successful(None)
              }
            }
            case AddressLookupSuccess(_) => Future.successful(None)
          }
          .recoverWith { case _: AddressLookupException.type =>
            Future.successful(None)
          }
      }
    }
  }

  def lookupNoRepeat()(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[(AddressLookupSuccess, PostcodeViewModel)]] = {
    sessionCache.getPostcodeAndLine1Details.flatMap {
      case None => Future.successful(None)
      case Some(postcodeViewModel) => {
        addressLookupConnector
          .lookup(postcodeViewModel.sanitisedPostcode, postcodeViewModel.addressLine1)
          .flatMap {
            case addressesResponse @ AddressLookupSuccess(addresses) if addresses.nonEmpty && addresses.forall(_.lookupFieldsDefined) =>
              Future.successful(Some((addressesResponse, postcodeViewModel)))
            case _ => Future.successful(None)
          }
          .recoverWith { case _: AddressLookupException.type =>
            Future.successful(None)
          }
      }
    }
  }
}
