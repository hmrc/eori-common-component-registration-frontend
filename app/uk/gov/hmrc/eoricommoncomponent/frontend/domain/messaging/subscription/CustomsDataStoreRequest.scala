package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CaseClassAuditHelper


case class CustomsDataStoreRequest(eori: String, address: String, timestamp: String) extends CaseClassAuditHelper

object CustomsDataStoreRequest {
  implicit val jsonFormat: OFormat[CustomsDataStoreRequest] = Json.format[CustomsDataStoreRequest]
}
