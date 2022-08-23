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

package unit.controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import us.codecraft.xsoup.Xsoup

import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

case class CdsPage(html: String) {

  private val page = Jsoup.parse(html)

  def elementIsPresent(xpath: String): Boolean =
    !Xsoup.compile(xpath).evaluate(page).getElements.isEmpty

  def getElementValue(xpath: String): String =
    selectElement(xpath).`val`()

  def getElementValueForLabel(labelXpath: String): String = {
    val elementId = getElementAttributeFor(labelXpath)
    val element = Option(page.getElementById(elementId))
      .getOrElse(throw new IllegalStateException(s"Input element with ID '$elementId' was not found on the page."))
    element.`val`()
  }

  def getElementsTextAtIndex(xpath: String, index: Int): String = selectElements(xpath).get(index).text()

  def getSummaryListValue(xpath: String, key: String): String =
    selectElements(xpath)
      .find(row => row.getElementsByClass("govuk-summary-list__key").text() == key)
      .fold("")(_.getElementsByClass("govuk-summary-list__value").text())

  def getSummaryListLink(xpath: String, key: String, action: String): String =
    selectElements(xpath)
      .find(row => row.getElementsByClass("govuk-summary-list__key").text() == key)
      .fold("")(
        _.getElementsByClass("govuk-summary-list__actions")
          .select("a.govuk-link").find(e => e.text().contains(action)).fold("")(_.text())
      )

  def getSummaryListHref(xpath: String, key: String, action: String): String =
    selectElements(xpath)
      .find(row => row.getElementsByClass("govuk-summary-list__key").text() == key)
      .get.getElementsByClass("govuk-summary-list__actions")
      .select("a.govuk-link").find(e => e.text().contains(action)).fold("")(_.attr("href"))

  def summaryListElementPresent(xpath: String, key: String) =
    selectElements(xpath)
      .find(row => row.getElementsByClass("govuk-summary-list__key").text() == key).isDefined

  def summaryListHrefPresent(xpath: String, key: String, action: String): Boolean =
    selectElements(xpath)
      .find(row => row.getElementsByClass("govuk-summary-list__key").text() == key)
      .get.getElementsByClass("govuk-summary-list__actions")
      .select("a.govuk-link").find(e => e.text().contains(action)).fold(false)(_ => true)

  def getElementText(xpath: String): String =
    selectElement(xpath).text()

  def getElementsText(xpath: String): String =
    selectElements(xpath).text()

  def getElementsHtml(xpath: String): String =
    selectElements(xpath).html()

  def getElementsHref(xpath: String): String =
    selectElements(xpath).attr("href")

  def getElementAttribute(pathToElement: String, attributeName: String): String =
    selectElement(pathToElement).attr(attributeName)

  def getElementAttributeFor(xpath: String): String = getElementAttribute(xpath, "for")

  def getElementAttributeHref(xpath: String): String = getElementAttribute(xpath, "href")

  def getElementAttributeAction(xpath: String): String = getElementAttribute(xpath, "action")

  def radioButtonChecked(xpath: String): Boolean = selectElement(xpath).hasAttr("checked")

  def title(): String = page.title()

  def h1(): String = page.getElementsByTag("h1").text()

  def h2(): String = page.getElementsByTag("h2").text()

  def formAction(formId: String): String = {
    val element = Option(page.getElementById(formId))
    element.fold(throw new IllegalStateException(s"Element with ID $formId was not found on the page."))(
      x => x.attr("action")
    )
  }

  private def selectElement(xpath: String): Element = {
    val selectedElements = selectElements(xpath)
    assertOneElementInSequence(xpath, selectedElements)
    selectedElements.first()
  }

  private def selectElements(xpath: String): Elements =
    Xsoup.compile(xpath).evaluate(page).getElements

  private def assertOneElementInSequence(xpath: String, selectedElements: Elements): Unit = {
    if (selectedElements.size() == 0) throw new IllegalStateException(s"Element not found in the page for xpath $xpath")
    if (selectedElements.size() > 1)
      throw new IllegalStateException(s"More than one element found in the page for xpath $xpath")
  }

}
