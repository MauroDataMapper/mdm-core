/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.terminology

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessPermissionChangingAndVersioningFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.util.BuildSettings
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: terminology
 *
 *  |  GET     | /api/terminologies        | Action: index
 *  |  DELETE  | /api/terminologies/${id}  | Action: delete
 *  |  PUT     | /api/terminologies/${id}  | Action: update
 *  |  GET     | /api/terminologies/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/terminologies                 | Action: save
 *
 *  |  DELETE  | /api/terminologies/${terminologyId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/terminologies/${terminologyId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/terminologies/${terminologyId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/terminologies/${terminologyId}/readByEveryone         | Action: readByEveryone
 *
 *  |  PUT     | /api/terminologies/${terminologyId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/terminologies/${terminologyId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/terminologies/${terminologyId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/terminologies/${terminologyId}      | Action: changeFolder
 *  |  PUT     | /api/terminologies/${terminologyId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/terminologies/${terminologyId}/diff/${otherTerminologyId}  | Action: diff
 *
 *  |   GET    | /api/terminologies/providers/importers  | Action: importerProviders
 *  |   GET    | /api/terminologies/providers/exporters  | Action: exporterProviders
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyController
 */
@Integration
@Slf4j
class TerminologyFunctionalSpec extends ModelUserAccessPermissionChangingAndVersioningFunctionalSpec {

    @Transactional
    String getComplexTerminologyId() {
        Terminology.findByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getSimpleTerminologyId() {
        Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getLeftHandDiffModelId() {
        Terminology.findByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getRightHandDiffModelId() {
        Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getModelFolderId(String id) {
        Terminology.get(id).folder.id.toString()
    }

    @Override
    String getResourcePath() {
        'terminologies'
    }

    @Override
    String getSavePath() {
        "folders/${getTestFolderId()}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test Terminology'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: 'Complex Test Terminology'
        ]
    }

    @Override
    String getModelType() {
        'Terminology'
    }

    @Override
    String getModelUrlType() {
        'terminologies'
    }

    @Override
    String getModelPrefix() {
        'teFunctional Test Model'
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Complex Test Terminology",
      "branchName": "main",
      "documentationVersion": "1.0.0",
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
      "author": "Test Bootstrap",
      "organisation": "Oxford BRC",
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper",
        "defaultAuthority": true
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Simple Test Terminology",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "author": "Test Bootstrap",
      "organisation": "Oxford BRC",
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper",
        "defaultAuthority": true
      }
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "Terminology",
  "label": "Functional Test Terminology",
  "finalised": false,
  "type": "Terminology",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["show"],
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
  }
}'''
    }

    void 'EX01: Test getting available Terminology exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
            {
                "name": "TerminologyJsonExporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "JSON Terminology Exporter",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "TerminologyExporter",
                "fileExtension": "json",
                "contentType": "application/mauro.terminology+json",
                "canExportMultipleDomains": true
            },
            {
                "name": "TerminologyXmlExporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "XML Terminology Exporter",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "TerminologyExporter",
                "fileExtension": "xml",
                "contentType": "application/mauro.terminology+xml",
                "canExportMultipleDomains": true
            }
        ]'''
    }

    void 'IM01: Test getting available Terminology importers'() {

        when: 'not logged in then inaccessible'
        GET('providers/importers')

        then:
        verifyForbidden response

        when: 'logged in'
        loginAuthenticated()
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
            {
                "name": "TerminologyXmlImporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "XML Terminology Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [

                ],
                "providerType": "TerminologyImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter''' +
                               '''.TerminologyFileImporterProviderServiceParameters",
                "canImportMultipleDomains": true
            },
            {
                "name": "TerminologyJsonImporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "JSON Terminology Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [

                ],
                "providerType": "TerminologyImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter''' +
                               '''.TerminologyFileImporterProviderServiceParameters",
                "canImportMultipleDomains": true
            }
        ]'''
    }

    void 'L33 : test export a single Terminology (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N33 : test export a single Terminology (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R33 : test export a single Terminology (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
            "terminology": {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Terminology",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "documentationVersion": "1.0.0",
                "finalised": false,
                "authority": {
                    "id": "${json-unit.matches:id}",
                    "url": "http://localhost",
                    "label": "Mauro Data Mapper"
                }
            },
            "exportMetadata": {
                "exportedBy": "reader User",
                "exportedOn": "${json-unit.matches:offsetDateTime}",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "TerminologyJsonExporterService",
                    "version": "${json-unit.matches:version}"
                }
            }
        }'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'E33 : test export a single Terminology (as editor)'() {
        given:
        String id = getValidId()

        when:
        loginEditor()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
            "terminology": {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Terminology",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "documentationVersion": "1.0.0",
                "finalised": false,
                "authority": {
                    "id": "${json-unit.matches:id}",
                    "url": "http://localhost",
                    "label": "Mauro Data Mapper"
                }
            },
            "exportMetadata": {
                "exportedBy": "editor User",
                "exportedOn": "${json-unit.matches:offsetDateTime}",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "TerminologyJsonExporterService",
                    "version": "${json-unit.matches:version}"
                }
            }
        }'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L34 : test export multiple Terminologies (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/2.0',
             [terminologyIds: [id, getSimpleTerminologyId()]])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N34 : test export multiple Terminologies (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/2.0',
             [terminologyIds: [id, getSimpleTerminologyId()]])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R34 : test export multiple Terminologies (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0',
             [terminologyIds: [id, getSimpleTerminologyId()]], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
            "terminologies": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test Terminology",
                    "lastUpdated": "${json-unit.matches:offsetDateTime}",
                    "documentationVersion": "1.0.0",
                    "finalised": false,
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label": "Mauro Data Mapper"
                    }
                },
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Simple Test Terminology",
                    "lastUpdated": "${json-unit.matches:offsetDateTime}",
                    "documentationVersion": "1.0.0",
                    "finalised": false,
                    "author": "Test Bootstrap",
                    "organisation": "Oxford BRC",
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label":"Mauro Data Mapper"
                    },
                    "terms": [
                        {
                            "id": "${json-unit.matches:id}",
                            "label": "STT02: Simple Test Term 02",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "code": "STT02",
                            "definition": "Simple Test Term 02",
                            "depth": 1
                        },
                        {
                            "id": "${json-unit.matches:id}",
                            "label": "STT01: Simple Test Term 01",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "code": "STT01",
                            "definition": "Simple Test Term 01",
                            "depth": 1
                        }
                    ],
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
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "namespace": "terminology.test.com/simple",
                            "key": "mdk2",
                            "value": "mdv2"
                        },
                        {
                            "id": "${json-unit.matches:id}",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "namespace": "terminology.test.com/simple",
                            "key": "mdk1",
                            "value": "mdv1"
                        },
                        {
                            "id": "${json-unit.matches:id}",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "namespace": "terminology.test.com",
                            "key": "mdk2",
                            "value": "mdv2"
                        }
                    ]
                }
            ],
            "exportMetadata": {
            "exportedBy": "reader User",
            "exportedOn": "${json-unit.matches:offsetDateTime}",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "TerminologyJsonExporterService",
                    "version": "${json-unit.matches:version}"
                }
            }
        }'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'E34 : test export multiple Terminologies (as editor)'() {
        given:
        String id = getValidId()

        when:
        loginEditor()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0',
             [terminologyIds: [id, getSimpleTerminologyId()]], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
            "terminologies": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test Terminology",
                    "lastUpdated": "${json-unit.matches:offsetDateTime}",
                    "documentationVersion": "1.0.0",
                    "finalised": false,
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label": "Mauro Data Mapper"
                    }
                },
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Simple Test Terminology",
                    "lastUpdated": "${json-unit.matches:offsetDateTime}",
                    "documentationVersion": "1.0.0",
                    "finalised": false,
                    "author": "Test Bootstrap",
                    "organisation": "Oxford BRC",
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label":"Mauro Data Mapper"
                    },
                    "terms": [
                        {
                            "id": "${json-unit.matches:id}",
                            "label": "STT02: Simple Test Term 02",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "code": "STT02",
                            "definition": "Simple Test Term 02",
                            "depth": 1
                        },
                        {
                            "id": "${json-unit.matches:id}",
                            "label": "STT01: Simple Test Term 01",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "code": "STT01",
                            "definition": "Simple Test Term 01",
                            "depth": 1
                        }
                    ],
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
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "namespace": "terminology.test.com/simple",
                            "key": "mdk2",
                            "value": "mdv2"
                        },
                        {
                            "id": "${json-unit.matches:id}",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "namespace": "terminology.test.com/simple",
                            "key": "mdk1",
                            "value": "mdv1"
                        },
                        {
                            "id": "${json-unit.matches:id}",
                            "lastUpdated": "${json-unit.matches:offsetDateTime}",
                            "namespace": "terminology.test.com",
                            "key": "mdk2",
                            "value": "mdv2"
                        }
                    ]
                }
            ],
            "exportMetadata": {
            "exportedBy": "editor User",
            "exportedOn": "${json-unit.matches:offsetDateTime}",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "TerminologyJsonExporterService",
                    "version": "${json-unit.matches:version}"
                }
            }
        }'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L35 : test import basic Terminology (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/4.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'N35 : test import basic Terminology (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/4.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, testFolderId

        cleanup:
        removeValidIdObject(id)
    }

    void 'R35 : test import basic Terminology (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/4.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E35A : test import basic Terminology (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/4.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test Import'
        response.body().items.first().id != id
        String id2 = response.body().items.first().id

        cleanup:
        removeValidIdObject(id2)
        removeValidIdObject(id)
    }

    void 'E35B : test import basic Terminology as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/4.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test Terminology'
        response.body().items.first().id != id

        when:
        String newId = response.body().items.first().id
        GET("$newId/versionLinks")

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().domainType == 'VersionLink'
        response.body().items.first().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        response.body().items.first().sourceModel.id == newId
        response.body().items.first().targetModel.id == id
        response.body().items.first().sourceModel.domainType == response.body().items.first().targetModel.domainType

        cleanup:
        removeValidIdObject(newId, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }

    void 'L36 : test import multiple Terminologies (as not logged in)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0',
             [terminologyIds: [getSimpleTerminologyId(), getComplexTerminologyId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response
    }

    void 'N36 : test import multiple Terminologies (as authenticated/no access)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0',
             [terminologyIds: [getSimpleTerminologyId(), getComplexTerminologyId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, null
    }

    void 'R36 : test import multiple Terminologies (as reader)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0',
             [terminologyIds: [getSimpleTerminologyId(), getComplexTerminologyId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, null
    }

    void 'E36 : test import multiple Terminologies (as editor)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/4.0',
             [terminologyIds: [getSimpleTerminologyId(), getComplexTerminologyId()]], STRING_ARG)


        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/4.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString
                    .replace(/Simple Test Terminology/, 'Simple Test Terminology 2')
                    .replace(/Complex Test Terminology/, 'Complex Test Terminology 2')
                    .bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 2

        Object object = response.body().items[0]
        Object object2 = response.body().items[1]
        String id = object.id
        String id2 = object2.id

        object.label == 'Simple Test Terminology 2'
        object2.label == 'Complex Test Terminology 2'
        object.id != object2.id

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(id2)
    }

    String getExpectedDiffJson() {
        Files.readString(Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'terminologies', 'diff.json'))
    }
}
