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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

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
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class FolderFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

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
        assert response.body().availableActions == ['show', 'update', 'delete']
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
  "availableActions": ["update","delete","show"]
}'''
    }

    /*TODO searching

        void 'test searching for label "test" in the test folder'() {
            given:
            def term = 'test'

            when: 'not logged in'
            RestResponse response = restGet("${testFolder.id}/search?search={search}", [search: term])

            then:
            verifyUnauthorised(response)

            when: 'logged in as normal user'
            loginEditor()
            response = restGet("${testFolder.id}/search?search={search}", [search: term])

            then:
            verifyResponse OK, response, '''{
      "count": 2,
      "items": [
        {
          "domainType": "DataModel",
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        },
        {
          "domainType": "DataModel",
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel"
        }
      ]
    }'''
        }

        void 'test searching for label "test" in empty folder'() {
            given:
            def id = createNewItem()
            def term = 'test'

            when: 'not logged in'
            RestResponse response = restGet("${id}/search?search={search}", [search: term])

            then:
            verifyUnauthorised(response)

            when: 'logged in as normal user'
            loginEditor()
            response = restGet("${id}/search?search={search}", [search: term])

            then:
            verifyResponse OK, response, '''{"count": 0,"items": []}'''
        }
        */
}