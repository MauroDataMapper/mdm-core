package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.async


import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExportService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 19/05/2022
 */
@Slf4j
@Integration
class DomainExportFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    DomainExportService domainExportService
    DataModelJsonExporterService dataModelJsonExporterService

    @Transactional
    DataModel getComplexDataModel() {
        DataModel.findByLabel(BootstrapModels.COMPLEX_DATAMODEL_NAME)
    }

    @Transactional
    DataModel getSimpleDataModel() {
        DataModel.findByLabel(BootstrapModels.SIMPLE_DATAMODEL_NAME)
    }

    @Transactional
    CatalogueUser getFunctionalTestUser() {
        CatalogueUser.findByEmailAddress(userEmailAddresses.creator)
    }

    @Override
    String getResourcePath() {
        'domainExports'
    }

    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .withoutAvailableActions()
            .whereAnonymousUsers {
                cannotIndex()
                cannotCreate()
            }
            .whereAuthenticatedUsers {
                canIndex()
                cannotCreate()
            }
            .whereReaders {
                cannotCreate()
            }
            .whereReviewers {
                cannotCreate()
            }
            .whereAuthors {
                cannotCreate()
            }
            .whereEditors {
                cannotUpdate()
                cannotCreate()
            }
            .whereContainerAdmins {
                cannotUpdate()
                cannotCreate()
            }
            .whereAdmins {
                cannotUpdate()
                cannotCreate()
            }
    }

    @Override
    String getEditorIndexJson() {
        '{"count":0,"items":[]}'
    }

    @Override
    Map getValidJson() {
        return null
    }

    @Override
    Map getInvalidJson() {
        return null
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "exported": {
    "domainType": "DataModel",
    "domainId": "${json-unit.matches:id}"
  },
  "exporter": {
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "name": "DataModelJsonExporterService",
    "nersion": "${json-unit.matches:version}"
  },
  "export": {
    "fileName": "Complex Test DataModel.json",
    "fileType": "text/json",
    "contentType": "application/mdm+json",
    "fileSize": "${json-unit.any-number}"
  },
  "exportedOn": "${json-unit.matches:offsetDateTime}",
  "exportedBy": "creator@test.com",
  "links": {
    "relative": "${json-unit.regex}/api/domainExports/[\\\\w-]+?/download",
    "absolute": "${json-unit.regex}http://localhost:\\\\d+/api/domainExports/.+?/download"
  }
}'''
    }

    @Override
    @Transactional
    String getValidId() {
        DataModel dataModel = getComplexDataModel()
        CatalogueUser user = getFunctionalTestUser()
        assert dataModel
        assert user
        ByteArrayOutputStream byteArrayOutputStream = dataModelJsonExporterService.exportDomain(user, dataModel.id, [:])
        domainExportService.createAndSaveNewDomainExport(dataModelJsonExporterService, dataModel,
                                                         dataModelJsonExporterService.getFileName(dataModel),
                                                         byteArrayOutputStream, user).id.toString()
    }

    @Transactional
    String getMultiModelValidId() {
        DataModel dataModel1 = getComplexDataModel()
        DataModel dataModel2 = getSimpleDataModel()
        CatalogueUser user = getFunctionalTestUser()
        assert dataModel1
        assert dataModel2
        assert user
        ByteArrayOutputStream byteArrayOutputStream = dataModelJsonExporterService.exportDomains(user, [dataModel1.id, dataModel2.id], [:])
        domainExportService.createAndSaveNewDomainExport(dataModelJsonExporterService, [dataModel1, dataModel2],
                                                         dataModelJsonExporterService.getFileName(dataModel1),
                                                         byteArrayOutputStream, user).id.toString()
    }

    @Transactional
    @Override
    void removeValidIdObject(String id) {
        domainExportService.delete(id)
    }

    @Override
    void verifyNotAllowedResponse(HttpResponse response, String id) {
        verifyResponse FORBIDDEN, response
    }

    void verify03CannotCreateResponse(HttpResponse<Map> response, String name) {
        verifyResponse HttpStatus.NOT_FOUND, response
        assert response.body().path
    }

    void 'DE-#prefix-01 : test downloading exported domain'() {
        given:
        def id = getValidId()
        String rId = UUID.randomUUID().toString()
        login(name)

        when:
        GET("$rId/download")

        then:
        verifyNotFound(response, rId)

        when:
        GET("$id/download", STRING_ARG)

        then:
        if (expectations.can(name, 'see')) verifyJsonResponse(OK, exportedJson())
        else verifyResponse HttpStatus.NOT_FOUND, jsonCapableResponse

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | 'Anonymous'
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'DE-#prefix-02 : test downloading multi exported domain'() {
        given:
        def id = getMultiModelValidId()
        login(name)

        when: 'verify that get on a multi model works'
        GET("$id")

        then:
        if (expectations.can(name, 'see')) verifyResponse(OK, response)
        else verifyNotFound(response, id)

        when:
        GET("$id/download", STRING_ARG)

        then:
        if (expectations.can(name, 'see')) verifyJsonResponse(OK, exportedMultiJson())
        else verifyResponse HttpStatus.NOT_FOUND, jsonCapableResponse

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | 'Anonymous'
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    String exportedJson() {
        '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Complex Test DataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "classifiers": [
      {
        "id": "${json-unit.matches:id}",
        "label": "test classifier",
        "lastUpdated": "${json-unit.matches:offsetDateTime}"
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "test classifier2",
        "lastUpdated": "${json-unit.matches:offsetDateTime}"
      }
    ],
    "metadata": [
      {
        "id": "${json-unit.matches:id}",
        "namespace": "test.com",
        "key": "mdk2",
        "value": "mdv2",
        "lastUpdated": "${json-unit.matches:offsetDateTime}"
      },
      {
        "id": "${json-unit.matches:id}",
        "namespace": "test.com/test",
        "key": "mdk1",
        "value": "mdv2",
        "lastUpdated": "${json-unit.matches:offsetDateTime}"
      },
      {
        "id": "${json-unit.matches:id}",
        "namespace": "test.com",
        "key": "mdk1",
        "value": "mdv1",
        "lastUpdated": "${json-unit.matches:offsetDateTime}"
      }
    ],
    "annotations": [
      {
        "id": "${json-unit.matches:id}",
        "createdBy": "development@test.com",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "label": "test annotation 1"
      },
      {
        "id": "${json-unit.matches:id}",
        "createdBy": "development@test.com",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "label": "test annotation 2",
        "description": "with description"
      }
    ],
    "type": "Data Standard",
    "author": "admin person",
    "organisation": "brc",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    },
    "dataTypes": [
      {
        "id": "${json-unit.matches:id}",
        "label": "yesnounknown",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "domainType": "EnumerationType",
        "enumerationValues": [
          {
            "id": "${json-unit.matches:id}",
            "index": 0,
            "key": "Y",
            "value": "Yes",
            "lastUpdated": "${json-unit.matches:offsetDateTime}"
          },
          {
            "id": "${json-unit.matches:id}",
            "index": 2,
            "key": "U",
            "value": "Unknown",
            "lastUpdated": "${json-unit.matches:offsetDateTime}"
          },
          {
            "id": "${json-unit.matches:id}",
            "index": 1,
            "key": "N",
            "value": "No",
            "lastUpdated": "${json-unit.matches:offsetDateTime}"
          }
        ]
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "integer",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "domainType": "PrimitiveType"
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "string",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "domainType": "PrimitiveType"
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "child",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "domainType": "ReferenceType",
        "referenceClass": {
          "id": "${json-unit.matches:id}",
          "label": "child",
          "dataClassPath": "parent|child"
        }
      }
    ],
    "childDataClasses": [
      {
        "id": "${json-unit.matches:id}",
        "label": "emptyclass",
        "description": "dataclass with desc",
        "lastUpdated": "${json-unit.matches:offsetDateTime}"
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "parent",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "metadata": [
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com",
            "key": "mdcl1",
            "value": "mdcl1",
            "lastUpdated": "${json-unit.matches:offsetDateTime}"
          }
        ],
        "maxMultiplicity": -1,
        "minMultiplicity": 1,
        "dataClasses": [
          {
            "id": "${json-unit.matches:id}",
            "label": "child",
            "lastUpdated": "${json-unit.matches:offsetDateTime}"
          }
        ],
        "dataElements": [
          {
            "id": "${json-unit.matches:id}",
            "label": "child",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "metadata": [
              {
                "id": "${json-unit.matches:id}",
                "namespace": "test.com",
                "key": "mdel1",
                "value": "mdel1",
                "lastUpdated": "${json-unit.matches:offsetDateTime}"
              }
            ],
            "dataType": {
              "id": "${json-unit.matches:id}",
              "label": "child",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "domainType": "ReferenceType",
              "referenceClass": {
                "id": "${json-unit.matches:id}",
                "label": "child",
                "dataClassPath": "parent|child"
              }
            },
            "maxMultiplicity": 1,
            "minMultiplicity": 1
          }
        ]
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "content",
        "description": "A dataclass with elements",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "maxMultiplicity": 1,
        "minMultiplicity": 0,
        "dataElements": [
          {
            "id": "${json-unit.matches:id}",
            "label": "ele1",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "dataType": {
              "id": "${json-unit.matches:id}",
              "label": "string",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "domainType": "PrimitiveType"
            },
            "maxMultiplicity": 20,
            "minMultiplicity": 0
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "element2",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "dataType": {
              "id": "${json-unit.matches:id}",
              "label": "integer",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "domainType": "PrimitiveType"
            },
            "maxMultiplicity": 1,
            "minMultiplicity": 1
          }
        ]
      }
    ]
  },
  "exportMetadata": {
    "exportedBy": "creator User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''
    }

    String exportedMultiJson() {
        '''{
  "dataModels": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "metadata": [
        {
          "id": "${json-unit.matches:id}",
          "namespace": "test.com",
          "key": "mdk1",
          "value": "mdv1",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "namespace": "test.com",
          "key": "mdk2",
          "value": "mdv2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "namespace": "test.com/test",
          "key": "mdk1",
          "value": "mdv2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "annotations": [
        {
          "id": "${json-unit.matches:id}",
          "createdBy": "development@test.com",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "label": "test annotation 2",
          "description": "with description"
        },
        {
          "id": "${json-unit.matches:id}",
          "createdBy": "development@test.com",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "label": "test annotation 1"
        }
      ],
      "type": "Data Standard",
      "author": "admin person",
      "organisation": "brc",
      "documentationVersion": "1.0.0",
      "finalised": false,
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper"
      },
      "dataTypes": [
        {
          "id": "${json-unit.matches:id}",
          "label": "yesnounknown",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "domainType": "EnumerationType",
          "enumerationValues": [
            {
              "id": "${json-unit.matches:id}",
              "index": 1,
              "key": "N",
              "value": "No",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            },
            {
              "id": "${json-unit.matches:id}",
              "index": 2,
              "key": "U",
              "value": "Unknown",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            },
            {
              "id": "${json-unit.matches:id}",
              "index": 0,
              "key": "Y",
              "value": "Yes",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ]
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "integer",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "domainType": "PrimitiveType"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "string",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "domainType": "PrimitiveType"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "child",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "domainType": "ReferenceType",
          "referenceClass": {
            "id": "${json-unit.matches:id}",
            "label": "child",
            "dataClassPath": "parent|child"
          }
        }
      ],
      "childDataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "label": "emptyclass",
          "description": "dataclass with desc",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "parent",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "metadata": [
            {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com",
              "key": "mdcl1",
              "value": "mdcl1",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ],
          "maxMultiplicity": -1,
          "minMultiplicity": 1,
          "dataClasses": [
            {
              "id": "${json-unit.matches:id}",
              "label": "child",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ],
          "dataElements": [
            {
              "id": "${json-unit.matches:id}",
              "label": "child",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "metadata": [
                {
                  "id": "${json-unit.matches:id}",
                  "namespace": "test.com",
                  "key": "mdel1",
                  "value": "mdel1",
                  "lastUpdated": "${json-unit.matches:offsetDateTime}"
                }
              ],
              "dataType": {
                "id": "${json-unit.matches:id}",
                "label": "child",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "domainType": "ReferenceType",
                "referenceClass": {
                  "id": "${json-unit.matches:id}",
                  "label": "child",
                  "dataClassPath": "parent|child"
                }
              },
              "maxMultiplicity": 1,
              "minMultiplicity": 1
            }
          ]
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "content",
          "description": "A dataclass with elements",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "dataElements": [
            {
              "id": "${json-unit.matches:id}",
              "label": "ele1",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "dataType": {
                "id": "${json-unit.matches:id}",
                "label": "string",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "domainType": "PrimitiveType"
              },
              "maxMultiplicity": 20,
              "minMultiplicity": 0
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "element2",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "dataType": {
                "id": "${json-unit.matches:id}",
                "label": "integer",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "domainType": "PrimitiveType"
              },
              "maxMultiplicity": 1,
              "minMultiplicity": 1
            }
          ]
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Simple Test DataModel",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "metadata": [
        {
          "id": "${json-unit.matches:id}",
          "namespace": "test.com",
          "key": "mdk2",
          "value": "mdv2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "namespace": "test.com/simple",
          "key": "mdk2",
          "value": "mdv2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "namespace": "test.com/simple",
          "key": "mdk1",
          "value": "mdv1",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "type": "Data Standard",
      "documentationVersion": "1.0.0",
      "finalised": false,
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper"
      },
      "childDataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "label": "simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "metadata": [
            {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com/simple",
              "key": "mdk1",
              "value": "mdv1",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ]
        }
      ]
    }
  ],
  "exportMetadata": {
    "exportedBy": "creator User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "3.1"
    }
  }
}
'''
    }
}