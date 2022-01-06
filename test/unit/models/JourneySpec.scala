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

package unit.models

import org.scalatest.{EitherValues, MustMatchers, OptionValues, WordSpec}
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey

class JourneySpec extends WordSpec with MustMatchers with EitherValues with OptionValues {

  "Journey" must {

    val pathBindable  = implicitly[PathBindable[Journey.Value]]
    val queryBindable = implicitly[QueryStringBindable[Journey.Value]]

    "bind to `Register` from path" in {

      val result =
        pathBindable.bind("key", "register").right.value

      result mustEqual Journey.Register
    }

    "bind to `Subscription` from path" in {

      val result =
        pathBindable.bind("key", "subscribe").right.value

      result mustEqual Journey.Subscribe
    }

    "fail to bind anything else from path" in {

      val result =
        pathBindable.bind("key", "foobar").left.value

      result mustEqual "invalid value"
    }

    "unbind from `Register` to path" in {

      val result =
        pathBindable.unbind("key", Journey.Register)

      result mustEqual "register"
    }

    "unbind from `Subscription` to path" in {

      val result =
        pathBindable.unbind("key", Journey.Subscribe)

      result mustEqual "subscribe"
    }

    "bind to `Register` from query" in {

      val result =
        queryBindable.bind("key", Map("key" -> Seq("register"))).value.right.value

      result mustEqual Journey.Register
    }

    "bind to `Subscription` from query" in {

      val result =
        queryBindable.bind("key", Map("key" -> Seq("subscribe"))).value.right.value

      result mustEqual Journey.Subscribe
    }

    "fail to bind anything else from query" in {

      val result =
        queryBindable.bind("key", Map("key" -> Seq("foobar"))).value.left.value

      result mustEqual "invalid value"
    }

    "unbind from `Register` to query" in {

      val result =
        queryBindable.unbind("key", Journey.Register)

      result mustEqual "register"
    }

    "unbind from `Subscription` to query" in {

      val result =
        queryBindable.unbind("key", Journey.Subscribe)

      result mustEqual "subscribe"
    }
  }
}
