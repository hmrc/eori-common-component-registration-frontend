/*
 * Copyright 2026 HM Revenue & Customs
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

package unit.domain.email.emailaddress

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.emailaddress.{Domain, EmailAddress, EmailAddressValidation, Mailbox}

class EmailAddressSpec extends AnyWordSpec with ScalaCheckPropertyChecks with Matchers with EmailAddressGenerators {

  "Creating an EmailAddress class" should {
    "work for a valid email" in {
      forAll(validEmailAddresses()) { address =>
        EmailAddress(address).value should be(address)
      }
    }

    "throw an exception for an invalid email" in {
      an[IllegalArgumentException] should be thrownBy EmailAddress("sausages")
    }

    "throw an exception for an valid email starting with invalid characters" in {
      forAll(validEmailAddresses()) { address =>
        an[IllegalArgumentException] should be thrownBy EmailAddress("§" + address)
      }
    }

    "throw an exception for an valid email ending with invalid characters" in {
      forAll(validEmailAddresses()) { address =>
        an[IllegalArgumentException] should be thrownBy EmailAddress(address + "§")
      }
    }

    "throw an exception for an empty email" in {
      an[IllegalArgumentException] should be thrownBy EmailAddress("")
    }

    "throw an exception for a repeated email" in {
      an[IllegalArgumentException] should be thrownBy EmailAddress("test@domain.comtest@domain.com")
    }

    "throw an exception when the '@' is missing" in {
      forAll { (s: String) =>
        whenever(!s.contains("@")) {
          an[IllegalArgumentException] should be thrownBy EmailAddress(s)
        }
      }
    }
  }

  "An EmailAddress class" should {
    "implicitly convert to a String of the address" in {
      val e: String = EmailAddress("test@domain.com")
      e should be("test@domain.com")
    }
    "toString to a String of the address" in {
      val e = EmailAddress("test@domain.com")
      e.toString should be("test@domain.com")
    }
    "have a local part" in forAll(validMailbox, validDomain) { (mailbox, domain) =>
      val exampleAddr = EmailAddress(s"$mailbox@$domain")
      exampleAddr.mailbox should (be(a[Mailbox]) and have(Symbol("value")(mailbox)))
      exampleAddr.domain should (be(a[Domain]) and have(Symbol("value")(domain)))
    }
  }

  "A email address domain" should {
    "be extractable from an address" in forAll(validMailbox, validDomain) { (mailbox, domain) =>
      EmailAddress(s"$mailbox@$domain").domain should (be(a[Domain]) and have(Symbol("value")(domain)))
    }
    "be creatable for a valid domain" in forAll(validDomain) { domain =>
      Domain(domain) should (be(a[Domain]) and have(Symbol("value")(domain)))
    }
    "not create for invalid domains" in {
      an[IllegalArgumentException] should be thrownBy Domain("")
      an[IllegalArgumentException] should be thrownBy Domain("e.")
      an[IllegalArgumentException] should be thrownBy Domain(".uk")
      an[IllegalArgumentException] should be thrownBy Domain(".com")
      an[IllegalArgumentException] should be thrownBy Domain("*domain")
    }
    "compare equal if identical" in forAll(validDomain, validMailbox, validMailbox) { (domain, mailboxA, mailboxB) =>
      val exampleA = EmailAddress(s"$mailboxA@$domain")
      val exampleB = EmailAddress(s"$mailboxB@$domain")
      exampleA.domain should equal(exampleB.domain)
    }
    "toString to a String of the domain" in {
      Domain("domain.com").toString should be("domain.com")
    }
    "implicitly convert to a String of the domain" in {
      val e: String = Domain("domain.com")
      e should be("domain.com")
    }
  }

  "An email domain validation" should {
    "return true for a domain that has MX record (ex: gmail.com)" in {
      val email = new EmailAddressValidation
      email.isValid("mike@gmail.com") shouldBe true
      email.isValid("mike@msn.co.uk") shouldBe true
    }
    "return false for an invalid domain" in {
      val email = new EmailAddressValidation
      email.isValid("mike@fortytwoisnotananswer.org") shouldBe false
    }
  }

  "A email address mailbox" should {

    "be extractable from an address" in forAll(validMailbox, validDomain) { (mailbox, domain) =>
      EmailAddress(s"$mailbox@$domain").mailbox should (be(a[Mailbox]) and have(Symbol("value")(mailbox)))
    }
    "compare equal" in forAll(validMailbox, validDomain, validDomain) { (mailbox, domainA, domainB) =>
      val exampleA = EmailAddress(s"$mailbox@$domainA")
      val exampleB = EmailAddress(s"$mailbox@$domainB")
      exampleA.mailbox should equal(exampleB.mailbox)
    }
    "toString to a String of the domain" in {
      EmailAddress("test@domain.com").mailbox.toString should be("test")
    }
    "implicitly convert to a String of the domain" in {
      val e: String = EmailAddress("test@domain.com").mailbox
      e should be("test")
    }
  }
}
