/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpStatus
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController* Controller: folder
 *  | POST   | /api/folders/${folderId}/folders       | Action: save   |
 *  | GET    | /api/folders/${folderId}/folders       | Action: index  |
 *  | DELETE | /api/folders/${folderId}/folders/${id} | Action: delete |
 *  | PUT    | /api/folders/${folderId}/folders/${id} | Action: update |
 *  | GET    | /api/folders/${folderId}/folders/${id} | Action: show   |
 */
@Integration
@Slf4j
class NestedFolderFunctionalSpec extends ResourceFunctionalSpec<Folder> {

    @Shared
    UUID parentFolderId

    @RunOnce
    @Rollback
    def setup() {
        log.debug('Check and setup test data')
        Folder parent = new Folder(label: 'Parent Functional Test Folder', createdBy: 'functionalTest@test.com').save(flush: true)
        parentFolderId = parent.id
        assert parentFolderId
    }

    @Transactional
    def cleanupSpec() {
        Folder.get(parentFolderId).delete(flush: true)
        assert Folder.count() == 0
    }

    @Override
    String getResourcePath() {
        "folders/${parentFolderId}/folders"
    }

    @Override
    Map getValidJson() {
        [label: 'Functional Test Folder']
    }

    @Override
    Map getInvalidJson() {
        [label: null]
    }

    @Override
    String getDeleteEndpoint(String id) {
        "${super.getDeleteEndpoint(id)}?permanent=true"
    }

    @Override
    boolean hasDefaultCreation() {
        true
    }

    @Override
    boolean isNestedTest() {
        true
    }

    @Override
    int getExpectedInitialResourceCount() {
        parentFolderId ? 1 : 0
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "Folder",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder",
  "path": "fo:Parent Functional Test Folder|fo:Functional Test Folder",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["update","delete","show"]
}'''
    }

    void 'Test the parent show action correctly renders an instance'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the show action is called to retrieve a resource'
        String id = response.body().id
        GET("${baseUrl}folders/${parentFolderId}", Argument.of(String))

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": true,
  "domainType": "Folder",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["update","delete","show"],
  "id": "${json-unit.matches:id}",
  "label": "Parent Functional Test Folder",
  "path": "fo:Parent Functional Test Folder"
}''')

        cleanup:
        DELETE(getDeleteEndpoint(id))
    }

    @Rollback
    void 'Test the save action fails when using the same label persists an instance'() {
        given:
        List<String> createdIds = []

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id
        Folder.count() == 2

        when: 'The save action is executed with the same valid data'
        createdIds << response.body().id
        POST('', validJson)

        then: 'The response is correct as cannot have 2 folders with the same name'
        response.status == HttpStatus.UNPROCESSABLE_ENTITY
        Folder.count() == 2

        cleanup:
        createdIds.each {id ->
            DELETE(getDeleteEndpoint(id))
            assert response.status() == HttpStatus.NO_CONTENT
        }
    }

    void 'Test the soft delete action correctly deletes an instance'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the delete action is executed on an unknown instance'
        String id = response.body().id
        DELETE(UUID.randomUUID().toString())

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the soft delete action is executed on an existing instance'
        DELETE("$id", Argument.of(String))

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "Folder",
  "deleted": true,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["update","delete","show"],
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder",
  "path": "fo:Parent Functional Test Folder|fo:Functional Test Folder"
}''')

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'Test the permanent delete action correctly deletes an instance'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the delete action is executed on an unknown instance'
        String id = response.body().id
        DELETE("${UUID.randomUUID()}?permanent=true")

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the delete action is executed on an existing instance'
        DELETE("${id}?permanent=true")

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT
    }

    void 'test can retrieve by valid path'() {
        given:
        def pathParent = 'Parent%20Functional%20Test%20Folder'
        def pathNested = 'Functional%20Test%20Folder'

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        String id = response.body().id

        when: 'Retrieve by path of parent folder'
        GET("folders/path/fo:${pathParent}", MAP_ARG, true)

        then: 'The parent folder is found'
        verifyResponse HttpStatus.OK, response
        responseBody().label == 'Parent Functional Test Folder'

        when: 'Retrieve nested folder by path of nested folder only'
        GET("folders/path/fo:${pathNested}", MAP_ARG, true)

        then: 'The nested folder is not found'
        verifyResponse HttpStatus.NOT_FOUND, response

        when: 'Retrieve nested folder by path of nested folder and parent folder'
        GET("folders/path/fo:${pathParent}%7cfo:${pathNested}", MAP_ARG, true)

        then: 'The nested folder is found'
        verifyResponse HttpStatus.OK, response
        responseBody().label == 'Functional Test Folder'

        when: 'Retrieve nested folder by path of nested folder and ID of parent folder'
        GET("folders/${parentFolderId}/path/fo:${pathNested}", MAP_ARG, true)

        then: 'The nested folder is found'
        verifyResponse HttpStatus.OK, response
        responseBody().label == 'Functional Test Folder'

        cleanup:
        DELETE(getDeleteEndpoint(id))
    }
}
