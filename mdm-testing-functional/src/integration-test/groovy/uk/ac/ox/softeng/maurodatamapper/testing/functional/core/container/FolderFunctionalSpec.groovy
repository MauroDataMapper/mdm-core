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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
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
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withSoftDeleteByDefault()
            .withDefaultCreation()
            .whereEditorsCannotChangePermissions()
            .whereAnonymousUsers {
                canIndex()
            }
            .whereAuthenticatedUsers {
                canCreate()
                canIndex()
                cannotSee()
            }
            .whereReaders {
                canCreate()
                canSee()
                canIndex()
            }
            .whereReviewers {
                canCreate()
                canSee()
                canIndex()
            }
            .whereAuthors {
                canCreate()
                canSee()
                canIndex()
                cannotEditDescription()
                cannotUpdate()
            }
            .whereEditors {
                cannotDelete()
                cannotEditDescription()
                cannotUpdate()
            }
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'softDelete', 'update')
            .whereEditorsCanAction('show')
    }

    @Override
    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert response.body().label == count ? "New Folder (${count})".toString() : 'New Folder'
        assert response.body().availableActions == expectations.containerAdminAvailableActions
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
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test VersionedFolder",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "VersionedFolder",
      "hasChildFolders": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Folder",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "Folder",
      "hasChildFolders": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test VersionedFolder 2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "VersionedFolder",
      "hasChildFolders": false
    },
    { "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "hasChildFolders": true,
      "domainType": "Folder",
      "label": "Parent Functional Test Folder"
    }
  ]
}'''
    }

    @Override
    String getContainerAdminIndexJson() {
        '''{
  "count": 5,
  "items": [
    { "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "id": "${json-unit.matches:id}",
      "hasChildFolders": true,
      "domainType": "Folder",
      "label": "Parent Functional Test Folder"
    },
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
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test VersionedFolder",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "VersionedFolder",
      "hasChildFolders": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test VersionedFolder 2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "VersionedFolder",
      "hasChildFolders": false
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
  "availableActions": ["show"]
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
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
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

    void 'S05 : test searching for "simple" in the test folder using POST with pagination and offset'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST("${getTestFolderId()}/search", [searchTerm: term, sort: 'label', max: 2, offset: 5], STRING_ARG)

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

    void 'Test the permanent delete action correctly deletes an instance with folder inside'() {
        when: 'Creating a top folder'
        String id = getValidId()

        loginCreator()
        POST("$id/folders", validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when: 'When the delete action is executed on an existing instance'
        loginContainerAdmin()
        String subFolderId = responseBody().id
        DELETE("${id}?permanent=true")

        then: 'The response is correct'
        response.status == NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == NOT_FOUND

        cleanup:
        //Shouldn't be necessary to cleanup roles but log files indicates that occasionally they are left over
        cleanUpRoles(subFolderId)
        cleanUpRoles(id)
        cleanupUserGroups()
    }

    void 'Test the permanent delete action correctly deletes an instance with folder and datamodel inside'() {
        when: 'Creating a top folder'
        String id = getValidId()

        loginCreator()
        POST("$id/folders", validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when: 'The save action is executed with valid data for a datamodel'
        String subFolderId = responseBody().id
        POST("$id/dataModels", [
            label: 'Functional Test DataModel'
        ])

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when: 'When the delete action is executed on an existing instance'
        loginContainerAdmin()
        DELETE("${id}?permanent=true")

        then: 'The response is correct'
        response.status == NO_CONTENT

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == NOT_FOUND

        cleanup:
        //Shouldn't be necessary to cleanup roles but log files indicates that occasionally they are left over
        cleanUpRoles(subFolderId)
        cleanUpRoles(id)
        cleanupUserGroups()
    }

    void 'Test create folder with one user group specified'() {
        when: 'logged in as reader user'
        loginCreator()
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
      "availableActions": [
        "comment",
        "delete",
        "editDescription",
        "save",
        "show",
        "softDelete",
        "update"
      ],
      "createdBy": "creator@test.com",
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
        loginCreator()
        POST("", getValidJsonWithTwoGroups())

        then:
        response.status == CREATED
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
      "availableActions": [
        "comment",
        "delete",
        "editDescription",
        "save",
        "show",
        "softDelete",
        "update"
      ],
      "createdBy": "creator@test.com",
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
      "availableActions": [
        "comment",
        "delete",
        "editDescription",
        "save",
        "show",
        "softDelete",
        "update"
      ],
      "createdBy": "creator@test.com",
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
        loginCreator()
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

    void '#prefix-14 : test moving folder with #name role into admin folder [not allowed] (as #name)'() {
        given:
        String id = getValidId()

        when:
        login(name)
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        if (canUpdate) verifyNotFound response, getTestFolder2Id()
        else if (canRead) verifyForbidden response
        else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead | canUpdate
        'LO'   | null            | false   | false
        'NA'   | 'Authenticated' | false   | false
        'RE'   | 'Reader'        | true    | false
        'RV'   | 'Reviewer'      | true    | false
        'AU'   | 'Author'        | true    | false
        'ED'   | 'Editor'        | true    | expectations.editorsCan('update')
    }

    void '#prefix-14 : test moving folder with admin role into admin folder [allowed] (as #name)'() {
        given:
        String id = getValidId()

        when: 'logged in'
        login(name)
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyResponse OK, response

        and:
        getFolderParentFolderId(id) == getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void '#prefix-15 : test moving folder with #name role into #name folder (as #name)'() {
        given:
        String id = getValidId()
        loginCreator()
        POST(getSavePath(), [
            label: 'Functional Test Folder 4'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        String otherId = response.body().id
        addAccessShares(otherId)
        logout()

        when:
        login(name)
        PUT("$id/folder/${otherId}", [:])

        then:
        if (canUpdate) {
            verifyResponse OK, response
            assert getFolderParentFolderId(id) == otherId
        } else if (canRead) verifyForbidden response
        else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(otherId)

        where:
        prefix | name             | canRead | canUpdate
        'LO'   | null             | false   | false
        'NA'   | 'Authenticated'  | false   | false
        'RE'   | 'Reader'         | true    | false
        'RV'   | 'Reviewer'       | true    | false
        'AU'   | 'Author'         | true    | false
        'ED'   | 'Editor'         | true    | expectations.editorsCan('update')
        'CA'   | 'ContainerAdmin' | true    | true
        'AD'   | 'Admin'          | true    | true
    }

    void 'P01 : test changing public status with public model access maintained'() {

        given: 'create folder with publically readable DM'
        String id = getValidId()
        loginCreator()
        POST("$id/dataModels", [
            label: 'Functional Test DataModel'
        ])
        verifyResponse(CREATED, response)
        String dmId = responseBody().id
        POST("$id/dataModels", [
            label: 'Functional Test DataModel 2'
        ])
        verifyResponse(CREATED, response)
        String dmId2 = responseBody().id
        PUT("dataModels/${dmId}/readByEveryone", [:], MAP_ARG, true)
        verifyResponse(OK, response)

        when: 'getting the folder its not public but its readable due to the DM'
        // it has to be readable as it will be "clickable" in the tree
        logout()
        GET("$id")

        then:
        verifyResponse(OK, response)

        when: 'getting the datamodel it is public'
        GET("dataModels/${dmId}", MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when: 'getting the datamodel 2 it is not public'
        GET("dataModels/${dmId2}", MAP_ARG, true)

        then:
        verifyResponse(NOT_FOUND, response)

        when: 'setting folder public'
        loginContainerAdmin()
        PUT("${id}/readByEveryone", [:])

        then:
        verifyResponse(OK, response)
        responseBody().readableByEveryone

        when: 'getting the folder it is public'
        logout()
        GET("$id")

        then:
        verifyResponse(OK, response)

        when: 'getting the datamodel it is public'
        GET("dataModels/${dmId}", MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when: 'getting the datamodel 2 it is public due to the folder being public'
        GET("dataModels/${dmId2}", MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when: 'setting folder not public'
        loginContainerAdmin()
        DELETE("${id}/readByEveryone", [:])

        then:
        verifyResponse(OK, response)
        !responseBody().readableByEveryone

        when: 'getting the folder its not public but its readable due to the DM'
        // it has to be readable as it will be "clickable" in the tree
        logout()
        GET("$id")

        then:
        verifyResponse(OK, response)

        when: 'getting the datamodel it is public'
        GET("dataModels/${dmId}", MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when: 'getting the datamodel 2 it is not public'
        GET("dataModels/${dmId2}", MAP_ARG, true)

        then:
        verifyResponse(NOT_FOUND, response)

        when: 'datamodel is deleted the folder is no longer readable'
        loginAdmin()
        DELETE("dataModels/${dmId}?permanent=true", MAP_ARG, true)

        then:
        verifyResponse(NO_CONTENT, response)

        when:
        logout()
        GET("$id")

        then:
        verifyResponse(NOT_FOUND, response)

        when: 'getting the datamodel 2 it is not public'
        GET("dataModels/${dmId2}", MAP_ARG, true)

        then:
        verifyResponse(NOT_FOUND, response)

        cleanup:
        loginAdmin()
        DELETE("dataModels/${dmId2}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        removeValidIdObject(id)
    }

    void 'FE-#prefix-01 : test export a single Folder (as #name)'() {
        when:
        login(name)
        GET("${testFolderId}/export/uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter/FolderJsonExporterService/1.0", canRead ? STRING_ARG : MAP_ARG)

        then:
        if (!canRead) verifyNotFound(response, testFolderId)
        else {
            verifyJsonResponse(OK, '''{
  "folder": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Folder",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "domainType": "Folder"
  },
  "exportMetadata": {
    "exportedBy": "${json-unit.any-string}",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter",
      "name": "FolderJsonExporterService",
      "version": "1.0"
    }
  }
}''')
        }

        where:
        prefix | name             | canRead
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | true
        'RV'   | 'Reviewer'       | true
        'AU'   | 'Author'         | true
        'ED'   | 'Editor'         | true
        'CA'   | 'ContainerAdmin' | true
        'AD'   | 'Admin'          | true
    }

    void 'FE-#prefix-02 : test export a Folder with child Folders (as #name)'() {
        given:
        List<String> ids = []

        when:
        loginCreator()
        POST("${testFolderId}/folders", [label: 'Functional Test Folder 2'])
        ids << response.body().id
        POST("${testFolderId}/folders", [label: 'Functional Test Folder 3'])
        ids << response.body().id
        POST("${ids[1]}/folders", [label: 'Functional Test Folder 4'])
        ids << response.body().id

        login(name)
        GET("${testFolderId}/export/uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter/FolderJsonExporterService/1.0", canRead ? STRING_ARG : MAP_ARG)

        then:
        if (!canRead) verifyNotFound(response, testFolderId)
        else {
            verifyJsonResponse(OK, '''{
  "folder": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Folder",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "domainType": "Folder",
    "childFolders": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Folder 2",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "domainType": "Folder"
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Folder 3",
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "domainType": "Folder",
        "childFolders": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Folder 4",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "domainType": "Folder"
          }
        ]
      }
    ]
  },
  "exportMetadata": {
    "exportedBy": "${json-unit.any-string}",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter",
      "name": "FolderJsonExporterService",
      "version": "1.0"
    }
  }
}''')
        }

        cleanup:
        ids.reverseEach { removeValidIdObject(it) }
        // Should not be necessary to clean up roles but log files indicate that occasionally they are left over
        ids.each { cleanUpRoles(it) }
        cleanupUserGroups()

        where:
        prefix | name             | canRead
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | true
        'RV'   | 'Reviewer'       | true
        'AU'   | 'Author'         | true
        'ED'   | 'Editor'         | true
        'CA'   | 'ContainerAdmin' | true
        'AD'   | 'Admin'          | true
    }
}
