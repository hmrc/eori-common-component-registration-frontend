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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.emailaddress

import com.google.inject.ImplementedBy
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.emailaddress.DomainModel.{Domain, EmailAddress, Mailbox}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.emailaddress.EmailAddressValidation.validEmail

import javax.naming.Context.INITIAL_CONTEXT_FACTORY as ICF
import javax.inject.Singleton
import javax.naming.directory.InitialDirContext
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.matching.Regex

object DomainModel:
  opaque type EmailAddress <: String = String
  opaque type Mailbox <: String = String
  opaque type Domain <: String = String

  extension (emailAddress: EmailAddress)
    def mailbox: Mailbox = emailAddress match {
      case EmailAddressValidation.validEmail(m, _) => Mailbox(m)
      case _ => throw new IllegalStateException("EmailAddress contains an invalid email address")
    }

    def domain: Domain = emailAddress match {
      case EmailAddressValidation.validEmail(_, d) => Domain(d)
      case _ => throw new IllegalStateException("EmailAddress contains an invalid email address")
    }

object EmailAddress:
  def apply(value: String): EmailAddress = value match {
    case EmailAddressValidation.validEmail(_, d) =>
      Domain(d)
      value.asInstanceOf[EmailAddress]
    case invalidEmail =>
      throw new IllegalArgumentException(s"'$invalidEmail' is not a valid email address")
  }

object Mailbox:
  def apply(value: String): Mailbox = value.asInstanceOf[Mailbox]

object Domain:
  def apply(value: String): Domain = value match {
    case EmailAddressValidation.validDomain(_) => value.asInstanceOf[Domain]
    case invalidDomain =>
      throw new IllegalArgumentException(s"'$invalidDomain' is not a valid email domain")
  }

@ImplementedBy(classOf[EmailAddressValidation])
trait EmailValidation {
  def isValid(email: String): Boolean
}

@Singleton
class EmailAddressValidation extends EmailValidation {
  private val DNS_CONTEXT_FACTORY = "com.sun.jndi.dns.DnsContextFactory"
  private val env = new java.util.Hashtable[String, String]()
  env.put(ICF, DNS_CONTEXT_FACTORY)
  private def isHostMailServer(domain: String) = {
    val ictx = new InitialDirContext(env)

    def getAttributeValue(domain: String, attribute: String) =
      Try {
        ictx.getAttributes(domain, Array(attribute)).getAll.asScala.toList
      }.toEither

    getAttributeValue(domain, "MX") match {
      case Right(value) if value.nonEmpty => true
      case _ =>
        getAttributeValue(domain, "A") match {
          case Right(value) => value.nonEmpty
          case Left(_) => false
        }
    }
  }

  def isValid(email: String): Boolean =
    email match {
      case validEmail(_, _) if isHostMailServer(EmailAddress(email).domain) => true
      case _ => false
    }
}

object EmailAddressValidation {
  val validEmail: Regex = """^([a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)$""".r
  val validDomain: Regex = """^([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)$""".r
}
