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
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

/**
 * <pre>
 * Controller: folder
 *  |  DELETE  | /api/folders/${folderId}/folders/${id}  | Action: delete
 *  |  PUT     | /api/folders/${folderId}/folders/${id}  | Action: update
 *  |  GET     | /api/folders/${folderId}/folders/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/folders  | Action: save
 *  |  GET     | /api/folders/${folderId}/folders  | Action: index
 *  </pre
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class NestedFolderFunctionalSpec extends UserAccessFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Override
    String getResourcePath() {
        "folders/${getTestFolderId()}/folders"
    }

    @Override
    String getEditsPath() {
        'folders'
    }

    @Override
    Map getValidJson() {
        [
            label: 'Nested Functional Test Folder'
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
        ~/\[Folder:Nested Functional Test Folder] added as child of \[Folder:Functional Test Folder]/
    }


    @Override
    Boolean isDisabledNotDeleted() {
        true
    }

    @Override
    Boolean hasDefaultCreation() {
        true
    }

    @Override
    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert response.body().label == count ? "New Folder (${count})".toString() : 'New Folder'
        assert response.body().availableActions == ['show', 'update', 'save', 'softDelete', 'delete']
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
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0,"items": []}'
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "Folder",
  "id": "${json-unit.matches:id}",
  "label": "Nested Functional Test Folder",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["show","update","save","softDelete","delete"]
  }'''
    }


    /**
     * Whilst this is actually tested in the user access tests we ignore the labels being created
     * we need to make sure that folders and subsequent folders are named correctly
     * We test this inside thie test folder.
     *
     void 'test default folder generation'() {given:
     loginEditor()

     when: 'requesting a default folder be created'
     def response = post('')

     then:
     verifyResponse CREATED, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "New Folder",')

     when: 'requesting a second default folder'
     response = post('')

     then:
     verifyResponse CREATED, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "New Folder (1)",')

     when: 'requesting a third default folder'
     def secondId = response.json.id
     response = post('')

     then:
     verifyResponse CREATED, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "New Folder (2)",')

     when: 'renaming the second folder'
     response = put(secondId) {json {label = 'Renamed folder'}}then:
     verifyResponse OK, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "Renamed folder",')

     when: 'requesting a fourth default folder, this should ignore the fact that (1) is gone and still go to (3)'
     response = post('')

     then:
     verifyResponse CREATED, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "New Folder (3)",')

     when: 'renaming the fourth folder'
     response = put("${response.json.id}") {json {label = 'Another folder'}}then:
     verifyResponse OK, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "Another folder",')

     when: 'requesting a fifth default folder, this should go to (3)'
     response = post('')

     then:
     verifyResponse CREATED, response, defaultCreationJson.replaceFirst(/"label": "\$\{json-unit\.ignore}",/, '"label": "New Folder (3)",')}*/

}