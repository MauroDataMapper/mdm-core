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
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.PendingFeature

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

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


    @Autowired(required = false)
    List<ModelService> modelServices

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
            label: 'Functional Test Folder 3'
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
            label      : 'Functional Test Folder 4',
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
            label      : 'Functional Test Folder 5',
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
            label      : 'Functional Test Folder 6',
            description: 'Description of Functional Test Folder 6',
            groups     : [
                [groupI: editorsUserGroup.id, groupRoleI: getEditorGroupRoleId()]
            ]
        ]
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[VersionedFolder:Functional Test Folder 3] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[VersionedFolder:Functional Test Folder 3] changed properties \[description]/
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

    @Transactional
    @Override
    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using transaction', id)
        Folder folder = Folder.get(id)
        folder.delete(flush: true)
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "VersionedFolder",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder 3",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["comment","delete","editDescription","save","show","softDelete","update"],
  "branchName": "main",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
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
        response.status == HttpStatus.NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == HttpStatus.NOT_FOUND

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
        response.status == HttpStatus.NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == HttpStatus.NOT_FOUND

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
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update"],
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
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update"],
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
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update"],
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

    @PendingFeature
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
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update"],
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

    void 'F01 : Test finalising Model (as reader)'() {
        given:
        Map data = getValidIdWithContent()

        when: 'logged in as reader'
        loginReader()
        PUT("$data.id/finalise", ["version": "3.9.0"])

        then:
        verifyForbidden response

        cleanup:
        cleanupIdWithContent(data)
    }

    void 'F02 : finalisation endpoint for Versioned Folder (as editor)'() {
        given:
        Map data = getValidIdWithContent()

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
        cleanupIdWithContent(data)
    }

    void 'F03 : Test finalising Model (as editor) with a versionTag'() {
        given:
        Map data = getValidIdWithContent()

        when: 'logged in as editor'
        loginEditor()
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
        cleanupIdWithContent(data)
    }

    List<String> getFinalisedEditorAvailableActions() {
        [
            'show',
            //            'createNewVersions',
            //            'newForkModel',
            'comment',
            //            'newModelVersion',
            //            'newDocumentationVersion',
            //            'newBranchModelVersion',
            'softDelete',
            'delete'
        ].sort()
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

        [id          : id,
         dataModel1Id: dataModel1Id,
         dataModel2Id: dataModel2Id
        ]
    }

    void cleanupIdWithContent(Map<String, String> data) {
        loginEditor()
        // We use transactions to clear folder which means the DMs don't get deleted so have to do that manually
        DELETE("dataModels/$data.dataModel1Id?permanent=true", MAP_ARG, true)
        verifyResponse HttpStatus.NO_CONTENT, response
        DELETE("dataModels/$data.dataModel2Id?permanent=true", MAP_ARG, true)
        verifyResponse HttpStatus.NO_CONTENT, response
        removeValidIdObject(data.id)
    }
}