package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
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

    @OnceBefore
    @Rollback
    def checkAndSetupData() {
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
  "availableActions": ["delete", "show", "update"],
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder"
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
  "availableActions": ["delete", "show", "update"],
  "id": "${json-unit.matches:id}",
  "label": "Parent Functional Test Folder"
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
  "availableActions": ["delete", "show", "update"],
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Folder"
}''')

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    @Rollback
    void 'Test the permanent delete action correctly deletes an instance'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the delete action is executed on an unknown instance'
        String id = response.body().id
        DELETE("${baseUrl}folders/${UUID.randomUUID()}?permanent=true")

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the delete action is executed on an existing instance'
        DELETE("${baseUrl}folders/${id}?permanent=true")

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT
        !Folder.get(id)
    }
}
