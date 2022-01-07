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

package common.support.testdata

import common.support.testdata.subscription.SubscriptionDataGenerators
import org.scalacheck.{Gen, Prop}
import org.scalatestplus.scalacheck.Checkers

trait GenTestRunner extends SubscriptionDataGenerators with Checkers {

  def testWithGen[T](gen: Gen[T])(test: T => Unit): Unit =
    check(Prop.forAllNoShrink(gen) { generatedValue =>
      test(generatedValue)
      Prop.proved
    })

}
