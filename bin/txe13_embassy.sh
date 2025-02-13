#!/bin/bash

curl --location 'http://localhost:6751/txe13/eori/subscription/v1' \
--header 'Content-Type: application/json' \
--header 'Cookie: mdtpdi=mdtpdi#329c9d91-54a3-4b54-b772-2368ec46c636#1738929937440_JvKIODQQ1hyOSJ/Q7Z6s8g==' \
--data-raw '{
  "edgeCaseType": "01",
  "cdsFullName": "Masahiro Moro",
  "organisation": {
    "dateOfEstablishment": "",
    "organisationName": "Embassy Of Japan"
  },
  "cdsEstablishmentAddress": {
    "city": "London",
    "countryCode": "GB",
    "postcode": "SE28 1AA",
    "streetAndNumber": "101-104 Piccadilly, Greater London"
  },
  "legalStatus": "",
  "separateCorrespondenceAddressIndicator": true,
  "consentToDisclosureOfPersonalData": true,
  "contactInformation": {
    "personOfContact": "Masahiro Moro",
    "streetAndNumber": "101-104 Piccadilly Greater London",
    "city": "London",
    "countryCode": "GB",
    "isAgent": true,
    "isGroup": false,
    "email": "masahiro.moro@gmail.com",
    "postcode": "SE28 1AA",
    "telephoneNumber": "07806674501"
  },
  "serviceName": "HMRC-GVMS-ORG"
}'
