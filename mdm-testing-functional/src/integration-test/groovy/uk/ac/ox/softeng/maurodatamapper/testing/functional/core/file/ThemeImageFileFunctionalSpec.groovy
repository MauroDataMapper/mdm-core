/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.file

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class ThemeImageFileFunctionalSpec extends FunctionalSpec {

    String PROPERTY_KEY = "theme.image";
    String USE_DEFAULT_IMAGE = "USE DEFAULT IMAGE";

    @Shared
    UUID userId

    @Shared
    UUID apiPropertyId

    @Shared
    Path resourcesPath

    @Override
    String getResourcePath() {
        "admin/properties/"
    }

    @RunOnce
    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources').toAbsolutePath()
        assert Files.exists(resourcesPath.resolve('image_data_file.txt'))
        checkResourceCount()
        checkAndSetupData()
    }

    @Transactional
    def checkResourceCount() {
        log.debug('Check resource count is {}', getExpectedInitialResourceCount())
        sessionFactory.currentSession.flush()
        if (UserImageFile.count() != getExpectedInitialResourceCount()) {
            log.error('{} {} resources left over from previous test', [UserImageFile.count(), UserImageFile.simpleName].toArray())
            assert UserImageFile.count() == getExpectedInitialResourceCount()
        }
    }

    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        getOrCreateApiProperties()
        assert CatalogueUser.count() == 10
        assert ApiProperty.count() == 45
        sessionFactory.currentSession.flush()
        userId = CatalogueUser.findByEmailAddress(UnloggedUser.UNLOGGED_EMAIL_ADDRESS).id
        assert userId
        apiPropertyId = ApiProperty.findByKey(PROPERTY_KEY).id
        assert apiPropertyId
    }

    void getOrCreateApiProperties() {
        ApiProperty apiProperty = ApiProperty.findByKey(PROPERTY_KEY)
        if (!apiProperty) {
            apiProperty = new ApiProperty(
                key: PROPERTY_KEY,
                value: USE_DEFAULT_IMAGE,
                lastUpdatedBy: 'user@test.com',
                createdBy: 'user@test.com')
        }

        checkAndSave(apiProperty)

    }


    void createNewItem(String resourceEndPoint, Map model) {
        POST(resourceEndPoint, model)
        verifyResponse(HttpStatus.CREATED, response)
    }

    Map getValidJson() {
        [
            image: Files.readString(resourcesPath.resolve('image_data_file.txt')),
            type : 'image/png'
        ]
    }

    Map getInvalidJson() {
        [
            image: null,
            type : 'image/png'
        ]
    }

    int getExpectedInitialResourceCount() {
        0
    }

    void assertDefaultStatus(UUID apiPropertyId, Boolean defaultExpected) {
        GET("admin/properties/${apiPropertyId.toString()}", MAP_ARG, true)
        if (defaultExpected) {
            assert responseBody().value == USE_DEFAULT_IMAGE
        }
        else {
            assert responseBody().value != USE_DEFAULT_IMAGE
        }
    }

    void 'R1 : Test the show action correctly returns #expected if admin login is #loginAsAdmin and no image has been saved using apiPropertyId'() {
        when: 'When the show action is called to retrieve a resource'
        GET("${apiPropertyId}/image")

        then: 'The response is correct'
        verifyResponse HttpStatus.NOT_FOUND, response
    }

    @Transactional
    void 'R2 : Test the save action correctly persists an instance'() {
        String resourceEndPoint = "${apiPropertyId}/image"
        ApiProperty apiProperty = ApiProperty.findByKey(PROPERTY_KEY)

        when: 'The save action is executed with no content'
        POST(resourceEndPoint, [:])

        then: 'The response is correct'
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
        response.body().total >= 1
        response.body().errors.size() == response.body().total

        assertDefaultStatus(apiProperty.id, true)

        when: 'The save action is executed with invalid data'
        POST(resourceEndPoint, invalidJson)

        then: 'The response is correct'
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
        response.body().total >= 1
        response.body().errors.size() == response.body().total
        assertDefaultStatus(apiProperty.id, true)

        when: 'The save action is executed with valid data'
        createNewItem(resourceEndPoint, validJson)

        then: 'The response is correct'
        responseBody().id
        responseBody().domainType == 'ThemeImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${apiPropertyId}-theme".toString()
        responseBody().userId == null
        !responseBody().fileContents
        assertDefaultStatus(apiProperty.id, false)

        cleanup:
        DELETE(resourceEndPoint)
        assert response.status() == HttpStatus.NO_CONTENT
        assertDefaultStatus(apiProperty.id, true)
    }

    @Transactional
    void 'R3 : Test the update action correctly updates an instance'() {
        String resourceEndPoint = "${apiPropertyId}/image"
        ApiProperty apiProperty = ApiProperty.findByKey(PROPERTY_KEY)

        given: 'The save action is executed with valid data'
        createNewItem(resourceEndPoint, validJson)

        when: 'The update action is called with invalid data'
        PUT(resourceEndPoint, invalidJson)

        then: 'The response is unprocessable entity'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'The update action is called with valid data'
        PUT(resourceEndPoint, validJson)

        then: 'The response is correct'
        responseBody().id
        responseBody().domainType == 'ThemeImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${apiPropertyId}-theme".toString()
        responseBody().userId == null
        !responseBody().fileContents

        cleanup:
        DELETE(resourceEndPoint)
        assert response.status() == HttpStatus.NO_CONTENT
        assertDefaultStatus(apiProperty.id, true)
    }

    @Transactional
    void 'R4 : Test the show action correctly a saved image'() {
        String resourceEndPoint = "${apiPropertyId}/image"
        ApiProperty apiProperty = ApiProperty.findByKey(PROPERTY_KEY)

        given: 'The save action is executed with valid data'
        createNewItem(resourceEndPoint, validJson)

        when: 'When the show action is called to retrieve a resource'
        GET(resourceEndPoint)

        then: 'The response is correct'
        verifyResponse OK, response
        response.contentLength == 17510
        response.header('Content-Disposition') == "attachment;filename=\"${apiPropertyId}-theme\"".toString()
        response.header('Content-Type') == 'image/png;charset=utf-8'

        cleanup:
        DELETE(resourceEndPoint)
        assert response.status() == HttpStatus.NO_CONTENT
        assertDefaultStatus(apiProperty.id, true)
    }

    @Transactional
    void 'R5 : Test the delete action correctly deletes an instance'() {
        String resourceEndPoint = "${apiPropertyId}/image"
        ApiProperty apiProperty = ApiProperty.findByKey(PROPERTY_KEY)
        assertDefaultStatus(apiProperty.id, true)

        given: 'The save action is executed with valid data'
        createNewItem(resourceEndPoint, validJson)
        assertDefaultStatus(apiProperty.id, false)

        when: 'When the delete action is executed on an existing instance'
        DELETE(resourceEndPoint)

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT
        assertDefaultStatus(apiProperty.id, true)
    }
}
