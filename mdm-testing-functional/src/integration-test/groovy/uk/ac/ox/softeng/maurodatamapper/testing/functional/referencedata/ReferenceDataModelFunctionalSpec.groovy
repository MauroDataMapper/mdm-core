/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessPermissionChangingAndVersioningFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import net.javacrumbs.jsonunit.core.Option
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: referenceDataModel
 *
 *  |  GET     | /api/referenceDataModels        | Action: index
 *  |  DELETE  | /api/referenceDataModels/${id}  | Action: delete
 *  |  PUT     | /api/referenceDataModels/${id}  | Action: update
 *  |  GET     | /api/referenceDataModels/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/referenceDataModels                 | Action: save
 *
 *  |  DELETE  | /api/referenceDataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/referenceDataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/referenceDataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/referenceDataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *
 *  |  PUT     | /api/referenceDataModels/${dataModelId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/dataModels/${dataModelId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/dataModels/${dataModelId}      | Action: changeFolder
 *  |  PUT     | /api/referenceDataModels/${dataModelId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/referenceDataModels/providers/importers                      | Action: importerProviders
 *  |  GET     | /api/referenceDataModels/providers/exporters                      | Action: exporterProviders
 *  |  GET     | /api/referenceDataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 *
 *  |  POST    | /api/referenceDataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/referenceDataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/referenceDataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/referenceDataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/referenceDataModels/${dataModelId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelController
 */
@Integration
@Slf4j
class ReferenceDataModelFunctionalSpec extends ModelUserAccessPermissionChangingAndVersioningFunctionalSpec {

    @Shared
    Path resourcesPath

    @RunOnce
    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'referencedata').toAbsolutePath()
    }

    @Override
    Expectations getExpectations() {
        super.getExpectations().withoutMergingAvailable()
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    @Transactional
    String getComplexDataModelId() {
        ReferenceDataModel.findByLabel('Second Simple Reference Data Model').id.toString()
    }

    @Transactional
    String getSimpleDataModelId() {
        ReferenceDataModel.findByLabel('Simple Reference Data Model').id.toString()
    }

    @Transactional
    String getLeftHandDiffModelId() {
        ReferenceDataModel.findByLabel('Second Simple Reference Data Model').id.toString()
    }

    @Transactional
    String getRightHandDiffModelId() {
        ReferenceDataModel.findByLabel('Simple Reference Data Model').id.toString()
    }

    @Transactional
    String getModelFolderId(String id) {
        ReferenceDataModel.get(id).folder.id.toString()
    }

    @Override
    String getResourcePath() {
        'referenceDataModels'
    }

    @Override
    String getSavePath() {
        "folders/${getTestFolderId()}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test ReferenceDataModel'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: 'Simple Reference Data Model'
        ]
    }

    @Override
    String getModelType() {
        'ReferenceDataModel'
    }

    @Override
    String getModelUrlType() {
        'referenceDataModels'
    }

    @Override
    String getModelPrefix() {
        'rdm'
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model",
      "path": "rdm:Second Simple Reference Data Model$main",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "domainType": "Classifier",
          "path": "cl:test classifier simple"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model",
      "path": "rdm:Simple Reference Data Model$main",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "domainType": "Classifier",
          "path": "cl:test classifier simple"
        }
      ]
    }
  ]
}
'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ReferenceDataModel",
  "label": "Functional Test ReferenceDataModel",
  "path": "rdm:Functional Test ReferenceDataModel$main",
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "type": "ReferenceDataModel",
  "branchName": "main",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
  }
}'''
    }


    void 'Test getting available ReferenceDataModel exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "ReferenceDataJsonExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON Reference Data Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelExporter",
    "fileExtension": "json",
    "contentType": "application/mauro.referencedatamodel+json",
    "canExportMultipleDomains": false
  },
  {
    "name": "ReferenceDataXmlExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML Reference Data Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelExporter",
    "fileExtension": "xml",
    "contentType": "application/mauro.referencedatamodel+xml",
    "canExportMultipleDomains": false
  }
]'''
    }

    void 'Test getting available ReferenceDataModel importers'() {

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
    "name": "ReferenceDataJsonImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter''' +
                               '''.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "ReferenceDataXmlImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter''' +
                               '''.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "ReferenceDataCsvImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "CSV Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter''' +
                               '''.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  }
]'''

    }


    void 'L34 : test export a single Reference Data Model (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N34 : test export a single Reference Data Model (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R34 : test export a single Reference Data Model (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "referenceDataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test ReferenceDataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "ReferenceDataModel",
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
      "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
      "name": "ReferenceDataJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L35 : test export multiple Reference Data Models (json only exports first id) (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0',
             [referenceDataModelIds: [id, getSimpleDataModelId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N35 : test export multiple Reference Data Models (json only exports first id) (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0',
             [referenceDataModelIds: [id, getSimpleDataModelId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R35 : test export multiple Reference Data Models (json only exports first id) (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0',
             [referenceDataModelIds: [id, getSimpleDataModelId()]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "referenceDataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test ReferenceDataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "ReferenceDataModel",
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
      "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
      "name": "ReferenceDataJsonExporterService",
      "version": "${json-unit.matches:version}"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L36 : test import basic Reference Data Model (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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

    void 'N36 : test import basic Reference Data Model (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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

    void 'R36 : test import basic DataModel (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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

    void 'E36A : test import basic Reference Data Model (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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

   void 'E36B : test import basic Reference Data Model as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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
        response.body().items.first().label == 'Functional Test ReferenceDataModel'
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


    void 'E37: test importing simple test Reference Data Model from CSV as editor and then searching the loaded values'() {
        given:
        loginEditor()

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataCsvImporterService/4.1', [
            finalised                      : true,
            folderId                       : testFolderId.toString(),
            modelName                      : 'FT Test Reference Data Model',
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : 'CSV',
                fileContents: loadTestFile('simpleReferenceData.csv').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedExport.json')), Option.IGNORING_EXTRA_FIELDS

        when:
        GET("${id}/referenceDataValues", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValues.json')), Option.IGNORING_EXTRA_FIELDS

        when:
        GET("${id}/referenceDataValues?max=2", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValuesMax.json')), Option.IGNORING_EXTRA_FIELDS

        when:
        GET("${id}/referenceDataValues?asRows=true", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValuesAsRows.json')), Option.IGNORING_EXTRA_FIELDS

        when:
        GET("${id}/referenceDataValues?asRows=true&max=2", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValuesAsRowsMax.json')), Option.IGNORING_EXTRA_FIELDS

        when:
        GET("${id}/referenceDataValues/search?search=Row6Value2", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedSearchValuesRow6.json')), Option.IGNORING_EXTRA_FIELDS

        when:
        GET("${id}/referenceDataValues/search?search=Row6Value2&asRows=true", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedSearchValuesRow6AsRows.json')), Option.IGNORING_EXTRA_FIELDS

        cleanup:
        logout()
        removeValidIdObjectUsingTransaction(id)
    }

    @Override
    String getExpectedDiffJson() {
        '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Second Simple Reference Data Model",
  "count": 8,
  "diffs": [
    {
      "label": {
        "left": "Second Simple Reference Data Model",
        "right": "Simple Reference Data Model"
      }
    },
    {
      "rules": {
        "created": [
          {
            "value": {
              "id": "${json-unit.matches:id}"
            }
          }
        ]
      }
    },
    {
      "referenceDataTypes": {
        "modified": [
          {
            "leftId": "${json-unit.matches:id}",
            "rightId": "${json-unit.matches:id}",
            "label": "string",
            "leftBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Second Simple Reference Data Model",
                "domainType": "ReferenceDataModel",
                "finalised": false
              }
            ],
            "rightBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Simple Reference Data Model",
                "domainType": "ReferenceDataModel",
                "finalised": false
              }
            ],
            "count": 1,
            "diffs": [
              {
                "rules": {
                  "created": [
                    {
                      "value": {
                        "id": "${json-unit.matches:id}"
                      }
                    }
                  ]
                }
              }
            ]
          }
        ]
      }
    },
    {
      "referenceDataElements": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "Column C",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Second Simple Reference Data Model",
                  "domainType": "ReferenceDataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "Column A",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Second Simple Reference Data Model",
                  "domainType": "ReferenceDataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "Column B",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Second Simple Reference Data Model",
                  "domainType": "ReferenceDataModel",
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
              "label": "Organisation code",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Reference Data Model",
                  "domainType": "ReferenceDataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "Organisation name",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Reference Data Model",
                  "domainType": "ReferenceDataModel",
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