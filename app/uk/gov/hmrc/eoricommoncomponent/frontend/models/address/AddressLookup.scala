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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.address

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import scala.util.Try

sealed trait AddressLookupResponse

case class AddressLookupSuccess(addresses: Seq[Address]) extends AddressLookupResponse {

  def sorted(): AddressLookupSuccess = {
    val sortedAddresses = addresses.sortWith { (a, b) =>
      val numbersInA = numbersIn(a)
      val numbersInB = numbersIn(b)

      def sort(zipped: Seq[(Option[Int], Option[Int])]): Boolean = zipped match {
        case (Some(nA), Some(nB)) :: tail if nA == nB => sort(tail)
        case (Some(nA), Some(nB)) :: _ => nA < nB
        case (Some(_), None) :: _ => true
        case (None, Some(_)) :: _ => false
        case _ => a.addressLine1.toLowerCase < b.addressLine1.toLowerCase
      }

      sort(numbersInA.zipAll(numbersInB, None, None).toList)
    }

    AddressLookupSuccess(sortedAddresses)
  }

  private def numbersIn(address: Address): Seq[Option[Int]] =
    "([0-9]+)".r.findAllIn(address.addressLine1.toLowerCase).map(n => Try(n.toInt).toOption).toSeq.reverse :+ None

}
