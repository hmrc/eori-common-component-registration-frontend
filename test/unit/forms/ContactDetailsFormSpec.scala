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

package unit.forms

import base.UnitSpec
import play.api.data.FormError
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsViewModel

class ContactDetailsFormSpec extends UnitSpec {

  def form = ContactDetailsForm.contactDetailsCreateForm()

  "contactDetailsCreateForm" should {

    "map an empty phone number to a None" in {
      val data = Map("full-name" -> "Some name", "telephone" -> "")
      val res  = form.bind(data)
      res.errors shouldBe Seq.empty
      res.value shouldBe Some(ContactDetailsViewModel(fullName = "Some name", emailAddress = None, telephone = None))
    }

    "map a valid phone number to a Some" in {
      val data = Map("full-name" -> "Some name", "telephone" -> "012345678")
      val res  = form.bind(data)
      res.errors shouldBe Seq.empty
      res.value shouldBe Some(
        ContactDetailsViewModel(fullName = "Some name", emailAddress = None, telephone = Some("012345678"))
      )
    }

    "return an error where the phone number is too long" in {
      val data = Map("full-name" -> "Some name", "telephone" -> "123456789012345678901234567890")
      val res  = form.bind(data)
      res.errors shouldBe Seq(FormError("telephone", "cds.contact-details.page-error.telephone.wrong-length.too-long"))
    }

  }

}
