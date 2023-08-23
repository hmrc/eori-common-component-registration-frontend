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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import java.time.LocalDate
import com.google.inject.{ImplementedBy, Inject, Singleton}

@Singleton
class TimeServiceImpl @Inject() extends TimeService {
  def getTodaysDate: LocalDate = LocalDate.now()
}

@ImplementedBy(classOf[TimeServiceImpl])
trait TimeService {
  def getTodaysDate: LocalDate
}