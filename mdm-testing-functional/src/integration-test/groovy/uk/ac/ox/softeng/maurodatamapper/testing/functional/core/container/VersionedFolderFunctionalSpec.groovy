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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TestMergeData
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.merge.VersionedFolderMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 *  Controller: versionedFolder
 *  |  DELETE  | /api/versionedFolders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  PUT     | /api/versionedFolders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/versionedFolders/${folderId}/readByEveryone       | Action: readByEveryone
 *  |  PUT     | /api/versionedFolders/${folderId}/readByEveryone       | Action: readByEveryone
 *  |  POST    | /api/versionedFolders        | Action: save
 *  |  GET     | /api/versionedFolders        | Action: index
 *  |  DELETE  | /api/versionedFolders/${id}  | Action: delete
 *  |  PUT     | /api/versionedFolders/${id}  | Action: update
 *  |  GET     | /api/versionedFolders/${id}  | Action: show
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class VersionedFolderFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

    @Shared
    VersionedFolderMergeBuilder builder

    @Autowired(required = false)
    List<ModelService> modelServices

    @OnceBefore
    void createBuilder() {
        builder = new VersionedFolderMergeBuilder(this)
    }

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Override
    String getResourcePath() {
        'versionedFolders'
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test VersionedFolder 3'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: null
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Testing folder description'
        ]
    }

    @Transactional
    String getEditorGroupRoleId() {
        GroupRole editorGroupRole = GroupRole.findByName('editor')
        editorGroupRole.id
    }

    @Transactional
    Map getValidJsonWithOneGroup() {
        UserGroup editorsUserGroup = UserGroup.findByName('editors')
        [
            label      : 'Functional Test VersionedFolder 4',
            description: 'Description of Functional Test Folder 4',
            groups     : [
                [groupId: editorsUserGroup.id, groupRoleId: getEditorGroupRoleId()]
            ]
        ]
    }

    @Transactional
    Map getValidJsonWithTwoGroups() {
        UserGroup editorsUserGroup = UserGroup.findByName('editors')
        UserGroup readersUserGroup = UserGroup.findByName('readers')
        [
            label      : 'Functional Test VersionedFolder 5',
            description: 'Description of Functional Test Folder 5',
            groups     : [
                [groupId: editorsUserGroup.id, groupRoleId: getEditorGroupRoleId()],
                [groupId: readersUserGroup.id, groupRoleId: getEditorGroupRoleId()]
            ]
        ]
    }

    //With an error in the attribute names
    @Transactional
    Map getInvalidJsonWithOneGroup() {
        UserGroup editorsUserGroup = UserGroup.findByName('editors')
        [
            label      : 'Functional Test VersionedFolder 6',
            description: 'Description of Functional Test Folder 6',
            groups     : [
                [groupI: editorsUserGroup.id, groupRoleI: getEditorGroupRoleId()]
            ]
        ]
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[VersionedFolder:Functional Test VersionedFolder 3] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[VersionedFolder:Functional Test VersionedFolder 3] changed properties \[description]/
    }

    @Override
    Boolean isDisabledNotDeleted() {
        true
    }

    @Override
    Boolean hasDefaultCreation() {
        true
    }

    Boolean getAuthenticatedUsersCanCreate() {
        true
    }

    @Override
    Boolean getReaderCanCreate() {
        true
    }

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'comment', 'editDescription', 'finalise', 'update', 'save', 'softDelete', 'delete'].sort()
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
    }

    @Override
    int getExpectedCountOfGroupsWithAccess() {
        2
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert response.body().count == 0
    }

    @Override
    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
        response.body().items.size() == 0
    }

    @Override
    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert response.body().label == count ? "New Folder (${count})".toString() : 'New Folder'
        assert response.body().availableActions == getEditorAvailableActions().sort()
        assert response.body().readableByEveryone == false
        assert response.body().readableByAuthenticatedUsers == false
    }

    @Override
    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using permanent API call', id)
        loginAdmin()
        DELETE("${id}?permanent=true")
        response.status() in [NO_CONTENT, NOT_FOUND]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test VersionedFolder",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "VersionedFolder",
      "hasChildFolders": false,
      "branchName": "main",
      "documentationVersion": "1.0.0"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test VersionedFolder 2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "VersionedFolder",
      "hasChildFolders": false,
      "branchName": "main",
      "documentationVersion": "1.0.0"
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "VersionedFolder",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test VersionedFolder 3",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["comment","delete","editDescription","finalise","save","show","softDelete","update"],
  "branchName": "main",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
  }
}'''
    }

    void 'S01 : test searching for label "Test" in empty folder'() {
        given:
        def id = getValidId()
        def term = 'test'

        when: 'not logged in'
        GET("${id}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, id)

        when: 'not logged in'
        loginAuthenticated()
        GET("${id}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, id)

        when: 'logged in as reader user'
        loginReader()
        GET("${id}/search?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()

        cleanup:
        removeValidIdObject(id)
    }

    void 'D01 : Test the permanent delete action correctly deletes an instance with folder inside'() {
        when: 'Creating a top folder'
        String id = getValidId()

        loginEditor()
        POST("$id/folders", validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when: 'When the delete action is executed on an existing instance'
        DELETE("${id}?permanent=true")

        then: 'The response is correct'
        response.status == NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == NOT_FOUND

        cleanup:
        cleanupUserGroups()
    }

    void 'D02 : Test the permanent delete action correctly deletes an instance with folder and datamodel inside'() {
        when: 'Creating a top folder'
        String id = getValidId()

        loginEditor()
        POST("$id/folders", validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when: 'The save action is executed with valid data for a datamodel'
        POST("folders/$id/dataModels", [
            label: 'Functional Test DataModel'
        ], MAP_ARG, true)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when: 'When the delete action is executed on an existing instance'
        DELETE("${id}?permanent=true")

        then: 'The response is correct'
        response.status == NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == NOT_FOUND

        cleanup:
        cleanupUserGroups()
    }

    void 'G01 : Test create folder with one user group specified'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getValidJsonWithOneGroup())

        then:
        response.status == CREATED
        response.body().id
        String folderId = response.body().id

        when:
        GET("versionedFolders/${folderId}/groupRoles/${getEditorGroupRoleId()}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
            "count": 1,
            "items": [
                {
                    "id": "${json-unit.matches:id}",
                    "availableActions": ["comment","delete","editDescription","finalise","save","show","softDelete","update"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "VersionedFolder",
                    "securableResourceId": "${json-unit.matches:id}",
                    "userGroup": {
                        "id": "${json-unit.matches:id}",
                        "name": "editors"
                    },
                    "groupRole": {
                        "id": "${json-unit.matches:id}",
                        "name": "editor",
                        "displayName": "Editor"
                    }
                }
            ]
        }'''

        cleanup:
        removeValidIdObject(folderId)
    }

    void 'G02 : Test create folder with two user groups specified'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getValidJsonWithTwoGroups())

        then:
        response.status == CREATED
        response.body().id
        String folderId = response.body().id

        when:
        GET("versionedFolders/${folderId}/groupRoles/${getEditorGroupRoleId()}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
            "count": 2,
            "items": [
                {
                    "id": "${json-unit.matches:id}",
                    "availableActions": ["comment","delete","editDescription","finalise","save","show","softDelete","update"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "VersionedFolder",
                    "securableResourceId": "${json-unit.matches:id}",
                    "userGroup": {
                        "id": "${json-unit.matches:id}",
                        "name": "editors"
                    },
                    "groupRole": {
                        "id": "${json-unit.matches:id}",
                        "name": "editor",
                        "displayName": "Editor"
                    }
                },
                {
                    "id": "${json-unit.matches:id}",
                    "availableActions": ["comment","delete","editDescription","finalise","save","show","softDelete","update"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "VersionedFolder",
                    "securableResourceId": "${json-unit.matches:id}",
                    "userGroup": {
                        "id": "${json-unit.matches:id}",
                        "name": "readers"
                    },
                    "groupRole": {
                        "id": "${json-unit.matches:id}",
                        "name": "editor",
                        "displayName": "Editor"
                    }
                }
            ]
        }'''

        cleanup:
        removeValidIdObject(folderId)
    }

    void 'G03 : Test create folder with one user group invalidly specified'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getInvalidJsonWithOneGroup())

        then:
        response.status == CREATED
        response.body().id
        String folderId = response.body().id

        when:
        GET("versionedFolders/${folderId}/groupRoles/${getEditorGroupRoleId()}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
            "count": 0,
            "items": [                
            ]
        }'''

        cleanup:
        removeValidIdObject(folderId)
    }

    @Ignore
    void 'G04 : Test create folder with one user group specified can be accessed through the folders group roles endpoint'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getValidJsonWithOneGroup())

        then:
        response.status == CREATED
        response.body().id
        String folderId = response.body().id

        when:
        GET("folders/${folderId}/groupRoles/${getEditorGroupRoleId()}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
            "count": 1,
            "items": [
                {
                    "id": "${json-unit.matches:id}",
                    "availableActions": ["comment","delete","editDescription","finalise","save","show","softDelete","update"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "VersionedFolder",
                    "securableResourceId": "${json-unit.matches:id}",
                    "userGroup": {
                        "id": "${json-unit.matches:id}",
                        "name": "editors"
                    },
                    "groupRole": {
                        "id": "${json-unit.matches:id}",
                        "name": "editor",
                        "displayName": "Editor"
                    }
                }
            ]
        }'''

        cleanup:
        removeValidIdObject(folderId)
    }

    void 'I01 :  Test importing non-finalised model into a top level VF (as admin)'() {
        given:
        String id = getValidId()

        when:
        loginAdmin()
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.0', [
            finalised : false,
            modelName : 'Functional Test Import',
            folderId  : id,
            importFile: [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: '''{
            "dataModel": {
                "id": "d8023de6-5329-4b8b-8a1b-27c2abeaffcd",
                "label": "Import Model",
                "lastUpdated": "2021-02-11T17:43:53.2Z",
                "author": "Import Author",
                "organisation": "Import Organisation",
                "documentationVersion": "1.0.0",
                "finalised": false,
                "authority": {
                    "id": "82429f5a-c3f9-45f2-8ed5-0426f5b0030d",
                    "url": "http://localhost",
                    "label": "Mauro Data Mapper"
                }
            },
            "exportMetadata": {
                "exportedBy": "Admin User",
                "exportedOn": "2021-02-14T18:32:37.522Z",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
                    "name": "DataModelJsonExporterService",
                    "version": "3.0"
                }
            }
        }'''.bytes.toList()
            ]
        ], MAP_ARG, true)

        then:
        verifyResponse CREATED, response

        cleanup:
        cleanupIds(id)
    }

    void 'I02 :  Test importing non-finalised model into a sub level VF (as admin)'() {
        given:
        loginEditor()
        POST("folders/${testFolderId}/versionedFolders", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id

        when:
        loginAdmin()
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.0', [
            finalised : false,
            modelName : 'Functional Test Import',
            folderId  : id,
            importFile: [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: '''{
            "dataModel": {
                "id": "d8023de6-5329-4b8b-8a1b-27c2abeaffcd",
                "label": "Import Model",
                "lastUpdated": "2021-02-11T17:43:53.2Z",
                "author": "Import Author",
                "organisation": "Import Organisation",
                "documentationVersion": "1.0.0",
                "finalised": false,
                "authority": {
                    "id": "82429f5a-c3f9-45f2-8ed5-0426f5b0030d",
                    "url": "http://localhost",
                    "label": "Mauro Data Mapper"
                }
            },
            "exportMetadata": {
                "exportedBy": "Admin User",
                "exportedOn": "2021-02-14T18:32:37.522Z",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
                    "name": "DataModelJsonExporterService",
                    "version": "3.0"
                }
            }
        }'''.bytes.toList()
            ]
        ], MAP_ARG, true)

        then:
        verifyResponse CREATED, response

        cleanup:
        cleanupIds(id)
    }

    void 'F01 : Test finalising Model (as reader)'() {
        given:
        Map data = getValidIdWithContent()
        loginReader()

        when: 'getting the folder before finalisation'
        GET(data.id)

        then:
        verifyResponse(OK, response)

        when: 'finalised'
        PUT("$data.id/finalise", ["version": "3.9.0"])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(data.id)
    }

    void 'F02 : Test finalisation endpoint for Versioned Folder (as editor)'() {
        given:
        Map data = getValidIdWithContent()
        loginEditor()

        when: 'getting the folder before finalisation'
        GET(data.id)

        then:
        response.status == OK
        responseBody().availableActions == getEditorAvailableActions()
        !responseBody().finalised
        responseBody().domainType == 'VersionedFolder'
        !responseBody().modelVersion
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when: 'getting the datamodel 1 before finalisation'
        GET("dataModels/$data.dataModel1Id", MAP_ARG, true)

        then: 'cannot finalise model inside versioned folder'
        response.status == OK
        responseBody().availableActions == (getEditorAvailableActions() - ResourceActions.FINALISE_ACTION)
        !responseBody().finalised
        !responseBody().modelVersion
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when: 'getting the datamodel 2 before finalisation'
        GET("dataModels/$data.dataModel2Id", MAP_ARG, true)

        then:
        response.status == OK
        responseBody().availableActions == (getEditorAvailableActions() - ResourceActions.FINALISE_ACTION)
        !responseBody().finalised
        !responseBody().modelVersion
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when: 'The folder gets finalised'
        PUT("$data.id/finalise", [versionChangeType: 'Major'])

        then:
        response.status == OK
        responseBody().availableActions == getFinalisedEditorAvailableActions()
        responseBody().finalised
        responseBody().domainType == 'VersionedFolder'
        responseBody().modelVersion == '1.0.0'

        when:
        GET("dataModels/$data.dataModel1Id", MAP_ARG, true)

        then:
        response.status == OK
        responseBody().finalised
        responseBody().domainType == 'DataModel'
        responseBody().modelVersion == '1.0.0'
        responseBody().availableActions == getFinalisedEditorAvailableActions() - ResourceActions.EDITOR_VERSIONING_ACTIONS

        when:
        GET("dataModels/$data.dataModel2Id", MAP_ARG, true)

        then:
        response.status == OK
        responseBody().finalised
        responseBody().domainType == 'DataModel'
        responseBody().modelVersion == '1.0.0'
        responseBody().availableActions == getFinalisedEditorAvailableActions() - ResourceActions.EDITOR_VERSIONING_ACTIONS

        cleanup:
        cleanupIds(data.id)
    }

    void 'F03 : Test finalising Model (as editor) with a versionTag'() {
        given:
        Map data = getValidIdWithContent()
        loginEditor()

        when: 'putting a tag'
        PUT("$data.id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Release'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == getFinalisedEditorAvailableActions()
        responseBody().modelVersion == '1.0.0'
        responseBody().modelVersionTag == 'Functional Test Release'

        when: 'log out and log back in again in as editor available actions are correct and modelVersionTag is set'
        logout()
        loginEditor()
        GET(data.id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions()
        responseBody().modelVersionTag == 'Functional Test Release'

        when: 'log out and log back in again in as admin available actions are correct and modelVersionTag is set'
        logout()
        loginAdmin()
        GET(data.id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions()
        responseBody().modelVersionTag == 'Functional Test Release'

        cleanup:
        cleanupIds(data.id)
    }

    void 'BMV01 : test creating a new branch model version of a VersionedFolder (as reader)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id

        when:
        loginReader()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(data.id)
    }

    void 'BMV02 : test creating a new branch model version of an unfinalised VersionedFolder (as editor)'() {
        given:
        Map data = getValidIdWithContent()
        String id = data.id

        when:
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(data.id)
    }

    void 'BMV03 : test creating a new model version of a VersionedFolder (no branch name) (as editor)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id
        loginEditor()

        when: 'getting the list of versioned folders'
        GET('?label=3')

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'creating a new main branch'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        GET("$branchId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        responseBody().items.first().sourceModel.id == branchId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when: 'getting the list of versioned folders'
        GET('?label=3')

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any { it.id == id }
        responseBody().items.any { it.id == branchId }

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion
        }

        cleanup:
        cleanupIds(id, branchId)
    }

    void 'BMV04 : test creating a new branch model version of a VersionedFolder (as editor)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        GET("$branchId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        responseBody().items.first().sourceModel.id == branchId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when:
        GET('?label=3')

        then:
        verifyResponse OK, response
        responseBody().count == 3

        when:
        log.debug(responseBody().toString())
        String mainBranchId = responseBody().items.find {
            it.label == validJson.label &&
            !(it.id in [branchId, id])
        }?.id

        then:
        mainBranchId

        when:
        GET(mainBranchId)

        then:
        verifyResponse OK, response
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }

        when: 'getting the models inside the main branch folder'
        GET("folders/$mainBranchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion
        }

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == 'newBranchModelVersion' &&
            !it.modelVersion
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == 'newBranchModelVersion' &&
            !it.modelVersion
        }

        cleanup:
        cleanupIds(id, branchId, mainBranchId)
    }

    void 'BMV05 : test creating a new model version of a VersionedFolder and finalising (as editor)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when:
        String branchId = responseBody().id
        PUT("$branchId/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()
        responseBody().modelVersion == '2.0.0'

        when: 'getting the models inside the first finalised folder'
        GET("folders/$id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }

        when: 'getting the models inside the second finalised folder'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '2.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '2.0.0'
        }

        cleanup:
        cleanupIds(id, branchId)
    }

    void 'BMV06 : test creating a new branch model version of a VersionedFolder and trying to finalise (as editor)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        responseBody().availableActions == getEditorAvailableActions().sort()
        String mainBranchId = responseBody().id

        when:
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        String branchId = responseBody().id
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion
        responseBody().availableActions == ((getEditorAvailableActions() - [ResourceActions.FINALISE_ACTION]) + [ResourceActions.MERGE_INTO_ACTION]).sort()

        when:
        PUT("$branchId/finalise", [versionChangeType: 'Major'])

        then:
        verifyForbidden response

        when:
        GET('?label=3')

        then:
        verifyResponse OK, response
        responseBody().count == 3

        cleanup:
        cleanupIds(id, branchId, mainBranchId)
    }

    void 'BMV07 : test creating a new branch model with DM, T and CS'() {
        given:
        Map data = builder.buildComplexModelsForBranching()
        loginEditor()
        String commonAncestorId = data.commonAncestorId

        when:
        PUT("$commonAncestorId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        String branchId = responseBody().id

        and:
        getIdFromPath(branchId, 'dm:Functional Test DataModel 1$main')
        getIdFromPath(branchId, 'te:Functional Test Terminology 1$main')
        getIdFromPath(branchId, 'cs:Functional Test CodeSet 1$main')

        when: 'getting the Ts inside the branch'
        GET("folders/$branchId/terminologies", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.first().id == getIdFromPath(branchId, 'te:Functional Test Terminology 1$main')

        when: 'getting the DMs inside the branch'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when:
        String branchedDataModelId = responseBody().items.first().id

        then:
        branchedDataModelId == getIdFromPath(branchId, 'dm:Functional Test DataModel 1$main')

        when: 'getting the model data type from the DM inside the branch'
        GET("dataModels/$branchedDataModelId/dataTypes", MAP_ARG, true)

        then: 'the branched model data type points to the branched terminology'
        verifyResponse(OK, response)
        responseBody().count == 1
        def first = responseBody().items.first()
        first.id == getIdFromPath(branchId, 'dm:Functional Test DataModel 1$main|dt:Functional Test Model Data Type')
        first.domainType == 'ModelDataType'
        first.modelResourceDomainType == 'Terminology'
        first.modelResourceId == getIdFromPath(branchId, 'te:Functional Test Terminology 1$main')

        when: 'getting the CSs inside the branch'
        GET("folders/$branchId/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.first().id == getIdFromPath(branchId, 'cs:Functional Test CodeSet 1$main')

        cleanup:
        cleanupIds(branchId, commonAncestorId)
    }

    void 'BMV08 : test creating a new branch model with DM, T and CS with non default branch'() {
        given:
        Map data = builder.buildComplexModelsForBranching()
        loginEditor()
        String commonAncestorId = data.commonAncestorId

        when:
        PUT("$commonAncestorId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        String mainBranchId = responseBody().id

        when:
        PUT("$commonAncestorId/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        String branchId = responseBody().id

        and:
        getIdFromPath(mainBranchId, 'dm:Functional Test DataModel 1$main')
        getIdFromPath(mainBranchId, 'te:Functional Test Terminology 1$main')
        getIdFromPath(mainBranchId, 'cs:Functional Test CodeSet 1$main')

        and:
        getIdFromPath(branchId, 'dm:Functional Test DataModel 1$newBranchModelVersion')
        getIdFromPath(branchId, 'te:Functional Test Terminology 1$newBranchModelVersion')
        getIdFromPath(branchId, 'cs:Functional Test CodeSet 1$newBranchModelVersion')

        when: 'getting the DMs inside the branch'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.first().id == getIdFromPath(branchId, 'dm:Functional Test DataModel 1$newBranchModelVersion')

        when: 'getting the Ts inside the branch'
        GET("folders/$branchId/terminologies", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.first().id == getIdFromPath(branchId, 'te:Functional Test Terminology 1$newBranchModelVersion')

        when: 'getting the CSs inside the branch'
        GET("folders/$branchId/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.first().id == getIdFromPath(branchId, 'cs:Functional Test CodeSet 1$newBranchModelVersion')

        cleanup:
        cleanupIds(branchId, mainBranchId, commonAncestorId)
    }

    void 'BMV09 : test creating a new branch model version of the complex VersionedFolder (as editor)'() {
        given:
        Map data = builder.buildComplexModelsForBranching()
        loginEditor()
        String id = data.commonAncestorId
        GET("terminologies/$data.terminologyCaId/terms", MAP_ARG, true)
        verifyResponse(OK, response)
        responseBody().count == 6
        List<String> finalisedTermIds = responseBody().items.collect { it.id }
        GET("codeSets/$data.codeSetCaId/terms", MAP_ARG, true)
        verifyResponse(OK, response)
        responseBody().count == 5
        List<String> finalisedCodeSetTermIds = responseBody().items.collect { it.id }

        expect:
        finalisedCodeSetTermIds.every { it in finalisedTermIds }

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == 'Functional Test VersionedFolder Complex'
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion
        String branchId = responseBody().id

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/terminologies", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/terminologies", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String branchedTerminologyId = responseBody().items.first().id

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String branchedCodeSetId = responseBody().items.first().id

        when: 'checking the terms used inside the branched codeset they point to the branched terminology'
        GET("terminologies/$branchedTerminologyId/terms", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 8
        List<String> branchedTermIds = responseBody().items.collect { it.id }
        !branchedTermIds.any { it in finalisedTermIds }

        when:
        GET("codeSets/$branchedCodeSetId/terms", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 6
        List<String> branchedCodeSetTermIds = responseBody().items.collect { it.id }
        !branchedCodeSetTermIds.any { it in finalisedTermIds }
        branchedCodeSetTermIds.every { it in branchedTermIds }

        cleanup:
        cleanupIds(id, branchId)
    }

    void 'BMV10 : test creating a new branch model version of the complex VersionedFolder (as editor)'() {
        given:
        Map data = builder.buildSubFolderModelsForBranching()
        loginEditor()
        String id = data.commonAncestorId
        GET("terminologies/$data.terminologyCaId/terms", MAP_ARG, true)
        verifyResponse(OK, response)
        responseBody().count == 6
        List<String> finalisedTermIds = responseBody().items.collect { it.id }
        GET("codeSets/$data.codeSetCaId/terms", MAP_ARG, true)
        verifyResponse(OK, response)
        responseBody().count == 5
        List<String> finalisedCodeSetTermIds = responseBody().items.collect { it.id }

        when: 'checking finalisation status'
        GET("dataModels/$data.dataModelCaId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().modelVersion == '1.0.0'

        when: 'checking finalisation status'
        GET("dataModels/$data.dataModel2Id", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().modelVersion == '1.0.0'

        when: 'checking finalisation status'
        GET("dataModels/$data.dataModel3Id", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().modelVersion == '1.0.0'

        when: 'branching'
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        String branchId = responseBody().id
        verifyResponse CREATED, response

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'getting the models inside the new branch folder'
        GET("folders/$branchId/terminologies", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 0

        when: 'getting the sub folders inside the new branch'
        GET("folders/$branchId/folders", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2

        when: 'getting the folders inside the new branch sub folder'
        String subFolderId = responseBody().items.find { it.label == 'Sub Folder in VersionedFolder' }.id
        String subFolder2Id = responseBody().items.find { it.label == 'Sub Folder 2 in VersionedFolder' }.id
        GET("folders/$subFolderId/folders", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when:
        String subSubFolderId = responseBody().items.find { it.label == 'Sub-Sub Folder in VersionedFolder' }.id
        GET("folders/$subFolder2Id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when:
        GET("folders/$subSubFolderId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'getting the models inside the sub folders'
        GET("folders/$subFolderId/terminologies", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when: 'checking the terms used inside the branched codeset they point to the branched terminology'
        String branchedTerminologyId = responseBody().items.first().id
        GET("terminologies/$branchedTerminologyId/terms", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 8
        List<String> branchedTermIds = responseBody().items.collect { it.id }
        !branchedTermIds.any { it in finalisedTermIds }

        when: 'getting the models inside the sub folders'
        GET("folders/$subSubFolderId/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1

        when:
        String branchedCodeSetId = responseBody().items.first().id
        GET("codeSets/$branchedCodeSetId/terms", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 6
        List<String> branchedCodeSetTermIds = responseBody().items.collect { it.id }
        !branchedCodeSetTermIds.any { it in finalisedTermIds }
        branchedCodeSetTermIds.every { it in branchedTermIds }

        cleanup:
        cleanupIds(id, branchId)
    }

    void 'FMV01 : test creating a new fork model of an unfinalised VersionedFolder (as reader)'() {
        given:
        Map data = getValidIdWithContent()
        String id = data.id
        String forkModelName = 'Functional Test Fork'

        when: 'logged in as reader'
        loginReader()
        PUT("$id/newForkModel", [label: forkModelName])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(id)
    }

    void 'FMV02 : test creating a new fork model of a VersionedFolder (as reader)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id
        String forkModelName = 'Functional Test Fork'

        when: 'logged in as reader'
        loginReader()
        PUT("$id/newForkModel", [label: forkModelName])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == forkModelName

        when:
        String forkId = responseBody().id
        GET("$forkId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_FORK_OF.label
        responseBody().items.first().sourceModel.id == forkId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when:
        GET('?label=fork')

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().label == forkModelName
        responseBody().items.first().id == forkId

        when: 'getting the fork as the creator'
        GET(forkId)

        then:
        verifyResponse(OK, response)

        when: 'getting the fork as editor its not found as permissions dont exist'
        loginEditor()
        GET(forkId)

        then:
        verifyNotFound(response, forkId)

        cleanup:
        loginReader()
        DELETE("$forkId?permanent=true")
        verifyResponse NO_CONTENT, response
        loginEditor()
        DELETE("$id?permanent=true")
        verifyResponse NO_CONTENT, response
        cleanUpRoles(forkId, id)
    }

    void 'FMV03 : test creating a new fork model of an unfinalised VersionedFolder (as editor)'() {
        given:
        Map data = getValidIdWithContent()
        String id = data.id
        String forkModelName = 'Functional Test Fork'

        when: 'logged in as reader'
        loginEditor()
        PUT("$id/newForkModel", [label: forkModelName])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(id)
    }

    void 'FMV04 : test creating a new fork model of a VersionedFolder (as editor)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id
        String forkModelName = 'Functional Test Fork'

        when: 'logged in as writer'
        loginEditor()
        PUT("$id/newForkModel", [label: forkModelName])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == forkModelName

        when:
        String forkId = responseBody().id
        GET("$forkId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_FORK_OF.label
        responseBody().items.first().sourceModel.id == forkId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0'
        }

        when: 'getting the models inside the fork folder'
        GET("folders/$forkId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == "Functional Test DataModel 1 (${forkModelName})" &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion
        }
        responseBody().items.any {
            it.label == "Functional Test DataModel 2 (${forkModelName})" &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion
        }

        cleanup:
        cleanupIds(forkId, id)
    }

    void 'DMV01 : test creating a new documentation version of a VersionedFolder (as reader)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id

        when:
        loginReader()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(id)
    }

    void 'DMV02 : test creating a new documentation version of an unfinalised VersionedFolder (as editor)'() {
        given:
        Map data = getValidIdWithContent()
        String id = data.id

        when:
        loginEditor()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        cleanupIds(id)
    }

    void 'DMV03 : test creating a new documentation version of a VersionedFolder (as editor)'() {
        given:
        Map data = getValidFinalisedIdWithContent()
        String id = data.id

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '2.0.0'

        when:
        String docId = responseBody().id
        GET("$docId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        responseBody().items.first().sourceModel.id == docId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when: 'getting the models inside the finalised folder'
        GET("folders/$id/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0' &&
            it.documentationVersion == '1.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            it.modelVersion == '1.0.0' &&
            it.documentationVersion == '1.0.0'
        }

        when: 'getting the models inside the doc folder'
        GET("folders/$docId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {
            it.label == 'Functional Test DataModel 1' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion &&
            it.documentationVersion == '2.0.0'
        }
        responseBody().items.any {
            it.label == 'Functional Test DataModel 2' &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
            !it.modelVersion &&
            it.documentationVersion == '2.0.0'
        }

        cleanup:
        cleanupIds(id, docId)
    }

    void 'CA01 : test finding common ancestor (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()

        when:
        GET("$data.anotherBranch/commonAncestor/$data.interestingBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.v5
        responseBody().label == validJson.label

        when:
        GET("$data.anotherBranch/commonAncestor/$data.testBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.v3
        responseBody().label == validJson.label

        when:
        GET("$data.newBranch/commonAncestor/$data.testBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.v1
        responseBody().label == validJson.label

        when:
        GET("$data.newBranch/commonAncestor/$data.anotherBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.v1
        responseBody().label == validJson.label

        when:
        GET("$data.anotherFork/commonAncestor/$data.anotherBranch")

        then:
        verifyResponse BAD_REQUEST, response
        responseBody().message ==
        "VersionedFolder [${data.anotherFork}] does not share its label with [${data.anotherBranch}] therefore they cannot have a common ancestor"

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'LMV01 : test finding latest model version (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        String expectedId = data.v5
        String newBranchId = data.testBranch
        String latestDraftId = data.main

        when: 'logged in as editor'
        loginEditor()
        GET("$newBranchId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '5.0.0'

        when:
        GET("$latestDraftId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '5.0.0'

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'LFM01 : test finding latest finalised model (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        String expectedId = data.v5
        String newBranchId = data.testBranch
        String latestDraftId = data.main

        when: 'logged in as editor'
        loginEditor()
        GET("$newBranchId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == validJson.label
        responseBody().modelVersion == '5.0.0'

        when:
        GET("$latestDraftId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == validJson.label
        responseBody().modelVersion == '5.0.0'

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'CMB01 : test getting current draft model on main branch from side branch (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()

        when: 'logged in as editor'
        GET("$data.v1/currentMainBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.main
        responseBody().label == validJson.label

        when:
        GET("$data.v5/currentMainBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.main
        responseBody().label == validJson.label

        when:
        GET("$data.testBranch/currentMainBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.main
        responseBody().label == validJson.label

        when:
        GET("$data.anotherBranch/currentMainBranch")

        then:
        verifyResponse OK, response
        responseBody().id == data.main
        responseBody().label == validJson.label

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'AB01 : test getting all draft models (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        List expectedBrancheIds = [data.main, data.anotherBranch, data.interestingBranch, data.testBranch, data.newBranch]
        loginEditor()

        when:
        GET("$data.v5/availableBranches")

        then:
        verifyResponse OK, response
        responseBody().count == 5
        responseBody().items.each { it.id in expectedBrancheIds }
        responseBody().items.each { it.label == validJson.label }

        when:
        GET("$data.v2/availableBranches")

        then:
        verifyResponse OK, response
        responseBody().count == 5
        responseBody().items.each { it.id in expectedBrancheIds }
        responseBody().items.each { it.label == validJson.label }

        when:
        GET("$data.v1/availableBranches")

        then:
        verifyResponse OK, response
        responseBody().count == 5
        responseBody().items.each { it.id in expectedBrancheIds }
        responseBody().items.each { it.label == validJson.label }

        when:
        GET("$data.main/availableBranches")

        then:
        verifyResponse OK, response
        responseBody().count == 5
        responseBody().items.each { it.id in expectedBrancheIds }
        responseBody().items.each { it.label == validJson.label }

        cleanup:
        cleanupModelVersionTree(data)
    }

    @Unroll
    void 'MVT01 : Test getting versionTreeModel at #tag (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()

        when: 'getting the tree'
        GET("${data[tag]}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(getExpectedModelTreeVersionString(data), jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)

        // For all versions and branches we should get the same tree, no matter where it was requested
        // But it should NOT include the branch of the fork
        where:
        tag << ['v1', 'v2', 'v3', 'v4', 'v5',
                'newBranch', 'testBranch', 'main', 'anotherBranch', 'interestingBranch']
    }

    void 'MVT02 : Test getting versionTreeModel at fork only shows the fork and its branch'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.fork}",
    "label": "Functional Test Fork ${modelType}",
    "branch": null,
    "modelVersion": "0.1.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
     {
        "id": "${data.forkMain}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.forkMain}",
    "label": "Functional Test Fork ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  }]"""

        when: 'getting the tree'
        GET("${data.fork}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'MVT03 : Test getting versionTreeModel at anotherFork only shows the fork'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.anotherFork}",
    "label": "Functional Test AnotherFork ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": []
  }]"""

        when: 'getting the tree'
        GET("${data.anotherFork}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    @Unroll
    void 'SMVT01 : Test getting simple versionTreeModel at #tag (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()

        when: 'getting the tree'
        GET("${data[tag]}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(getExpectedSimpleModelTreeVersionString(data), jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)

        // For all versions and branches we should get the same tree, no matter where it was requested
        // But it should NOT include the branch of the fork
        where:
        tag << ['v1', 'v2', 'v3', 'v4', 'v5',
                'newBranch', 'testBranch', 'main', 'anotherBranch', 'interestingBranch']
    }

    void 'SMVT02 : Test getting simple versionTreeModel at fork only shows the fork and its branch'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.fork}",
    "branch": null,
    "modelVersion": "0.1.0",
    "documentationVersion": "1.0.0",
    "displayName": "V0.1.0"
  },
  {
    "id": "${data.forkMain}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V0.1.0)"
  }]"""

        when: 'getting the tree'
        GET("${data.fork}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'SMVT03 : Test getting simple versionTreeModel at anotherFork only shows the fork'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.anotherFork}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main"
  }]"""

        when: 'getting the tree'
        GET("${data.anotherFork}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'DI01 : test diffing 2 versioned folders (as not logged in)'() {

        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when: 'not logged in'
        GET("$mergeData.source/diff/$mergeData.target")

        then:
        verifyNotFound response, mergeData.source

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'DI02 : test diffing 2 versioned folders (as authenticated/no access)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginAuthenticated()
        GET("$mergeData.source/diff/$mergeData.target")

        then:
        verifyNotFound response, mergeData.source

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'DI03 : test diffing 2 versioned folders (as reader of LH model)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging(true, false)

        when: 'able to read right model only'
        loginReader()
        GET("$mergeData.source/diff/$mergeData.target")

        then:
        verifyNotFound response, mergeData.target

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'DI04 : test diffing 2 versioned folders (as reader of RH model)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging(false, true)

        when:
        loginReader()
        GET("$mergeData.source/diff/$mergeData.target")

        then:
        verifyNotFound response, mergeData.source

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'DI05 : test diffing 2 simple versioned folders (as reader of both models)'() {

        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginReader()
        GET("$mergeData.source/diff/$mergeData.target", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Functional Test VersionedFolder Simple",
  "count": 1,
  "diffs": [
    {
      "branchName": {
        "left": "left",
        "right": "main"
      }
    }
  ]
}
'''

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'DI06 : test diffing 2 complex versioned folders (as reader of both models)'() {

        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging()

        when:
        loginReader()
        GET("$mergeData.source/diff/$mergeData.target", STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedComplexDiffJson()

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MD01 : test finding merge difference of two versioned folders (as not logged in)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MD02 : test finding merge difference of two versioned folders (as authenticated/logged in)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginAuthenticated()
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MD03 : test finding merge difference of two versioned folders (as reader)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginReader()
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse OK, response
        responseBody().targetId == mergeData.target
        responseBody().sourceId == mergeData.source

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MD04 : test finding merge difference of two versioned folders (as editor)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginEditor()
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse OK, response
        responseBody().targetId == mergeData.target
        responseBody().sourceId == mergeData.source

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MD05 : test finding merge difference of two complex versioned folders (as reader)'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging()

        when:
        loginReader()
        GET("$mergeData.source/mergeDiff/$mergeData.target", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedMergeDiffJson

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MD06 : test finding merge difference of two complex sub folder versioned folders (as reader)'() {
        given:
        TestMergeData mergeData = builder.buildSubFolderModelsForMerging()

        when:
        loginReader()
        GET("$mergeData.source/mergeDiff/$mergeData.target", STRING_ARG)

        then:
        verifyJsonResponse OK, VersionedFolderMergeBuilder.getExpectedSubFolderMergeDiffJson()

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI01 : test merge into of two versioned folders (as not logged in)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        GET("$mergeData.source/mergeInto/$mergeData.target")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI02 : test merge into of two versioned folders (as authenticated/logged in)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginAuthenticated()
        GET("$mergeData.source/mergeInto/$mergeData.target")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI03 : test merge into of two versioned folders (as reader)'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginReader()
        PUT("$mergeData.source/mergeInto/$mergeData.target", [:])

        then:
        verifyForbidden(response)

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI04 : test merging diff with no patch data'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginEditor()
        PUT("$mergeData.source/mergeInto/$mergeData.target", [:])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors[0].message.contains('cannot be null')

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI05 : test merging diff with URI id not matching body id'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginEditor()
        PUT("$mergeData.source/mergeInto/$mergeData.target", [patch:
                                                                  [
                                                                      targetId: mergeData.target,
                                                                      sourceId: UUID.randomUUID().toString(),
                                                                      label   : "Functional Test Model",
                                                                      count   : 0,
                                                                      patches : []
                                                                  ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Source versioned folder id passed in request body does not match source versioned folder id in URI.'

        when:
        PUT("$mergeData.source/mergeInto/$mergeData.target", [patch:
                                                                  [
                                                                      targetId: UUID.randomUUID().toString(),
                                                                      sourceId: mergeData.source,
                                                                      label   : "Functional Test Model",
                                                                      count   : 0,
                                                                      patches : []
                                                                  ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Target versioned folder id passed in request body does not match target versioned folder id in URI.'

        when:
        PUT("$mergeData.source/mergeInto/$mergeData.target", [patch:
                                                                  [
                                                                      targetId: mergeData.target,
                                                                      sourceId: mergeData.source,
                                                                      label   : "Functional Test Model",
                                                                      count   : 0,
                                                                      patches : []
                                                                  ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().id == mergeData.target

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI06 : test merge into of two versioned folders'() {
        given:
        TestMergeData mergeData = builder.buildSimpleVersionedFoldersForMerging()

        when:
        loginEditor()
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch:
                [
                    targetId: mergeData.target,
                    sourceId: mergeData.source,
                    label   : "Functional Test Model",
                    count   : 0,
                    patches : []
                ]
        ])

        then:
        verifyResponse OK, response
        responseBody().id == mergeData.target

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI07 : test merge into of two complex versioned folders'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging()

        when:
        loginReader()
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse(OK, response)

        when:
        def diffs = responseBody().diffs
        loginEditor()
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch:
                [
                    targetId: mergeData.target,
                    sourceId: mergeData.source,
                    label   : "Functional Test Model",
                    count   : diffs.size(),
                    patches : diffs
                ]
        ])

        then:
        verifyResponse OK, response
        responseBody().id == mergeData.target
        responseBody().description == 'source description on the versioned folder'

        when:
        Map targetDataModelMap = mergeData.targetMap.dataModel
        GET("dataModels/$targetDataModelMap.dataModelId", MAP_ARG, true)

        then:
        responseBody().description == 'DescriptionLeft'

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/dataClasses", MAP_ARG, true)

        then:
        responseBody().items.label as Set == ['existingClass', 'modifyAndModifyReturningDifference', 'modifyLeftOnly',
                                              'addAndAddReturningDifference', 'modifyAndDelete', 'addLeftOnly',
                                              'modifyRightOnly', 'addRightOnly', 'modifyAndModifyReturningNoDifference',
                                              'addAndAddReturningNoDifference'] as Set
        responseBody().items.find { dataClass -> dataClass.label == 'modifyAndDelete' }.description == 'Description'
        responseBody().items.find { dataClass -> dataClass.label == 'addAndAddReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { dataClass -> dataClass.label == 'modifyAndModifyReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { dataClass -> dataClass.label == 'modifyLeftOnly' }.description == 'Description'

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/dataClasses/$targetDataModelMap.existingClass/dataClasses", MAP_ARG, true)

        then:
        responseBody().items.label as Set == ['addRightToExistingClass', 'addLeftToExistingClass'] as Set

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/dataClasses/$targetDataModelMap.existingClass/dataElements", MAP_ARG, true)

        then:
        responseBody().items.label as Set == ['addLeftOnly', 'Functional Test Data Element with Model Data Type'] as Set

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/dataTypes", MAP_ARG, true)

        then:
        responseBody().items.label as Set == ['addLeftOnly', 'Functional Test Model Data Type'] as Set

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/metadata", MAP_ARG, true)

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        when:
        Map targetTerminologyMap = mergeData.targetMap.terminology
        GET("terminologies/$targetTerminologyMap.terminologyId", MAP_ARG, true)

        then:
        responseBody().description == 'DescriptionLeft'

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/terms", MAP_ARG, true)

        then:
        responseBody().items.code as Set == ['AAARD', 'ALO', 'ALOCS', 'ARO', 'MAD', 'MAMRD', 'MLO', 'SALO', 'SMLO', 'DLOCS'] as Set
        responseBody().items.find { term -> term.code == 'MAD' }.description == 'Description'
        responseBody().items.find { term -> term.code == 'AAARD' }.description == 'DescriptionLeft'
        responseBody().items.find { term -> term.code == 'MAMRD' }.description == 'DescriptionLeft'
        responseBody().items.find { term -> term.code == 'MLO' }.description == 'Description'

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/termRelationshipTypes", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.label as Set == ['inverseOf', 'sameSourceActionType', 'similarSourceAction', 'sameActionAs', 'parentTo'] as Set
        responseBody().items.find { term -> term.label == 'inverseOf' }.description == 'inverseOf(Modified)'

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$targetTerminologyMap.modifyLeftOnly/termRelationships", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.size == 1
        responseBody().items.label as Set == ['sameSourceActionType'] as Set

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId" +
            "/terms/$targetTerminologyMap.modifyLeftOnly" +
            "/termRelationships/$targetTerminologyMap.sameSourceActionTypeOnSecondModifyLeftOnly",
            MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == targetTerminologyMap.secondModifyLeftOnly
        responseBody().targetTerm.id == targetTerminologyMap.modifyLeftOnly

        when:
        String addLeftOnly = getIdFromPath(mergeData.target, 'te:Functional Test Terminology 1$main|tm:ALO')
        String secondAddLeftOnly = getIdFromPath(mergeData.target, 'te:Functional Test Terminology 1$main|tm:SALO')
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$addLeftOnly/termRelationships", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.size == 2
        responseBody().items.label as Set == ['similarSourceAction', 'sameSourceActionType'] as Set

        when:
        String sameSourceActionType = responseBody().items.find { it.label == 'sameSourceActionType' }.id
        String similarSourceAction = responseBody().items.find { it.label == 'similarSourceAction' }.id
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$addLeftOnly/termRelationships/$sameSourceActionType", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == addLeftOnly
        responseBody().targetTerm.id == secondAddLeftOnly

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$addLeftOnly/termRelationships/$similarSourceAction", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == addLeftOnly
        responseBody().targetTerm.id == targetTerminologyMap.addAndAddReturningDifference

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/metadata", MAP_ARG, true)

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        when:
        Map targetCodeSetMap = mergeData.targetMap.codeSet
        GET("codeSets/$targetCodeSetMap.codeSetId/terms", MAP_ARG, true)

        then:
        verifyResponse OK, response
        // MAD cannot be added back into the CS as part of the merge
        responseBody().items.code as Set == ['AAARD', 'ALO' /*, 'MAD'*/, 'MAMRD', 'MLO', 'ALOCS'] as Set
        responseBody().items.each { t ->
            Assert.assertEquals("${t.code} has correct terminology", targetTerminologyMap.terminologyId, t.model)
        }

        when:
        GET("codeSets/$targetCodeSetMap.codeSetId/metadata", MAP_ARG, true)

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI08 : test merge into of two sub folder versioned folders'() {
        given:
        TestMergeData mergeData = builder.buildSubFolderModelsForMerging()

        when:
        loginReader()
        GET("$mergeData.source/mergeDiff/$mergeData.target")

        then:
        verifyResponse(OK, response)

        when:
        def diffs = responseBody().diffs
        loginEditor()
        PUT("$mergeData.source/mergeInto/$mergeData.target", [
            patch:
                [
                    targetId: mergeData.target,
                    sourceId: mergeData.source,
                    label   : "Functional Test Model",
                    count   : diffs.size(),
                    patches : diffs
                ]
        ])

        then:
        verifyResponse OK, response
        responseBody().id == mergeData.target
        responseBody().description == 'source description on the versioned folder'

        when:
        GET("${mergeData.target}/folders")

        then:
        verifyResponse(OK, response)
        responseBody().count == 3
        responseBody().items.find { it.label == 'New Sub Folder in VersionedFolder' }

        when:
        GET("${mergeData.targetMap.subFolder2Id}/folders")

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.find { it.label == 'New Sub-Sub Folder 2 in VersionedFolder' }

        when:
        Map targetDataModelMap = mergeData.targetMap.dataModel1
        GET("dataModels/$targetDataModelMap.dataModelId", MAP_ARG, true)

        then:
        responseBody().description == 'DescriptionLeft'

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/dataClasses", MAP_ARG, true)

        then:
        responseBody().items.label as Set == ['existingClass', 'modifyAndModifyReturningDifference', 'modifyLeftOnly',
                                              'addAndAddReturningDifference', 'modifyAndDelete', 'addLeftOnly',
                                              'modifyRightOnly', 'addRightOnly', 'modifyAndModifyReturningNoDifference',
                                              'addAndAddReturningNoDifference'] as Set
        responseBody().items.find { dataClass -> dataClass.label == 'modifyAndDelete' }.description == 'Description'
        responseBody().items.find { dataClass -> dataClass.label == 'addAndAddReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { dataClass -> dataClass.label == 'modifyAndModifyReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { dataClass -> dataClass.label == 'modifyLeftOnly' }.description == 'Description'

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/dataClasses/$targetDataModelMap.existingClass/dataClasses", MAP_ARG, true)

        then:
        responseBody().items.label as Set == ['addRightToExistingClass', 'addLeftToExistingClass'] as Set

        when:
        GET("dataModels/$targetDataModelMap.dataModelId/metadata", MAP_ARG, true)

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        when:
        Map targetTerminologyMap = mergeData.targetMap.terminology
        GET("terminologies/$targetTerminologyMap.terminologyId", MAP_ARG, true)

        then:
        responseBody().description == 'DescriptionLeft'

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/terms", MAP_ARG, true)

        then:
        responseBody().items.code as Set == ['AAARD', 'ALO', 'ALOCS', 'ARO', 'MAD', 'MAMRD', 'MLO', 'SALO', 'SMLO', 'DLOCS'] as Set
        responseBody().items.find { term -> term.code == 'MAD' }.description == 'Description'
        responseBody().items.find { term -> term.code == 'AAARD' }.description == 'DescriptionLeft'
        responseBody().items.find { term -> term.code == 'MAMRD' }.description == 'DescriptionLeft'
        responseBody().items.find { term -> term.code == 'MLO' }.description == 'Description'

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/termRelationshipTypes", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.label as Set == ['inverseOf', 'sameSourceActionType', 'similarSourceAction', 'sameActionAs', 'parentTo'] as Set
        responseBody().items.find { term -> term.label == 'inverseOf' }.description == 'inverseOf(Modified)'

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$targetTerminologyMap.modifyLeftOnly/termRelationships", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.size == 1
        responseBody().items.label as Set == ['sameSourceActionType'] as Set

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId" +
            "/terms/$targetTerminologyMap.modifyLeftOnly" +
            "/termRelationships/$targetTerminologyMap.sameSourceActionTypeOnSecondModifyLeftOnly",
            MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == targetTerminologyMap.secondModifyLeftOnly
        responseBody().targetTerm.id == targetTerminologyMap.modifyLeftOnly

        when:
        String addLeftOnly = getIdFromPath(mergeData.target, 'fo:Sub Folder in VersionedFolder|te:Functional Test Terminology 1$main|tm:ALO')
        String secondAddLeftOnly = getIdFromPath(mergeData.target, 'fo:Sub Folder in VersionedFolder|te:Functional Test Terminology 1$main|tm:SALO')
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$addLeftOnly/termRelationships", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.size == 2
        responseBody().items.label as Set == ['similarSourceAction', 'sameSourceActionType'] as Set

        when:
        String sameSourceActionType = responseBody().items.find { it.label == 'sameSourceActionType' }.id
        String similarSourceAction = responseBody().items.find { it.label == 'similarSourceAction' }.id
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$addLeftOnly/termRelationships/$sameSourceActionType", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == addLeftOnly
        responseBody().targetTerm.id == secondAddLeftOnly

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/terms/$addLeftOnly/termRelationships/$similarSourceAction", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == addLeftOnly
        responseBody().targetTerm.id == targetTerminologyMap.addAndAddReturningDifference

        when:
        GET("terminologies/$targetTerminologyMap.terminologyId/metadata", MAP_ARG, true)

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        when:
        Map targetCodeSetMap = mergeData.targetMap.codeSet
        GET("codeSets/$targetCodeSetMap.codeSetId/terms", MAP_ARG, true)

        then:
        verifyResponse OK, response
        // MAD cannot be added back into the CS as part of the merge
        responseBody().items.code as Set == ['AAARD', 'ALO' /*, 'MAD'*/, 'MAMRD', 'MLO', 'ALOCS'] as Set
        responseBody().items.each { t ->
            Assert.assertEquals("${t.code} has correct terminology", targetTerminologyMap.terminologyId, t.model)
        }

        when:
        GET("codeSets/$targetCodeSetMap.codeSetId/metadata", MAP_ARG, true)

        then:
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyOnSource' }.value == 'source has modified this'
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'modifyAndDelete' }.value == 'source has modified this also'
        !responseBody().items.find { it.namespace == 'functional.test' && it.key == 'metadataDeleteFromSource' }
        responseBody().items.find { it.namespace == 'functional.test' && it.key == 'addToSourceOnly' }

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'MI09 : test branch and merge the complex VersionedFolder when a Model Data Type has differences (as editor)'() {
        given:
        Map data = builder.buildComplexModelsForBranching()
        loginEditor()
        String id = data.commonAncestorId

        when: 'logged in as editor we create a new main branch of the VF'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then: 'the branch is created'
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == 'Functional Test VersionedFolder Complex'
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion
        String targetId = responseBody().id

        when: 'logged in as editor we create a new branch of the VF'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: 'source'])

        then: 'the branch is created'
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == 'Functional Test VersionedFolder Complex'
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'source'
        !responseBody().modelVersion
        String sourceId = responseBody().id

        when: 'getting the data models inside the main folder'
        GET("folders/$targetId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String targetDataModelId = responseBody().items[0].id

        when: 'getting the data models inside the branched folder'
        GET("folders/$sourceId/dataModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String sourceDataModelId = responseBody().items[0].id

        when: 'getting the code sets inside the branched folder'
        GET("folders/$sourceId/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String sourceCodeSetId = responseBody().items[0].id

        when: 'getting the code sets inside the main folder'
        GET("folders/$targetId/codeSets", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String targetCodeSetId = responseBody().items[0].id

        when: 'get the dataTypes on the branched Data Model'
        GET("dataModels/$sourceDataModelId/dataTypes", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        String modelDataTypeId = responseBody().items.find{it.label == 'Functional Test Model Data Type'}.id

        when: 'Update the model data type in the source branch to point at the CodeSet rather than Terminology'
        PUT("dataModels/$sourceDataModelId/dataTypes/$modelDataTypeId", [modelResourceDomainType: 'CodeSet', modelResourceId: sourceCodeSetId], MAP_ARG, true)

        then: 'The response is updated'
        verifyResponse(OK, response)

        when: 'Get the diffs between the branch and main'
        GET("$sourceId/mergeDiff/$targetId")

        then:
        verifyResponse(OK, response)
        def diffs = responseBody().diffs

        when: 'Merge the diffs between the branch and main'
        PUT("$sourceId/mergeInto/$targetId", [
            patch:
                [
                    targetId: targetId,
                    sourceId: sourceId,
                    label   : "Functional Test Model",
                    count   : diffs.size(),
                    patches : diffs
                ]
        ])

        then: 'The response is OK'
        verifyResponse(OK, response)

        when: 'get the dataTypes on the main branch Data Model'
        GET("dataModels/$targetDataModelId/dataTypes", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].modelResourceDomainType == 'CodeSet'
        responseBody().items[0].modelResourceId == targetCodeSetId


        cleanup:
        cleanupIds(id, sourceId, targetId)
    }



    void 'MP01 : test model permissions'() {
        given:
        TestMergeData mergeData = builder.buildComplexModelsForMerging()

        when:
        loginEditor()
        GET("dataModels/${mergeData.sourceMap.dataModel.dataModelId}", MAP_ARG, true)

        then: 'no option to merge into'
        responseBody().availableActions.sort() == [
            'show',
            'comment',
            'softDelete',
            'delete',
            'save',
            'update',
            'editDescription'
        ].sort()

        when:
        GET("dataModels/${mergeData.targetMap.dataModel.dataModelId}", MAP_ARG, true)

        then: 'no option to merge into or finalise'
        responseBody().availableActions.sort() == [
            'show',
            'comment',
            'softDelete',
            'delete',
            'save',
            'update',
            'editDescription'
        ].sort()

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    Map<String, String> buildModelVersionTree() {
        /*
                                                   /- anotherFork
      v1 --------------------------- v2 -- v3  -- v4 --------------- v5 --- main
        \\_ newBranch (v1)                  \_ testBranch (v3)          \__ anotherBranch (v5)
         \_ fork ---- main                                               \_ interestingBranch (v5)
      */
        // V1
        Map data = getValidFinalisedIdWithContent()
        String v1 = data.id
        loginEditor()
        // Fork and finalise fork
        PUT("$v1/newForkModel", [label: "Functional Test Fork ${modelType}" as String])
        verifyResponse CREATED, response
        String fork = responseBody().id
        PUT("$fork/finalise", [versionChangeType: VersionChangeType.MINOR])
        verifyResponse OK, response
        // Fork main branch
        PUT("$fork/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String forkMain = responseBody().id
        // V2 main branch
        PUT("$v1/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v2 = responseBody().id
        // newBranch from v1 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        PUT("$v1/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranch = responseBody().id
        // Finalise the main branch to v2
        PUT("$v2/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response
        // V3 main branch
        PUT("$v2/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v3 = responseBody().id
        // Finalise the main branch to v3
        PUT("$v3/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response
        // V4 main branch
        PUT("$v3/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v4 = responseBody().id
        // testBranch from v3 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        PUT("$v3/newBranchModelVersion", [branchName: 'testBranch'])
        verifyResponse CREATED, response
        String testBranch = responseBody().id
        // Finalise main branch to v4
        PUT("$v4/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response
        // Fork from v4
        PUT("$v4/newForkModel", [label: "Functional Test AnotherFork ${modelType}" as String])
        verifyResponse CREATED, response
        String anotherFork = responseBody().id
        // V5 and finalise
        PUT("$v4/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v5 = responseBody().id
        PUT("$v5/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response
        // Main branch
        PUT("$v5/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String main = responseBody().id
        // Another branch
        PUT("$v5/newBranchModelVersion", [branchName: 'anotherBranch'])
        verifyResponse CREATED, response
        String anotherBranch = responseBody().id
        // Interesting branch
        PUT("$v5/newBranchModelVersion", [branchName: 'interestingBranch'])
        verifyResponse CREATED, response
        String interestingBranch = responseBody().id
        logout()
        [v1       : v1, v2: v2, v3: v3, v4: v4, v5: v5,
         newBranch: newBranch, testBranch: testBranch, main: main, anotherBranch: anotherBranch, interestingBranch: interestingBranch,
         fork     : fork, anotherFork: anotherFork, forkMain: forkMain
        ]
    }

    void cleanupModelVersionTree(Map<String, String> data) {
        if (!data) return
        data.each { k, v ->
            removeValidIdObjectUsingTransaction(v)
        }
        cleanUpRoles(data.values())
    }

    List<String> getFinalisedEditorAvailableActions() {
        [
            'show',
            'createNewVersions',
            'newForkModel',
            'comment',
            'newModelVersion',
            'finalisedEditActions',
            'newDocumentationVersion',
            'newBranchModelVersion',
            'softDelete',
            'delete'
        ].sort()
    }

    Map<String, String> getValidFinalisedIdWithContent() {
        Map data = getValidIdWithContent()
        loginEditor()
        PUT("$data.id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
        data
    }

    Map<String, String> getValidIdWithContent() {
        String id = getValidId()
        loginEditor()

        POST("folders/$id/dataModels", [
            label: 'Functional Test DataModel 1'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String dataModel1Id = responseBody().id

        POST("folders/$id/dataModels", [
            label: 'Functional Test DataModel 2'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String dataModel2Id = responseBody().id
        logout()
        [id          : id,
         dataModel1Id: dataModel1Id,
         dataModel2Id: dataModel2Id
        ]
    }

    String getModelType() {
        'VersionedFolder'
    }

    void cleanupIds(String... ids) {
        loginEditor()
        ids.each { id ->
            DELETE("$id?permanent=true")
            response.status() in [NO_CONTENT, NOT_FOUND]
        }
        cleanUpRoles(ids)
    }

    String getExpectedModelTreeVersionString(Map data) {
        """[
  {
    "id": "${data.v1}",
    "label": "Functional Test ${modelType} 3",
    "branch": null,
    "modelVersion" : "1.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
     {
        "id": "${data.fork}",
        "description": "New Fork Of"
      },
      {
        "id": "${data.v2}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.newBranch}",
        "description": "New Model Version Of"
      }
      
    ]
  },
  {
    "id": "${data.newBranch}",
    "label": "Functional Test ${modelType} 3",
    "branch": "newBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
  {
    "id": "${data.fork}",
    "label": "Functional Test Fork ${modelType}",
    "branch": null,
    "modelVersion": "0.1.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": true,
    "targets": [
      
    ]
  },
  {
    "id": "${data.v2}",
    "label": "Functional Test ${modelType} 3",
    "branch": null,
    "modelVersion" : "2.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      {
        "id": "${data.v3}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.v3}",
    "label": "Functional Test ${modelType} 3",
    "branch": null,
    "modelVersion" : "3.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      {
        "id": "${data.v4}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.testBranch}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.testBranch}",
    "label": "Functional Test ${modelType} 3",
    "branch": "testBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
  {
   "id": "${data.v4}",
    "label": "Functional Test ${modelType} 3",
    "branch": null,
    "modelVersion" : "4.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      {
        "id": "${data.anotherFork}",
        "description": "New Fork Of"
      },
      {
        "id": "${data.v5}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.anotherFork}",
    "label": "Functional Test AnotherFork ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": true,
    "targets": [
      
    ]
  },
  {
    "id": "${data.v5}",
    "label": "Functional Test ${modelType} 3",
    "branch": null,
    "modelVersion" : "5.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
    {
        "id": "${data.main}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.anotherBranch}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.interestingBranch}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.main}",
    "label": "Functional Test ${modelType} 3",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
    {
    "id": "${data.anotherBranch}",
    "label": "Functional Test ${modelType} 3",
    "branch": "anotherBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
  {
    "id": "${data.interestingBranch}",
    "label": "Functional Test ${modelType} 3",
    "branch": "interestingBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  }
]"""
    }

    String getExpectedSimpleModelTreeVersionString(Map data) {
        """[
  {
    "id": "${data.v1}",
    "branch": null,
    "modelVersion" : "1.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V1.0.0"
  },
  {
    "id": "${data.newBranch}",
    "branch": "newBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "newBranch (V1.0.0)"
  },
  {
    "id": "${data.v2}",
    "branch": null,
    "modelVersion" : "2.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V2.0.0"
  },
  {
    "id": "${data.v3}",
    "branch": null,
    "modelVersion" : "3.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V3.0.0"
  },
  {
    "id": "${data.testBranch}",
    "branch": "testBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "testBranch (V3.0.0)"
  },
  {
    "id": "${data.v4}",
    "branch": null,
    "modelVersion" : "4.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V4.0.0"
  },
  {
    "id": "${data.v5}",
    "branch": null,
    "modelVersion" : "5.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V5.0.0"
  },
  {
    "id": "${data.main}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V5.0.0)"
  },
    {
    "id": "${data.anotherBranch}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "anotherBranch (V5.0.0)"
  },
  {
    "id": "${data.interestingBranch}",
    "branch": "interestingBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
   "displayName": "interestingBranch (V5.0.0)"
  }
]"""
    }

    String getIdFromPath(String rootResourceId, String path) {
        GET("$rootResourceId/path/${URLEncoder.encode(path, Charset.defaultCharset())}")
        verifyResponse OK, response
        assert responseBody().id
        responseBody().id
    }

    String getExpectedComplexDiffJson() {
        Files.readString(Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'versionedFolders', 'complexDiff.json'))
    }

    String getExpectedMergeDiffJson() {
        VersionedFolderMergeBuilder.getExpectedMergeDiffJson()
    }
}
