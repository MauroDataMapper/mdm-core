/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: terminology
 *  |   POST   | /api/folders/${folderId}/terminologies  | Action: save
 *  |   GET    | /api/folders/${folderId}/terminologies  | Action: index
 *
 *  |  DELETE  | /api/terminologies/${id}                | Action: delete
 *  |   PUT    | /api/terminologies/${id}                | Action: update
 *  |   GET    | /api/terminologies/${id}                | Action: show
 *  |   GET    | /api/terminologies                      | Action: index
 *
 *  |   GET    | /api/terminologies/providers/importers  | Action: importerProviders
 *  |   GET    | /api/terminologies/providers/exporters  | Action: exporterProviders

 *  |  DELETE  | /api/terminologies/${terminologyId}/readByAuthenticated  | Action: readByAuthenticated
 *  |   PUT    | /api/terminologies/${terminologyId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/terminologies/${terminologyId}/readByEveryone       | Action: readByEveryone
 *  |   PUT    | /api/terminologies/${terminologyId}/readByEveryone       | Action: readByEveryone
 *
 *  |   PUT    | /api/terminologies/${terminologyId}/newForkModel          | Action: newForkModel
 *  |   PUT    | /api/terminologies/${terminologyId}/newDocumentationVersion  | Action: newDocumentationVersion
 *  |   PUT    | /api/terminologies/${terminologyId}/finalise                 | Action: finalise

 *  |   PUT    | /api/terminologies/${terminologyId}/folder/${folderId}   | Action: changeFolder
 *  |   PUT    | /api/folders/${folderId}/terminologies/${terminologyId}  | Action: changeFolder
 *
 *  |   GET    | /api/terminologies/${terminologyId}/diff/${otherTerminologyId}  | Action: diff
 *
 *  |  DELETE  | /api/terminologies  | Action: deleteAll
 *
 *  |   GET    | /api/terminologies/${terminologyId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportModel
 *  |   POST   | /api/terminologies/import/${importerNamespace}/${importerName}/${importerVersion}  | Action: importModels
 *  |   POST   | /api/terminologies/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportModels
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyController
 */
@Integration
@Transactional
@Slf4j
class TerminologyFunctionalSpec extends ResourceFunctionalSpec<Terminology> {

    @Shared
    UUID folderId

    @Shared
    UUID movingFolderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        assert Folder.count() == 0
        assert Terminology.count() == 0
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Functional Test Folder 2', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec TerminologyFunctionalSpec')
        cleanUpResources(Folder, Classifier)
    }

    @Override
    String getResourcePath() {
        'terminologies'
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
  "id": "${json-unit.matches:id}",
  "domainType": "Terminology",
  "label": "Functional Test Model",
  "availableActions": ["delete","show","update"],
  "branchName": "main",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "type": "Terminology",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  }
}'''
    }

    void 'test getting Terminology exporters'() {
        when:
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'test getting Terminology importers'() {
        when:
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'test finalising Terminology'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [:])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update'] //TODO can this be restricted by the core plugin?
        response.body().finalised
        response.body().dateFinalised

        cleanup:
        cleanUpData(id)
    }

    void 'test creating a new fork model of a Terminology'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [:])
        verifyResponse OK, response

        when: 'adding one new model'
        PUT("$id/newForkModel", [label: 'Functional Test Terminology reader'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test Terminology reader",')


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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newForkModel", [label: 'Functional Test Terminology editor'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test Terminology editor",')

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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology editor"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
     {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology editor"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'test creating a new documentation version of a Terminology'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [:])
        verifyResponse OK, response

        when:
        PUT("$id/newDocumentationVersion", [:], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "Terminology",
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

    void 'test creating a new branch model version of a Terminology'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [:])
        verifyResponse OK, response

        when:
        PUT("$id/newBranchModelVersion", [branchName: 'testNewBranchModelVersion'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, expectedShowJson.replaceFirst(/"main"/, '"testNewBranchModelVersion"')

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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''
        cleanup:
        cleanUpData()
    }

    void 'test creating a main branch model version when one already exists'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [:])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse BAD_REQUEST, response
        response.body().message.contains('The [branchName] \'main\' already exists for the [label]')

        cleanup:
        cleanUpData()
    }

    void 'test changing folder from Terminology context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/folder/${movingFolderId}", [:])

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test changing folder from Folder context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        response = PUT("folders/${movingFolderId}/terminologies/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test diffing 2 Terminologys'() {
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

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export a single Terminology'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "terminology": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export multiple Terminologys (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [terminologyIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "terminology": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test import basic Terminology'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            terminologyName                : 'Functional Test Import',
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
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Import",
                      "type": "Data Standard"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test import basic Terminology as new documentation version'() {
        given:
        String id = createNewItem([
            label    : 'Functional Test Model',
            finalised: true
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : true,
            terminologyName                : 'Functional Test Model',
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
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'not yet implemented')
    void 'test delete multiple models'() {
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
                      "deleted": true,
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
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

    @PendingFeature(reason = 'no importers/exporters')
    void 'test importing simple test Terminology'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleTerminology').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test importing complex test Terminology'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexTerminology').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export simple Terminology'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleTerminology').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('simpleTerminology')).replaceFirst('"exportedBy": "Admin User",',
                                                                                     '"exportedBy": "Unlogged User",')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export complex Terminology'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexTerminology').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('complexTerminology')).replaceFirst('"exportedBy": "Admin User",',
                                                                                      '"exportedBy": "Unlogged User",')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }
}