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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: folder
 *  | POST   | /api/folders       | Action: save   |
 *  | GET    | /api/folders       | Action: index  |
 *  | DELETE | /api/folders/${id} | Action: delete |
 *  | PUT    | /api/folders/${id} | Action: update |
 *  | GET    | /api/folders/${id} | Action: show   |
 *
 *  | DELETE | /api/folders/${folderId}/permanent  | Action: delete |
 *
 *  |   GET    | /api/folders/${folderId}/search   | Action: search
 *  |   POST   | /api/folders/${folderId}/search   | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class FolderFunctionalSpec extends ResourceFunctionalSpec<Folder> {

    @OnceBefore
    @Rollback
    def check() {
        assert Folder.count() == 0
    }

    @Override
    String getResourcePath() {
        'folders'
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
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "Folder",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["update","delete","show"]
}'''
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
        Folder.count() == 1

        when: 'The save action is executed with the same valid data'
        createdIds << response.body().id
        POST('', validJson)

        then: 'The response is correct as cannot have 2 folders with the same name'
        response.status == HttpStatus.UNPROCESSABLE_ENTITY
        Folder.count() == 1

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
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["update","delete","show"]
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

        when: 'Trying to get the folder'
        GET(id)

        then:
        response.status() == HttpStatus.NOT_FOUND
    }

    void 'Test the permanent delete action correctly deletes an instance with folder inside'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'The save action is executed with valid data'
        String id = responseBody().id
        POST("$id/folders", validJson)

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
    }

    void 'test searching for label "test" in empty folder'() {
        given:
        def id = createNewItem(validJson)
        def term = 'test'

        when:
        GET("${id}/search?searchTerm=${term}")

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test searching for label "test" in empty folder using POST'() {
        given:
        def id = createNewItem(validJson)
        def term = 'test'

        when:
        POST("${id}/search", [searchTerm: term])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
