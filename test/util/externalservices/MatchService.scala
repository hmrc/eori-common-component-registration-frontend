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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status._

object MatchService {
  val registerWithId        = "/register-with-id"
  val registerWithEoriAndId = "/register-with-eori-and-id"
  val subscribe             = "/subscribe"
  val handleSubscription    = "/handle-subscription"
  val feedBack              = "/feedback"

  private val RegistrationWithIdPath: UrlPattern = urlMatching("/register-with-id")

  def returnHandleSubscriptionResponse(): StubMapping =
    stubFor(
      post(urlEqualTo(handleSubscription))
        willReturn aResponse()
          .withStatus(NO_CONTENT)
          .withBody(s"""{}""".stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnCreateSubscriptionResponse(): StubMapping =
    stubFor(
      post(urlEqualTo(subscribe))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
            {
              "subscriptionCreateResponse": {
                "responseCommon": {
                  "status": "OK",
                  "processingDate": "2016-08-18T14:01:05Z",
                  "returnParameters": [
                    {
                      "paramName": "ETMPFORMBUNDLENUMBER",
                      "paramValue": "077063075008"
                    },
                    {
                      "paramName": "POSITION",
                      "paramValue": "GENERATE"
                    }
                  ]
                },
                "responseDetail": {
                  "EORINo": "ZZZ1ZZZZ23ZZZZZZZ"
                }
              }
            }
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnCreateSubscriptionResponseWithUserEnteredDOEAndPersonTypeOrganisation(): StubMapping =
    stubFor(
      post(urlEqualTo(subscribe)).withRequestBody(
        matchingJsonPath(
          "$.subscriptionCreateRequest.requestDetail[?(@.dateOfEstablishment == '1900-11-11')][?(@.typeOfPerson == '2')]"
        )
      )
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
            {
              "subscriptionCreateResponse": {
                "responseCommon": {
                  "status": "OK",
                  "processingDate": "2016-08-18T14:01:05Z",
                  "returnParameters": [
                    {
                      "paramName": "ETMPFORMBUNDLENUMBER",
                      "paramValue": "077063075008"
                    },
                    {
                      "paramName": "POSITION",
                      "paramValue": "GENERATE"
                    }
                  ]
                },
                "responseDetail": {
                  "EORINo": "ZZZ1ZZZZ23ZZZZZZZ"
                }
              }
            }
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnCreateSubscriptionResponseWithUserEnteredDOEAndPersonType(): StubMapping =
    stubFor(
      post(urlEqualTo(subscribe)).withRequestBody(
        matchingJsonPath(
          "$.subscriptionCreateRequest.requestDetail[?(@.dateOfEstablishment == '1900-11-11')][?(@.typeOfPerson == '1')]"
        )
      )
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
            {
              "subscriptionCreateResponse": {
                "responseCommon": {
                  "status": "OK",
                  "processingDate": "2016-08-18T14:01:05Z",
                  "returnParameters": [
                    {
                      "paramName": "ETMPFORMBUNDLENUMBER",
                      "paramValue": "077063075008"
                    },
                    {
                      "paramName": "POSITION",
                      "paramValue": "GENERATE"
                    }
                  ]
                },
                "responseDetail": {
                  "EORINo": "ZZZ1ZZZZ23ZZZZZZZ"
                }
              }
            }
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnCreateSubscriptionResponseWithREG06DOE(): StubMapping =
    stubFor(
      post(urlEqualTo(subscribe)).withRequestBody(
        matchingJsonPath("$.subscriptionCreateRequest.requestDetail[?(@.dateOfEstablishment == '2018-05-16')]")
      )
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
            {
              "subscriptionCreateResponse": {
                "responseCommon": {
                  "status": "OK",
                  "processingDate": "2016-08-18T14:01:05Z",
                  "returnParameters": [
                    {
                      "paramName": "ETMPFORMBUNDLENUMBER",
                      "paramValue": "077063075008"
                    },
                    {
                      "paramName": "POSITION",
                      "paramValue": "GENERATE"
                    }
                  ]
                },
                "responseDetail": {
                  "EORINo": "ZZZ1ZZZZ23ZZZZZZZ"
                }
              }
            }
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnReponseForRegisterWithEoriAndIdIndividualWithoutDOEAndPersonType(): StubMapping =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithEORIAndIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2018-05-16T09:00:00Z"
           |    },
           |    "responseDetail": {
           |      "caseNumber": "C001",
           |      "outcome": "PASS",
           |      "responseData": {
           |        "SAFEID": "XE0000000866191",
           |        "trader": {
           |          "fullName": " John Doe",
           |          "shortName": "reg-existing-sole-trader"
           |        },
           |        "establishmentAddress": {
           |          "streetAndNumber": " Line 1",
           |          "city": "City name",
           |          "postalCode": "SE28 1AA",
           |          "countryCode":  "GB"
           |        },
           |        "contactDetail": {
           |          "address": {
           |            "streetAndNumber": "Line 1",
           |            "city": "City name",
           |            "postalCode": "SE28 1AA",
           |            "countryCode": "GB"
           |          },
           |          "contactName": "John Doe",
           |          "phone": "1234567",
           |          "fax": "89067",
           |          "email": "john.doe@example.com"
           |        },
           |        "VATIDs": [ {"countryCode": "AD" ,"vatNumber": "1234" } , {
           |          "countryCode": "GB" ,"vatNumber": "4567"} ],
           |        "hasInternetPublication": false,
           |        "principalEconomicActivity": "P001",
           |        "hasEstablishmentInCustomsTerritory": true,
           |        "legalStatus": " Official",
           |        "thirdCountryIDNumber": [ "1234","67890"],
           |        "startDate": "2018-05-15",
           |        "expiryDate": "2018-05-16"
           |      }
           |    }
           |  }
           |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnReponseForRegisterWithEoriAndIdIndividualWithtDOE(): StubMapping =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithEORIAndIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2018-05-16T09:00:00Z"
           |    },
           |    "responseDetail": {
           |      "caseNumber": "C001",
           |      "outcome": "PASS",
           |      "responseData": {
           |        "SAFEID": "XE0000000866191",
           |        "trader": {
           |          "fullName": " John Doe",
           |          "shortName": "reg-existing-sole-trader"
           |        },
           |        "establishmentAddress": {
           |          "streetAndNumber": " Line 1",
           |          "city": "City name",
           |          "postalCode": "SE28 1AA",
           |          "countryCode":  "GB"
           |        },
           |        "contactDetail": {
           |          "address": {
           |            "streetAndNumber": "Line 1",
           |            "city": "City name",
           |            "postalCode": "SE28 1AA",
           |            "countryCode": "GB"
           |          },
           |          "contactName": "John Doe",
           |          "phone": "1234567",
           |          "fax": "89067",
           |          "email": "john.doe@example.com"
           |        },
           |        "VATIDs": [ {"countryCode": "AD" ,"vatNumber": "1234" } , {
           |          "countryCode": "GB" ,"vatNumber": "4567"} ],
           |        "hasInternetPublication": false,
           |        "principalEconomicActivity": "P001",
           |        "hasEstablishmentInCustomsTerritory": true,
           |        "legalStatus": " Official",
           |        "thirdCountryIDNumber": [ "1234","67890"],
           |        "dateOfEstablishmentBirth": "2018-05-16",
           |        "startDate": "2018-05-15",
           |        "expiryDate": "2018-05-16",
           |        "personType": 2
           |      }
           |    }
           |  }
           |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnReponseForRegisterWithEoriAndIdWithoutContactDetailsDOEAndPersonType(): StubMapping =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithEORIAndIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2001-12-17T09:30:47Z"
           |    },
           |    "responseDetail": {
           |      "caseNumber": "C001",
           |      "outcome": "PASS",
           |      "responseData": {
           |        "SAFEID": "XA1234567890123",
           |        "trader": {
           |          "fullName": "Mr John Doe",
           |          "shortName": "Mr J"
           |        },
           |        "establishmentAddress": {
           |          "streetAndNumber": "Street 1",
           |          "city": "city",
           |          "postalCode": "SE28 1AA",
           |          "countryCode": "ZZ"
           |        },
           |          "VATIDs": [
           |          {
           |            "countryCode": "AD",
           |            "vatNumber": "1234"
           |          },
           |          {
           |            "countryCode": "GB",
           |            "vatNumber": "4567"
           |          }
           |        ],
           |        "hasInternetPublication": false,
           |        "principalEconomicActivity": "P001",
           |        "hasEstablishmentInCustomsTerritory": true,
           |        "legalStatus": "Official",
           |        "thirdCountryIDNumber": [
           |          "1234",
           |          "67890"
           |        ],
           |        "startDate": "2018-05-15",
           |        "expiryDate": "2018-05-16"
           |      }
           |    }
           |  }
           |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def stubEoriAlreadyExists600FailEndpoint(): Unit =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn
          aResponse()
            .withStatus(OK)
            .withBody(s"""
               |{
               |  "registerWithEORIAndIDResponse": {
               |    "responseCommon": {
               |      "status": "OK",
               |      "statusText": "600 - EORI already linked to a different ID",
               |      "processingDate": "2019-09-10T09:00:00Z"
               |    }
               |  }
               |}
            """.stripMargin)
            .withHeader(CONTENT_TYPE, JSON)
    )

  def stubIDAlreadyExists602FailEndpoint(): Unit =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn
          aResponse()
            .withStatus(OK)
            .withBody(s"""
             |{
             |  "registerWithEORIAndIDResponse": {
             |    "responseCommon": {
             |      "status": "OK",
             |      "statusText": "602 - ID already linked to a different EORI",
             |      "processingDate": "2019-09-10T09:00:00Z"
             |    }
             |  }
             |}
            """.stripMargin)
            .withHeader(CONTENT_TYPE, JSON)
    )

  def stubEoriAlreadyRejected601FailResponse() {
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn
          aResponse()
            .withStatus(OK)
            .withBody(s"""
               |{
               |  "registerWithEORIAndIDResponse": {
               |    "responseCommon": {
               |      "status": "OK",
               |      "statusText": "601 - Rejected previously and retry failed",
               |      "processingDate": "2019-09-10T09:00:00Z"
               |    }
               |  }
               |}
            """.stripMargin)
            .withHeader(CONTENT_TYPE, JSON)
    )
  }

  def returnReponseForRegisterWithEoriAndId(): StubMapping =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithEORIAndIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2001-12-17T09:30:47Z"
           |    },
           |    "responseDetail": {
           |      "caseNumber": "C001",
           |      "outcome": "PASS",
           |      "responseData": {
           |        "SAFEID": "XA1234567890123",
           |        "trader": {
           |          "fullName": "Mr John Doe",
           |          "shortName": "Mr J"
           |        },
           |        "establishmentAddress": {
           |          "streetAndNumber": "Street 1",
           |          "city": "city",
           |          "postalCode": "SE28 1AA",
           |          "countryCode": "ZZ"
           |        },
           |        "contactDetail": {
           |          "address": {
           |            "streetAndNumber": "Street 1",
           |            "city": "city",
           |            "postalCode": "SE28 1AA",
           |            "countryCode": "ZZ"
           |          },
           |          "contactName": "John Doe",
           |          "phone": "1234567",
           |          "fax": "89067",
           |          "email": "asp@example.com"
           |        },
           |        "VATIDs": [
           |          {
           |            "countryCode": "AD",
           |            "vatNumber": "1234"
           |          },
           |          {
           |            "countryCode": "GB",
           |            "vatNumber": "4567"
           |          }
           |        ],
           |        "hasInternetPublication": false,
           |        "principalEconomicActivity": "P001",
           |        "hasEstablishmentInCustomsTerritory": true,
           |        "legalStatus": "Official",
           |        "thirdCountryIDNumber": [
           |          "1234",
           |          "67890"
           |        ],
           |        "dateOfEstablishmentBirth": "2018-05-16",
           |        "startDate": "2018-05-15",
           |        "expiryDate": "2018-05-16"
           |      }
           |    }
           |  }
           |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnPlusTelFaxReponseForRegisterWithEoriAndId(): StubMapping =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithEORIAndIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2001-12-17T09:30:47Z"
           |    },
           |    "responseDetail": {
           |      "caseNumber": "C001",
           |      "outcome": "PASS",
           |      "responseData": {
           |        "SAFEID": "XA1234567890123",
           |        "trader": {
           |          "fullName": "Mr John Doe",
           |          "shortName": "Mr J"
           |        },
           |        "establishmentAddress": {
           |          "streetAndNumber": "Street 1",
           |          "city": "city",
           |          "postalCode": "SE28 1AA",
           |          "countryCode": "ZZ"
           |        },
           |        "contactDetail": {
           |          "address": {
           |            "streetAndNumber": "Street 1",
           |            "city": "city",
           |            "postalCode": "SE28 1AA",
           |            "countryCode": "ZZ"
           |          },
           |          "contactName": "John Doe",
           |          "phone": "+1234567",
           |          "fax": "+89067",
           |          "email": "asp@example.com"
           |        },
           |        "VATIDs": [
           |          {
           |            "countryCode": "AD",
           |            "vatNumber": "1234"
           |          },
           |          {
           |            "countryCode": "GB",
           |            "vatNumber": "4567"
           |          }
           |        ],
           |        "hasInternetPublication": false,
           |        "principalEconomicActivity": "P001",
           |        "hasEstablishmentInCustomsTerritory": true,
           |        "legalStatus": "Official",
           |        "thirdCountryIDNumber": [
           |          "1234",
           |          "67890"
           |        ],
           |        "dateOfEstablishmentBirth": "2018-05-16",
           |        "startDate": "2018-05-15",
           |        "expiryDate": "2018-05-16"
           |      }
           |    }
           |  }
           |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnFailResponseForRegisterWithEoriAndId(): StubMapping =
    stubFor(
      post(urlEqualTo(registerWithEoriAndId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithEORIAndIDResponse": {
           |    "responseCommon": {
           |      "processingDate": "2018-05-16T09:00:00Z",
           |      "status": "OK"
           |    },
           |    "responseDetail": {
           |      "outcome": "FAIL"
           |    }
           |  }
           |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnNoMatchFoundForOrganisation(): Unit =
    stubFor(
      post(urlEqualTo(registerWithId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody("""
          |{
          |  "registerWithIDResponse": {
          |    "responseCommon":    {
          |      "status": "OK",
          |      "statusText":"002 - No Match Found",
          |      "processingDate": "2016-07-08T08:35:13Z",
          |      "returnParameters": [{
          |       "paramName": "POSITION",
          |       "paramValue": "FAIL"
          |      }]
          |    }
          |  }
          |}
        """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnMatchFoundForOrganisation(): Unit =
    stubFor(
      post(urlEqualTo(registerWithId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithIDResponse": {
           |    "responseCommon":    {
           |      "status": "OK",
           |      "processingDate": "2016-07-08T08:35:13Z",
           |      "returnParameters": [      {
           |        "paramName": "SAP_NUMBER",
           |        "paramValue": "0123456789"
           |      }]
           |    },
           |    "responseDetail":    {
           |      "SAFEID": "XE0000123456789",
           |      "ARN": "",
           |      "isEditable": true,
           |      "isAnAgent": false,
           |      "isAnIndividual": false,
           |      "organisation":       {
           |        "organisationName": "orgName",
           |        "isAGroup": false,
           |        "organisationType": "Corporate Body",
           |        "code": "0003"
           |      },
           |      "address":       {
           |        "addressLine1": "Line 1",
           |        "addressLine2": "line 2",
           |        "postalCode": "SE28 1AA",
           |        "countryCode": "ZZ"
           |      },
           |      "contactDetails": {}
           |    }
           |  }
           |}
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnMatchFoundForPartnership(): Unit =
    stubFor(
      post(urlEqualTo(registerWithId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithIDResponse": {
           |    "responseCommon":    {
           |      "status": "OK",
           |      "processingDate": "2016-07-08T08:35:13Z",
           |      "returnParameters": [      {
           |        "paramName": "SAP_NUMBER",
           |        "paramValue": "0123456789"
           |      }]
           |    },
           |    "responseDetail":    {
           |      "SAFEID": "XE0000123456789",
           |      "ARN": "",
           |      "isEditable": true,
           |      "isAnAgent": false,
           |      "isAnIndividual": false,
           |      "organisation":       {
           |        "organisationName": "orgName",
           |        "isAGroup": false,
           |        "organisationType": "Partnership",
           |        "code": "0001"
           |      },
           |      "address":       {
           |        "addressLine1": "Line 1",
           |        "addressLine2": "line 2",
           |        "postalCode": "SE28 1AA",
           |        "countryCode": "ZZ"
           |      },
           |      "contactDetails": {}
           |    }
           |  }
           |}
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnMatchFoundForLLP(): Unit =
    stubFor(
      post(urlEqualTo(registerWithId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           |{
           |  "registerWithIDResponse": {
           |    "responseCommon":    {
           |      "status": "OK",
           |      "processingDate": "2016-07-08T08:35:13Z",
           |      "returnParameters": [      {
           |        "paramName": "SAP_NUMBER",
           |        "paramValue": "0123456789"
           |      }]
           |    },
           |    "responseDetail":    {
           |      "SAFEID": "XE0000123456789",
           |      "ARN": "",
           |      "isEditable": true,
           |      "isAnAgent": false,
           |      "isAnIndividual": false,
           |      "organisation":       {
           |        "organisationName": "orgName",
           |        "isAGroup": false,
           |        "organisationType": "LLP",
           |        "code": "0001"
           |      },
           |      "address":       {
           |        "addressLine1": "Line 1",
           |        "addressLine2": "line 2",
           |        "postalCode": "SE28 1AA",
           |        "countryCode": "ZZ"
           |      },
           |      "contactDetails": {}
           |    }
           |  }
           |}
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnMatchFoundForIndividual(): Unit =
    stubFor(
      post(urlEqualTo(registerWithId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           {
           |  "registerWithIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2016-07-08T08:35:13Z",
           |      "returnParameters": [
           |        {
           |          "paramName": "SAP_NUMBER",
           |          "paramValue": "0123456789"
           |        }
           |      ]
           |    },
           |    "responseDetail": {
           |      "SAFEID": "XE0000123456789",
           |      "ARN": "",
           |      "isEditable": true,
           |      "isAnAgent": false,
           |      "isAnIndividual": true,
           |      "individual": {
           |        "firstName": "John",
           |        "lastName": "Doe",
           |        "dateOfBirth": "1989-09-21"
           |      },
           |      "address": {
           |        "addressType": "0001",
           |        "addressLine1": "Line 1",
           |        "addressLine2": "line 2",
           |        "addressLine3": "line 3",
           |        "addressLine4": "line 4",
           |        "postalCode": "SE28 1AA",
           |        "countryCode": "GB"
           |      },
           |      "contactDetails": {
           |        "phoneNumber": "01632961234",
           |        "mobileNumber": "01632961235",
           |        "faxNumber": "01632961236",
           |        "emailAddress": "john.doe@example.com"
           |      }
           |    }
           |  }
           |}
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnMatchFoundForRowIndividual(): Unit =
    stubFor(
      post(urlEqualTo(registerWithId))
        willReturn aResponse()
          .withStatus(OK)
          .withBody(s"""
           {
           |  "registerWithIDResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2016-07-08T08:35:13Z",
           |      "returnParameters": [
           |        {
           |          "paramName": "SAP_NUMBER",
           |          "paramValue": "0123456789"
           |        }
           |      ]
           |    },
           |    "responseDetail": {
           |      "SAFEID": "XE0000123456789",
           |      "ARN": "",
           |      "isEditable": true,
           |      "isAnAgent": false,
           |      "isAnIndividual": true,
           |      "individual": {
           |        "firstName": "John",
           |        "lastName": "Doe",
           |        "dateOfBirth": "1989-09-21"
           |      },
           |      "address": {
           |        "addressType": "0001",
           |        "addressLine1": "Line 1",
           |        "addressLine2": "line 2",
           |        "addressLine3": "line 3",
           |        "addressLine4": "line 4",
           |        "countryCode": "FR"
           |      },
           |      "contactDetails": {
           |        "phoneNumber": "01632961234",
           |        "mobileNumber": "01632961235",
           |        "faxNumber": "01632961236",
           |        "emailAddress": "john.doe@example.com"
           |      }
           |    }
           |  }
           |}
      """.stripMargin)
          .withHeader(CONTENT_TYPE, JSON)
    )

  def returnTheMatchResponseWhenReceiveRequest(url: String, request: String, response: String): Unit =
    returnTheMatchResponseWhenReceiveRequest(url, request, response, OK)

  def returnTheMatchResponseWhenReceiveRequest(url: String, request: String, response: String, status: Int): Unit =
    stubFor(
      post(urlMatching(url))
        .withRequestBody(equalToJson(request))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def verifyRegisterWithIdNotCalled(): Unit =
    verify(0, postRequestedFor(RegistrationWithIdPath))

}
