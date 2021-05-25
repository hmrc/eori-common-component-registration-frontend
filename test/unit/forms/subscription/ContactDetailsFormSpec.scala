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

package unit.forms.subscription

import base.UnitSpec
import play.api.data.FormError
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.ContactDetailsForm

import scala.util.Random

class ContactDetailsFormSpec extends UnitSpec {

  private val form = ContactDetailsForm.form()

  "Contact Details Form" should {

    "return errors" when {

      "full name and telephone is empty" in {

        val formData = Map("full-name" -> "", "telephone" -> "")

        val boundForm = form.bind(formData)

        val fullNameError  = FormError("full-name", "cds.subscription.contact-details.form-error.full-name")
        val telephoneError = FormError("telephone", "cds.contact-details.page-error.telephone.isEmpty")

        val expectedErrors = Seq(fullNameError, telephoneError)

        boundForm.errors shouldBe expectedErrors
      }

      "full name and telephone is too long" in {

        val fullName  = Random.alphanumeric.take(71).mkString("")
        val telephone = Seq.fill(25)(1).mkString("")
        val formData  = Map("full-name" -> fullName, "telephone" -> telephone)

        val boundForm = form.bind(formData)

        val fullNameError  = FormError("full-name", "cds.subscription.full-name.error.too-long")
        val telephoneError = FormError("telephone", "cds.contact-details.page-error.telephone.wrong-length.too-long")

        val expectedErrors = Seq(fullNameError, telephoneError)

        boundForm.errors shouldBe expectedErrors
      }

      "telephone don't match the regex" in {

        val formData = Map("full-name" -> "Full name", "telephone" -> "!@£$%^&")

        val boundForm = form.bind(formData)

        val telephoneError = FormError("telephone", "cds.contact-details.page-error.telephone.wrong-format")

        val expectedErrors = Seq(telephoneError)

        boundForm.errors shouldBe expectedErrors
      }
    }

    Seq("!", "\"", "$", "%", "&", "'").map { elem =>
      s"return errors for special character $elem in telephone input" in {

        val invalidTelephone = Seq.fill(10)(elem).mkString("")

        val formData = Map("full-name" -> "Full name", "telephone" -> invalidTelephone)

        val boundForm = form.bind(formData)

        val telephoneError = FormError("telephone", "cds.contact-details.page-error.telephone.wrong-format")

        boundForm.errors shouldBe Seq(telephoneError)
      }
    }

    "return no errors" when {

      "full name and telephone is correct" in {

        val formData = Map("full-name" -> "Full name", "telephone" -> "01234123123")

        val boundForm = form.bind(formData)

        boundForm.errors shouldBe Seq.empty
      }

      "telephone contains all allowed characters" in {

        val formData = Map("full-name" -> "Full name", "telephone" -> """() \ / - * # +""")

        val boundForm = form.bind(formData)

        boundForm.errors shouldBe Seq.empty
      }
    }

    "trim the full name and address" in {

      val formData = Map("full-name" -> "      Full name     ", "telephone" -> """     01234123123     """)

      val boundForm = form.bind(formData)

      boundForm.errors shouldBe Seq.empty

      boundForm.value.get.fullName shouldBe "Full name"
      boundForm.value.get.telephone shouldBe "01234123123"
    }
  }
}
