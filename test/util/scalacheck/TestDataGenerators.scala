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

package util.scalacheck

import java.time.LocalDate
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import util.scalacheck.TestDataGenerators.Implicits._

trait TestDataGenerators {

  val emptyString: Gen[String] = Gen.const("")

  val maxLengthOfName: Int = MatchingForms.Length35

  val nameGenerator: Gen[String] = for {
    nameLength <- Gen.chooseNum(1, maxLengthOfName)
    name       <- Gen.listOfN(nameLength, Gen.alphaChar) map (_.mkString)
  } yield name

  val dateOfBirthGenerator = for {
    days  <- Gen.chooseNum(1, 365)
    years <- Gen.chooseNum(0, 110)
  } yield LocalDate.now() minusYears years minusDays days

  val maxLengthOfAddressLine: Int = MatchingForms.Length35

  val addressLineGenerator: Gen[String] = for {
    nameLength <- Gen.chooseNum(1, maxLengthOfAddressLine)
    address    <- Gen.listOfN(nameLength, Gen.alphaNumChar) map (_.mkString)
  } yield address

  def oversizedNameGenerator(maxLengthConstraint: Int = maxLengthOfName): Gen[String] =
    nameGenerator.oversizeWithAlphaChars(maxLengthConstraint)

  def oversizedAddressLineGenerator(maxLengthConstraint: Int = maxLengthOfAddressLine): Gen[String] =
    addressLineGenerator.oversizeWithAlphaNumChars(maxLengthConstraint)

  // Quick and dirty approach for now, may need to extend this if validation tightens up.
  // TODO: should generate enough space not to breach the limit of an acceptable postcode length.
  val postcodeGenerator: Gen[String] = for {
    area     <- Gen.oneOf(Gen.alphaChar map (_.toString), Gen.listOfN(2, Gen.alphaChar) map (_.mkString))
    district <- Gen.chooseNum(1, 99)
    space    <- Gen.option(Gen.const(" "))
    sector   <- Gen.chooseNum(0, 9)
    unit     <- Gen.listOfN(2, Gen.alphaChar) map (_.mkString)
    spacePayload = space getOrElse ""
  } yield s"$area$district$spacePayload$sector$unit"

  val GBUpperCase = "GB"

  val countryGenerator: Gen[String] = for {
    capitalisedCaseCountry <- Gen.oneOf(GBUpperCase, "US", "ES", "NL")
    convertToLowerCase     <- Arbitrary.arbitrary[Boolean]
  } yield if (convertToLowerCase) capitalisedCaseCountry.toLowerCase else capitalisedCaseCountry

  val countryWithoutGBGenerator: Gen[String] = countryGenerator.filter(_.toLowerCase != GBUpperCase.toLowerCase)

  case class IndividualGens[E](
    firstNameGen: Gen[String] = nameGenerator,
    middleNameGen: Gen[Option[String]] = Gen.option(nameGenerator),
    lastNameGen: Gen[String] = nameGenerator,
    extraBitGen: Gen[E]
  )

  sealed trait AbstractIndividualGenerator[E, Result] {

    protected case class DataItems(firstName: String, middleName: Option[String], lastName: String, extraBit: E)

    def apply(result: DataItems): Result

    def apply(gens: IndividualGens[E]): Gen[Result] =
      for {
        firstName  <- gens.firstNameGen
        middleName <- gens.middleNameGen
        lastName   <- gens.lastNameGen
        extraBit   <- gens.extraBitGen
      } yield apply(DataItems(firstName, middleName, lastName, extraBit))

  }

  def individualNameAndDateOfBirthGens(): IndividualGens[LocalDate] = IndividualGens(extraBitGen = dateOfBirthGenerator)

  val individualNameAndDateOfBirthGenerator: AbstractIndividualGenerator[LocalDate, IndividualNameAndDateOfBirth] =
    new AbstractIndividualGenerator[LocalDate, IndividualNameAndDateOfBirth] {

      def apply(data: DataItems): IndividualNameAndDateOfBirth =
        IndividualNameAndDateOfBirth(data.firstName, data.middleName, data.lastName, dateOfBirth = data.extraBit)

    }

}

object TestDataGenerators {

  object Implicits {

    implicit class GenOps[T](val gen: Gen[T]) extends AnyVal {
      def asOption: Gen[Option[T]] = Gen.option(gen)

      def asMandatoryOption: Gen[Option[T]] = gen map (Some(_))
    }

    implicit class StringGenOps(val strings: Gen[String]) extends AnyVal {

      def oversized(maxLength: Int)(extraGen: Gen[Char]): Gen[String] =
        for {
          s: String <- strings
          toOversize = 0 max (maxLength - s.length + 1)
          extraLength <- Gen.chooseNum(toOversize, maxLength)
          extraString <- Gen.listOfN(extraLength, extraGen) map (_.mkString)
        } yield s + extraString

      def oversizeWithAlphaChars(maxLength: Int): Gen[String] = oversized(maxLength)(Gen.alphaChar)

      def oversizeWithAlphaNumChars(maxLength: Int): Gen[String] = oversized(maxLength)(Gen.alphaNumChar)
    }

  }

}
