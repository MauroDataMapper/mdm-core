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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.ReferenceDataPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TestMergeData
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelController* Controller: referenceDataModel
 *  | POST   | /api/referenceDataModels       | Action: save   |
 *  | GET    | /api/referenceDataModels       | Action: index  |
 *  | DELETE | /api/referenceDataModels/${id} | Action: delete |
 *  | PUT    | /api/referenceDataModels/${id} | Action: update |
 *  | GET    | /api/referenceDataModels/${id} | Action: show   |
 *
 *  | GET    | /api/referenceDataModels/types                                   | Action: types                   |
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/newVersion               | Action: newVersion              |
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/newBranchModelVersion  | Action: newBranchModelVersion |
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/newDocumentationVersion  | Action: newDocumentationVersion |
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/finalise                  | Action: finalise                 |
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/diff/${otherDataModelId} | Action: diff                    |
 *
 *  | POST   | /api/referenceDataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                | Action: exportReferenceDataModels |
 *  | POST   | /api/referenceDataModels/import/${importerNamespace}/${importerName}/${importerVersion}                | Action: importReferenceDataModels |
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion} | Action: exportReferenceDataModel  |
 *
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/folder/${folderId}  | Action: changeFolder |
 *  | PUT    | /api/folders/${folderId}/referenceDataModels/${referenceDataModelId} | Action: changeFolder |
 *
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/suggestLinks/${otherReferenceDataModelId}" | Action: suggestLinksModel | TODO
 *
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/search | Action: search |
 *  | POST   | /api/referenceDataModels/${referenceDataModelId}/search | Action: search |
 */
@Integration
@Slf4j
class ReferenceDataModelFunctionalSpec extends ResourceFunctionalSpec<ReferenceDataModel> {

    @Shared
    UUID folderId

    @Shared
    UUID movingFolderId

    @Shared
    Path csvResourcesPath

    @Shared
    ReferenceDataPluginMergeBuilder builder

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Reference Data Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Reference Data Functional Test Folder 2', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId
        csvResourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'csv').toAbsolutePath()
        builder = new ReferenceDataPluginMergeBuilder(this)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ReferenceDataModelFunctionalSpec')
        cleanUpResources(Folder, Classifier, ReferenceDataModel, ReferencePrimitiveType, ReferenceDataElement, ReferenceDataValue, ReferenceDataType)
    }

    @Override
    String getResourcePath() {
        'referenceDataModels'
    }

    @Override
    String getSavePath() {
        "folders/${folderId}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Reference Data Functional Test Model'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: null
        ]
    }
    byte[] loadCsvFile(String filename) {
        Path testFilePath = csvResourcesPath.resolve("${filename}.csv")
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    @Override
    String getDeleteEndpoint(String id) {
        "${super.getDeleteEndpoint(id)}?permanent=true"
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "domainType": "ReferenceDataModel",
  "availableActions": ['delete', 'show', 'update'],
  "branchName": "main",
  "finalised": false,
  "label": "Reference Data Functional Test Model",
  "type": "ReferenceDataModel",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "id": "${json-unit.matches:id}",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Test Authority",
    "defaultAuthority": true
  }
}'''
    }

    void 'test getting ReferenceData exporters'() {
        when:
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "ReferenceDataJsonExporterService",
    "version": "4.0",
    "displayName": "JSON Reference Data Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": false
  },
  {
    "name": "ReferenceDataXmlExporterService",
    "version": "5.0",
    "displayName": "XML Reference Data Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "ReferenceDataModelExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": false
  }
]'''
    }

    void 'test getting ReferenceData importers'() {
        when:
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "ReferenceDataJsonImporterService",
    "version": "4.0",
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
    "version": "5.0",
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
    "version": "4.1",
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

    void 'test getting ReferenceData default reference datatype providers'() {
        when:
        GET('providers/defaultReferenceDataTypeProviders', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "ReferenceDataTypeService",
    "version": "1.0.0",
    "displayName": "Basic Default DataTypes",
    "dataTypes": [
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Text",
        "description": "A piece of text"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Number",
        "description": "A whole number"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Decimal",
        "description": "A decimal number"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Date",
        "description": "A date"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "DateTime",
        "description": "A date with a timestamp"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Timestamp",
        "description": "A timestamp"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Boolean",
        "description": "A true or false value"
      },
      {
        "domainType": "ReferencePrimitiveType",
        "label": "Duration",
        "description": "A time period in arbitrary units"
      }
    ]
  }
]'''
    }

    void 'test finalising ReferenceData'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when: 'The ReferenceData is finalised'
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'as expected'
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().finalised
        response.body().dateFinalised

        when: 'List edits for the ReferenceData'
        GET("$id/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is not a CHANGE NOTICE edit'
        !response.body().items.find {
            it.description == 'Functional Test Change Notice'
        }

        cleanup:
        cleanUpData(id)
    }

    void 'test finalising ReferenceData with a change notice'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when: 'The ReferenceData is finalised'
        PUT("$id/finalise", [versionChangeType: 'Major', changeNotice: 'Functional Test Change Notice'])

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'as expected'
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().finalised
        response.body().dateFinalised

        when: 'List edits for the ReferenceData'
        GET("$id/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is a CHANGE NOTICE edit'
        response.body().items.find {
            it.description == 'Functional Test Change Notice'
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

    void 'VF01 : test creating a new fork model of a ReferenceData'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'adding one new model'
        PUT("$id/newForkModel", [label: 'Functional Test ReferenceData reader'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Reference Data Functional Test Model",/, '"label": "Functional Test ReferenceData reader",')


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
      "sourceMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceData reader"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
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
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceData reader"
      },
      "targetModel": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newForkModel", [label: 'Functional Test ReferenceData editor'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Reference Data Functional Test Model",/, '"label": "Functional Test ReferenceData editor",')

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
      "sourceMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceData reader"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      }
    },
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceData editor"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
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
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceData reader"
      },
      "targetModel": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      }
    },
     {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceData editor"
      },
      "targetModel": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'VD01 : test creating a new documentation version of a ReferenceData'() {
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
      "sourceMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
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
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      },
      "targetModel": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
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
        response.body().errors[0].message.contains('cannot have a new version as it has been superseded by [Reference Data Functional Test Model')

        cleanup:
        cleanUpData()
    }

    void 'VB01a : test creating a new main branch model version of a ReferenceData'() {
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
      "sourceMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
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
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      },
      "targetModel": {
        "domainType": "ReferenceDataModel",
        "id": "${json-unit.matches:id}",
        "label": "Reference Data Functional Test Model"
      }
    }
  ]
}'''
        cleanup:
        cleanUpData()
    }

    void 'VB01b : performance test creating a new main branch model version of a small ReferenceData'() {
        given: 'finalised model is created'
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataCsvImporterService/4.1', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            modelName                      : 'FT Test Reference Data Model',
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : 'CSV',
                fileContents: loadCsvFile('simpleCSV').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        when:
        long start = System.currentTimeMillis()
        PUT("$id/newBranchModelVersion", [:])
        long newBranchModelVersionDuration = System.currentTimeMillis() - start
        log.debug('newBranchModelVersion took {}', Utils.getTimeString(newBranchModelVersionDuration))

        then:
        verifyResponse CREATED, response
        newBranchModelVersionDuration < 5000

        cleanup:
        cleanUpData()
    }

    void 'VB01c : performance test creating a new main branch model version of a big ReferenceData'() {
        given: 'finalised model is created'
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataCsvImporterService/4.1', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            modelName                      : 'FT Test Reference Data Model',
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : 'CSV',
                fileContents: loadCsvFile('bigCSV').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        when:
        long start = System.currentTimeMillis()
        PUT("$id/newBranchModelVersion", [:])
        long newBranchModelVersionDuration = System.currentTimeMillis() - start
        log.debug('newBranchModelVersion took {}', Utils.getTimeString(newBranchModelVersionDuration))

        then:
        verifyResponse CREATED, response
        newBranchModelVersionDuration < 5000

        cleanup:
        cleanUpData()
    }

    void 'VB02 : test creating a main branch model version finalising and then creating another main branch of a ReferenceData'() {
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
            it.targetMultiFacetAwareItem.id == id &&
            it.sourceMultiFacetAwareItem.id == secondId
        }
        // This is unconfirmed as its copied
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetMultiFacetAwareItem.id == id &&
            it.sourceMultiFacetAwareItem.id == thirdId &&
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
            it.targetMultiFacetAwareItem.id == id &&
            it.sourceMultiFacetAwareItem.id == secondId
        }
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetMultiFacetAwareItem.id == secondId &&
            it.sourceMultiFacetAwareItem.id == thirdId
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
        responseBody().errors.first().message == 'Property [branchName] of class [class uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel] ' +
        'with value [main] already exists for label [Reference Data Functional Test Model]'

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

    void 'VB05 : test finding common ancestor of two Model<T> (as editor)'() {
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
            it.label == 'Reference Data Functional Test Model' &&
            !(it.id in [id, leftId, rightId])
        }?.id
        mainId

        when: 'check CA between L and R'
        GET("$leftId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Reference Data Functional Test Model'

        when: 'check CA between R and L'
        GET("$rightId/commonAncestor/$leftId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Reference Data Functional Test Model'

        when: 'check CA between L and M'
        GET("$leftId/commonAncestor/$mainId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Reference Data Functional Test Model'

        when: 'check CA between M and L'
        GET("$mainId/commonAncestor/$leftId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Reference Data Functional Test Model'

        when: 'check CA between M and R'
        GET("$mainId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Reference Data Functional Test Model'

        when: 'check CA between R and M'
        GET("$rightId/commonAncestor/$mainId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Reference Data Functional Test Model'

        cleanup:
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(mainId)
        cleanUpData(id)
    }

    void 'VB06 : test finding latest finalised model of a referencedata'() {
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
        responseBody().label == 'Reference Data Functional Test Model'
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == 'Reference Data Functional Test Model'
        responseBody().modelVersion == '2.0.0'

        cleanup:
        cleanUpData(newBranchId)
        cleanUpData(expectedId)
        cleanUpData(latestDraftId)
        cleanUpData(id)
    }

    void 'VB07 : test finding latest model version of a referencedata'() {
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

    void 'MD01 : test finding merge difference of two referencedata'() {
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
        GET("$leftId/mergeDiff/$mainId", STRING_ARG)
        log.debug('{}', jsonResponseBody())
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == leftId

        when:
        GET("$rightId/mergeDiff/$mainId", STRING_ARG)
        log.debug('{}', jsonResponseBody())
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == rightId

        cleanup:
        cleanUpData(mainId)
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(id)
    }

    void 'MD02 : test finding merge difference of two complex referencedata'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())

        when:
        GET("$mergeData.source/mergeDiff/$mergeData.target", STRING_ARG)

        then:
        log.debug('{}', jsonResponseBody())
        verifyJsonResponse OK, expectedMergeDiffJson

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
    }

    void 'MD03 : test finding merge difference of two referencedata with the new style'() {
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
        GET("$leftId/mergeDiff/$mainId", STRING_ARG)
        log.debug('{}', jsonResponseBody())
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == leftId

        when:
        GET("$rightId/mergeDiff/$mainId", STRING_ARG)
        log.debug('{}', jsonResponseBody())
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == rightId

        cleanup:
        cleanUpData(mainId)
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(id)
    }

    void 'MD05 : test finding merge diff with new style diff with aliases gh-112'() {
        given:
        String id = createNewItem(validJson)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'interestingBranch'])
        verifyResponse CREATED, response
        String source = responseBody().id
        PUT(source, [aliases: ['not main branch', 'mergeInto']])
        verifyResponse OK, response

        when:
        GET("$source/mergeDiff/$target", STRING_ARG)
        log.warn('{}', jsonResponseBody())
        GET("$source/mergeDiff/$target")

        then:
        verifyResponse OK, response
        responseBody().targetId == target
        responseBody().sourceId == source
        responseBody().diffs.first().path == 'rdm:Reference Data Functional Test Model$interestingBranch@aliasesString'
        responseBody().diffs.first().sourceValue == 'mergeInto|not main branch'
        responseBody().diffs.first().type == 'modification'

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MD06 : test finding merge diff on a branch which has already been merged'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())
        GET("$mergeData.source/mergeDiff/$mergeData.target")
        verifyResponse OK, response
        List<Map> patches = responseBody().diffs
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])
        verifyResponse OK, response

        when: 'add RDE to existing RDM after a merge'
        POST("$mergeData.sourceMap.referenceDataModelId/referenceDataElements", [label: 'addAnotherLeftToAddLeftOnly', referenceDataType: mergeData.sourceMap.commonReferenceDataTypeId])
        verifyResponse CREATED, response

        log.debug('-------------- Second Merge Request ------------------')

        and:
        GET("$mergeData.source/mergeDiff/$mergeData.target", STRING_ARG)

        then: 'the merge diff is correct'
        verifyJsonResponse OK, expectedSecondMergeDiffJson

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
    }

    void 'MI01 : test merging diff with no patch data'() {
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

    void 'MI02 : test merging diff with URI id not matching body id'() {
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
                                                  targetId: target,
                                                  sourceId: UUID.randomUUID().toString(),
                                                  label   : 'Reference Data Functional Test Model',
                                                  count   : 0,
                                                  diffs   : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Source model id passed in request body does not match source model id in URI.'

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  targetId: UUID.randomUUID().toString(),
                                                  sourceId: source,
                                                  label   : 'Reference Data Functional Test Model',
                                                  count   : 0,
                                                  diffs   : []
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

    void 'MI04 : test merging metadata diff into draft model'() {
        given: 'A ReferenceDataModel is created'
        String id = createNewItem(validJson)

        and: 'Metadata is added to the ReferenceDataModel'
        POST("$id/metadata", [namespace: 'functional.test.namespace', key: 'deleteMetadataSource', value: 'original'])
        verifyResponse CREATED, response
        POST("$id/metadata", [namespace: 'functional.test.namespace', key: 'modifyMetadataSource', value: 'original'])
        verifyResponse CREATED, response

        and: 'A ReferenceDataType is added to the ReferenceDataModel'
        POST("$id/referenceDataTypes", ['label': 'A', 'domainType': 'ReferencePrimitiveType'])
        verifyResponse CREATED, response
        String referenceDataTypeId = response.body().id

        and: 'A ReferenceDataElement is added to the ReferenceDataModel'
        POST("$id/referenceDataElements", ['label': 'modifyLeftOnly', 'referenceDataType': referenceDataTypeId])
        verifyResponse CREATED, response
        String referenceDataElement1Id = response.body().id

        and: 'The ReferenceDataModel is finalised'
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        and: 'A new model version is created'
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        //to modify
        GET("$source/path/rde%3AmodifyLeftOnly")
        verifyResponse OK, response
        String modifyLeftOnly = responseBody().id

        GET("$source/metadata")
        verifyResponse OK, response
        String deleteMetadataSource = responseBody().items.find { it.key == 'deleteMetadataSource' }.id
        String modifyMetadataSource = responseBody().items.find { it.key == 'modifyMetadataSource' }.id

        then:
        //ReferenceDataModel description
        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        //ReferenceDataElement
        PUT("$source/referenceDataElements/$modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response

        //metadata
        DELETE("$source/metadata/$deleteMetadataSource")
        verifyResponse NO_CONTENT, response

        PUT("$source/metadata/$modifyMetadataSource", [value: 'Modified Description'])
        verifyResponse OK, response

        POST("$source/metadata", [namespace: 'functional.test.namespace', key: 'addMetadataSource', value: 'original'])
        verifyResponse CREATED, response
        String addMetadataSource = responseBody().id

        POST("referenceDataElements/$modifyLeftOnly/metadata", [
            namespace: 'functional.test.namespace',
            key      : 'addMetadataModifyLeftOnly',
            value    : 'original'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        String addMetadataModifyLeftOnly = responseBody().id

        //ReferenceDataModel description
        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response

        when:
        // for mergeInto json
        GET("$target/path/rde%3AmodifyLeftOnly")
        verifyResponse OK, response
        modifyLeftOnly = responseBody().id

        GET("$target/metadata")
        verifyResponse OK, response
        deleteMetadataSource = responseBody().items.find {it.key == 'deleteMetadataSource'}.id
        modifyMetadataSource = responseBody().items.find {it.key == 'modifyMetadataSource'}.id

        GET("$source/mergeDiff/$target", STRING_ARG)
        log.info('{}', jsonResponseBody())
        GET("$source/mergeDiff/$target")

        then:
        verifyResponse OK, response
        responseBody().diffs.size() == 6

        when:
        List<Map> patches = responseBody().diffs
        PUT("$source/mergeInto/$target", [
                patch: [
                        targetId: responseBody().targetId,
                        sourceId: responseBody().sourceId,
                        label   : responseBody().label,
                        count   : patches.size(),
                        patches : patches]
        ])

        then:
        verifyResponse OK, response
        responseBody().id == target

        when:
        GET("$target/referenceDataElements")

        then:
        verifyResponse OK, response
        responseBody().items.label as Set == ['modifyLeftOnly'] as Set
        responseBody().items.find { rde -> rde.label == 'modifyLeftOnly' }.description == 'Description'

        when:
        verifyResponse OK, response
        GET("$target/metadata")

        then:
        responseBody().items.key as Set == ['addMetadataSource', 'modifyMetadataSource'] as Set
        responseBody().items.find { metadata -> metadata.key == 'modifyMetadataSource' }.value == 'Modified Description'

        when:
        GET("referenceDataElements/$modifyLeftOnly/metadata", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.key as Set == ['addMetadataModifyLeftOnly'] as Set

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    /**
     * In this test we create a ReferenceDataModel containing one ReferenceDataElement. The ReferenceDataModel is finalised, and a new branch 'source'
     * created. On the source branch, a second ReferenceDataElement is added to the ReferenceDataModel. The source branch is then merged
     * back into main, and we check that the ReferenceDataElement which was created on the source branch is correctly added to the
     * ReferenceDataModel on the main branch.
     */
    void 'MI05 : test merging diff in which a ReferenceDataElement has been created on a ReferenceDataModel'() {
        given: 'A ReferenceDataModel is created'
        String id = createNewItem(validJson)

        and: 'A ReferenceDataType is added to the ReferenceDataModel'
        POST("$id/referenceDataTypes", ['label': 'A', 'domainType': 'ReferencePrimitiveType'])
        String referenceDataTypeId = response.body().id

        when: 'A ReferenceDataElement is added to the ReferenceDataModel'
        POST("$id/referenceDataElements", ['label': 'RDE1', 'referenceDataType': referenceDataTypeId])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        String referenceDataElement1Id = response.body().id

        when: 'The ReferenceDataModel is finalised'
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'A new model version is created'
        PUT("$id/newBranchModelVersion", [branchName: 'main'])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def mainReferenceDataModelId = response.body().id

        when: 'A new model version is created'
        PUT("$id/newBranchModelVersion", [branchName: 'source'])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        String sourceReferenceDataModelId = response.body().id

        when: 'Get the ReferenceDataElements on the source model'
        GET("$sourceReferenceDataModelId/referenceDataElements")

        then: 'The result is OK with one ReferenceDataElement listed'
        verifyResponse OK, response
        response.body().count == 1
        String sourceReferenceDataElementId = response.body().items[0].id

        when: 'A new DataType is added to the source ReferenceDataModel'
        POST("$sourceReferenceDataModelId/referenceDataTypes", ['label': 'B', 'domainType': 'ReferencePrimitiveType'])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        String dataTypeId = response.body().id

        when: 'A new ReferenceDataElement is added to the source ReferenceDataModel'
        POST("$sourceReferenceDataModelId/referenceDataElements", ['label': 'RDE2', 'referenceDataType': dataTypeId])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        String sourceReferenceDataElement2Id = response.body().id

        GET("$sourceReferenceDataModelId/mergeDiff/$mainReferenceDataModelId")

        then:
        verifyResponse OK, response

        when:
        List<Map> patches = responseBody().diffs
        PUT("$sourceReferenceDataModelId/mergeInto/$mainReferenceDataModelId", [
                patch: [
                        targetId: responseBody().targetId,
                        sourceId: responseBody().sourceId,
                        label   : responseBody().label,
                        count   : patches.size(),
                        patches : patches]
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get the ReferenceDataElements on the main branch model'
        GET("$mainReferenceDataModelId/referenceDataElements")

        then: 'The response is OK and there are two ReferenceDataElements'
        verifyResponse OK, response
        response.body().count == 2
        response.body().items.findAll {it.label == 'RDE1'}.size() == 1
        response.body().items.findAll {it.label == 'RDE2'}.size() == 1

        cleanup:
        cleanUpData(sourceReferenceDataModelId)
        cleanUpData(id)
    }

    void 'MI06 : test merging diff with no patch data with new style'() {
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

    void 'MI07 : test merging diff with URI id not matching body id with new style'() {
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
                                                  targetId: target,
                                                  sourceId: UUID.randomUUID().toString(),
                                                  label   : 'Reference Data Functional Test Model',
                                                  count   : 0,
                                                  patches : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Source model id passed in request body does not match source model id in URI.'

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  targetId: UUID.randomUUID().toString(),
                                                  sourceId: source,
                                                  label   : 'Reference Data Functional Test Model',
                                                  count   : 0,
                                                  patches : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Target model id passed in request body does not match target model id in URI.'

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  targetId: target,
                                                  sourceId: source,
                                                  label   : 'Reference Data Functional Test Model',
                                                  count   : 0,
                                                  patches : []
                                              ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().id == target

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MI08 : test merging diff into draft model using new style'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())

        when:
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse OK, response
        responseBody().diffs.size() == 15

        when:
        List<Map> patches = responseBody().diffs
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])

        then:
        verifyResponse OK, response
        responseBody().id == mergeData.target
        responseBody().description == 'DescriptionLeft'

        when:
        GET("$mergeData.target/referenceDataElements")

        then:
        responseBody().items.label as Set == ['modifyAndModifyReturningDifference', 'modifyLeftOnly',
                                              'addAndAddReturningDifference', 'modifyAndDelete', 'addLeftOnly',
                                              'modifyRightOnly', 'addRightOnly', 'modifyAndModifyReturningNoDifference',
                                              'addAndAddReturningNoDifference'] as Set
        responseBody().items.find { rde -> rde.label == 'modifyAndDelete' }.description == 'Description'
        responseBody().items.find { rde -> rde.label == 'addAndAddReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { rde -> rde.label == 'modifyAndModifyReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { rde -> rde.label == 'modifyLeftOnly' }.description == 'Description'

        when:
        GET("$mergeData.target/referenceDataTypes")

        then:
        responseBody().items.label as Set == ['addLeftOnly', 'addRightOnly', 'commonReferenceDataType'] as Set

        when:
        GET("${mergeData.target}/metadata")

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
    }

    void 'MI09 : test merging new style diff with metadata creation gh-111'() {
        given:
        String id = createNewItem(validJson)
        POST("$id/rules", [name: 'Bootstrapped versioning V2Model Rule'])
        verifyResponse(CREATED, response)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'interestingBranch'])
        verifyResponse CREATED, response
        String source = responseBody().id

        POST("$source/metadata", [namespace: 'test.com', key: 'testProperty', value: 'testValue'])
        verifyResponse(CREATED, response)

        String ruleId = builder.getIdFromPath(source, 'rdm:Reference Data Functional Test Model$interestingBranch|ru:Bootstrapped versioning V2Model Rule')
        POST("$source/rules/${ruleId}/representations", [
            language      : 'sql',
            representation: 'testing'
        ])
        verifyResponse(CREATED, response)

        when:
        PUT("$source/mergeInto/$target", [
            changeNotice: 'Metadata test',
            deleteBranch: false,
            patch       : [
                sourceId: source,
                targetId: target,
                count   : 2,
                patches : [
                    [
                        path                                 : 'rdm:Reference Data Functional Test Model$interestingBranch|md:test.com.testProperty',
                        isMergeConflict                      : false,
                        isSourceModificationAndTargetDeletion: false,
                        type                                 : 'creation',
                        branchSelected                       : 'source',
                        branchNameSelected                   : 'interestingBranch'
                    ],
                    [
                        path                                 : 'rdm:Reference Data Functional Test Model$interestingBranch|ru:Bootstrapped versioning V2Model Rule|rr:sql',
                        isMergeConflict                      : false,
                        isSourceModificationAndTargetDeletion: false,
                        type                                 : 'creation',
                        branchSelected                       : 'source',
                        branchNameSelected                   : 'interestingBranch'
                    ]
                ]
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().id == target

        when:
        GET("${target}/metadata")

        then:
        responseBody().items.find { it.namespace == 'test.com' && it.key == 'testProperty' }

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MI10 : test merge into with new style diff with aliases gh-112'() {
        given:
        String id = createNewItem(validJson)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'interestingBranch'])
        verifyResponse CREATED, response
        String source = responseBody().id
        PUT(source, [aliases: ['not main branch', 'mergeInto']])

        when:
        GET("$source/mergeDiff/$target")

        then:
        verifyResponse OK, response

        when:
        List<Map> patches = responseBody().diffs
        PUT("$source/mergeInto/$target", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])

        then:
        verifyResponse OK, response
        responseBody().id == target
        responseBody().aliases.size() == 2
        responseBody().aliases.any { it == 'mergeInto' }
        responseBody().aliases.any { it == 'not main branch' }

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MI11 : test merge into on a branch which has already been merged'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())
        GET("$mergeData.source/mergeDiff/$mergeData.target")
        verifyResponse OK, response
        List<Map> patches = responseBody().diffs
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])
        verifyResponse OK, response

        when: 'get the RDEs on the target after the first merge'
        GET("$mergeData.target/referenceDataElements")

        then: 'there are 9 RDEs'
        verifyResponse(OK, response)
        responseBody().count == 9

        when: 'add RDE after a merge'
        POST("$mergeData.sourceMap.referenceDataModelId/referenceDataElements", [
                label   : 'addAnotherLeftReferenceDataElement',
                referenceDataType: mergeData.sourceMap.commonReferenceDataTypeId
        ])
        verifyResponse CREATED, response
        log.debug('-------------- Second Merge Request ------------------')

        and:
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse OK, response

        when:
        patches = responseBody().diffs
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])

        then:
        verifyResponse OK, response

        when: 'get the RDEs on the target after the second merge'
        GET("$mergeData.target/referenceDataElements")

        then: 'there are now 10 RDEs'
        verifyResponse(OK, response)
        responseBody().count == 10

        when:
        String addLeftOnly = responseBody().items.find { it.label == 'addAnotherLeftReferenceDataElement' }.id
        GET("$mergeData.targetMap.referenceDataModelId/referenceDataElements/$addLeftOnly")

        then:
        verifyResponse(OK, response)
        responseBody().label == 'addAnotherLeftReferenceDataElement'

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
    }

    @PendingFeature(reason = 'Not yet implemented')
    void 'test changing folder from ReferenceData context'() {
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

    @PendingFeature(reason = 'Not yet implemented')
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

    void 'test diffing 2 ReferenceData'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)
        String otherId = createNewItem([label: 'Reference Data Functional Test Model 2'])

        when:
        GET("${id}/diff/${otherId}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "count": 1,
  "label": "Reference Data Functional Test Model",
  "diffs": [
    {
      "label": {
        "left": "Reference Data Functional Test Model",
        "right": "Reference Data Functional Test Model 2"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test export a single ReferenceData'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "referenceDataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Reference Data Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "ReferenceDataModel",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Test Authority"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
      "name": "ReferenceDataJsonExporterService",
      "version": "4.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test export multiple ReferenceData (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0',
             [referenceDataModelIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "referenceDataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Reference Data Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "ReferenceDataModel",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Test Authority"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
      "name": "ReferenceDataJsonExporterService",
      "version": "4.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'test import basic ReferenceData'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Functional Test Import",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "1.0.0"
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test import basic ReferenceData as new documentation version'() {
        given:
        String id = createNewItem([
            label       : 'Functional Test Model',
            finalised   : true,
            modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
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
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Functional Test Model",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "2.0.0",
      "modelVersion": "1.0.0"
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'DA : test delete multiple models'() {
        given:
        def idstoDelete = []
        (1..4).each {n ->
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
      "domainType": "ReferenceDataModel",
      "label": "${json-unit.matches:id}",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "${json-unit.matches:id}",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "${json-unit.matches:id}",
      "type": "ReferenceDataModel",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "${json-unit.matches:id}",
      "type": "ReferenceDataModel",
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

    void 'test importing simple test ReferenceData from CSV'() {
        given:
        log.debug("${loadCsvFile('simpleCSV').toList().toString()}")

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataCsvImporterService/4.1', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            modelName                      : 'FT Test Reference Data Model',
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : 'CSV',
                fileContents: loadCsvFile('simpleCSV').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'test importing ReferenceData with classifiers'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('importSimpleWithClassifiers').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'test export ReferenceData'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/4.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('importSimpleValue').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('importSimpleValue'))
            .replaceFirst('"exportedBy": "Admin User",', '"exportedBy": "Unlogged User",')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter/ReferenceDataJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'Not yet implemented')
    void 'test diffing 2 complex and simple ReferenceData'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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

        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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
                "count": 21,
                "label": "Complex Test DataModel",
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
                          "namespace": "test.com/test",
                          "id": "${json-unit.matches:id}",
                          "value": "mdv2",
                          "key": "mdk1"
                        },
                        {
                          "namespace": "test.com",
                          "id": "${json-unit.matches:id}",
                          "value": "mdv1",
                          "key": "mdk1"
                        }
                      ],
                      "created": [
                        {
                          "namespace": "test.com/simple",
                          "id": "${json-unit.matches:id}",
                          "value": "mdv2",
                          "key": "mdk2"
                        },
                        {
                          "namespace": "test.com/simple",
                          "id": "${json-unit.matches:id}",
                          "value": "mdv1",
                          "key": "mdk1"
                        }
                      ]
                    }
                  },
                  {
                    "annotations": {
                      "deleted": [
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "test annotation 2"
                        },
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "test annotation 1"
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
                        {
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
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "yesnounknown",
                          "breadcrumbs": [
                            {
                              "domainType": "DataModel",
                              "finalised": false,
                              "id": "${json-unit.matches:id}",
                              "label": "Complex Test DataModel"
                            }
                          ]
                        },
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "child",
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
                    }
                  },
                  {
                    "dataClasses": {
                      "deleted": [
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "parent",
                          "breadcrumbs": [
                            {
                              "domainType": "DataModel",
                              "finalised": false,
                              "id": "${json-unit.matches:id}",
                              "label": "Complex Test DataModel"
                            }
                          ]
                        },
                        {
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
                        },
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "emptyclass",
                          "breadcrumbs": [
                            {
                              "domainType": "DataModel",
                              "finalised": false,
                              "id": "${json-unit.matches:id}",
                              "label": "Complex Test DataModel"
                            }
                          ]
                        },
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "content",
                          "breadcrumbs": [
                            {
                              "domainType": "DataModel",
                              "finalised": false,
                              "id": "${json-unit.matches:id}",
                              "label": "Complex Test DataModel"
                            }
                          ]
                        }
                      ],
                      "created": [
                        {
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
                      ]
                    }
                  },
                  {
                    "dataElements": {
                      "deleted": [
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "ele1",
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
                        {
                          "id": "${json-unit.matches:id}",
                          "label": "element2",
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
                        {
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
                      ]
                    }
                  }
                ]
              }'''

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    @PendingFeature(reason = 'Not yet implemented')
    void 'test searching for label "emptyclass" in complex model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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

    @PendingFeature(reason = 'Not yet implemented')
    void 'test searching for label "emptyclass" in simple model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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
    @PendingFeature(reason = 'Not yet implemented')
    void setupForLinkSuggestions(String simpleDataModelId) {
        //DataClass dataClass = DataClass.byDataModelId(Utils.toUuid(simpleDataModelId)).eq('label', 'simple').find()
        //assert dataClass

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

    @PendingFeature(reason = 'Not yet implemented')
    void 'test get link suggestions for a model with no data elements in the target'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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

        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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

    @PendingFeature(reason = 'Not yet implemented')
    void 'test get link suggestions for a model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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

        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataJsonImporterService/2.0', [
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


    void 'T01 test listing and searching Reference Data Values'() {
        given:
        log.debug("${loadCsvFile('simpleCSV').toList().toString()}")

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer/ReferenceDataCsvImporterService/4.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            modelName                      : 'FT Test Reference Data Model',
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : 'CSV',
                fileContents: loadCsvFile('simpleCSV').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        when:
        GET("${id}/referenceDataValues", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValues'))

        when:
        GET("${id}/referenceDataValues?max=2", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValuesMax'))

        when:
        GET("${id}/referenceDataValues?asRows=true", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValuesAsRows'))

        when:
        GET("${id}/referenceDataValues?asRows=true&max=2", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedGetValuesAsRowsMax'))

        when:
        GET("${id}/referenceDataValues/search?search=Row6Value2", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedSearchValuesRow6'))

        when:
        GET("${id}/referenceDataValues/search?search=Row6Value2&asRows=true", STRING_ARG)

        then:
        verifyJsonResponse OK, new String(loadTestFile('expectedSearchValuesRow6AsRows'))

        when:
        GET("${id}/referenceDataValues/search?asRows=true&max=10&offset=0", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse

        when:
        POST("${id}/referenceDataValues/search?asRows=true&max=10&offset=0", [:])

        then:
        verifyResponse OK, response

        cleanup:
        cleanUpData(id)
    }

    String getExpectedMergeDiffJson() {
        '''{
  "sourceId": "${json-unit.matches:id}",
  "targetId": "${json-unit.matches:id}",
  "path": "rdm:Functional Test ReferenceData 1$source",
  "label": "Functional Test ReferenceData 1",
  "count": 15,
  "diffs": [
    {
      "fieldName": "description",
      "path": "rdm:Functional Test ReferenceData 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "rdm:Functional Test ReferenceData 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|rde:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|rde:modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|rde:deleteAndModify",
      "isMergeConflict": true,
      "isSourceDeletionAndTargetModification": true,
      "type": "deletion"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|rde:deleteLeftOnly",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "rdm:Functional Test ReferenceData 1$source|rde:addAndAddReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "referenceDataTypePath",
      "path": "rdm:Functional Test ReferenceData 1$source|rde:addAndAddReturningDifference@referenceDataTypePath",
      "sourceValue": "rdm:Functional Test ReferenceData 1$source|rdt:addLeftOnly",
      "targetValue": "rdm:Functional Test ReferenceData 1$main|rdt:addRightOnly",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "referenceDataTypePath",
      "path": "rdm:Functional Test ReferenceData 1$source|rde:addAndAddReturningNoDifference@referenceDataTypePath",
      "sourceValue": "rdm:Functional Test ReferenceData 1$source|rdt:addLeftOnly",
      "targetValue": "rdm:Functional Test ReferenceData 1$main|rdt:addRightOnly",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "rdm:Functional Test ReferenceData 1$source|rde:modifyAndModifyReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "rdm:Functional Test ReferenceData 1$source|rde:modifyLeftOnly@description",
      "sourceValue": "Description",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "rdm:Functional Test ReferenceData 1$source|rdt:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    }
  ]
}
'''
    }

    String getExpectedSecondMergeDiffJson() {
        '''{
  "sourceId": "${json-unit.matches:id}",
  "targetId": "${json-unit.matches:id}",
  "path": "rdm:Functional Test ReferenceData 1$source",
  "label": "Functional Test ReferenceData 1",
  "count": 1,
  "diffs": [
    {
      "path": "rdm:Functional Test ReferenceData 1$source|rde:addAnotherLeftToAddLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    }
  ]
}'''
    }
}