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
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessPermissionChangingAndVersioningFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: codeSet
 *
 *  |  GET     | /api/codeSets        | Action: index
 *  |  DELETE  | /api/codeSets/${id}  | Action: delete
 *  |  PUT     | /api/codeSets/${id}  | Action: update
 *  |  GET     | /api/codeSets/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/codeSets                 | Action: save
 *
 *  |  DELETE  | /api/codeSets/${codeSetId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/codeSets/${codeSetId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/codeSets/${codeSetId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/codeSets/${codeSetId}/readByEveryone         | Action: readByEveryone
 *
 *  |  PUT     | /api/codeSets/${codeSetId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/codeSets/${codeSetId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/codeSets/${codeSetId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/codeSets/${codeSetId}      | Action: changeFolder
 *  |  PUT     | /api/codeSets/${codeSetId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/codeSets/${codeSetId}/diff/${otherCodeSetId}  | Action: diff
 *
 *  |   GET    | /api/codeSets/providers/importers  | Action: importerProviders
 *  |   GET    | /api/codeSets/providers/exporters  | Action: exporterProviders
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetController
 */
@Integration
@Slf4j
class CodeSetFunctionalSpec extends ModelUserAccessPermissionChangingAndVersioningFunctionalSpec {

    @Transactional
    String getSimpleCodeSetId() {
        CodeSet.findByLabel(BootstrapModels.SIMPLE_CODESET_NAME).id.toString()
    }

    @Transactional
    String getComplexCodeSetId() {
        CodeSet.findByLabel(BootstrapModels.COMPLEX_CODESET_NAME).id.toString()
    }

    @Transactional
    String getRightHandDiffModelId() {
        CodeSet.findByLabel(BootstrapModels.SIMPLE_CODESET_NAME).id.toString()
    }

    @Transactional
    String getLeftHandDiffModelId() {
        CodeSet.findByLabel(BootstrapModels.COMPLEX_CODESET_NAME).id.toString()
    }

    @Override
    @Transactional
    String getModelFolderId(String id) {
        CodeSet.get(id).folder.id.toString()
    }

    @Override
    String getResourcePath() {
        'codeSets'
    }

    @Override
    String getSavePath() {
        "folders/${getTestFolderId()}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test CodeSet'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label    : 'Functional Test CodeSet',
            finalised: true
        ]
    }

    @Override
    String getModelType() {
        'CodeSet'
    }

    @Override
    String getModelUrlType() {
        'codeSets'
    }

    @Override
    String getModelPrefix() {
        'cs'
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "CodeSet",
      "label": "Unfinalised Simple Test CodeSet",
      "branchName": "main",
      "documentationVersion": "1.0.0",
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
      "domainType": "CodeSet",
      "label": "Simple Test CodeSet",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "modelVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
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
      "domainType": "CodeSet",
      "label": "Complex Test CodeSet",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "modelVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
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
  "domainType": "CodeSet",
  "label": "Functional Test CodeSet",
  "finalised": false,
  "type": "CodeSet",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": [
    "show"
  ],
  "branchName":"main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
  }
}'''
    }

    void 'Test getting available CodeSet exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "CodeSetXmlExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML CodeSet Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "CodeSetExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": true
  },
  {
    "name": "CodeSetJsonExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON CodeSet Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "CodeSetExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": true
  }
]'''
    }

    void 'Test getting available CodeSet importers'() {

        when: 'not logged in then inaccessible'
        GET('providers/importers')

        then:
        verifyForbidden response

        when: 'logged in'
        loginAuthenticated()
        GET('providers/importers', STRING_ARG)

        then: 'The response is Unauth'
        verifyJsonResponse OK, '''[
  {
    "name": "CodeSetXmlImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML CodeSet Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "CodeSetImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters",
    "canImportMultipleDomains": true
  },
  {
    "name": "CodeSetJsonImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON CodeSet Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "CodeSetImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters",
    "canImportMultipleDomains": true
  }
]'''
    }

    void 'L33 : test export a single CodeSet (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N33 : test export a single CodeSet (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R33 : test export a single CodeSet (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "codeSet": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test CodeSet",
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
      "name": "CodeSetJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'E33 : test export a single CodeSet (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginEditor()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "codeSet": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test CodeSet",
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
      "name": "CodeSetJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L34 : test export multiple CodeSets (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [id, getSimpleCodeSetId()]])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N34 : test export multiple CodeSets (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [id, getSimpleCodeSetId()]])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R34 : test export multiple CodeSets (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [id, getSimpleCodeSetId()]], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "codeSets": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test CodeSet",
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
      "label": "Simple Test CodeSet",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "documentationVersion": "1.0.0",
      "finalised": true,
      "dateFinalised": "${json-unit.matches:offsetDateTime}",
      "modelVersion": "1.0.0",
      "author": "Test Bootstrap",
      "organisation": "Oxford BRC",
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper"
      },
      "termPaths": [
        {
          "termPath": "te:Simple Test Terminology|tm:STT01: Simple Test Term 01"
        },
        {
          "termPath": "te:Simple Test Terminology|tm:STT02: Simple Test Term 02"
        }
      ],
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ]
    }
  ],
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
      "name": "CodeSetJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'E34 : test export multiple CodeSets (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginEditor()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [id, getSimpleCodeSetId()]], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "codeSets": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test CodeSet",
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
      "label": "Simple Test CodeSet",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "documentationVersion": "1.0.0",
      "finalised": true,
      "dateFinalised": "${json-unit.matches:offsetDateTime}",
      "modelVersion": "1.0.0",
      "author": "Test Bootstrap",
      "organisation": "Oxford BRC",
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper"
      },
      "termPaths": [
        {
          "termPath": "te:Simple Test Terminology|tm:STT01: Simple Test Term 01"
        },
        {
          "termPath": "te:Simple Test Terminology|tm:STT02: Simple Test Term 02"
        }
      ],
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ]
    }
  ],
  "exportMetadata": {
    "exportedBy": "editor User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
      "name": "CodeSetJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L35 : test import basic CodeSet (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/4.0', [
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

    void 'N35 : test import basic CodeSet (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/4.0', [
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

    void 'R35 : test import basic CodeSet (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/4.0', [
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

    void 'E35A : test import basic CodeSet (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/4.0', [
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
        removeValidIdObjectUsingTransaction(id2)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(id2, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }

    void 'E35B : test import basic CodeSet as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/4.0', [
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
        response.body().items.first().label == 'Functional Test CodeSet'
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
        removeValidIdObjectUsingTransaction(newId)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(newId, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }

    void 'L36 : test import multiple CodeSets (as not logged in)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [getSimpleCodeSetId(), getComplexCodeSetId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0', [
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

    void 'N36 : test import multiple CodeSets (as authenticated/no access)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [getSimpleCodeSetId(), getComplexCodeSetId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0', [
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

    void 'R36 : test import multiple CodeSets (as reader)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [getSimpleCodeSetId(), getComplexCodeSetId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0', [
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

    void 'E36 : test import multiple CodeSets (as editor)'() {
        given:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
             [codeSetIds: [getSimpleCodeSetId(), getComplexCodeSetId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/4.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importAsNewBranchModelVersion  : true, // Needed to import models
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 2

        Object object = response.body().items[0]
        Object object2 = response.body().items[1]
        String id = object.id
        String id2 = object2.id

        object.label == 'Simple Test CodeSet'
        object2.label == 'Complex Test CodeSet'
        object.id != object2.id

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(id2)
        removeValidIdObject(id, NOT_FOUND)
        removeValidIdObject(id2, NOT_FOUND)
    }

    String getExpectedDiffJson() {
        '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Complex Test CodeSet",
  "count": 107,
  "diffs": [
    {
      "label": {
        "left": "Complex Test CodeSet",
        "right": "Simple Test CodeSet"
      }
    },
    {
      "metadata": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "terminology.test.com/simple",
              "key": "mdk2",
              "value": "mdv2"
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "terminology.test.com/simple",
              "key": "mdk1",
              "value": "mdv1"
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "terminology.test.com",
              "key": "mdk2",
              "value": "mdv2"
            }
          }
        ]
      }
    },
    {
      "dateFinalised": {
        "left": "${json-unit.matches:offsetDateTime}",
        "right": "${json-unit.matches:offsetDateTime}"
      }
    },
    {
      "terms": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT75: Complex Test Term 75",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT86: Complex Test Term 86",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT93: Complex Test Term 93",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT9: Complex Test Term 9",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT81: Complex Test Term 81",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT53: Complex Test Term 53",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT87: Complex Test Term 87",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT68: Complex Test Term 68",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT36: Complex Test Term 36",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT51: Complex Test Term 51",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT8: Complex Test Term 8",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT85: Complex Test Term 85",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT29: Complex Test Term 29",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT60: Complex Test Term 60",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT5: Complex Test Term 5",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT76: Complex Test Term 76",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT21: Complex Test Term 21",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT100: Complex Test Term 100",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT78: Complex Test Term 78",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT12: Complex Test Term 12",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT24: Complex Test Term 24",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT38: Complex Test Term 38",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT7: Complex Test Term 7",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT18: Complex Test Term 18",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT49: Complex Test Term 49",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT13: Complex Test Term 13",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT65: Complex Test Term 65",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT32: Complex Test Term 32",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT92: Complex Test Term 92",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT27: Complex Test Term 27",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT47: Complex Test Term 47",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT31: Complex Test Term 31",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT46: Complex Test Term 46",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT57: Complex Test Term 57",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT90: Complex Test Term 90",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT35: Complex Test Term 35",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT72: Complex Test Term 72",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT56: Complex Test Term 56",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT54: Complex Test Term 54",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT61: Complex Test Term 61",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT14: Complex Test Term 14",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT96: Complex Test Term 96",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT63: Complex Test Term 63",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT98: Complex Test Term 98",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT69: Complex Test Term 69",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT77: Complex Test Term 77",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT88: Complex Test Term 88",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT33: Complex Test Term 33",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT99: Complex Test Term 99",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT101",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT95: Complex Test Term 95",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT43: Complex Test Term 43",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT84: Complex Test Term 84",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT45: Complex Test Term 45",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT97: Complex Test Term 97",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT10: Complex Test Term 10",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT79: Complex Test Term 79",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT23: Complex Test Term 23",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT00: Complex Test Term 00",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT89: Complex Test Term 89",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT19: Complex Test Term 19",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT25: Complex Test Term 25",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT62: Complex Test Term 62",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT1: Complex Test Term 1",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT59: Complex Test Term 59",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT17: Complex Test Term 17",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT39: Complex Test Term 39",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT41: Complex Test Term 41",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT91: Complex Test Term 91",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT4: Complex Test Term 4",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT80: Complex Test Term 80",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT50: Complex Test Term 50",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT74: Complex Test Term 74",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT2: Complex Test Term 2",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT3: Complex Test Term 3",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT37: Complex Test Term 37",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT40: Complex Test Term 40",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT83: Complex Test Term 83",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT52: Complex Test Term 52",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT94: Complex Test Term 94",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT22: Complex Test Term 22",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT48: Complex Test Term 48",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT20: Complex Test Term 20",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT6: Complex Test Term 6",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT82: Complex Test Term 82",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT71: Complex Test Term 71",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT28: Complex Test Term 28",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT15: Complex Test Term 15",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT42: Complex Test Term 42",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT64: Complex Test Term 64",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT11: Complex Test Term 11",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT55: Complex Test Term 55",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT67: Complex Test Term 67",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT34: Complex Test Term 34",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT70: Complex Test Term 70",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT44: Complex Test Term 44",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT26: Complex Test Term 26",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT58: Complex Test Term 58",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT73: Complex Test Term 73",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT30: Complex Test Term 30",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT66: Complex Test Term 66",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "CTT16: Complex Test Term 16",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test Terminology",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            }
          }
        ]
      }
    }
  ]
}
'''
    }
}
