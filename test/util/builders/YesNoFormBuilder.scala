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

package util.builders

// TODO Remove this object, Maps are totally unnecessary, because tests overrides all those values
// Use Map directly in test spec
object YesNoFormBuilder {

  val ValidRequest = Map("yes-no-answer" -> true.toString)

  val validRequestNo = Map("yes-no-answer" -> false.toString)

  val invalidRequest = Map("yes-no-answer" -> "")
}
