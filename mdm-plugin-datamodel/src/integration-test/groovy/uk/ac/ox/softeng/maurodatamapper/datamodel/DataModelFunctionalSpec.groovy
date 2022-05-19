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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExport
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.DataModelPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TestMergeData
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

import java.util.concurrent.CancellationException
import java.util.concurrent.Future

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.ACCEPTED
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
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
 *  | GET    | /api/dataModels/${dataModelId}/diff/${otherModelId} | Action: diff                    |
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
class DataModelFunctionalSpec extends ResourceFunctionalSpec<DataModel> implements XmlComparer {

    AsyncJobService asyncJobService

    @Shared
    UUID folderId

    @Shared
    UUID versionedFolderId

    @Shared
    UUID otherVersionedFolderId

    @Shared
    UUID movingFolderId

    @Shared
    DataModelPluginMergeBuilder builder

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Functional Test Folder 2', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId

        versionedFolderId = new VersionedFolder(label: 'Functional Test VersionedFolder', createdBy: FUNCTIONAL_TEST, authority: testAuthority).save(flush: true).id
        assert versionedFolderId
        otherVersionedFolderId = new VersionedFolder(label: 'Functional Test VersionedFolder 2', createdBy: FUNCTIONAL_TEST, authority: testAuthority).save(flush: true).id
        assert otherVersionedFolderId
        builder = new DataModelPluginMergeBuilder(this)
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
    "label": "Test Authority",
    "defaultAuthority": true
  }
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
    "canExportMultipleDomains": true,
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
    "canExportMultipleDomains": true,
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
    "canImportMultipleDomains": true,
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
            it.description == 'Functional Test Change Notice'
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
            it.title == 'CHANGENOTICE' && it.description == 'Functional Test Change Notice'
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
      "sourceMultiFacetAwareItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetMultiFacetAwareItem": {
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
      "sourceMultiFacetAwareItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetMultiFacetAwareItem": {
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
      "sourceMultiFacetAwareItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel editor"
      },
      "targetMultiFacetAwareItem": {
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
      "sourceMultiFacetAwareItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
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
      "sourceMultiFacetAwareItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
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

    @Requires({
        // Only run on jenkins
        env.containsKey('JENKINS')
    })
    void 'VB01b : performance test creating a new main branch model version of a simple DataModel'() {
        given: 'finalised model is created'
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

    @Requires({
        // Only run on jenkins
        env.containsKey('JENKINS')
    })
    void 'VB01c : performance test creating a new main branch model version of a complex DataModel'() {
        given: 'finalised model is created'
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

    void 'VB05 : test creating a main branch model version of a DataModel with ordered DataClasses, DataElements and EnumerationValues'() {
        given:
        String originalModelId = createNewItem(validJson)

        for (int i in 1..5) {
            POST("$originalModelId/dataClasses", [label: 'Root Data Class ' + i, idx: i - 1])
            verifyResponse CREATED, response
        }
        String originalParentClassId = responseBody().id

        for (int i in 1..5) {
            POST("$originalModelId/dataClasses/$originalParentClassId/dataClasses", [label: 'Child Data Class ' + i, idx: i])
            verifyResponse CREATED, response
        }

        Map enumerationTypeData = [
            label            : 'Functional Test Enumeration Type',
            domainType       : 'EnumerationType',
            enumerationValues: (1..5).collect {i ->
                [
                    key  : 'Key ' + i,
                    value: 'Value ' + i,
                    index: i - 1
                ]
            }
        ]
        POST("$originalModelId/dataTypes", enumerationTypeData)
        verifyResponse CREATED, response
        String originalEnumerationTypeId = responseBody().id

        for (int i in 1..5) {
            POST("$originalModelId/dataClasses/$originalParentClassId/dataElements",
                 [label: 'Data Element ' + i, dataType: originalEnumerationTypeId, idx: i - 1])
            verifyResponse CREATED, response
        }

        when:
        GET("$originalModelId/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Root Data Class') && dc.label.endsWith((i + 1).toString())
        }

        when:
        GET("$originalModelId/dataClasses/$originalParentClassId/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Child Data Class') && dc.label.endsWith((i + 1).toString())
        }

        when:
        GET("$originalModelId/dataClasses/$originalParentClassId/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.eachWithIndex {de, i ->
            assert de.domainType == 'DataElement'
            assert de.label.startsWith('Data Element') && de.label.endsWith((i + 1).toString())
        }

        when:
        GET("$originalModelId/dataTypes/$originalEnumerationTypeId")

        then:
        verifyResponse OK, response
        responseBody().domainType == 'EnumerationType'
        responseBody().enumerationValues.every {ev ->
            ev.key.startsWith('Key') && ev.key.endsWith((ev.index + 1).toString()) &&
            ev.value.startsWith('Value') && ev.value.endsWith((ev.index + 1).toString())
        }

        when:
        PUT("$originalModelId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$originalModelId/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response
        String newModelId = responseBody().id

        when:
        GET("$newModelId/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Root Data Class') && dc.label.endsWith((i + 1).toString())
        }
        String newParentClassId = responseBody().items.last().id

        when:
        GET("$newModelId/dataClasses/$newParentClassId/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Child Data Class') && dc.label.endsWith((i + 1).toString())
        }

        when:
        GET("$newModelId/dataClasses/$newParentClassId/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.eachWithIndex {de, i ->
            assert de.domainType == 'DataElement'
            assert de.label.startsWith('Data Element') && de.label.endsWith((i + 1).toString())
        }

        when:
        GET("$newModelId/dataTypes")
        verifyResponse OK, response
        String newEnumerationTypeId = responseBody().items[0].id
        GET("$newModelId/dataTypes/$newEnumerationTypeId")

        then:
        verifyResponse OK, response
        responseBody().domainType == 'EnumerationType'
        responseBody().enumerationValues.every {ev ->
            ev.key.startsWith('Key') && ev.key.endsWith((ev.index + 1).toString()) &&
            ev.value.startsWith('Value') && ev.value.endsWith((ev.index + 1).toString())
        }

        cleanup:
        cleanUpData(originalModelId)
        cleanUpData(newModelId)
    }

    void 'VB06 : test finding common ancestor of two datamodels'() {
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

    void 'VB07 : test finding latest finalised model of a datamodel'() {
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

    void 'VB08 : test finding latest model version of a datamodel'() {
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


    void waitForAysncToComplete(String id) {
        log.debug('Waiting to complete {}', id)
        Future p = asyncJobService.getAsyncJobFuture(id)
        try {
            p.get()
        } catch (CancellationException ignored) {
        }
        log.debug('Completed')
    }


    void 'VB09 : test async creating a new main branch model version of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        PUT("$id/newBranchModelVersion", [asynchronous: true])

        then:
        verifyResponse(ACCEPTED, response)
        responseBody().id

        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        !responseBody().message

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
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
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

    void 'VB10 : test async creating a main branch model version when one already exists'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'another main branch created'
        PUT("$id/newBranchModelVersion", [asynchronous: true])

        then:
        verifyResponse(ACCEPTED, response)


        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'FAILED'
        responseBody().message == '''Invalid model
  Property [branchName] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel] with value [main] already exists for label [Functional Test Model]'''

        cleanup:
        cleanUpData()
    }

    void 'VB11 : test async creating a main branch model version and cancelling it'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'another main branch created'
        PUT("$id/newBranchModelVersion", [asynchronous: true])

        then:
        verifyResponse(ACCEPTED, response)

        when:
        String jobId = responseBody().id
        DELETE("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'CANCELLED'

        when:
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'CANCELLED'

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 0,
  "items": [ ]
}'''

        when:
        GET("$id/availableBranches", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 0,
  "items": [
    
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'VB12 : test async creating a new non-main branch model version of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        PUT("$id/newBranchModelVersion", [branchName: 'async-test', asynchronous: true])

        then:
        verifyResponse(ACCEPTED, response)
        responseBody().id

        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        !responseBody().message

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "linkType": "Refines",
      "domainType": "SemanticLink",
      "unconfirmed": false,
      "sourceMultiFacetAwareItem": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "linkType": "Refines",
      "domainType": "SemanticLink",
      "unconfirmed": false,
      "sourceMultiFacetAwareItem": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
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
      "id": "${json-unit.matches:id}",
      "linkType": "New Model Version Of",
      "domainType": "VersionLink",
      "sourceModel": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "linkType": "New Model Version Of",
      "domainType": "VersionLink",
      "sourceModel": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/availableBranches", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Functional Test Model",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Functional Test Model",
      "type": "Data Standard",
      "branchName": "async-test",
      "documentationVersion": "1.0.0"
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'MD01 : test finding merge difference of two datamodels'() {
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
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == leftId

        when:
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

    void 'MD02 : test finding merge difference of two datamodels with the new style'() {
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
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == leftId

        when:
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

    void 'MD03 : test finding merge difference of two complex datamodels with the new style'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())

        when:
        GET("$mergeData.source/mergeDiff/$mergeData.target", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedMergeDiffJson

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
    }

    void 'MD04 : test finding merge diff with new style diff with aliases gh-112'() {
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
        GET("$source/mergeDiff/$target")

        then:
        verifyResponse OK, response
        responseBody().targetId == target
        responseBody().sourceId == source
        responseBody().diffs.first().path == 'dm:Functional Test Model$interestingBranch@aliasesString'
        responseBody().diffs.first().sourceValue == 'mergeInto|not main branch'
        responseBody().diffs.first().type == 'modification'

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MD05 : test finding merge diff on a branch which has already been merged'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())
        log.debug('-------------- First Merge Diff ------------------')
        GET("$mergeData.source/mergeDiff/$mergeData.target")
        verifyResponse OK, response
        List<Map> patches = responseBody().diffs
        log.debug('-------------- First Merge Into ------------------')
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])
        verifyResponse OK, response

        when: 'add DE and DC to existing class after a merge'
        POST("$mergeData.sourceMap.dataModelId/dataClasses/$mergeData.sourceMap.addLeftOnly/dataClasses", [label: 'addAnotherLeftToAddLeftOnly'])
        verifyResponse CREATED, response
        POST("$mergeData.sourceMap.dataModelId/dataClasses/$mergeData.sourceMap.addLeftToExistingClass/dataElements", [
            label   : 'addAnotherLeftToExistingClass',
            dataType: mergeData.sourceMap.addLeftOnlyDataType
        ])
        verifyResponse CREATED, response

        and:
        log.debug('-------------- Second Merge Diff ------------------')
        // See comments in DataElementService.handlesModificationPatchOfField for explanation of why we get a merge conflict on dataTypePath
        GET("$mergeData.source/mergeDiff/$mergeData.target", STRING_ARG)

        then:
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
                                                  label   : "Functional Test Model",
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
                                                  label   : "Functional Test Model",
                                                  count   : 0,
                                                  patches : []
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

    void 'MI03 : test merging metadata diff into draft model'() {
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
        GET("$source/path/dc%3AmodifyLeftOnly")
        verifyResponse OK, response
        String modifyLeftOnly = responseBody().id

        GET("$source/metadata")
        verifyResponse OK, response
        String deleteMetadataSource = responseBody().items.find {it.key == 'deleteMetadataSource'}.id
        String modifyMetadataSource = responseBody().items.find {it.key == 'modifyMetadataSource'}.id

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
        GET("$target/path/dc%3AmodifyLeftOnly")
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
        GET("dataClasses/$modifyLeftOnly/metadata", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.key as Set == ['addMetadataModifyLeftOnly'] as Set

        when:
        GET("$target/metadata")

        then:
        responseBody().items.key as Set == ['addMetadataSource', 'modifyMetadataSource'] as Set
        responseBody().items.find {metadata -> metadata.key == 'modifyMetadataSource'}.value == 'Modified Description'


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
    void 'MI04 : test merging diff in which a DataElement has been created on a DataClass - failing test for MC-9433'() {
        given: 'A DataModel is created'
        String id = createNewItem(validJson)

        when: 'A DataClass is added to the DataModel'
        POST("$id/dataClasses", ['label': "Functional Test DataClass"])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def dataClassId = response.body().id

        when: 'The DataModel is finalised'
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'A new model version is created'
        PUT("$id/newBranchModelVersion", [branchName: 'main'])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def mainDataModelId = response.body().id

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
        POST("$sourceDataModelId/dataTypes", ['label': 'A', 'domainType': 'PrimitiveType'])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def dataTypeId = response.body().id

        when: 'A new DataElement is added to the source DataClass'
        POST("$sourceDataModelId/dataClasses/$sourceDataClassId/dataElements", ['label': "New Data Element", 'dataType': ['id': dataTypeId]])

        then: 'The response is CREATED'
        verifyResponse CREATED, response
        def sourceDataElementId = response.body().id

        GET("$sourceDataModelId/mergeDiff/$mainDataModelId")

        then:
        verifyResponse OK, response

        when:
        List<Map> patches = responseBody().diffs
        PUT("$sourceDataModelId/mergeInto/$mainDataModelId", [
            patch: [
                targetId: responseBody().targetId,
                sourceId: responseBody().sourceId,
                label   : responseBody().label,
                count   : patches.size(),
                patches : patches]
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get the DataElements on the main branch model'
        String dcId = builder.getIdFromPath(mainDataModelId, 'dm:Functional Test Model$main|dc:Functional Test DataClass')
        GET("$mainDataModelId/dataClasses/$dcId/dataElements")

        then: 'The response is OK and there is one DataElement'
        verifyResponse OK, response
        response.body().count == 1
        response.body().items[0].label == 'New Data Element'

        cleanup:
        cleanUpData(sourceDataModelId)
        cleanUpData(mainDataModelId)
        cleanUpData(id)
    }

    void 'MI05 : test merging diff with no patch data with new style'() {
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

    void 'MI06 : test merging diff with URI id not matching body id with new style'() {
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
                                                  label   : "Functional Test Model",
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
                                                  label   : "Functional Test Model",
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
                                                  label   : "Functional Test Model",
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

    void 'MI07 : test merging diff into draft model using new style'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(folderId.toString())

        when:
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse OK, response
        responseBody().diffs.size() == 17

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
        GET("$mergeData.target/dataClasses")

        then:
        responseBody().items.label as Set == ['existingClass', 'modifyAndModifyReturningDifference', 'modifyLeftOnly',
                                              'addAndAddReturningDifference', 'modifyAndDelete', 'addLeftOnly',
                                              'modifyRightOnly', 'addRightOnly', 'modifyAndModifyReturningNoDifference',
                                              'addAndAddReturningNoDifference'] as Set
        responseBody().items.find {dataClass -> dataClass.label == 'modifyAndDelete'}.description == 'Description'
        responseBody().items.find {dataClass -> dataClass.label == 'addAndAddReturningDifference'}.description == 'DescriptionLeft'
        responseBody().items.find {dataClass -> dataClass.label == 'modifyAndModifyReturningDifference'}.description == 'DescriptionLeft'
        responseBody().items.find {dataClass -> dataClass.label == 'modifyLeftOnly'}.description == 'Description'

        when:
        GET("$mergeData.target/dataClasses/$mergeData.targetMap.existingClass/dataClasses")

        then:
        responseBody().items.label as Set == ['addRightToExistingClass', 'addLeftToExistingClass'] as Set

        when:
        GET("$mergeData.target/dataClasses/$mergeData.targetMap.existingClass/dataElements")

        then:
        responseBody().items.label as Set == ['addLeftOnly', 'existingDataElement'] as Set

        when:
        GET("$mergeData.target/dataTypes")

        then:
        responseBody().items.label as Set == ['addLeftOnly', 'existingDataType1', 'existingDataType2'] as Set

        when:
        GET("${mergeData.target}/metadata")

        then:
        responseBody().items.find {it.namespace == 'functional.test' && it.key == 'modifyOnSource'}.value == 'source has modified this'
        responseBody().items.find {it.namespace == 'functional.test' && it.key == 'modifyAndDelete'}.value == 'source has modified this also'
        !responseBody().items.find {it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource'}
        responseBody().items.find {it.namespace == 'functional.test' && it.key == 'addToSourceOnly'}

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
    }

    void 'MI08 : test merging diff with metadata creation gh-111'() {
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

        String ruleId = builder.getIdFromPath(source, 'dm:Functional Test Model$interestingBranch|ru:Bootstrapped versioning V2Model Rule')
        POST("$source/rules/${ruleId}/representations", [
            language      : 'sql',
            representation: 'testing'
        ])
        verifyResponse(CREATED, response)

        when:
        PUT("$source/mergeInto/$target", [
            changeNotice: "Metadata test",
            deleteBranch: false,
            patch       : [
                sourceId: source,
                targetId: target,
                count   : 2,
                patches : [
                    [
                        path                                 : 'dm:Functional Test Model$interestingBranch|md:test.com.testProperty',
                        isMergeConflict                      : false,
                        isSourceModificationAndTargetDeletion: false,
                        type                                 : 'creation',
                        branchSelected                       : 'source',
                        branchNameSelected                   : 'interestingBranch'
                    ],
                    [

                        path                                 : 'dm:Functional Test Model$interestingBranch|ru:Bootstrapped versioning V2Model Rule|rr:sql',
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
        responseBody().items.find {it.namespace == 'test.com' && it.key == 'testProperty'}

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MI09 : test merge into with new style diff with aliases gh-112'() {
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
        responseBody().aliases.any {it == 'mergeInto'}
        responseBody().aliases.any {it == 'not main branch'}

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'MI10 : test merge into on a branch which has already been merged'() {
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

        when: 'add DE and DC to existing class after a merge'
        POST("$mergeData.sourceMap.dataModelId/dataClasses/$mergeData.sourceMap.addLeftOnly/dataClasses", [label: 'addAnotherLeftToAddLeftOnly'])
        verifyResponse CREATED, response
        POST("$mergeData.sourceMap.dataModelId/dataClasses/$mergeData.sourceMap.addLeftToExistingClass/dataElements", [
            label   : 'addAnotherLeftToExistingClass',
            dataType: mergeData.sourceMap.addLeftOnlyDataType
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

        when:
        GET("$mergeData.target/dataClasses/$mergeData.targetMap.existingClass/dataElements")

        then:
        verifyResponse(OK, response)
        responseBody().count == 2

        when:
        GET("$mergeData.target/dataClasses")

        then:
        verifyResponse(OK, response)

        when:
        String addLeftOnly = responseBody().items.find {it.label == 'addLeftOnly'}.id
        GET("$mergeData.targetMap.dataModelId/dataClasses/$addLeftOnly/dataClasses")

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'addAnotherLeftToAddLeftOnly'}

        cleanup:
        cleanUpData(mergeData.source)
        cleanUpData(mergeData.target)
        cleanUpData(mergeData.commonAncestor)
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
        // just grab the raw json for visual checking
        GET("$testId/diff/$mainId", STRING_ARG)
        log.debug('{}', jsonResponseBody())

        and:
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
        parentId = responseBody().items.find {it.label == 'parent'}.id
        String contentId = responseBody().items.find {it.label == 'content'}.id
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
        // just grab the raw json for visual checking
        GET("$testId/diff/$mainId", STRING_ARG)
        log.debug('{}', jsonResponseBody())

        and:
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
        Map contentDiff = dataClassesDiffs.modified.find {it.label == 'content'}
        contentDiff.diffs.size() == 1
        contentDiff.diffs.first().description.left == 'a change to the description'
        contentDiff.diffs.first().description.right == 'some interesting content'

        and:
        Map parentDiff = dataClassesDiffs.modified.find {it.label == 'parent'}
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
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)

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
      "label": "Test Authority"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "3.1"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'E02A : test export simple DataModel JSON'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])
        String expected = new String(loadTestFile('simpleDataModel')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'E02B : test export simple DataModel XML'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/5.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.XML.name,
                fileContents: loadTestFile('simpleDataModel', 'xml').toList()
            ]
        ])
        String expected = new String(loadTestFile('simpleDataModel', 'xml')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        when:
        HttpResponse<String> xmlResponse =
            GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.1", STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        compareXml(expected, xmlResponse.body())

        cleanup:
        cleanUpData(id)
    }

    void 'E03A : test export complex DataModel JSON'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        String expected = new String(loadTestFile('complexDataModel')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'E03B : test export complex DataModel XML'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/5.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.XML.name,
                fileContents: loadTestFile('complexDataModel', 'xml').toList()
            ]
        ])
        String expected = new String(loadTestFile('complexDataModel', 'xml')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        when:
        HttpResponse<String> xmlResponse =
            GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.1", STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        compareXml(expected, xmlResponse.body())

        cleanup:
        cleanUpData(id)
    }

    void 'E04 : test export multiple DataModels'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
             [dataModelIds: [id, id2]], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "dataModels": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Model",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "type": "Data Standard",
      "documentationVersion": "1.0.0",
      "finalised": false,
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Test Authority"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Model 2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "type": "Data Standard",
      "documentationVersion": "1.0.0",
      "finalised": false,
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Test Authority"
      }
    }
  ],
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "3.1"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'E04A : test export multiple DataModels JSON'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleAndComplexDataModels').toList()
            ]
        ])
        String expected = new String(loadTestFile('simpleAndComplexDataModels')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String id2 = response.body().items[1].id

        and:
        id
        id2

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
             [dataModelIds: [id, id2]], STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'E04B : test export multiple DataModels XML'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/5.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.XML.name,
                fileContents: loadTestFile('simpleAndComplexDataModels', 'xml').toList()
            ]
        ])
        String expected = new String(loadTestFile('simpleAndComplexDataModels', 'xml')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String id2 = response.body().items[1].id

        and:
        id
        id2

        when:
        HttpResponse<String> xmlResponse = POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.1',
                                                [dataModelIds: [id, id2]], STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        compareXml(expected, xmlResponse.body())

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    @Transactional
    void cleanupDomainExport(String id) {
        DomainExport.get(id).delete(flush: true)
    }


    void 'E05 : test export complex DataModel JSON async'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])
        String expected = new String(loadTestFile('complexDataModel')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1?asynchronous=true")

        then:
        verifyResponse ACCEPTED, response

        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        responseBody().message ==~ /Download at ${baseUrl}domainExport\/.+?\/download/

        when:
        GET('domainExport', STRING_ARG, true)

        then:
        verifyJsonResponse(OK, '''{
  "count": 1,
  "items": [
    {
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
      "exportedBy": "unlogged_user@mdm-core.com",
      "links": {
        "relative": "${json-unit.regex}/api/domainExport/[\\\\w-]+?/download",
        "absolute": "${json-unit.regex}http://localhost:\\\\d+/api/domainExport/.+?/download"
      }
    }
  ]
}''')

        when:
        GET('domainExport', MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when:
        String deId = responseBody().items.first().id
        GET(responseBody().items.first().links.absolute, STRING_ARG, true)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanupDomainExport(deId)
        cleanUpData(id)
    }

    void 'E06 : test export complex DataModel JSON async twice'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1?asynchronous=true")

        then:
        verifyResponse ACCEPTED, response

        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        responseBody().message ==~ /Download at ${baseUrl}domainExport\/.+?\/download/

        when:
        GET('domainExport', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1


        when: 'export again'
        String deId = responseBody().items.first().id
        String firstExportDateTime = responseBody().items.first().exportedOn
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1?asynchronous=true")

        then:
        verifyResponse ACCEPTED, response
        responseBody().id != jobId

        when:
        jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        responseBody().message ==~ /Download at ${baseUrl}domainExport\/.+?\/download/

        when:
        GET('domainExport', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {it.id == deId && it.exportedOn == firstExportDateTime}
        responseBody().items.any {it.id != deId && it.exportedOn != firstExportDateTime}

        cleanup:
        cleanupDomainExport(deId)
        cleanUpData(id)
    }


    void 'E07 : test export multiple DataModels JSON async'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleAndComplexDataModels').toList()
            ]
        ])
        String expected = new String(loadTestFile('simpleAndComplexDataModels', 'json')).replace(/Admin User/, 'Unlogged User')

        expect:
        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String id2 = response.body().items[1].id

        and:
        id
        id2

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
             [
                 dataModelIds: [id, id2],
                 asynchronous: true])

        then:
        verifyResponse ACCEPTED, response

        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        responseBody().message ==~ /Download \[.+?, .+?\] at ${baseUrl}domainExport\/.+?\/download/

        when:
        GET('domainExport', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().items.first().exported.domainIds == [id, id2]

        when:
        String deId = responseBody().items.first().id
        GET(responseBody().items.first().links.absolute, STRING_ARG, true)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanupDomainExport(deId)
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'I01 : test import basic DataModel'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
                      "documentationVersion": "1.0.0",
                      "modelVersion": "2.0.0"
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

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
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
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
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
        verifyResponse CREATED, response

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
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

    void 'I05 : test import DataModel with classifiers'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

    void 'I06A : test import simple DataModel JSON'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleDataModel').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I06B : test importing simple DataModel XML'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/5.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.XML.name,
                fileContents: loadTestFile('complexDataModel', 'xml').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I07A : test import complex DataModel JSON'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I07B : test import complex DataModel XML'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/5.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel', 'xml').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        String id = response.body().items[0].id

        and:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'I08A : test import multiple DataModels JSON'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleAndComplexDataModels').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String id2 = response.body().items[1].id

        and:
        id
        id2

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'I08B : test import multiple DataModels XML'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelXmlImporterService/5.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.XML.name,
                fileContents: loadTestFile('simpleAndComplexDataModels', 'xml').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String id2 = response.body().items[1].id

        and:
        id
        id2

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'I09 : test import complex DataModel async'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ],
            asynchronous: true
        ])

        then:
        verifyResponse ACCEPTED, response

        when:
        String jobId = responseBody().id
        waitForAysncToComplete(jobId)
        GET("asyncJobs/$jobId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().status == 'COMPLETED'
        !responseBody().message

        when:
        GET("dataModels/path/${Utils.safeUrlEncode('dm:Complex Test DataModel$main')}")

        then:
        verifyResponse(OK, response)

        cleanup:
        cleanUpData()
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

    void 'H01 : test getting simple DataModel hierarchy'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
      "label": "Test Authority",
      "defaultAuthority": true
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
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
      "label": "Test Authority",
      "defaultAuthority": true
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
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
  "count": 17,
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
              "namespace": "test.com/test",
              "key": "mdk1",
              "value": "mdv2"
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com",
              "key": "mdk1",
              "value": "mdv1"
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
    }
  ]
}'''

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    void 'test searching for label "emptyclass" in complex model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

    void 'IMI01 : test importing DataType'() {
        given:
        // Get DataModel
        String id = createNewItem(validJson)
        // Get second DataModel
        String otherId = createNewItem([
            label: 'Functional Test Model 1'
        ])
        // Get finalised DataModel
        String finalisedId = createNewItem([
            label: 'Functional Test Model 2'
        ])
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Get internal DT
        POST("$id/dataTypes", [
            label     : 'Functional Test DataType',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("$finalisedId/dataTypes", [
            label     : 'Functional Test DataType 2',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String importableId = responseBody().id

        POST("$finalisedId/dataTypes", [
            label     : 'Functional Test DataType',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String sameLabelId = responseBody().id

        POST("$otherId/dataTypes", [
            label     : 'Functional Test DataType 3',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String nonImportableId = responseBody().id

        when: 'importing non-existent'
        PUT("$id/dataTypes/$finalisedId/${nonImportableId}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'importing non importable id'
        PUT("$id/dataTypes/$otherId/$nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "PrimitiveType [${nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$id/dataTypes/$id/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "PrimitiveType [${internalId}] to be imported belongs to the DataModel already"

        when: 'importing with same label id'
        PUT("$id/dataTypes/$finalisedId/$sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataTypes] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "DataModel] has non-unique values [Functional Test DataType] on property [label]"

        when: 'importing importable id'
        PUT("$id/dataTypes/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of datatypes'
        log.info 'getting list of datatypes'
        GET("$id/dataTypes")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 2
        responseBody().items.any {it.id == internalId && !it.imported}
        responseBody().items.any {it.id == importableId && it.imported}

        cleanup:
        cleanUpData(id)
        cleanUpData(otherId)
        cleanUpData(finalisedId)
    }

    void 'IMI02 : test importing DataType and removing'() {
        given:
        // Get DataModel
        String id = createNewItem(validJson)
        // Get finalised DataModel
        String finalisedId = createNewItem([
            label: 'Functional Test Model 1'
        ])
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Get internal DT
        POST("$id/dataTypes", [
            label     : 'Functional Test DataType',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("$finalisedId/dataTypes", [
            label     : 'Functional Test DataType 2',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        String importableId = responseBody().id

        when: 'importing importable id'
        PUT("$id/dataTypes/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'removing non-existent'
        DELETE("$id/dataTypes/$finalisedId/${UUID.randomUUID()}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'removing internal id'
        DELETE("$id/dataTypes/$id/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "PrimitiveType [${internalId}] belongs to the DataModel and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$id/dataTypes/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of datatypes'
        GET("$id/dataTypes")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == internalId}

        cleanup:
        cleanUpData(id)
        cleanUpData(finalisedId)
    }

    void 'IMI03 : test importing DataClasses'() {
        given:
        // Get DataModel
        String id = createNewItem(validJson)
        // Get second DataModel
        String otherId = createNewItem([
            label: 'Functional Test Model 1'
        ])
        // Get finalised DataModel
        String finalisedId = createNewItem([
            label: 'Functional Test Model 2'
        ])
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Get internal DT
        POST("$id/dataClasses", [
            label: 'Functional Test DataClass',])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("$finalisedId/dataClasses", [
            label: 'Functional Test DataClass 2',])
        verifyResponse CREATED, response
        String importableId = responseBody().id
        POST("$finalisedId/dataClasses", [
            label: 'Functional Test DataClass',])
        verifyResponse CREATED, response
        String sameLabelId = responseBody().id

        POST("$otherId/dataClasses", [
            label: 'Functional Test DataClass 3',])
        verifyResponse CREATED, response
        String nonImportableId = responseBody().id

        when: 'importing non-existent'
        PUT("$id/dataClasses/$finalisedId/${nonImportableId}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'importing non importable id'
        PUT("$id/dataClasses/$otherId/$nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$id/dataClasses/$id/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${internalId}] to be imported belongs to the DataModel already"

        when: 'importing with same label id'
        PUT("$id/dataClasses/$finalisedId/$sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataClasses] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "DataModel] has non-unique values [Functional Test DataClass] on property [label]"

        when: 'importing importable id'
        PUT("$id/dataClasses/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataclasses'
        log.info 'getting list of dataclasses'
        GET("$id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 2
        responseBody().items.any {it.id == internalId && !it.imported}
        responseBody().items.any {it.id == importableId && it.imported}

        cleanup:
        cleanUpData(id)
        cleanUpData(otherId)
        cleanUpData(finalisedId)
    }

    void 'IMI04 : test importing DataClass and removing'() {
        given:
        // Get DataModel
        String id = createNewItem(validJson)
        // Get finalised DataModel
        String finalisedId = createNewItem([
            label: 'Functional Test Model 1'
        ])
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Get internal DT
        POST("$id/dataClasses", [
            label: 'Functional Test DataClass',])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("$finalisedId/dataClasses", [
            label: 'Functional Test DataClass 2',])
        verifyResponse CREATED, response
        String importableId = responseBody().id

        when: 'importing importable id'
        PUT("$id/dataClasses/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'removing non-existent'
        DELETE("$id/dataClasses/$finalisedId/${UUID.randomUUID()}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'removing internal id'
        DELETE("$id/dataClasses/$id/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${internalId}] belongs to the DataModel and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$id/dataClasses/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataClasses'
        GET("$id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == internalId}

        cleanup:
        cleanUpData(id)
        cleanUpData(finalisedId)
    }

    void 'IMI05 : test importing DataClasses inside same VF'() {
        given:
        // Get DataModel inside VF
        POST("folders/${versionedFolderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse(CREATED, response)
        String id = responseBody().id

        // Get second DataModel inside same VF

        POST("folders/${versionedFolderId}/${getResourcePath()}", [
            label: 'Functional Test Model 1'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String sameVfId = responseBody().id

        POST("folders/${otherVersionedFolderId}/${getResourcePath()}", [
            label: 'Functional Test Model 2'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String otherVfId = responseBody().id

        // Get finalised DataModel
        String finalisedId = createNewItem([
            label: 'Functional Test Model 3'
        ])
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Get finalised DataModel
        String nonFinalisedId = createNewItem([
            label: 'Functional Test Model 4'
        ])

        // Get internal DC
        POST("$id/dataClasses", [
            label: 'Functional Test DataClass',])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("$sameVfId/dataClasses", [
            label: 'Functional Test DataClass 2',])
        verifyResponse CREATED, response
        String vfImportableId = responseBody().id

        POST("$finalisedId/dataClasses", [
            label: 'Functional Test DataClass 3',])
        verifyResponse CREATED, response
        String finalisedImportableId = responseBody().id

        POST("$nonFinalisedId/dataClasses", [
            label: 'Functional Test DataClass 4',])
        verifyResponse CREATED, response
        String nonImportableId = responseBody().id

        POST("$otherVfId/dataClasses", [
            label: 'Functional Test DataClass 5',])
        verifyResponse CREATED, response
        String otherVfNonImportableId = responseBody().id

        when: 'importing non importable id'
        PUT("$id/dataClasses/$nonFinalisedId/$nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing non importable id'
        PUT("$id/dataClasses/$otherVfId/$otherVfNonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${otherVfNonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing importable id'
        PUT("$id/dataClasses/$sameVfId/$vfImportableId", [:])

        then:
        verifyResponse OK, response

        when: 'importing importable id'
        PUT("$id/dataClasses/$finalisedId/$finalisedImportableId", [:])

        then:
        verifyResponse OK, response

        cleanup:
        cleanUpData(id)
        cleanUpData(sameVfId)
        cleanUpData(otherVfId)
        cleanUpData(finalisedId)
        cleanUpData(nonFinalisedId)
    }

    @Unroll
    @Requires({
        // Only run on jenkins
        env.containsKey('JENKINS')
    })
    void 'DC01 : test breaking dataclasses [Attempt #i]'() {
        given:
        String ca = builder.buildCommonAncestorDataModel(folderId.toString())
        PUT("$ca/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$ca/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String mainId = responseBody().id

        when:
        PUT("$ca/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String sourceId = responseBody().id
        String deleteAndDelete = builder.getIdFromPath(sourceId, "dm:Functional Test DataModel 1\$source|dc:deleteAndDelete")
        String existingClass = builder.getIdFromPath(sourceId, "dm:Functional Test DataModel 1\$source|dc:existingClass")
        String deleteLeftOnlyFromExistingClass = builder.getIdFromPath(sourceId, "dm:Functional Test DataModel 1\$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass")
        DELETE("$sourceId/dataClasses/$deleteAndDelete")

        then:
        verifyResponse NO_CONTENT, response

        when:
        DELETE("$sourceId/dataClasses/$existingClass/dataClasses/$deleteLeftOnlyFromExistingClass")

        then:
        verifyResponse NO_CONTENT, response

        cleanup:
        cleanUpData(ca)
        cleanUpData(mainId)
        cleanUpData(sourceId)

        where:
        i << (1..100)
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
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
        verifyJsonResponse OK, expectedLinkSuggestions([child: '', ele1: '', element2: ''])

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    void 'LS02 : test get link suggestions for a model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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

    void 'SUBSET01 : test subsetting the Complex Test DataModel'() {
        def source = [:]
        def target = [:]
        def target2 = [:]

        given:
        target.dataModelId = createNewItem(validJson)
        target2.dataModelId = createNewItem([
            label: 'Functional Test Target Model 2'
        ])

        and:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
        source.dataModelId = response.body().items[0].id

        and:
        GET("/${source.dataModelId}/dataClasses")
        verifyResponse OK, response
        source.emptyClass = response.body().items.find {it.label == 'empty'}
        source.contentClass = response.body().items.find {it.label == 'content'}
        source.parentClass = response.body().items.find {it.label == 'parent'}

        and:
        GET("/${source.dataModelId}/dataClasses/${source.parentClass.id}/dataClasses")
        verifyResponse OK, response
        source.parentClass.childClass = response.body().items.find {it.label == 'child'}

        and:
        GET("/${source.dataModelId}/dataClasses/${source.contentClass.id}/dataElements")
        verifyResponse OK, response
        source.contentClass.ele1 = response.body().items.find {it.label == 'ele1'}
        source.contentClass.element2 = response.body().items.find {it.label == 'element2'}

        and:
        GET("/${source.dataModelId}/dataClasses/${source.parentClass.id}/dataClasses")
        verifyResponse OK, response
        source.parentClass.childClass = response.body().items.find {it.label == 'child'}

        and: 'there is a Data Element called grandchild on the child Data Class'
        POST("/${source.dataModelId}/dataClasses/${source.parentClass.childClass.id}/dataElements", [
            'label'   : 'grandchild',
            'dataType': ['label': 'string', 'domainType': 'PrimitiveType']
        ])
        verifyResponse CREATED, response
        source.parentClass.childClass.grandchild = response.body()

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        /**
         * Subset DataElement 'ele1' which belongs to the 'content' Data Class. This should:
         * 1. Create the content Data Class and ele1 Data Element on targetDataModel
         * 2. Appear in the /intersects response
         */
        when: 'subset ele1'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", ['additions': [source.contentClass.ele1.id], 'deletions': []])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")
        target.contentClass = response.body().items[0]

        then: 'There is the content Data Class'
        verifyResponse OK, response
        response.body().items.size() == 1
        response.body().items[0].label == 'content'
        target.contentClass.id

        when: 'Get Data Elements on targetDataModel'
        GET("/${target.dataModelId}/dataElements")

        then: 'There is the ele1 Data Element in the content Data Class'
        verifyResponse OK, response
        response.body().items.size() == 1
        response.body().items[0].label == 'ele1'
        response.body().items[0].dataClass == target.contentClass.id

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with one results'
        verifyResponse OK, response
        response.body().intersects.size() == 1
        response.body().intersects.contains(source.contentClass.ele1.id)

        when: 'Get the intersection between complex and target2'
        GET("/${source.dataModelId}/intersects/${target2.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains one intersection'
        response.body().items.find {it.targetDataModelId == target.dataModelId}.intersects.size() == 1

        and: 'The result for target2 contains no intersections'
        response.body().items.find {it.targetDataModelId == target2.dataModelId}.intersects.size() == 0

        /**
         * Subset delete DataElement 'ele1' which belongs to the 'content' Data Class. This should:
         * 1. Remove ele1 Data Element from targetDataModel
         * 2. Remove ele1 from the /intersects response
         */
        when: 'subset ele1'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", ['additions': [], 'deletions': [source.contentClass.ele1.id]])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")

        then: 'There are no Data Classes'
        verifyResponse OK, response
        response.body().items.size() == 0

        when: 'Get Data Elements on targetDataModel'
        GET("/${target.dataModelId}/dataElements")

        then: 'There are no Data Elements'
        verifyResponse OK, response
        response.body().items.size() == 0

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersection between complex and target2'
        GET("/${source.dataModelId}/intersects/${target2.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains no intersections'
        response.body().items.find {it.targetDataModelId == target.dataModelId}.intersects.size() == 0

        and: 'The result for target2 contains no intersections'
        response.body().items.find {it.targetDataModelId == target2.dataModelId}.intersects.size() == 0

        /**
         * Subset delete DataElement 'ele1' (again) and also 'element2', and 'childde' which belongs to the parent | child Data Class.
         * This should leave us with:
         * 1. The content Data Class with both ele1 and element2
         * 2. A parent Data Class containing a child Data Class containing a child Data Element
         * 3. All three Data Elements listed in the /intersection response
         */
        when: 'subset ele1, element2 and child'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", [
            'additions': [
                source.contentClass.ele1.id,
                source.contentClass.element2.id,
                source.parentClass.childClass.grandchild.id
            ],
            'deletions': []
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")
        target.contentClass = response.body().items.find {it.label == 'content'}
        target.parentClass = response.body().items.find {it.label == 'parent'}

        then: 'There is the content Data Class and parent Data Class and child Data Class'
        verifyResponse OK, response
        response.body().items.size() == 2
        target.contentClass.id
        target.parentClass.id

        when: 'Get the Data Classes of the parent Data Class'
        GET("/${target.dataModelId}/dataClasses/${target.parentClass.id}/dataClasses")
        target.parentClass.childClass = response.body().items.find {it.label == 'child'}

        then: 'The response is OK and includes the child Data Class'
        verifyResponse OK, response
        response.body().items.size() == 1
        target.parentClass.childClass.id

        when: 'Get Data Elements on targetDataModel'
        GET("/${target.dataModelId}/dataElements")
        target.contentClass.ele1 = response.body().items.find {it.label == 'ele1'}
        target.contentClass.element2 = response.body().items.find {it.label == 'element2'}
        target.parentClass.childClass.grandchild = response.body().items.find {it.label == 'grandchild'}

        then: 'There are the ele1 and element2 Data Elements in the content Data Class'
        verifyResponse OK, response
        response.body().items.size() == 3
        target.contentClass.ele1.id
        target.contentClass.element2.id
        target.parentClass.childClass.grandchild.id

        and: 'The grandchild belongs to child'
        target.parentClass.childClass.grandchild.dataClass == target.parentClass.childClass.id

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with three results'
        verifyResponse OK, response
        response.body().intersects.size() == 3
        response.body().intersects.contains(source.contentClass.ele1.id)
        response.body().intersects.contains(source.contentClass.element2.id)
        response.body().intersects.contains(source.parentClass.childClass.grandchild.id)

        when: 'Get the intersection between complex and target2'
        GET("/${source.dataModelId}/intersects/${target2.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains three intersections'
        response.body().items.find {it.targetDataModelId == target.dataModelId}.intersects.size() == 3

        and: 'The result for target2 contains no intersections'
        response.body().items.find {it.targetDataModelId == target2.dataModelId}.intersects.size() == 0

        when: 'subset ele1, element2 and child onto target2'
        PUT("/${source.dataModelId}/subset/${target2.dataModelId}", [
            'additions': [
                source.contentClass.ele1.id,
                source.contentClass.element2.id,
                source.parentClass.childClass.grandchild.id
            ],
            'deletions': []
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains three intersections'
        response.body().items.find {it.targetDataModelId == target.dataModelId}.intersects.size() == 3

        and: 'The result for target2 now has intersections'
        response.body().items.find {it.targetDataModelId == target2.dataModelId}.intersects.size() == 3

        when: 'Delete the grandchild from the subset'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", [
            'additions': [],
            'deletions': [source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")

        then: 'The parent Data Classes has been deleted from the targetDataModel'
        response.body().items.find {it.label == 'content'}
        !response.body().items.find {it.label == 'parent'}

        cleanup:
        cleanUpData(source.dataModelId)
        cleanUpData(target.dataModelId)
        cleanUpData(target2.dataModelId)
    }

    String expectedLinkSuggestions(Map<String, String> results) {
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
                          "results": [''' + results.child + '''

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
                          "results": [''' + results.ele1 + '''

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
                          "results": [''' + results.element2 + '''

                          ]
                        }
                      ]
                    }'''
    }

    Map<String, String> expectedLinkSuggestionResults() {
        [child   : '',
         ele1    : '''
                    {
          "dataElement": {
            "id": "${json-unit.matches:id}",
            "domainType": "DataElement",
            "label": "ele1",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Simple Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "simple",
                "domainType": "DataClass"
              }
            ],
            "description": "most obvious match",
            "dataClass": "${json-unit.matches:id}",
            "dataType": {
              "id": "${json-unit.matches:id}",
              "domainType": "PrimitiveType",
              "label": "string",
              "model": "${json-unit.matches:id}",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          "score": "${json-unit.any-number}"
        },
        {
          "dataElement": {
            "id": "${json-unit.matches:id}",
            "domainType": "DataElement",
            "label": "ele2",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Simple Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "simple",
                "domainType": "DataClass"
              }
            ],
            "description": "least obvious match",
            "dataClass": "${json-unit.matches:id}",
            "dataType": {
              "id": "${json-unit.matches:id}",
              "domainType": "PrimitiveType",
              "label": "string",
              "model": "${json-unit.matches:id}",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          "score": "${json-unit.any-number}"
        }''',
         element2: '']
    }

    String getExpectedMergeDiffJson() {
        '''{
  "sourceId": "${json-unit.matches:id}",
  "targetId": "${json-unit.matches:id}",
  "path": "dm:Functional Test DataModel 1$source",
  "label": "Functional Test DataModel 1",
  "count": 17,
  "diffs": [
    {
      "fieldName": "description",
      "path": "dm:Functional Test DataModel 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:deleteAndModify",
      "isMergeConflict": true,
      "isSourceDeletionAndTargetModification": true,
      "type": "deletion"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:deleteLeftOnly",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "dm:Functional Test DataModel 1$source|dc:addAndAddReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:existingClass|dc:addLeftToExistingClass",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:existingClass|de:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "fieldName": "dataTypePath",
      "path": "dm:Functional Test DataModel 1$source|dc:existingClass|de:existingDataElement@dataTypePath",
      "sourceValue": "dm:Functional Test DataModel 1$source|dt:existingDataType2",
      "targetValue": "dm:Functional Test DataModel 1$1.0.0|dt:existingDataType1",
      "commonAncestorValue": "dm:Functional Test DataModel 1$1.0.0|dt:existingDataType1",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "dm:Functional Test DataModel 1$source|dc:modifyAndModifyReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "dm:Functional Test DataModel 1$source|dc:modifyLeftOnly@description",
      "sourceValue": "Description",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dt:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "dm:Functional Test DataModel 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    }
  ]
}'''
    }

    String getExpectedSecondMergeDiffJson() {
        '''{
  "sourceId": "${json-unit.matches:id}",
  "targetId": "${json-unit.matches:id}",
  "path": "dm:Functional Test DataModel 1$source",
  "label": "Functional Test DataModel 1",
  "count": 3,
  "diffs": [
    {
      "path": "dm:Functional Test DataModel 1$source|dc:addLeftOnly|dc:addAnotherLeftToAddLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "dm:Functional Test DataModel 1$source|dc:existingClass|dc:addLeftToExistingClass|de:addAnotherLeftToExistingClass",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "fieldName": "dataTypePath",
      "path": "dm:Functional Test DataModel 1$source|dc:existingClass|de:existingDataElement@dataTypePath",
      "sourceValue": "dm:Functional Test DataModel 1$source|dt:existingDataType2",
      "targetValue": "dm:Functional Test DataModel 1$main|dt:existingDataType2",
      "commonAncestorValue": "dm:Functional Test DataModel 1$1.0.0|dt:existingDataType1",
      "isMergeConflict": true,
      "type": "modification"
    }
  ]
}'''
    }
}
