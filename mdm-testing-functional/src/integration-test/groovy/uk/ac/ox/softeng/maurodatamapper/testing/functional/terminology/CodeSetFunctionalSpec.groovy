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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.terminology

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.PendingFeature

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
class CodeSetFunctionalSpec extends ModelUserAccessAndPermissionChangingFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Transactional
    String getTestFolder2Id() {
        Folder.findByLabel('Functional Test Folder 2').id.toString()
    }

    @Transactional
    String getSimpleCodeSetId() {
        CodeSet.findByLabel(BootstrapModels.SIMPLE_CODESET_NAME).id.toString()
    }

    @Transactional
    String getCodeSetFolderId(String id) {
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
    Map getValidUpdateJson() {
        [
            description: 'This is a new testing CodeSet'
        ]
    }

    @Override
    String getModelType() {
        'CodeSet'
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
    }

    @Override
    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'finalise', 'delete']
    }

    List<String> getReaderAvailableActions() {
        ['show', 'comment']
    }

    @Override
    Boolean isDisabledNotDeleted() {
        true
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "CodeSet",
      "label": "Simple Test CodeSet",
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
        "label": "Mauro Data Mapper"
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
    "show","comment","editDescription","update","save","softDelete","finalise","delete"
  ],
  "branchName":"main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  }
}'''
    }

    void 'Test getting available CodeSet exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[]'''
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
        verifyJsonResponse OK, '''[]'''

    }

    void 'L20 : test changing folder from CodeSet context (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N20 : test changing folder from CodeSet context (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R20 : test changing folder from CodeSet context (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E20 : test changing folder from CodeSet context (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the codeSet but not the folder 2'
        loginEditor()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'A20 : test changing folder from CodeSet context (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyResponse OK, response

        and:
        getCodeSetFolderId(id) == getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'L21 : test changing folder from Folder context (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("folders/${getTestFolder2Id()}/codeSets/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N21 : test changing folder from Folder context (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("folders/${getTestFolder2Id()}/codeSets/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R21 : test changing folder from Folder context (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("folders/${getTestFolder2Id()}/codeSets/$id", [:], MAP_ARG, true)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E21 : test changing folder from Folder context (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the codeSet but not the folder 2'
        loginEditor()
        PUT("folders/${getTestFolder2Id()}/codeSets/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'A21 : test changing folder from Folder context (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("folders/${getTestFolder2Id()}/codeSets/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        and:
        getCodeSetFolderId(id) == getTestFolder2Id()

        when: 'logged in as reader as no access to folder 2 or reader share'
        loginReader()
        GET(id)

        then:
        verifyNotFound response, id

        when: 'logged in as editor no access to folder 2 but has direct DM access'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)
    }

    @PendingFeature(reason = 'no 2nd model to diff against')
    void 'L22 : test diffing 2 CodeSets (as not logged in)'() {

        when: 'not logged in'
        GET("${getComplexCodeSetId()}/diff/${getSimpleCodeSetId()}")

        then:
        verifyNotFound response, getComplexCodeSetId()
    }

    @PendingFeature(reason = 'no 2nd model to diff against')
    void 'N22 : test diffing 2 CodeSets (as authenticated/no access)'() {
        when:
        loginAuthenticated()
        GET("${getComplexCodeSetId()}/diff/${getSimpleCodeSetId()}")

        then:
        verifyNotFound response, getComplexCodeSetId()
    }

    @PendingFeature(reason = 'no 2nd model to diff against')
    void 'R22A : test diffing 2 CodeSets (as reader of LH model)'() {
        given:
        String id = getValidId()
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when: 'able to read right model only'
        loginReader()
        GET("${getComplexCodeSetId()}/diff/${id}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    @PendingFeature(reason = 'no 2nd model to diff against')
    void 'R22B : test diffing 2 CodeSets (as reader of RH model)'() {
        given:
        String id = getValidId()
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when:
        loginReader()
        GET("${id}/diff/${getComplexCodeSetId()}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    @PendingFeature(reason = 'no 2nd model to diff against')
    void 'R22C : test diffing 2 CodeSets (as reader of both models)'() {
        when:
        loginReader()
        GET("${getComplexCodeSetId()}/diff/${getSimpleCodeSetId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedDiffJson()
    }

    void 'L23 : test export a single CodeSet (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N23 : test export a single CodeSet (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    @PendingFeature(reason = "Not yet implemented")
    void 'R23 : test export a single CodeSet (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "codeSet": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test CodeSet",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L24 : test export multiple CodeSets (json only exports first id) (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0',
             [codeSetIds: [id, getSimpleCodeSetId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N24 : test export multiple CodeSets (json only exports first id) (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0',
             [codeSetIds: [id, getSimpleCodeSetId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    @PendingFeature(reason = "Not yet implemented")
    void 'R24 : test export multiple CodeSets (json only exports first id) (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0',
             [codeSetIds: [id, getSimpleCodeSetId()]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "codeSet": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test CodeSet",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    @PendingFeature(reason = "Not yet implemented")
    void 'L25 : test import basic CodeSet (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            codeSetName                    : 'Functional Test Import',
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

    @PendingFeature(reason = "Not yet implemented")
    void 'N25 : test import basic CodeSet (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            codeSetName                    : 'Functional Test Import',
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

    @PendingFeature(reason = "Not yet implemented")
    void 'R25 : test import basic CodeSet (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            codeSetName                    : 'Functional Test Import',
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

    @PendingFeature(reason = "Not yet implemented")
    void 'E25A : test import basic CodeSet (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            codeSetName                    : 'Functional Test Import',
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

    @PendingFeature(reason = "Not yet implemented")
    void 'E25B : test import basic CodeSet as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.codeSet.provider.importer/JsonImporterService/2.0', [
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

    String getExpectedDiffJson() {
        '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Complex Test CodeSet",
  "count": 106,
  "diffs": [
    {
      "label": {
        "left": "Complex Test CodeSet",
        "right": "Simple Test CodeSet"
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
      "terms": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT49: Complex Test Term 49",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT92: Complex Test Term 92",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT66: Complex Test Term 66",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT77: Complex Test Term 77",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT53: Complex Test Term 53",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT89: Complex Test Term 89",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT79: Complex Test Term 79",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT56: Complex Test Term 56",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT46: Complex Test Term 46",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT87: Complex Test Term 87",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT18: Complex Test Term 18",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT62: Complex Test Term 62",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT35: Complex Test Term 35",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT70: Complex Test Term 70",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT16: Complex Test Term 16",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT98: Complex Test Term 98",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT75: Complex Test Term 75",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT45: Complex Test Term 45",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT90: Complex Test Term 90",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT39: Complex Test Term 39",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT63: Complex Test Term 63",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT31: Complex Test Term 31",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT82: Complex Test Term 82",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT85: Complex Test Term 85",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT22: Complex Test Term 22",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT20: Complex Test Term 20",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT86: Complex Test Term 86",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT26: Complex Test Term 26",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT24: Complex Test Term 24",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT32: Complex Test Term 32",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT44: Complex Test Term 44",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT73: Complex Test Term 73",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT59: Complex Test Term 59",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT40: Complex Test Term 40",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT30: Complex Test Term 30",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT100: Complex Test Term 100",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT81: Complex Test Term 81",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT42: Complex Test Term 42",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT52: Complex Test Term 52",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT91: Complex Test Term 91",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT11: Complex Test Term 11",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT00: Complex Test Term 00",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT14: Complex Test Term 14",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT64: Complex Test Term 64",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT4: Complex Test Term 4",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT7: Complex Test Term 7",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT76: Complex Test Term 76",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT48: Complex Test Term 48",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT97: Complex Test Term 97",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT9: Complex Test Term 9",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT1: Complex Test Term 1",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT6: Complex Test Term 6",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT21: Complex Test Term 21",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT69: Complex Test Term 69",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT55: Complex Test Term 55",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT74: Complex Test Term 74",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT43: Complex Test Term 43",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT2: Complex Test Term 2",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT37: Complex Test Term 37",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT72: Complex Test Term 72",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT25: Complex Test Term 25",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT36: Complex Test Term 36",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT80: Complex Test Term 80",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT78: Complex Test Term 78",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT88: Complex Test Term 88",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT28: Complex Test Term 28",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT67: Complex Test Term 67",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT15: Complex Test Term 15",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT41: Complex Test Term 41",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT38: Complex Test Term 38",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT23: Complex Test Term 23",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT13: Complex Test Term 13",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT34: Complex Test Term 34",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT17: Complex Test Term 17",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT54: Complex Test Term 54",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT60: Complex Test Term 60",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT94: Complex Test Term 94",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT84: Complex Test Term 84",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT61: Complex Test Term 61",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT8: Complex Test Term 8",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT3: Complex Test Term 3",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT99: Complex Test Term 99",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT19: Complex Test Term 19",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT83: Complex Test Term 83",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT93: Complex Test Term 93",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT47: Complex Test Term 47",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT51: Complex Test Term 51",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT10: Complex Test Term 10",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT95: Complex Test Term 95",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT65: Complex Test Term 65",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT50: Complex Test Term 50",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT96: Complex Test Term 96",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT27: Complex Test Term 27",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT71: Complex Test Term 71",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT5: Complex Test Term 5",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT58: Complex Test Term 58",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT68: Complex Test Term 68",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT33: Complex Test Term 33",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT57: Complex Test Term 57",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT12: Complex Test Term 12",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "CTT29: Complex Test Term 29",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          }
        ],
        "created": [
          {
            "id": "${json-unit.matches:id}",
            "label": "STT01: Simple Test Term 01",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Simple Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "STT02: Simple Test Term 02",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Simple Test CodeSet",
                "domainType": "CodeSet",
                "finalised": false
              }
            ]
          }
        ]
      }
    }
  ]
}'''
    }
}