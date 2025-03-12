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

package util.scalacheck

import org.scalacheck.Gen
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import util.scalacheck.TestDataGenerators.Implicits._

import java.time.LocalDate

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

  def oversizedNameGenerator(maxLengthConstraint: Int = maxLengthOfName): Gen[String] =
    nameGenerator.oversizeWithAlphaChars(maxLengthConstraint)

  case class IndividualGens[E](
    firstNameGen: Gen[String] = nameGenerator,
    lastNameGen: Gen[String] = nameGenerator,
    extraBitGen: Gen[E]
  )

  sealed trait AbstractIndividualGenerator[E, Result] {

    protected case class DataItems(firstName: String, lastName: String, extraBit: E)

    def apply(result: DataItems): Result

    def apply(gens: IndividualGens[E]): Gen[Result] =
      for {
        firstName <- gens.firstNameGen
        lastName  <- gens.lastNameGen
        extraBit  <- gens.extraBitGen
      } yield apply(DataItems(firstName, lastName, extraBit))

  }

  def individualNameAndDateOfBirthGens(): IndividualGens[LocalDate] = IndividualGens(extraBitGen = dateOfBirthGenerator)

  val individualNameAndDateOfBirthGenerator: AbstractIndividualGenerator[LocalDate, IndividualNameAndDateOfBirth] =
    new AbstractIndividualGenerator[LocalDate, IndividualNameAndDateOfBirth] {

      def apply(data: DataItems): IndividualNameAndDateOfBirth =
        IndividualNameAndDateOfBirth(data.firstName, data.lastName, dateOfBirth = data.extraBit)

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
          s: String   <- strings
          toOversize   = 0 max (maxLength - s.length + 1)
          extraLength <- Gen.chooseNum(toOversize, maxLength)
          extraString <- Gen.listOfN(extraLength, extraGen) map (_.mkString)
        } yield s + extraString

      def oversizeWithAlphaChars(maxLength: Int): Gen[String] = oversized(maxLength)(Gen.alphaChar)

      def oversizeWithAlphaNumChars(maxLength: Int): Gen[String] = oversized(maxLength)(Gen.alphaNumChar)
    }

  }

}
