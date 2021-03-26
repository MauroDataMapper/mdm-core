/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see DataModelController* Controller: dataModel
 *  | POST   | /api/dataModels       | Action: save   |
 *  | GET    | /api/dataModels       | Action: index  |
 *  | DELETE | /api/dataModels/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${id} | Action: update |
 *  | GET    | /api/dataModels/${id} | Action: show   |
 *
 *  | GET    | /api/dataModels/types                                   | Action: types                   |
 *  | GET    | /api/dataModels/${dataModelId}/hierarchy                | Action: hierarchy               |
 *  | PUT    | /api/dataModels/${dataModelId}/newVersion               | Action: newVersion              |
 *  | PUT    | /api/dataModels/${dataModelId}/newBranchModelVersion  | Action: newBranchModelVersion |
 *  | PUT    | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion |
 *  | PUT    | /api/dataModels/${dataModelId}/finalise                  | Action: finalise                 |
 *  | GET    | /api/dataModels/${dataModelId}/diff/${otherDataModelId} | Action: diff                    |
 *
 *  | POST   | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                | Action: exportDataModels |
 *  | POST   | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                | Action: importDataModels |
 *  | GET    | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion} | Action: exportDataModel  |
 *
 *  | PUT    | /api/dataModels/${dataModelId}/folder/${folderId}  | Action: changeFolder |
 *  | PUT    | /api/folders/${folderId}/dataModels/${dataModelId} | Action: changeFolder |
 *
 *  | GET    | /api/dataModels/${dataModelId}/suggestLinks/${otherDataModelId}" | Action: suggestLinksModel | TODO
 *
 *  | GET    | /api/dataModels/${dataModelId}/search | Action: search |
 *  | POST   | /api/dataModels/${dataModelId}/search | Action: search |
 */
@Integration
@Slf4j
//@Stepwise
class DataModelFunctionalSpec extends ResourceFunctionalSpec<DataModel> {

    @Shared
    UUID folderId

    @Shared
    UUID movingFolderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Functional Test Folder 2', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(Folder, Classifier, SemanticLink)
    }

    @Override
    String getResourcePath() {
        'dataModels'
    }

    @Override
    String getSavePath() {
        "folders/${folderId}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test Model'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: null
        ]
    }

    @Override
    String getDeleteEndpoint(String id) {
        "${super.getDeleteEndpoint(id)}?permanent=true"
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "domainType": "DataModel",
  "availableActions": ['delete', 'show', 'update'],
  "branchName": "main",
  "finalised": false,
  "label": "Functional Test Model",
  "type": "Data Standard",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "id": "${json-unit.matches:id}",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  }
}'''
    }

    String getExpectedMergeDiffJson() {
        '''{
    "leftId": "${json-unit.matches:id}",
    "rightId": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "count": 11,
    "diffs": [
        {
            "description": {
                "left": "DescriptionRight",
                "right": "DescriptionLeft",
                "isMergeConflict": true,
                "commonAncestorValue": null
            }
        },
        {
            "branchName": {
                "left": "main",
                "right": "source",
                "isMergeConflict": false
            }
        },
        {
            "dataClasses": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "deleteAndModify",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        },
                        "isMergeConflict": true,
                        "commonAncestorValue": {
                            "id": "${json-unit.matches:id}",
                            "label": "deleteAndModify",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "DataModel",
                                    "finalised": true
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "deleteLeftOnly",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        },
                        "isMergeConflict": false
                    }
                ],
                "created": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "addLeftOnly",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        },
                        "isMergeConflict": false
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "modifyAndDelete",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        },
                        "isMergeConflict": true,
                        "commonAncestorValue": {
                            "id": "${json-unit.matches:id}",
                            "label": "modifyAndDelete",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "DataModel",
                                    "finalised": true
                                }
                            ]
                        }
                    }
                ],
                "modified": [
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "addAndAddReturningDifference",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                    "left": "DescriptionRight",
                                    "right": "DescriptionLeft",
                                    "isMergeConflict": true,
                                    "commonAncestorValue": null
                                }
                            }
                        ]
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "existingClass",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "count": 2,
                        "diffs": [
                            {
                                "dataClasses": {
                                    "deleted": [
                                        {
                                            "value": {
                                                "id": "${json-unit.matches:id}",
                                                "label": "deleteLeftOnlyFromExistingClass",
                                                "breadcrumbs": [
                                                    {
                                                        "id": "${json-unit.matches:id}",
                                                        "label": "Functional Test Model",
                                                        "domainType": "DataModel",
                                                        "finalised": false
                                                    },
                                                    {
                                                        "id": "${json-unit.matches:id}",
                                                        "label": "existingClass",
                                                        "domainType": "DataClass"
                                                    }
                                                ]
                                            },
                                            "isMergeConflict": false
                                        }
                                    ],
                                    "created": [
                                        {
                                            "value": {
                                                "id": "${json-unit.matches:id}",
                                                "label": "addLeftToExistingClass",
                                                "breadcrumbs": [
                                                    {
                                                        "id": "${json-unit.matches:id}",
                                                        "label": "Functional Test Model",
                                                        "domainType": "DataModel",
                                                        "finalised": false
                                                    },
                                                    {
                                                        "id": "${json-unit.matches:id}",
                                                        "label": "existingClass",
                                                        "domainType": "DataClass"
                                                    }
                                                ]
                                            },
                                            "isMergeConflict": false
                                        }
                                    ]
                                }
                            }
                        ]
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "modifyAndModifyReturningDifference",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                    "left": "DescriptionRight",
                                    "right": "DescriptionLeft",
                                    "isMergeConflict": true,
                                    "commonAncestorValue": null
                                }
                            }
                        ]
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "modifyLeftOnly",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                    "left": null,
                                    "right": "Description",
                                    "isMergeConflict": false
                                }
                            }
                        ]
                    }
                ]
            }
        }
    ]
}'''
    }

    void 'test getting DataModel types'() {
        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Data Asset",
          "Data Standard"
        ]'''
    }

    void 'test getting DataModel exporters'() {
        when:
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "providerType": "DataModelExporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "XML DataModel Exporter",
    "fileExtension": "xml",
    "name": "DataModelXmlExporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "canExportMultipleDomains": false,
    "version": "${json-unit.matches:version}",
    "fileType": "text/xml"
  },
  {
    "providerType": "DataModelExporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "JSON DataModel Exporter",
    "fileExtension": "json",
    "name": "DataModelJsonExporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "canExportMultipleDomains": false,
    "version": "${json-unit.matches:version}",
    "fileType": "text/json"
  }
]'''
    }

    void 'test getting DataModel importers'() {
        when:
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "providerType": "DataModelImporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "XML DataModel Importer",
    "name": "DataModelXmlImporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": true,
    "version": "${json-unit.matches:version}"
  },
  {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "providerType": "DataModelImporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "JSON DataModel Importer",
    "name": "DataModelJsonImporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": false,
    "version": "${json-unit.matches:version}"
  }
]'''
    }

    void 'test getting DataModel default datatype providers'() {
        when:
        GET('providers/defaultDataTypeProviders', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "dataTypes": [
      {
        "domainType": "PrimitiveType",
        "description": "A piece of text",
        "label": "Text"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A whole number",
        "label": "Number"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A decimal number",
        "label": "Decimal"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A date",
        "label": "Date"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A date with a timestamp",
        "label": "DateTime"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A timestamp",
        "label": "Timestamp"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A true or false value",
        "label": "Boolean"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A time period in arbitrary units",
        "label": "Duration"
      }
    ],
    "displayName": "Basic Default DataTypes",
    "name": "DataTypeService",
    "version": "1.0.0"
  }
]'''
    }

    void 'test finalising DataModel'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().finalised
        response.body().dateFinalised

        when: 'List edits for the DataModel'
        GET("$id/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is not a CHANGE NOTICE edit'
        !response.body().items.find {
            it.description == "Functional Test Change Notice"
        }

        cleanup:
        cleanUpData(id)
    }

    void 'test finalising DataModel with a changeNotice'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [versionChangeType: 'Major', changeNotice: 'Functional Test Change Notice'])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().finalised
        response.body().dateFinalised

        when: 'List edits for the DataModel'
        GET("$id/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is a CHANGE NOTICE edit'
        response.body().items.find {
            it.title == "CHANGENOTICE" && it.description == "Functional Test Change Notice"
        }


        cleanup:
        cleanUpData(id)
    }

    void 'Test undoing a soft delete using the admin endpoint'() {
        given: 'model is deleted'
        String id = createNewItem(validJson)
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().deleted == null

        cleanup:
        cleanUpData(id)
    }

    void 'Test undoing a soft delete via update'() {
        given: 'model is deleted'
        String id = createNewItem(validJson)
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        PUT(id, [deleted: false])

        then:
        verifyResponse(FORBIDDEN, response)
        responseBody().additional == 'Cannot update a deleted Model'

        cleanup:
        cleanUpData(id)
    }

    void 'VF01 : test creating a new fork model of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'adding one new model'
        PUT("$id/newForkModel", [label: 'Functional Test DataModel reader'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test DataModel reader",')


        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newForkModel", [label: 'Functional Test DataModel editor'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test DataModel editor",')

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel editor"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
     {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel editor"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'VD01 : test creating a new documentation version of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        PUT("$id/newDocumentationVersion", [:], STRING_ARG)

        then:
        verifyJsonResponse CREATED, expectedShowJson
            .replaceFirst(/"documentationVersion": "1\.0\.0",/, '"documentationVersion": "2.0.0",')

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Documentation Version Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        response.body().total == 1
        response.body().errors.size() == 1
        response.body().errors[0].message.contains('cannot have a new version as it has been superseded by [Functional Test Model')

        cleanup:
        cleanUpData()
    }

    void 'VB01a : test creating a new main branch model version of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        PUT("$id/newBranchModelVersion", [:], STRING_ARG)

        then:
        verifyJsonResponse CREATED, expectedShowJson

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Model Version Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''
        cleanup:
        cleanUpData()
    }

    void 'VB01b : performance test creating a new main branch model version of a simple DataModel'() {
        given: 'finalised model is created'
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        long start = System.currentTimeMillis()
        PUT("$id/newBranchModelVersion", [:])
        long newBranchModelVersionDuration = System.currentTimeMillis() - start
        log.debug('newBranchModelVersion took {}', Utils.getTimeString(newBranchModelVersionDuration))

        then:
        verifyResponse CREATED, response
        newBranchModelVersionDuration < 1000

        cleanup:
        cleanUpData()
    }

    void 'VB01c : performance test creating a new main branch model version of a complex DataModel'() {
        given: 'finalised model is created'
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        long start = System.currentTimeMillis()
        PUT("$id/newBranchModelVersion", [:])
        long newBranchModelVersionDuration = System.currentTimeMillis() - start
        log.debug('newBranchModelVersion took {}', Utils.getTimeString(newBranchModelVersionDuration))

        then:
        verifyResponse CREATED, response
        newBranchModelVersionDuration < 1500

        cleanup:
        cleanUpData()
    }


    void 'VB02 : test creating a main branch model version finalising and then creating another main branch of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create second model'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'finalising second model'
        String secondId = responseBody().id
        PUT("$secondId/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response

        when: 'create new branch from second model'
        PUT("$secondId/newBranchModelVersion", [:])

        then:
        String thirdId = responseBody().id
        verifyResponse CREATED, response

        when: 'get first model SLs'
        GET("$id/semanticLinks")

        then: 'first model is the target of refines for both second and third model'
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetCatalogueItem.id == id &&
            it.sourceCatalogueItem.id == secondId
        }
        // This is unconfirmed as its copied
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetCatalogueItem.id == id &&
            it.sourceCatalogueItem.id == thirdId &&
            it.unconfirmed
        }

        when: 'getting the first model VLs'
        GET("$id/versionLinks")

        then: 'first model is the target of new model version of for second model only'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any {
            it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label &&
            it.targetModel.id == id &&
            it.sourceModel.id == secondId
        }

        when: 'get second model SLs'
        GET("$secondId/semanticLinks")

        then: 'second model is the target of refines for third model and source for first model'
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetCatalogueItem.id == id &&
            it.sourceCatalogueItem.id == secondId
        }
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetCatalogueItem.id == secondId &&
            it.sourceCatalogueItem.id == thirdId
        }

        when: 'getting the second model VLs'
        GET("$secondId/versionLinks")

        then: 'second model is the target of new model version of for third model and source for first model'
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.any {
            it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            it.targetModel.id == secondId &&
            it.sourceModel.id == thirdId
        }
        responseBody().items.any {
            it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            it.targetModel.id == id &&
            it.sourceModel.id == secondId
        }

        cleanup:
        cleanUpData()
    }

    void 'VB03 : test creating a main branch model version when one already exists'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'another main branch created'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == 'Property [branchName] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel] ' +
        'with value [main] already exists for label [Functional Test Model]'

        cleanup:
        cleanUpData()
    }

    void 'VB04 : test creating a non-main branch model version without main existing'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [branchName: 'functionalTest'])

        then:
        verifyResponse CREATED, response

        when:
        GET("$id/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.every {
            it.linkType == 'New Model Version Of'
            it.targetModel.id == id
        }

        cleanup:
        cleanUpData()
    }

    void 'VB05 : test finding common ancestor of two datamodels'() {
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when:
        GET('')

        then:
        verifyResponse OK, response
        String mainId = responseBody().items.find {
            it.label == 'Functional Test Model' &&
            !(it.id in [id, leftId, rightId])
        }?.id
        mainId

        when: 'check CA between L and R'
        GET("$leftId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between R and L'
        GET("$rightId/commonAncestor/$leftId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between L and M'
        GET("$leftId/commonAncestor/$mainId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between M and L'
        GET("$mainId/commonAncestor/$leftId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between M and R'
        GET("$mainId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between R and M'
        GET("$rightId/commonAncestor/$mainId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        cleanup:
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(mainId)
        cleanUpData(id)
    }

    void 'VB06 : test finding latest finalised model of a datamodel'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when:
        GET("$newBranchId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == 'Functional Test Model'
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == 'Functional Test Model'
        responseBody().modelVersion == '2.0.0'

        cleanup:
        cleanUpData(newBranchId)
        cleanUpData(expectedId)
        cleanUpData(latestDraftId)
        cleanUpData(id)
    }

    void 'VB07 : test finding latest model version of a datamodel'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when:
        GET("$newBranchId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '2.0.0'

        cleanup:
        cleanUpData(newBranchId)
        cleanUpData(expectedId)
        cleanUpData(latestDraftId)
        cleanUpData(id)
    }

    void 'VB08a : test finding merge difference of two datamodels'() {
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when:
        GET("$leftId/mergeDiff/$rightId")

        then:
        verifyResponse OK, response
        responseBody().leftId == rightId
        responseBody().rightId == leftId

        when:
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().leftId == mainId
        responseBody().rightId == leftId

        when:
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().leftId == mainId
        responseBody().rightId == rightId

        cleanup:
        cleanUpData(mainId)
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(id)
    }

    void 'VB08b : test finding merge difference of two complex datamodels'() {
        given:
        String id = createNewItem(validJson)

        POST("$id/dataClasses", [label: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteRightOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyRightOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteAndDelete'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteAndModify'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndDelete'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndModifyReturningNoDifference'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'existingClass'])
        verifyResponse CREATED, response
        String existingClass = responseBody().id
        POST("$id/dataClasses/$existingClass/dataClasses", [label: 'deleteLeftOnlyFromExistingClass'])
        verifyResponse CREATED, response
        POST("$id/dataClasses/$existingClass/dataClasses", [label: 'deleteRightOnlyFromExistingClass'])
        verifyResponse CREATED, response

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        GET("$source/path/dm%3A%7Cdc%3AexistingClass")
        verifyResponse OK, response
        existingClass = responseBody().id
        GET("dataClasses/$existingClass/path/dc%3A%7Cdc%3AdeleteLeftOnlyFromExistingClass", MAP_ARG, true)
        verifyResponse OK, response
        String deleteLeftOnlyFromExistingClass = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteLeftOnly")
        verifyResponse OK, response
        String deleteLeftOnly = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteAndDelete")
        verifyResponse OK, response
        String deleteAndDelete = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteAndModify")
        verifyResponse OK, response
        String deleteAndModify = responseBody().id

        GET("$source/path/dm%3A%7Cdc%3AmodifyLeftOnly")
        verifyResponse OK, response
        String modifyLeftOnly = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndDelete")
        verifyResponse OK, response
        String modifyAndDelete = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndModifyReturningNoDifference")
        verifyResponse OK, response
        String modifyAndModifyReturningNoDifference = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndModifyReturningDifference")
        verifyResponse OK, response
        String modifyAndModifyReturningDifference = responseBody().id

        then:
        DELETE("$source/dataClasses/$deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$existingClass/dataClasses/$deleteLeftOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("$source/dataClasses/$modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("$source/dataClasses/$existingClass/dataClasses", [label: 'addLeftToExistingClass'])
        verifyResponse CREATED, response
        POST("$source/dataClasses", [label: 'addLeftOnly'])
        verifyResponse CREATED, response
        POST("$source/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("$source/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionLeft'])
        verifyResponse CREATED, response

        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        when:
        GET("$target/path/dm%3A%7Cdc%3AexistingClass")
        verifyResponse OK, response
        existingClass = responseBody().id
        GET("dataClasses/$existingClass/path/dc%3A%7Cdc%3AdeleteRightOnlyFromExistingClass", MAP_ARG, true)
        verifyResponse OK, response
        String deleteRightOnlyFromExistingClass = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AdeleteRightOnly")
        verifyResponse OK, response
        String deleteRightOnly = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AdeleteAndDelete")
        verifyResponse OK, response
        deleteAndDelete = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndDelete")
        verifyResponse OK, response
        modifyAndDelete = responseBody().id

        GET("$target/path/dm%3A%7Cdc%3AmodifyRightOnly")
        verifyResponse OK, response
        String modifyRightOnly = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AdeleteAndModify")
        verifyResponse OK, response
        deleteAndModify = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndModifyReturningNoDifference")
        verifyResponse OK, response
        modifyAndModifyReturningNoDifference = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndModifyReturningDifference")
        verifyResponse OK, response
        modifyAndModifyReturningDifference = responseBody().id

        then:
        DELETE("$target/dataClasses/$existingClass/dataClasses/$deleteRightOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("$target/dataClasses/$deleteRightOnly")
        verifyResponse NO_CONTENT, response
        DELETE("$target/dataClasses/$deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("$target/dataClasses/$modifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("$target/dataClasses/$modifyRightOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("$target/dataClasses/$existingClass/dataClasses", [label: 'addRightToExistingClass'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addRightOnly'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response

        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response

        when:
        GET("$source/mergeDiff/$target", STRING_ARG)
        //        GET("$source/mergeDiff/$target")

        then:
        verifyJsonResponse OK, expectedMergeDiffJson

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'VB09a : test merging diff with no patch data'() {
        given:
        String id = createNewItem(validJson)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        PUT("$source/mergeInto/$target", [:])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors[0].message.contains('cannot be null')

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'VB09b : test merging diff with URI id not matching body id'() {
        given:
        String id = createNewItem(validJson)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  leftId : "$target" as String,
                                                  rightId: "${UUID.randomUUID().toString()}" as String,
                                                  label  : "Functional Test Model",
                                                  count  : 0,
                                                  diffs  : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Source model id passed in request body does not match source model id in URI.'

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  leftId : "${UUID.randomUUID().toString()}" as String,
                                                  rightId: "$source" as String,
                                                  label  : "Functional Test Model",
                                                  count  : 0,
                                                  diffs  : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Target model id passed in request body does not match target model id in URI.'

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'VB09c : test merging diff into draft model'() {
        given:
        String id = createNewItem(validJson)

        POST("$id/dataClasses", [label: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteAndModify'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndDelete'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'existingClass'])
        verifyResponse CREATED, response
        String existingClass = responseBody().id
        POST("$id/dataClasses/$existingClass/dataClasses", [label: 'deleteLeftOnlyFromExistingClass'])
        verifyResponse CREATED, response

        //        POST("$id/dataTypes", [label: 'deleteDataTypeSource', domainType: 'PrimitiveType'])
        //        verifyResponse CREATED, response
        //        POST("$id/dataTypes", [label: 'modifyDataTypeSource', domainType: 'PrimitiveType'])
        //        verifyResponse CREATED, response

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        //to delete
        GET("$source/path/dm%3A%7Cdc%3AexistingClass")
        verifyResponse OK, response
        existingClass = responseBody().id
        GET("dataClasses/$existingClass/path/dc%3A%7Cdc%3AdeleteLeftOnlyFromExistingClass", MAP_ARG, true)
        verifyResponse OK, response
        String deleteLeftOnlyFromExistingClass = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteLeftOnly")
        verifyResponse OK, response
        String deleteLeftOnly = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteAndModify")
        verifyResponse OK, response
        String deleteAndModify = responseBody().id
        //to modify
        GET("$source/path/dm%3A%7Cdc%3AmodifyLeftOnly")
        verifyResponse OK, response
        String modifyLeftOnly = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndDelete")
        verifyResponse OK, response
        String sourceModifyAndDelete = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndModifyReturningDifference")
        verifyResponse OK, response
        String modifyAndModifyReturningDifference = responseBody().id

        //        GET("$source/path/dm%3A%7Cdt%3AdeleteDataTypeSource")
        //        verifyResponse OK, response
        //        String deleteDataTypeSource = responseBody().id
        //        GET("$source/path/dm%3A%7Cdt%3AmodifyDataTypeSource")
        //        verifyResponse OK, response
        //        String modifyDataTypeSource = responseBody().id

        then:
        //dataModel description
        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        //dataClasses
        DELETE("$source/dataClasses/$deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$deleteAndModify")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$existingClass/dataClasses/$deleteLeftOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response

        PUT("$source/dataClasses/$modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$sourceModifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("$source/dataClasses/$existingClass/dataClasses", [label: 'addLeftToExistingClass'])
        verifyResponse CREATED, response
        String addLeftToExistingClass = responseBody().id
        POST("$source/dataClasses", [label: 'addLeftOnly'])
        verifyResponse CREATED, response
        String addLeftOnly = responseBody().id
        POST("$source/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionLeft'])
        verifyResponse CREATED, response

        //dataTypes
        //        DELETE("$source/dataTypes/$deleteDataTypeSource")
        //        verifyResponse NO_CONTENT, response
        //
        //        PUT("$source/dataClasses/$modifyDataTypeSource", [description: 'Description'])
        //        verifyResponse OK, response
        //
        //        POST("$source/dataTypes", [label: 'addDataTypeSource', domainType: 'PrimitiveType'])
        //        verifyResponse CREATED, response
        //        String addDataTypeSource = responseBody().id

        when:
        // for mergeInto json
        GET("$target/path/dm%3A%7Cdc%3AexistingClass")
        verifyResponse OK, response
        existingClass = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndDelete")
        verifyResponse OK, response
        String targetModifyAndDelete = responseBody().id

        GET("$target/path/dm%3A%7Cdc%3AdeleteAndModify")
        verifyResponse OK, response
        deleteAndModify = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndModifyReturningDifference")
        verifyResponse OK, response
        modifyAndModifyReturningDifference = responseBody().id

        then:
        //dataModel description
        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response

        //dataClasses
        DELETE("$target/dataClasses/$targetModifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("$target/dataClasses/$deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("$target/dataClasses/$existingClass/dataClasses", [label: 'addRightToExistingClass'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response
        String addAndAddReturningDifference = responseBody().id

        when:
        // for mergeInto json
        GET("$target/path/dm%3A%7Cdc%3AdeleteLeftOnly")
        verifyResponse OK, response
        deleteLeftOnly = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyLeftOnly")
        verifyResponse OK, response
        modifyLeftOnly = responseBody().id
        GET("dataClasses/$existingClass/path/dc%3A%7Cdc%3AdeleteLeftOnlyFromExistingClass", MAP_ARG, true)
        verifyResponse OK, response
        deleteLeftOnlyFromExistingClass = responseBody().id

        GET("$source/mergeDiff/$target", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedMergeDiffJson

        when:
        String modifiedDescriptionSource = 'modifiedDescriptionSource'

        def requestBody = [
            changeNotice: 'Functional Test Merge Change Notice',
            patch       : [
                leftId : target,
                rightId: source,
                label  : "Functional Test Model",
                diffs  : [
                    [
                        fieldName: "description",
                        value    : modifiedDescriptionSource
                    ],
                    [
                        fieldName: "dataClasses",

                        deleted  : [
                            [
                                id   : deleteAndModify,
                                label: "deleteAndModify"
                            ],
                            [
                                id   : deleteLeftOnly,
                                label: "deleteLeftOnly"
                            ]
                        ],
                        created  : [
                            [
                                id   : addLeftOnly,
                                label: "addLeftOnly"
                            ],
                            [
                                id   : sourceModifyAndDelete,
                                label: "modifyAndDelete"
                            ]
                        ],
                        modified : [
                            [
                                leftId: addAndAddReturningDifference,
                                label : "addAndAddReturningDifference",
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : "addedDescriptionSource"
                                    ]
                                ]
                            ],
                            [
                                leftId: existingClass,
                                label : "existingClass",
                                diffs : [
                                    [
                                        fieldName: "dataClasses",

                                        deleted  : [
                                            [
                                                id   : deleteLeftOnlyFromExistingClass,
                                                label: "deleteLeftOnlyFromExistingClass"
                                            ]
                                        ],
                                        created  : [
                                            [
                                                id   : addLeftToExistingClass,
                                                label: "addLeftToExistingClass"
                                            ]
                                        ]

                                    ]
                                ]
                            ],
                            [
                                leftId: modifyAndModifyReturningDifference,
                                label : "modifyAndModifyReturningDifference",
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : modifiedDescriptionSource
                                    ]
                                ]
                            ],
                            [
                                leftId: modifyLeftOnly,
                                label : "modifyLeftOnly",
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : "modifiedDescriptionSourceOnly"
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
            ]
        ]


        PUT("$source/mergeInto/$target", requestBody)

        then:
        verifyResponse OK, response
        responseBody().id == target
        responseBody().description == modifiedDescriptionSource

        when:
        GET("$target/dataClasses")

        then:
        responseBody().items.label as Set == ['existingClass', 'modifyAndModifyReturningDifference', 'modifyLeftOnly',
                                              'addAndAddReturningDifference', 'modifyAndDelete', 'addLeftOnly'] as Set
        responseBody().items.find { dataClass -> dataClass.label == 'modifyAndDelete' }.description == 'Description'
        responseBody().items.find { dataClass -> dataClass.label == 'addAndAddReturningDifference' }.description == 'addedDescriptionSource'
        responseBody().items.find { dataClass -> dataClass.label == 'modifyAndModifyReturningDifference' }.description == modifiedDescriptionSource
        responseBody().items.find { dataClass -> dataClass.label == 'modifyLeftOnly' }.description == 'modifiedDescriptionSourceOnly'

        when:
        GET("$target/dataClasses/$existingClass/dataClasses")

        then:
        responseBody().items.label as Set == ['addRightToExistingClass', 'addLeftToExistingClass'] as Set

        when: 'List edits for the Target DataModel'
        GET("$target/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is a CHANGE NOTICE edit'
        response.body().items.find {
            it.title == "CHANGENOTICE" && it.description == "Functional Test Merge Change Notice"
        }

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'VB09d : test merging metadata diff into draft model'() {
        given:
        String id = createNewItem(validJson)

        POST("$id/dataClasses", [label: 'modifyLeftOnly'])
        verifyResponse CREATED, response

        POST("$id/metadata", [namespace: 'functional.test.namespace', key: 'deleteMetadataSource', value: 'original'])
        verifyResponse CREATED, response
        POST("$id/metadata", [namespace: 'functional.test.namespace', key: 'modifyMetadataSource', value: 'original'])
        verifyResponse CREATED, response

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        //to modify
        GET("$source/path/dm%3A%7Cdc%3AmodifyLeftOnly")
        verifyResponse OK, response
        String modifyLeftOnly = responseBody().id

        GET("$source/metadata")
        verifyResponse OK, response
        String deleteMetadataSource = responseBody().items.find { it.key == 'deleteMetadataSource' }.id
        String modifyMetadataSource = responseBody().items.find { it.key == 'modifyMetadataSource' }.id

        then:
        //dataModel description
        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        //dataClasses
        PUT("$source/dataClasses/$modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response

        //metadata
        DELETE("$source/metadata/$deleteMetadataSource")
        verifyResponse NO_CONTENT, response

        PUT("$source/metadata/$modifyMetadataSource", [value: 'Modified Description'])
        verifyResponse OK, response

        POST("$source/metadata", [namespace: 'functional.test.namespace', key: 'addMetadataSource', value: 'original'])
        verifyResponse CREATED, response
        String addMetadataSource = responseBody().id

        POST("dataClasses/$modifyLeftOnly/metadata", [
            namespace: 'functional.test.namespace',
            key      : 'addMetadataModifyLeftOnly',
            value    : 'original'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        String addMetadataModifyLeftOnly = responseBody().id

        //dataModel description
        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response

        when:
        // for mergeInto json
        GET("$target/path/dm%3A%7Cdc%3AmodifyLeftOnly")
        verifyResponse OK, response
        modifyLeftOnly = responseBody().id

        GET("$target/metadata")
        verifyResponse OK, response
        deleteMetadataSource = responseBody().items.find { it.key == "deleteMetadataSource" }.id
        modifyMetadataSource = responseBody().items.find { it.key == "modifyMetadataSource" }.id

        GET("$source/mergeDiff/$target", STRING_ARG)

        then:
        verifyJsonResponse OK, '''
{
    "leftId": "${json-unit.matches:id}",
    "rightId": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "count": 7,
    "diffs": [
        {
            "description": {
                "left": "DescriptionRight",
                "right": "DescriptionLeft",
                "isMergeConflict": true,
                "commonAncestorValue": null
            }
        },
        {
            "metadata": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "namespace": "functional.test.namespace",
                            "key": "deleteMetadataSource",
                            "value": "original"
                        },
                        "isMergeConflict": false
                    }
                ],
                "created": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "namespace": "functional.test.namespace",
                            "key": "addMetadataSource",
                            "value": "original"
                        },
                        "isMergeConflict": false
                    }
                ],
                "modified": [
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "namespace": "functional.test.namespace",
                        "key": "modifyMetadataSource",
                        "count": 1,
                        "diffs": [
                            {
                                "value": {
                                    "left": "original",
                                    "right": "Modified Description",
                                    "isMergeConflict": false
                                }
                            }
                        ]
                    }
                ]
            }
        },
        {
            "branchName": {
                "left": "main",
                "right": "source",
                "isMergeConflict": false
            }
        },
        {
            "dataClasses": {
                "modified": [
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "modifyLeftOnly",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                        ],
                        "count": 2,
                        "diffs": [
                            {
                                "description": {
                                    "left": null,
                                    "right": "Description",
                                    "isMergeConflict": false
                                }
                            },
                            {
                                "metadata": {
                                    "created": [
                                        {
                                            "value": {
                                                "id": "${json-unit.matches:id}",
                                                "namespace": "functional.test.namespace",
                                                "key": "addMetadataModifyLeftOnly",
                                                "value": "original"
                                            },
                                            "isMergeConflict": false
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        }
    ]
}'''

        when:
        String modifiedDescriptionSource = 'modifiedDescriptionSource'

        def requestBody = [
            patch: [
                leftId : target,
                rightId: source,
                label  : "Functional Test Model",
                diffs  : [
                    [
                        fieldName: "description",
                        value    : modifiedDescriptionSource
                    ],
                    [
                        fieldName: "metadata",

                        deleted  : [
                            [
                                id   : deleteMetadataSource,
                                label: "deleteMetadataSource"
                            ]
                        ],
                        created  : [
                            [
                                id   : addMetadataSource,
                                label: "addMetadataSource"
                            ]
                        ],
                        modified : [
                            [
                                leftId: modifyMetadataSource,
                                label : "modifyMetadataSource",
                                diffs : [
                                    [
                                        fieldName: "value",
                                        value    : modifiedDescriptionSource
                                    ]
                                ]
                            ]
                        ]
                    ],
                    [
                        fieldName: "dataClasses",
                        modified : [
                            [
                                leftId: modifyLeftOnly,
                                label : "modifyLeftOnly",
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : "modifiedDescriptionSourceOnly"
                                    ],
                                    [
                                        fieldName: "metadata",
                                        created  : [
                                            [
                                                id   : addMetadataModifyLeftOnly,
                                                label: "addMetadataModifyLeftOnly"
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
            ]
        ]


        PUT("$source/mergeInto/$target", requestBody)

        then:
        verifyResponse OK, response
        responseBody().id == target
        responseBody().description == modifiedDescriptionSource

        when:
        GET("$target/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.label as Set == ['modifyLeftOnly'] as Set
        responseBody().items.find { dataClass -> dataClass.label == 'modifyLeftOnly' }.description == 'modifiedDescriptionSourceOnly'

        when:
        verifyResponse OK, response
        GET("$target/metadata")

        then:
        responseBody().items.key as Set == ['addMetadataSource', 'modifyMetadataSource'] as Set
        responseBody().items.find { metadata -> metadata.key == 'modifyMetadataSource' }.value == 'modifiedDescriptionSource'

        when:
        GET("dataClasses/$modifyLeftOnly/metadata", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.key as Set == ['addMetadataModifyLeftOnly'] as Set

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    /**
     * In this test we create a DataModel containing one DataClass. The DataModel is finalised, and a new branch 'source'
     * created. On the source branch, a DataElement is added to the DataClass. The source branch is then merged 
     * back into main, and we check that the DataElement which was created on the source branch is correctly added to the
     * DataClass on the main branch.
     */
    void 'VB09e : test merging diff in which a DataElement has been created on a DataClass - failing test for MC-9433'() {
        given: 'A DataModel is created'
        String id = createNewItem(validJson)

        when: 'A DataClass is added to the DataModel'
        POST("$id/dataClasses", ["label": "Functional Test DataClass"])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def dataClassId = response.body().id

        when: 'The DataModel is finalised'
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'A new model version is created'
        PUT("$id/newBranchModelVersion", [branchName: 'source'])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def sourceDataModelId = response.body().id

        when: 'Get the DataClasses on the source model'
        GET("$sourceDataModelId/dataClasses")

        then: 'The result is OK with one DataClass listed'
        verifyResponse OK, response
        response.body().count == 1
        def sourceDataClassId = response.body().items[0].id

        when: 'A new DataType is added to the source DataModel'
        POST("$sourceDataModelId/dataTypes", ["label": "A", "domainType": "PrimitiveType"])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def dataTypeId = response.body().id
        
        when: 'A new DataElement is added to the source DataClass'
        POST("$sourceDataModelId/dataClasses/$sourceDataClassId/dataElements", ["label": "New Data Element", "dataType": ["id": dataTypeId]])        

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def sourceDataElementId = response.body().id

        when:
        def requestBody = [
            patch: [
                "rightId": sourceDataModelId,
                "leftId" : id,
                "diffs"  : [
                    [
                        "fieldName": "dataClasses",
                        "created"  : [],
                        "deleted"  : [],
                        "modified" : [
                            [
                                "leftId": sourceDataClassId,
                                "diffs" : [
                                    [
                                        "fieldName": "dataClasses",
                                        "created"  : [],
                                        "deleted"  : [],
                                        "modified" : []
                                    ],
                                    [
                                        "fieldName": "dataTypes",
                                        "created"  : [],
                                        "deleted"  : [],
                                        "modified" : []
                                    ],
                                    [
                                        "fieldName": "dataElements",
                                        "created"  : [
                                            [
                                                "id": sourceDataElementId
                                            ]
                                        ],
                                        "deleted"  : [],
                                        "modified" : []
                                    ],
                                    [
                                        "fieldName": "metadata",
                                        "created"  : [],
                                        "deleted"  : [],
                                        "modified" : []
                                    ]
                                ]
                            ]
                        ]
                    ],
                    [
                        "fieldName": "dataTypes",
                        "created"  : [],
                        "deleted"  : [],
                        "modified" : []
                    ],
                    [
                        "fieldName": "dataElements",
                        "created"  : [],
                        "deleted"  : [],
                        "modified" : []
                    ],
                    [
                        "fieldName": "metadata",
                        "created"  : [],
                        "deleted"  : [],
                        "modified" : []
                    ]
                ]
            ]
        ]

        PUT("$sourceDataModelId/mergeInto/$id", requestBody)

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get the DataElements on the main branch model'
        GET("$id/dataClasses/$dataClassId/dataElements")

        then: 'The response is OK and there is one DataElement'
        verifyResponse OK, response
        response.body().count == 1
        response.body().items[0].label == 'New Data Element'

        cleanup:
        cleanUpData(sourceDataModelId)
        cleanUpData(id)
    }    

    void 'test changing folder from DataModel context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/folder/${movingFolderId}", [:])

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test changing folder from Folder context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        response = PUT("folders/${movingFolderId}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'D01 : test diffing 2 DataModels'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)
        String otherId = createNewItem([label: 'Functional Test Model 2'])

        when:
        GET("${id}/diff/${otherId}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "count": 1,
  "label": "Functional Test Model",
  "diffs": [
    {
      "label": {
        "left": "Functional Test Model",
        "right": "Functional Test Model 2"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'D02 : test diffing branches'() {
        given:
        // Create base model and finalise
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Create a new branch main
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id

        // Create a new branch test
        PUT("$id/newBranchModelVersion", [branchName: 'test'])
        verifyResponse CREATED, response
        String testId = responseBody().id

        // Add dataclass test2 to main branch
        POST("$mainId/dataClasses", [label: 'test2'])
        verifyResponse CREATED, response

        // Add dataclass test2 to test branch
        POST("$testId/dataClasses", [label: 'test2'])
        verifyResponse CREATED, response

        when: 'performing diff'
        GET("$testId/diff/$mainId")

        then:
        verifyResponse OK, response
        responseBody()

        cleanup:
        cleanUpData(id)
        cleanUpData(testId)
        cleanUpData(mainId)
    }

    void 'D03 : test diffing branches on modifications'() {
        given:
        // Create base model
        String id = createNewItem(validJson)
        // Create content
        POST("$id/dataClasses", [label: 'parent'])
        verifyResponse(CREATED, response)
        String parentId = responseBody().id
        POST("$id/dataClasses/${parentId}/dataClasses", [label: 'child'])
        verifyResponse(CREATED, response)
        POST("$id/dataClasses", [label: 'content', description: 'some interesting content'])
        verifyResponse(CREATED, response)

        //Finalise
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Create a new branch main
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id

        // Create a new branch test
        PUT("$id/newBranchModelVersion", [branchName: 'test'])
        verifyResponse CREATED, response
        String testId = responseBody().id

        //Change child DC label
        GET("$testId/dataClasses")
        verifyResponse(OK, response)
        parentId = responseBody().items.find { it.label == 'parent' }.id
        String contentId = responseBody().items.find { it.label == 'content' }.id
        GET("$testId/dataClasses/${parentId}/dataClasses")
        verifyResponse(OK, response)
        assert responseBody().items.first().id
        assert responseBody().items.first().label == 'child'
        String childId = responseBody().items.first().id
        PUT("$testId/dataClasses/${parentId}/dataClasses/$childId", [label: 'child edit'])
        verifyResponse(OK, response)
        // change description of the content
        PUT("$testId/dataClasses/$contentId", [description: 'a change to the description'])

        when: 'performing diff'
        GET("$testId/diff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().count == 4
        responseBody().diffs.size() == 2
        responseBody().diffs.first().branchName.left == 'test'
        responseBody().diffs.first().branchName.right == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        and:
        Map dataClassesDiffs = responseBody().diffs[1].dataClasses
        dataClassesDiffs.modified.size() == 2

        and:
        Map contentDiff = dataClassesDiffs.modified.find { it.label == 'content' }
        contentDiff.diffs.size() == 1
        contentDiff.diffs.first().description.left == 'a change to the description'
        contentDiff.diffs.first().description.right == 'some interesting content'

        and:
        Map parentDiff = dataClassesDiffs.modified.find { it.label == 'parent' }
        parentDiff.diffs.size() == 1
        parentDiff.diffs.first().dataClasses.deleted.size() == 1
        parentDiff.diffs.first().dataClasses.created.size() == 1
        parentDiff.diffs.first().dataClasses.deleted.first().value.label == 'child edit'
        parentDiff.diffs.first().dataClasses.created.first().value.label == 'child'

        cleanup:
        cleanUpData(id)
        cleanUpData(testId)
        cleanUpData(mainId)
    }

    void 'E01 : test export a single DataModel'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'E02 : test export multiple DataModels (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0',
             [dataModelIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'I01 : test import basic DataModel'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Import",
                      "type": "Data Standard",
                      "branchName": "main",
                      "documentationVersion": "1.0.0"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    void 'I02 : test import basic DataModel as new documentation version'() {
        given:
        String id = createNewItem([
            label       : 'Functional Test Model',
            finalised   : true,
            modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            modelName                      : 'Functional Test Model',
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard",
                      "branchName": "main",
                      "documentationVersion": "2.0.0",
                      "modelVersion": "1.0.0"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    void 'I03 : test import basic DataModel as new branch model version'() {
        given:
        String id = createNewItem([
            label       : 'Functional Test Model',
            finalised   : true,
            modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            modelName                      : 'Functional Test Model',
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importAsNewBranchModelVersion  : true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard",
                      "branchName": "main",
                      "documentationVersion": "1.0.0"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }


    void 'I04 : test import basic DataModel as new main branch model version with another branch version that exists'() {
        given:
        String id = createNewItem([
            label       : 'Functional Test Model',
            finalised   : true,
            modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            modelName                      : 'Functional Test Model',
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importAsNewBranchModelVersion  : true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard",
                      "branchName": "main",
                      "documentationVersion": "1.0.0"
                    }
                  ]
                }'''

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            modelName                      : 'Functional Test Model',
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importAsNewBranchModelVersion  : true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().total == 1
        responseBody().errors[0].message == 'Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel] with value ' +
        '[Functional Test Model] must be unique by branch name'

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            modelName                      : 'Functional Test Model',
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importAsNewBranchModelVersion  : true,
            newBranchName                  : 'functionalTest',
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard",
                      "branchName": "functionalTest",
                      "documentationVersion": "1.0.0"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    void 'test delete multiple models'() {
        given:
        def idstoDelete = []
        (1..4).each { n ->
            idstoDelete << createNewItem([
                folder: folderId,
                label : UUID.randomUUID().toString()
            ])
        }

        when:
        DELETE('', [
            ids      : idstoDelete,
            permanent: false
        ], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    }
  ]
}'''

        when:
        DELETE('', [
            ids      : idstoDelete,
            permanent: true
        ])

        then:
        verifyResponse NO_CONTENT, response
    }

    void 'I05 : test importing simple test DataModel'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I06 : test importing complex test DataModel'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I07 : test importing DataModel with classifiers'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('fullModelWithClassifiers').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I08 : test importing 2 DataModel'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/3.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.XML.name,
                fileContents: loadTestFile('multiModels', 'xml').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id
        def id2 = response.body().items[1].id

        then:
        id
        id2

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'E03 : test export simple DataModel'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('simpleDataModel'))
            .replaceFirst('"exportedBy": "Admin User",', '"exportedBy": "Unlogged User",')
            .replace(/Test Authority/, 'Mauro Data Mapper')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'E04 : test export complex DataModel'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('complexDataModel'))
            .replaceFirst('"exportedBy": "Admin User",', '"exportedBy": "Unlogged User",')
            .replace(/Test Authority/, 'Mauro Data Mapper')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'H01 : test getting simple DataModel hierarchy'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        expect:
        id

        when:
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "childDataClasses": [
    {
      "dataClasses": [],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataElements": [],
      "domainType": "DataClass",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "simple",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel"
        }
      ]
    }
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "dataTypes": [],
  "domainType": "DataModel",
  "documentationVersion": "1.0.0",
  "availableActions": ["delete","show","update"],
  "branchName":"main",
  "finalised": false,
  "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    },
  "id": "${json-unit.matches:id}",
  "label": "Simple Test DataModel",
  "type": "Data Standard",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'H02 : test getting complex DataModel hierarchy'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        expect:
        id

        when:
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Complex Test DataModel",
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "branchName":"main",
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
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
  },
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "author": "admin person",
  "organisation": "brc",
  "dataTypes": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "EnumerationType",
      "label": "yesnounknown",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "enumerationValues": [
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
        },
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "key": "N",
          "value": "No",
          "category": null
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "integer",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "string",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceType",
      "label": "child",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "referenceClass": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataClass",
        "label": "child",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "domainType": "DataModel",
            "finalised": false
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "parent",
            "domainType": "DataClass"
          }
        ],
        "parentDataClass": "${json-unit.matches:id}"
      }
    }
  ],
  "childDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "content",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "A dataclass with elements",
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "maxMultiplicity": 1,
      "minMultiplicity": 0,
      "dataClasses": [
        
      ],
      "dataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "element2",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "content",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "delete",
            "show",
            "update"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "dataClass": "${json-unit.matches:id}",
          "dataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "PrimitiveType",
            "label": "integer",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          "maxMultiplicity": 1,
          "minMultiplicity": 1
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "ele1",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "content",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "delete",
            "show",
            "update"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "dataClass": "${json-unit.matches:id}",
          "dataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "PrimitiveType",
            "label": "string",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          "maxMultiplicity": 20,
          "minMultiplicity": 0
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "emptyclass",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "dataclass with desc",
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "parent",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "maxMultiplicity": -1,
      "minMultiplicity": 1,
      "dataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataClass",
          "label": "child",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "parent",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "delete",
            "show",
            "update"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "parentDataClass": "${json-unit.matches:id}",
          "dataClasses": [
            
          ],
          "dataElements": [
            
          ],
          "parentDataClass": "${json-unit.matches:id}"
        }
      ],
      "dataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "child",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "parent",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "delete",
            "show",
            "update"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "dataClass": "${json-unit.matches:id}",
          "dataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceType",
            "label": "child",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ],
            "referenceClass": {
              "id": "${json-unit.matches:id}",
              "domainType": "DataClass",
              "label": "child",
              "model": "${json-unit.matches:id}",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                },
                {
                  "id": "${json-unit.matches:id}",
                  "label": "parent",
                  "domainType": "DataClass"
                }
              ],
              "parentDataClass": "${json-unit.matches:id}"
            }
          },
          "maxMultiplicity": 1,
          "minMultiplicity": 1
        }
      ]
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test diffing 2 complex and simple DataModels'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        String complexDataModelId = response.body().items[0].id

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        String simpleDataModelId = response.body().items[0].id

        expect:
        complexDataModelId
        simpleDataModelId

        when:
        GET("${complexDataModelId}/diff/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
    "leftId": "${json-unit.matches:id}",
    "rightId": "${json-unit.matches:id}",
    "label": "Complex Test DataModel",
    "count": 20,
    "diffs": [
        {
            "label": {
                "left": "Complex Test DataModel",
                "right": "Simple Test DataModel"
            }
        },
        {
            "metadata": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "namespace": "test.com",
                            "key": "mdk1",
                            "value": "mdv1"
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "namespace": "test.com/test",
                            "key": "mdk1",
                            "value": "mdv2"
                        }
                    }
                ],
                "created": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "namespace": "test.com/simple",
                            "key": "mdk1",
                            "value": "mdv1"
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "namespace": "test.com/simple",
                            "key": "mdk2",
                            "value": "mdv2"
                        }
                    }
                ]
            }
        },
        {
            "annotations": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "test annotation 1"
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "test annotation 2"
                        }
                    }
                ]
            }
        },
        {
            "author": {
                "left": "admin person",
                "right": null
            }
        },
        {
            "organisation": {
                "left": "brc",
                "right": null
            }
        },
        {
            "dataTypes": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "string",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "integer",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "yesnounknown",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "child",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    }
                ]
            }
        },
        {
            "dataClasses": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "content",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "emptyclass",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "parent",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    }
                ],
                "created": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "simple",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                }
                            ]
                        }
                    }
                ]
            }
        },
        {
            "dataElements": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "element2",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                },
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "content",
                                    "domainType": "DataClass"
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "child",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                },
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "parent",
                                    "domainType": "DataClass"
                                }
                            ]
                        }
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "ele1",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel",
                                    "domainType": "DataModel",
                                    "finalised": false
                                },
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "content",
                                    "domainType": "DataClass"
                                }
                            ]
                        }
                    }
                ]
            }
        }
    ]
}'''

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }


    void 'test searching for label "emptyclass" in complex model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])

        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String term = 'emptyclass'

        expect:
        id

        when:
        GET("${id}/search?search=${term}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
                      "count": 1,
                      "items": [
                        {
                          "domainType": "DataClass",
                          "description": "dataclass with desc",
                          "id": "${json-unit.matches:id}",
                          "label": "emptyclass",
                          "model": "${json-unit.matches:id}",
                          "breadcrumbs": [
                            {
                              "domainType": "DataModel",
                              "finalised": false,
                              "id": "${json-unit.matches:id}",
                              "label": "Complex Test DataModel"
                            }
                          ]
                        }
                      ]
                    }'''
        cleanup:
        cleanUpData(id)
    }

    void 'test searching for label "emptyclass" in simple model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])

        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String term = 'emptyclass'

        expect:
        id

        when:
        GET("${id}/search?search=${term}",)

        then:
        verifyResponse OK, response
        response.body().count == 0
        response.body().items.size() == 0

        cleanup:
        cleanUpData(id)
    }

    @Transactional
    void setupForLinkSuggestions(String simpleDataModelId) {
        DataClass dataClass = DataClass.byDataModelId(Utils.toUuid(simpleDataModelId)).eq('label', 'simple').find()
        assert dataClass


        POST("${simpleDataModelId}/dataTypes", [
            domainType: 'PrimitiveType',
            label     : 'string'
        ])
        verifyResponse(CREATED, response)
        String dataTypeId = response.body().id

        assert dataTypeId

        POST("${simpleDataModelId}/dataClasses/${dataClass.id}/dataElements", [
            domainType : 'DataElement',
            label      : 'ele1',
            description: 'most obvious match',
            dataType   : [
                id: dataTypeId
            ]
        ])
        verifyResponse CREATED, response

        POST("${simpleDataModelId}/dataClasses/${dataClass.id}/dataElements", [
            domainType : 'DataElement',
            label      : 'ele2',
            description: 'least obvious match',
            dataType   : [
                id: dataTypeId
            ]
        ])
        verifyResponse CREATED, response
    }


    void 'LS01 : test get link suggestions for a model with no data elements in the target'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        String complexDataModelId = response.body().items[0].id

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        String simpleDataModelId = response.body().items[0].id


        expect:
        complexDataModelId
        simpleDataModelId

        when:
        GET("${complexDataModelId}/suggestLinks/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedLinkSuggestions(['', '', ''])

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    void 'LS02 : test get link suggestions for a model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        String complexDataModelId = response.body().items[0].id

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        verifyResponse CREATED, response
        String simpleDataModelId = response.body().items[0].id

        expect:
        complexDataModelId
        simpleDataModelId

        when:
        setupForLinkSuggestions(simpleDataModelId)

        GET("${complexDataModelId}/suggestLinks/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedLinkSuggestions(expectedLinkSuggestionResults())

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    String expectedLinkSuggestions(List<String> results) {
        '''{
                      "links": [
                        {
                          "sourceDataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                              "domainType": "ReferenceType",
                              "model": "${json-unit.matches:id}",
                              "id": "${json-unit.matches:id}",
                              "label": "child",
                              "breadcrumbs": [
                                {
                                  "domainType": "DataModel",
                                  "finalised": false,
                                  "id": "${json-unit.matches:id}",
                                  "label": "Complex Test DataModel"
                                }
                              ],
                              "referenceClass": {
                                "domainType": "DataClass",
                                "model": "${json-unit.matches:id}",
                                "parentDataClass": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "child",
                                "breadcrumbs": [
                                  {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Complex Test DataModel"
                                  },
                                  {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "parent"
                                  }
                                ]
                              }
                            },
                            "model": "${json-unit.matches:id}",
                            "maxMultiplicity": 1,
                            "id": "${json-unit.matches:id}",
                            "label": "child",
                            "minMultiplicity": 1,
                            "breadcrumbs": [
                              {
                                "domainType": "DataModel",
                                "finalised": false,
                                "id": "${json-unit.matches:id}",
                                "label": "Complex Test DataModel"
                              },
                              {
                                "domainType": "DataClass",
                                "id": "${json-unit.matches:id}",
                                "label": "parent"
                              }
                            ]
                          },
                          "results": [''' + results[2] + '''

                          ]
                        },
                        {
                          "sourceDataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                              "domainType": "PrimitiveType",
                              "model": "${json-unit.matches:id}",
                              "id": "${json-unit.matches:id}",
                              "label": "string",
                              "breadcrumbs": [
                                {
                                  "domainType": "DataModel",
                                  "finalised": false,
                                  "id": "${json-unit.matches:id}",
                                  "label": "Complex Test DataModel"
                                }
                              ]
                            },
                            "model": "${json-unit.matches:id}",
                            "maxMultiplicity": 20,
                            "id": "${json-unit.matches:id}",
                            "label": "ele1",
                            "minMultiplicity": 0,
                            "breadcrumbs": [
                              {
                                "domainType": "DataModel",
                                "finalised": false,
                                "id": "${json-unit.matches:id}",
                                "label": "Complex Test DataModel"
                              },
                              {
                                "domainType": "DataClass",
                                "id": "${json-unit.matches:id}",
                                "label": "content"
                              }
                            ]
                          },
                          "results": [''' + results[0] + '''

                          ]
                        },
                        {
                          "sourceDataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                              "domainType": "PrimitiveType",
                              "model": "${json-unit.matches:id}",
                              "id": "${json-unit.matches:id}",
                              "label": "integer",
                              "breadcrumbs": [
                                {
                                  "domainType": "DataModel",
                                  "finalised": false,
                                  "id": "${json-unit.matches:id}",
                                  "label": "Complex Test DataModel"
                                }
                              ]
                            },
                            "model": "${json-unit.matches:id}",
                            "maxMultiplicity": 1,
                            "id": "${json-unit.matches:id}",
                            "label": "element2",
                            "minMultiplicity": 1,
                            "breadcrumbs": [
                              {
                                "domainType": "DataModel",
                                "finalised": false,
                                "id": "${json-unit.matches:id}",
                                "label": "Complex Test DataModel"
                              },
                              {
                                "domainType": "DataClass",
                                "id": "${json-unit.matches:id}",
                                "label": "content"
                              }
                            ]
                          },
                          "results": [''' + results[1] + '''

                          ]
                        }
                      ]
                    }'''
    }

    List<String> expectedLinkSuggestionResults() {
        ['''
                    {
                        "score": "${json-unit.any-number}",
                        "dataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                                "domainType": "PrimitiveType",
                                "model": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "string",
                                "breadcrumbs": [
                                    {
                                        "domainType": "DataModel",
                                        "finalised": false,
                                        "id": "${json-unit.matches:id}",
                                        "label": "Simple Test DataModel"
                                    }
                                ]
                            },
                            "model": "${json-unit.matches:id}",
                            "description": "most obvious match",
                            "id": "${json-unit.matches:id}",
                            "label": "ele1",
                            "breadcrumbs": [
                                {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel"
                                },
                                {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "simple"
                                }
                            ]
                        }
                    },
                    {
                        "score": "${json-unit.any-number}",
                        "dataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                                "domainType": "PrimitiveType",
                                "model": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "string",
                                "breadcrumbs": [
                                    {
                                        "domainType": "DataModel",
                                        "finalised": false,
                                        "id": "${json-unit.matches:id}",
                                        "label": "Simple Test DataModel"
                                    }
                                ]
                            },
                            "model": "${json-unit.matches:id}",
                            "description": "least obvious match",
                            "id": "${json-unit.matches:id}",
                            "label": "ele2",
                            "breadcrumbs": [
                                {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel"
                                },
                                {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "simple"
                                }
                            ]
                        }
                    }''', '''
                    {
                        "score": "${json-unit.any-number}",
                        "dataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                                "domainType": "PrimitiveType",
                                "model": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "string",
                                "breadcrumbs": [
                                    {
                                        "domainType": "DataModel",
                                        "finalised": false,
                                        "id": "${json-unit.matches:id}",
                                        "label": "Simple Test DataModel"
                                    }
                                ]
                            },
                            "model": "${json-unit.matches:id}",
                            "description": "least obvious match",
                            "id": "${json-unit.matches:id}",
                            "label": "ele2",
                            "breadcrumbs": [
                                {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel"
                                },
                                {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "simple"
                                }
                            ]
                        }
                    }''', '''''']
    }
}