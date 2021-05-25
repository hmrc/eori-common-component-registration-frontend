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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{
  AddressViewModel,
  CompanyRegisteredCountry,
  VatDetails
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{CachedData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.ContactDetailsAdaptor
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionDetailsService @Inject() (
  sessionCache: SessionCache,
  contactDetailsAdaptor: ContactDetailsAdaptor,
  save4LaterConnector: Save4LaterConnector
)(implicit ec: ExecutionContext) {

  def saveKeyIdentifiers(groupId: GroupId, internalId: InternalId, service: Service)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val key = CachedData.groupIdKey
    sessionCache.safeId.flatMap { safeId =>
      val cacheIds = CacheIds(internalId, safeId, Some(service.code))
      save4LaterConnector.put[CacheIds](groupId.id, key, cacheIds)
    }
  }

  def saveSubscriptionDetails(
    insertNewDetails: SubscriptionDetails => SubscriptionDetails
  )(implicit hc: HeaderCarrier): Future[Unit] = sessionCache.subscriptionDetails flatMap { subDetails =>
    sessionCache.saveSubscriptionDetails(insertNewDetails(subDetails)).map(_ => ())

  }

  def cacheCompanyShortName(shortName: BusinessShortName)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(businessShortName = Some(shortName)))

  def cachedCompanyShortName(implicit hc: HeaderCarrier): Future[Option[BusinessShortName]] =
    sessionCache.subscriptionDetails.map(_.businessShortName)

  def cacheContactDetails(contactDetailsModel: ContactDetailsModel, isInReviewMode: Boolean = false)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    contactDetails(contactDetailsModel, isInReviewMode) flatMap { contactDetails =>
      saveSubscriptionDetails(sd => sd.copy(contactDetails = Some(contactDetails)))
    }

  def cacheAddressDetails(address: AddressViewModel)(implicit hc: HeaderCarrier): Future[Unit] = {
    def noneForEmptyPostcode(a: AddressViewModel) = a.copy(postcode = a.postcode.filter(_.nonEmpty))

    saveSubscriptionDetails(sd => sd.copy(addressDetails = Some(noneForEmptyPostcode(address))))
  }

  def cachedAddressDetails(implicit hc: HeaderCarrier): Future[Option[AddressViewModel]] =
    sessionCache.subscriptionDetails map (_.addressDetails)

  def cacheNameIdDetails(
    nameIdOrganisationMatchModel: NameIdOrganisationMatchModel
  )(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameIdOrganisationDetails = Some(nameIdOrganisationMatchModel)))

  def cacheNameAndCustomsId(name: String, customsId: CustomsId)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(
      sd =>
        sd.copy(
          nameIdOrganisationDetails = Some(NameIdOrganisationMatchModel(name, customsId.id)),
          customsId = Some(customsId)
        )
    )

  def cachedNameIdDetails(implicit hc: HeaderCarrier): Future[Option[NameIdOrganisationMatchModel]] =
    sessionCache.subscriptionDetails map (_.nameIdOrganisationDetails)

  def cacheNameDetails(
    nameOrganisationMatchModel: NameOrganisationMatchModel
  )(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameOrganisationDetails = Some(nameOrganisationMatchModel)))

  def cachedNameDetails(implicit hc: HeaderCarrier): Future[Option[NameOrganisationMatchModel]] =
    sessionCache.subscriptionDetails map (_.nameOrganisationDetails)

  def cachedUtrMatch(implicit hc: HeaderCarrier): Future[Option[UtrMatchModel]] =
    sessionCache.subscriptionDetails map (_.formData.utrMatch)

  def cachedNinoMatch(implicit hc: HeaderCarrier): Future[Option[NinoMatchModel]] =
    sessionCache.subscriptionDetails map (_.formData.ninoMatch)

  def cachedOrganisationType(implicit hc: HeaderCarrier): Future[Option[CdsOrganisationType]] =
    sessionCache.subscriptionDetails map (_.formData.organisationType)

  def cacheSicCode(sicCode: String)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(sicCode = Some(sicCode)))

  def cacheEoriNumber(eoriNumber: String)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(eoriNumber = Some(eoriNumber)))

  def cacheDateEstablished(date: LocalDate)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(dateEstablished = Some(date)))

  def cachePersonalDataDisclosureConsent(consent: Boolean)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(personalDataDisclosureConsent = Some(consent)))

  def cacheNameDobDetails(nameDob: NameDobMatchModel)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameDobDetails = Some(nameDob)))

  def cachedNameDobDetails(implicit hc: HeaderCarrier): Future[Option[NameDobMatchModel]] =
    sessionCache.subscriptionDetails.map(_.nameDobDetails)

  def cacheIdDetails(idMatchModel: IdMatchModel)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(idDetails = Some(idMatchModel)))

  def cacheCustomsId(subscriptionCustomsId: CustomsId)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(customsId = Some(subscriptionCustomsId)))

  def cacheNinoOrUtrChoice(ninoOrUtrChoice: NinoOrUtrChoice)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(
      sd => sd.copy(formData = sd.formData.copy(ninoOrUtrChoice = ninoOrUtrChoice.ninoOrUtrRadio))
    )

  def cacheNinoMatchForNoAnswer(ninoMatch: Option[NinoMatchModel])(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(formData = sd.formData.copy(ninoMatch = ninoMatch), customsId = None))

  def cacheUtrMatchForNoAnswer(utrMatch: Option[UtrMatchModel])(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(
      sd =>
        sd.copy(formData = sd.formData.copy(utrMatch = utrMatch), customsId = None, nameIdOrganisationDetails = None)
    )

  def cacheUtrMatch(utrMatch: Option[UtrMatchModel])(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(formData = sd.formData.copy(utrMatch = utrMatch)))

  def cacheNinoMatch(ninoMatch: Option[NinoMatchModel])(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(formData = sd.formData.copy(ninoMatch = ninoMatch)))

  def clearCachedCustomsId(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(customsId = None))

  def cacheUkVatDetails(ukVatDetails: VatDetails)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(ukVatDetails = Some(ukVatDetails)))

  def clearCachedUkVatDetails(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(ukVatDetails = None))

  def cacheVatRegisteredUk(yesNoAnswer: YesNo)(implicit hq: HeaderCarrier) =
    saveSubscriptionDetails(sd => sd.copy(vatRegisteredUk = Some(yesNoAnswer.isYes)))

  def cacheConsentToDisclosePersonalDetails(yesNoAnswer: YesNo)(implicit hq: HeaderCarrier) =
    saveSubscriptionDetails(sd => sd.copy(personalDataDisclosureConsent = Some(yesNoAnswer.isYes)))

  private def contactDetails(view: ContactDetailsModel, isInReviewMode: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[ContactDetailsModel] =
    if (!isInReviewMode && view.useAddressFromRegistrationDetails)
      sessionCache.registrationDetails map { registrationDetails =>
        contactDetailsAdaptor.toContactDetailsModelWithRegistrationAddress(view, registrationDetails.address)
      }
    else Future.successful(view)

  def cacheVatRegisteredEu(yesNoAnswer: YesNo)(implicit hq: HeaderCarrier): Future[Unit] =
    for {
      existingHolder <- sessionCache.subscriptionDetails
      updatedHolder = existingHolder.copy(vatRegisteredEu = Some(yesNoAnswer.isYes))
      _ <- sessionCache.saveSubscriptionDetails(updatedHolder)
    } yield ()

  def cachedCustomsId(implicit hc: HeaderCarrier): Future[Option[CustomsId]] =
    sessionCache.subscriptionDetails map (_.customsId)

  def cacheExistingEoriNumber(eori: ExistingEori)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(existingEoriNumber = Some(eori)))

  def cachedExistingEoriNumber(implicit hc: HeaderCarrier): Future[Option[ExistingEori]] =
    sessionCache.subscriptionDetails map (_.existingEoriNumber)

  def updateSubscriptionDetails(implicit hc: HeaderCarrier) =
    // TODO: to be refactored by redesigning the cache
    sessionCache.subscriptionDetails flatMap { subDetails =>
      sessionCache.saveRegistrationDetails(RegistrationDetailsOrganisation())
      sessionCache.saveRegistrationDetails(RegistrationDetailsIndividual())
      sessionCache.saveSub01Outcome(Sub01Outcome(""))
      sessionCache.saveSubscriptionDetails(
        SubscriptionDetails(
          nameOrganisationDetails = subDetails.nameOrganisationDetails,
          nameDobDetails = subDetails.nameDobDetails,
          formData = subDetails.formData
        )
      )
    }

  def cacheRegisteredCountry(country: CompanyRegisteredCountry)(implicit hc: HeaderCarrier): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(registeredCompany = Some(country)))

  def cachedRegisteredCountry()(implicit hc: HeaderCarrier): Future[Option[CompanyRegisteredCountry]] =
    sessionCache.subscriptionDetails.map(_.registeredCompany)

}
