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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{CachedData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.ContactDetailsAdaptor
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionDetailsService @Inject() (
  sessionCache: SessionCache,
  contactDetailsAdaptor: ContactDetailsAdaptor,
  save4LaterConnector: Save4LaterConnector
)(implicit ec: ExecutionContext) {

  def saveKeyIdentifiers(groupId: GroupId, internalId: InternalId, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Unit] = {
    val key = CachedData.groupIdKey
    sessionCache.safeId.flatMap { safeId =>
      val cacheIds = CacheIds(internalId, safeId, Some(service.code))
      save4LaterConnector.put[CacheIds](groupId.id, key, cacheIds)
    }
  }

  def saveSubscriptionDetails(
    insertNewDetails: SubscriptionDetails => SubscriptionDetails
  )(implicit request: Request[_]): Future[Unit] =
    sessionCache.subscriptionDetails flatMap { subDetails =>
      sessionCache.saveSubscriptionDetails(insertNewDetails(subDetails)).map(_ => ())
    }

  def cacheContactDetails(contactDetailsModel: ContactDetailsModel, isInReviewMode: Boolean = false)(implicit
    request: Request[_]
  ): Future[Unit] =
    contactDetails(contactDetailsModel, isInReviewMode) flatMap { contactDetails =>
      saveSubscriptionDetails(sd => sd.copy(contactDetails = Some(contactDetails)))
    }

  def cacheContactAddressDetails(address: AddressViewModel, contactDetails: ContactDetailsModel)(implicit
    request: Request[_]
  ): Future[Unit] = {
    val updatedAddress = address.copy(postcode = address.postcode.filter(_.nonEmpty))
    saveSubscriptionDetails(sd =>
      sd.copy(contactDetails =
        Some(
          ContactDetailsModel(
            contactDetails.fullName,
            contactDetails.emailAddress,
            contactDetails.telephone,
            contactDetails.fax,
            contactDetails.useAddressFromRegistrationDetails,
            Some(updatedAddress.street),
            Some(updatedAddress.city),
            updatedAddress.postcode,
            Some(updatedAddress.countryCode)
          )
        )
      )
    )
  }

  def cacheAddressDetails(address: ContactAddressMatchModel)(implicit request: Request[_]): Future[Unit] = {
    saveSubscriptionDetails(sd =>
      sd.copy(contactDetails =
        sd.contactDetails.map(cdm =>
          cdm.copy(
            street = Some(s"${address.lineOne} ${address.lineTwo.getOrElse("")}"),
            city = Some(s"${address.townCity}"),
            postcode = Some(s"${address.postcode}"),
            countryCode = Some(s"${address.country}")
          )
        )
      )
    )
  }

  def cacheNameDetails(
    nameOrganisationMatchModel: NameOrganisationMatchModel
  )(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameOrganisationDetails = Some(nameOrganisationMatchModel)))

  def cacheEmbassyName(embassyName: String)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(embassyName = Some(embassyName)))

  def cachedNameDetails(implicit request: Request[_]): Future[Option[NameOrganisationMatchModel]] =
    sessionCache.subscriptionDetails map (_.nameOrganisationDetails)

  def cachedUtrMatch(implicit request: Request[_]): Future[Option[UtrMatchModel]] =
    sessionCache.subscriptionDetails map (_.formData.utrMatch)

  def cachedNinoMatch(implicit request: Request[_]): Future[Option[NinoMatchModel]] =
    sessionCache.subscriptionDetails map (_.formData.ninoMatch)

  def cachedOrganisationType(implicit request: Request[_]): Future[Option[CdsOrganisationType]] =
    sessionCache.subscriptionDetails map (_.formData.organisationType)

  def cacheSicCode(sicCode: String)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(sicCode = Some(sicCode)))

  def cacheDateEstablished(date: LocalDate)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(dateEstablished = Some(date)))

  def cacheNameDobDetails(nameDob: NameDobMatchModel)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameDobDetails = Some(nameDob)))

  def cachedNameDobDetails(implicit request: Request[_]): Future[Option[NameDobMatchModel]] =
    sessionCache.subscriptionDetails.map(_.nameDobDetails)

  def cacheVatVerificationOption(
    verificationOption: VatVerificationOption
  )(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(vatVerificationOption = Some(verificationOption.isDateOption)))

  def cacheNinoOrUtrChoice(ninoOrUtrChoice: NinoOrUtrChoice)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(formData = sd.formData.copy(ninoOrUtrChoice = ninoOrUtrChoice.ninoOrUtrRadio)))

  def cacheUtrMatch(utrMatch: Option[UtrMatchModel])(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(formData = sd.formData.copy(utrMatch = utrMatch)))

  def cacheNinoMatch(ninoMatch: Option[NinoMatchModel])(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(formData = sd.formData.copy(ninoMatch = ninoMatch)))

  def cacheUkVatDetails(ukVatDetails: VatDetails)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(ukVatDetails = Some(ukVatDetails)))

  def clearCachedUkVatDetails(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(ukVatDetails = None))

  def clearCachedVatControlListResponse()(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(vatControlListResponse = None))

  def cacheVatControlListResponse(
    vatControlListResponse: VatControlListResponse
  )(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(vatControlListResponse = Some(vatControlListResponse)))

  def cacheVatRegisteredUk(yesNoAnswer: YesNo)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(vatRegisteredUk = Some(yesNoAnswer.isYes)))

  def cacheConsentToDisclosePersonalDetails(yesNoAnswer: YesNo)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(personalDataDisclosureConsent = Some(yesNoAnswer.isYes)))

  private def contactDetails(view: ContactDetailsModel, isInReviewMode: Boolean)(implicit
    request: Request[_]
  ): Future[ContactDetailsModel] =
    if (!isInReviewMode && view.useAddressFromRegistrationDetails)
      sessionCache.registrationDetails map { registrationDetails =>
        contactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(view, registrationDetails.address)
      }
    else Future.successful(view)

  def cachedCustomsId(implicit request: Request[_]): Future[Option[CustomsId]] =
    sessionCache.subscriptionDetails map (_.customsId)

  def cachedEmbassyName(implicit request: Request[_]): Future[Option[String]] =
    sessionCache.subscriptionDetails map (_.embassyName)

  private def updateSubscriptionDetails(implicit request: Request[_]): Future[Unit] =
    for {
      subDetails <- sessionCache.subscriptionDetails
      _          <- sessionCache.saveSub01Outcome(Sub01Outcome(""))
      _          <- sessionCache.saveSubscriptionDetails(
                      SubscriptionDetails(
                        nameOrganisationDetails = subDetails.nameOrganisationDetails,
                        nameDobDetails = subDetails.nameDobDetails,
                        formData = subDetails.formData,
                        embassyName = subDetails.embassyName
                      )
                    )
    } yield ()

  def updateSubscriptionDetailsOrganisation(implicit request: Request[_]): Future[Unit] =
    for {
      _ <- sessionCache.saveRegistrationDetails(RegistrationDetailsOrganisation())
      _ <- updateSubscriptionDetails
    } yield ()

  def updateSubscriptionDetailsOrgName(orgName: String)(implicit request: Request[_]): Future[Unit] = {
    sessionCache.registrationDetails.flatMap { case rdo: RegistrationDetailsOrganisation =>
      sessionCache
        .saveRegistrationDetails(rdo.copy(name = orgName))
        .map(_ => updateSubscriptionDetails)
    }
  }

  def updateSubscriptionDetailsIndividual(implicit request: Request[_]): Future[Unit] =
    for {
      _ <- sessionCache.saveRegistrationDetails(RegistrationDetailsIndividual())
      _ <- updateSubscriptionDetails
    } yield ()

  def updateSubscriptionDetailsEmbassyName(embassyName: String)(implicit request: Request[_]): Future[Unit] =
    for {
      _ <- sessionCache.saveRegistrationDetails(RegistrationDetailsEmbassy(embassyName))
      _ <- updateSubscriptionDetails
    } yield ()

}
