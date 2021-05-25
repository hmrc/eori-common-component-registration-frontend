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

package uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers

import play.api.data.Field
import play.twirl.api.Html

object repeatFields {

  def apply(field1: Field, field2: Field, min: Int = 1)(fieldRenderer: (Field, Field) => Html): Seq[Html] = {
    val indexesMax = if (field1.indexes.size > field2.indexes.size) field1.indexes else field2.indexes
    val indexes = indexesMax match {
      case Nil                              => 0 until min
      case complete if complete.size >= min => complete
      case partial                          =>
        // We don't have enough elements, append indexes starting from the largest
        val start  = partial.max + 1
        val needed = min - partial.size
        partial ++ (start until (start + needed))
    }

    indexes.map(i => fieldRenderer(field1("[" + i + "]"), field2("[" + i + "]")))
  }

}
