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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.play.language.LanguageUtils

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class EoriLanguageController @Inject() (languageUtils: LanguageUtils, cc: ControllerComponents)
    extends AbstractController(cc) with I18nSupport {
  private def fallbackURL(service: Service): String = routes.ApplicationController.startRegister(service).url
  def languageMap: Map[String, Lang]        = EoriLanguageController.languageMap

  private val SwitchIndicatorKey       = "switching-language"
  private val FlashWithSwitchIndicator = Flash(Map(SwitchIndicatorKey -> "true"))

  def switchToLanguage(language: String, service: Service): Action[AnyContent] = Action { implicit request =>
    val enabled: Boolean = languageMap.get(language).exists(languageUtils.isLangAvailable)
    val lang: Lang =
      if (enabled) languageMap.getOrElse(language, languageUtils.getCurrentLang)
      else languageUtils.getCurrentLang

    val redirectURL: String = request.headers
      .get(REFERER)
      .flatMap(asRelativeUrl)
      .getOrElse(fallbackURL(service))
    Redirect(redirectURL).withLang(Lang.apply(lang.code)).flashing(FlashWithSwitchIndicator)
  }

  private def asRelativeUrl(url: String): Option[String] =
    for {
      uri      <- Try(new URI(url)).toOption
      path     <- Option(uri.getRawPath).filterNot(_.isEmpty)
      query    <- Option(uri.getRawQuery).map("?" + _).orElse(Some(""))
      fragment <- Option(uri.getRawFragment).map("#" + _).orElse(Some(""))
    } yield s"$path$query$fragment"

}

object EoriLanguageController {

  def routeToSwitchLanguage: (String, Service) => Call =
    (lang: String, service: Service) => routes.EoriLanguageController.switchToLanguage(lang, service)

  def languageMap: Map[String, Lang] =
    Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))

}
