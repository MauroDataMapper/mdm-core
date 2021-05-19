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
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 *  Controller: folder
 *  |  DELETE  | /api/folders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  PUT     | /api/folders/${folderId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/folders/${folderId}/readByEveryone       | Action: readByEveryone
 *  |  PUT     | /api/folders/${folderId}/readByEveryone       | Action: readByEveryone
 *  |  POST    | /api/folders        | Action: save
 *  |  GET     | /api/folders        | Action: index
 *  |  DELETE  | /api/folders/${id}  | Action: delete
 *  |  PUT     | /api/folders/${id}  | Action: update
 *  |  GET     | /api/folders/${id}  | Action: show
 *  |  PUT     | /api/folders/${folderId}/folder/${destinationFolderId}      | Action: changeFolder
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class FolderFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Transactional
    String getTestFolder2Id() {
        Folder.findByLabel('Functional Test Folder 2').id.toString()
    }

    @Override
    String getResourcePath() {
        'folders'
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

    @Transactional
    String getFolderParentFolderId(String id) {
        Folder.get(id).parentFolder.id.toString()
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Folder:Functional Test Folder 3] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Folder:Functional Test Folder 3] changed properties \[description]/
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
        ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'delete', 'canAddRule']
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
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
    int getExpectedCountOfGroupsWithAccess() {
        2
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
  "count": 1,
  "items": [
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "hasChildFolders": false,
      "domainType": "Folder",
      "label": "Functional Test Folder"
    }
  ]
}'''
    }

    @Override
    String getAdminIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "hasChildFolders": false,
      "domainType": "Folder",
      "label": "Functional Test Folder"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "hasChildFolders": false,
      "domainType": "Folder",
      "label": "Functional Test Folder 2"
    }
  ]
}
'''
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "Folder",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder 3",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["comment","delete","editDescription","save","show","softDelete","update","canAddRule"]
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

    void 'S02 : test searching for "simple" in the test folder'() {
        given:
        String term = 'simple'

        when: 'not logged in'
        GET("${getTestFolderId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getTestFolderId())

        when: 'logged in as authenticated'
        loginAuthenticated()
        GET("${getTestFolderId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getTestFolderId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getTestFolderId()}/search?searchTerm=${term}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT01: Simple Test Term 01",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT02: Simple Test Term 02",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    },    
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Simple Test Terminology"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
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
  ]
}'''
    }

    void 'S03 : test searching for "simple" in the test folder using POST'() {
        given:
        String term = 'simple'

        when: 'not logged in'
        POST("${getTestFolderId()}/search", [searchTerm: term])

        then:
        verifyNotFound(response, getTestFolderId())

        when: 'logged in as authenticated'
        loginAuthenticated()
        POST("${getTestFolderId()}/search", [searchTerm: term])

        then:
        verifyNotFound(response, getTestFolderId())

        when: 'logged in as reader user'
        loginReader()
        POST("${getTestFolderId()}/search", [searchTerm: term, sort: 'label'], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT01: Simple Test Term 01",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT02: Simple Test Term 02",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    },    
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Simple Test Terminology"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
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
  ]
}'''
    }

    void 'S03 : test searching for "simple" in the test folder limited to DataModel'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST("${getTestFolderId()}/search", [searchTerm: term, sort: 'label', domainTypes: ['DataModel']])

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().label == BootstrapModels.SIMPLE_DATAMODEL_NAME
        responseBody().items.first().domainType == 'DataModel'
    }

    void 'S04 : test searching for "simple" in the test folder using POST with pagination'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST("${getTestFolderId()}/search", [searchTerm: term, sort: 'label', max: 2, offset: 0], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT01: Simple Test Term 01",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT02: Simple Test Term 02",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    void 'S05 : test searching for "simple" in the test folder using POST with pagination and offset'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST("${getTestFolderId()}/search", [searchTerm: term, sort: 'label', max: 2, offset: 2], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    }
  ]
}'''
    }


    void 'Test the permanent delete action correctly deletes an instance with folder inside'() {
        when: 'Creating a top folder'
        String id = getValidId()

        loginEditor()
        POST("$id/folders", validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the delete action is executed on an existing instance'
        String subFolderId = responseBody().id
        DELETE("${id}?permanent=true")

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == HttpStatus.NOT_FOUND

        cleanup:
        //Shouldn't be necessary to cleanup roles but log files indicates that occasionally they are left over
        cleanUpRoles(subFolderId)
        cleanUpRoles(id)
        cleanupUserGroups()
    }

    void 'Test the permanent delete action correctly deletes an instance with folder and datamodel inside'() {
        when: 'Creating a top folder'
        String id = getValidId()

        loginEditor()
        POST("$id/folders", validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'The save action is executed with valid data for a datamodel'
        String subFolderId = responseBody().id
        POST("$id/dataModels", [
            label: 'Functional Test DataModel'
        ])

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
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
        //Shouldn't be necessary to cleanup roles but log files indicates that occasionally they are left over
        cleanUpRoles(subFolderId)
        cleanUpRoles(id)
        cleanupUserGroups()
    }

    void 'Test create folder with one user group specified'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getValidJsonWithOneGroup())

        then:
        response.status == HttpStatus.CREATED
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
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update","canAddRule"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "Folder",
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

    void 'Test create folder with two user groups specified'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getValidJsonWithTwoGroups())

        then:
        response.status == HttpStatus.CREATED
        response.body().id
        String folderId = response.body().id

        when:
        GET("folders/${folderId}/groupRoles/${getEditorGroupRoleId()}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
            "count": 2,
            "items": [
                {
                    "id": "${json-unit.matches:id}",
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update","canAddRule"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "Folder",
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
                    "availableActions": ["comment","delete","editDescription","save","show","softDelete","update","canAddRule"],
                    "createdBy": "reader@test.com",
                    "securableResourceDomainType": "Folder",
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

    void 'Test create folder with one user group invalidly specified'() {
        when: 'logged in as reader user'
        loginReader()
        POST("", getInvalidJsonWithOneGroup())

        then:
        response.status == CREATED
        response.body().id
        String folderId = response.body().id

        when:
        GET("folders/${folderId}/groupRoles/${getEditorGroupRoleId()}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
            "count": 0,
            "items": [                
            ]
        }'''

        cleanup:
        removeValidIdObject(folderId)
    }

    void 'test changing folder (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'test changing folder (as authenticated/no access)'() {
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

    void 'test changing folder (as reader)'() {
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

    void 'test changing folder (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the datamodel but not the folder 2'
        loginEditor()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'test changing folder (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyResponse OK, response

        and:
        getFolderParentFolderId(id) == getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }
}
