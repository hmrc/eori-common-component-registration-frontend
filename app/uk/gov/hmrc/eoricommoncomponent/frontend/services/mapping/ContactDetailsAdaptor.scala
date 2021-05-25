/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping

import javax.inject.Singleton
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel

@Singleton
class ContactDetailsAdaptor {

  def toContactDetailsModelWithRegistrationAddress(
    view: ContactDetailsModel,
    registrationAddress: Address
  ): ContactDetailsModel = {
    val fourLineAddress = AddressViewModel(registrationAddress)
    ContactDetailsModel(
      fullName = view.fullName,
      emailAddress = view.emailAddress,
      telephone = view.telephone,
      fax = view.fax,
      street = Some(fourLineAddress.street),
      city = Some(fourLineAddress.city),
      postcode = registrationAddress.postalCode.filterNot(p => p.isEmpty),
      countryCode = Some(registrationAddress.countryCode),
      useAddressFromRegistrationDetails = view.useAddressFromRegistrationDetails
    )
  }

}
