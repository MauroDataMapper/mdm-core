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
package uk.ac.ox.softeng.maurodatamapper.test.functional


import grails.gorm.transactions.Transactional
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.datastore.gorm.GormEntity
import spock.lang.Shared

import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 26/11/2019
 */
@Slf4j
abstract class ResourceFunctionalSpec<D extends GormEntity> extends BaseFunctionalSpec {

    @Shared
    Path resourcesPath
    @Shared
    Path xmlResourcesPath

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'json').toAbsolutePath()
        xmlResourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'xml').toAbsolutePath()
    }

    @Transactional
    @OnceBefore
    def checkResourceCount() {
        log.debug('Check resource count is {}', getExpectedInitialResourceCount())
        sessionFactory.currentSession.flush()
        if (getResource().count() != getExpectedInitialResourceCount()) {
            log.error('{} {} resources left over from previous test', [getResource().count(), getResource().simpleName].toArray())
            getResource().deleteAll(getResource().list())
            sessionFactory.currentSession.flush()
            assert getResource().count() == getExpectedInitialResourceCount()
        }
    }

    void cleanUpData(String id = null) {
        if (id) {
            DELETE(getDeleteEndpoint(id))
            assert response.status() == HttpStatus.NO_CONTENT
            sleep(20)
        } else {
            GET('')
            assert response.status() == HttpStatus.OK
            def items = response.body().items
            items.each { i ->
                sleep(20) //TODO Workaround to reduce the risk of StaleObjectException
                DELETE(getDeleteEndpoint(i.id))
                assert response.status() in [HttpStatus.NO_CONTENT, HttpStatus.NOT_FOUND]
                sleep(20)
            }
        }
    }

    byte[] loadTestFile(String filename, String fileType = 'json') {
        Path testFilePath = fileType == 'json' ? resourcesPath.resolve("${filename}.json") : xmlResourcesPath.resolve("${filename}.xml")
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    Class<D> getResource() {
        ParameterizedType parameterizedType = getParamterizedTypeSuperClass(this.getClass())
        Class<D> resourceClass = (Class<D>) parameterizedType?.actualTypeArguments[0]
        if (!GormEntity.isAssignableFrom(resourceClass)) {
            throw new IllegalStateException("Resource Class ${resourceClass.simpleName} does not extend GormEntity")
        }
        resourceClass
    }

    private ParameterizedType getParamterizedTypeSuperClass(def clazz) {
        if (clazz instanceof ParameterizedType) return clazz
        getParamterizedTypeSuperClass(clazz.genericSuperclass)
    }

    String getSavePath() {
        getResourcePath()
    }

    String createNewItem(Map model) {
        POST(getSavePath(), model, MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        response.body().id
    }

    abstract Map getValidJson()

    abstract Map getInvalidJson()

    abstract String getExpectedShowJson()

    Map getValidUpdateJson() {
        getValidJson()
    }

    String getDeleteEndpoint(String id) {
        "${id}"
    }

    boolean hasDefaultCreation() {
        false
    }

    boolean isNestedTest() {
        false
    }

    int getExpectedInitialResourceCount() {
        0
    }

    void verifyR1EmptyIndexResponse() {
        verifyResponse(HttpStatus.OK, response)
        assert response.body() == [count: 0, items: []]
    }

    void verifyR3IndexResponse(String expectedId) {
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 1
        assert response.body().items.size() == 1
        assert response.body().items[0].id == expectedId
    }

    void verifyR4InvalidUpdateResponse() {
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)
    }

    void verifyR4UpdateResponse() {
        verifyResponse(HttpStatus.OK, response)
        assert response.body()
    }

    void verifyR5ShowResponse() {
        verifyJsonResponse(HttpStatus.OK, getExpectedShowJson())
    }

    void 'R1 : Test the empty index action'() {
        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyR1EmptyIndexResponse()

    }

    @Transactional
    void 'R2 : Test the save action correctly persists an instance'() {
        given:
        List<String> createdIds = []

        when: 'The save action is executed with no content'
        log.debug('No content save')
        POST(getSavePath(), [:], MAP_ARG, true)

        then: 'The response is correct'
        if (hasDefaultCreation()) {
            verifyResponse HttpStatus.CREATED, response
            response.body().id
            createdIds << response.body().id
            getResource().count() == nestedTest ? createdIds.size() : createdIds + 1
        } else {
            verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
            response.body().total >= 1
            response.body().errors.size() == response.body().total
        }

        when: 'The save action is executed with invalid data'
        log.debug('Invalid content save')
        POST(getSavePath(), invalidJson, MAP_ARG, true)

        then: 'The response is correct'
        if (hasDefaultCreation()) {
            verifyResponse HttpStatus.CREATED, response
            response.body().id
            createdIds << response.body().id
            getResource().count() == nestedTest ? createdIds.size() : createdIds + 1
        } else {
            verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
            response.body().total >= 1
            response.body().errors.size() == response.body().total
        }

        when: 'The save action is executed with valid data'
        log.debug('Valid content save')
        createNewItem(validJson)

        then: 'The response is correct'
        verifyResponse HttpStatus.CREATED, response
        response.body().id
        createdIds << response.body().id
        getResource().count() == nestedTest ? createdIds.size() : createdIds + 1

        cleanup:
        createdIds.each { id ->
            DELETE(getDeleteEndpoint(id))
            assert response.status() == HttpStatus.NO_CONTENT
        }
    }

    void 'R3 : Test the index action with content'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'List is called'
        String id = response.body().id
        GET('')

        then: 'We now list 1 folder due to public access'
        verifyR3IndexResponse(id)

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R4 : Test the update action correctly updates an instance'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'The update action is called with invalid data'
        String id = response.body().id
        PUT(id, invalidJson)

        then: 'The response is unprocessable entity'
        verifyR4InvalidUpdateResponse()

        when: 'The update action is called with valid data'
        PUT(id, validJson)

        then: 'The response is correct'
        verifyR4UpdateResponse()

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R5 : Test the show action correctly renders an instance'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the show action is called to retrieve a resource'
        String id = response.body().id
        GET(id, STRING_ARG)

        then: 'The response is correct'
        verifyR5ShowResponse()

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R6 : Test the delete action correctly deletes an instance'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the delete action is executed on an unknown instance'
        String id = response.body().id
        DELETE(getDeleteEndpoint(UUID.randomUUID().toString()))

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the delete action is executed on an existing instance'
        DELETE(getDeleteEndpoint(id))

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT
    }
}
