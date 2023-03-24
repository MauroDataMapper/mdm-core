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
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class ThemeImageFileFunctionalSpec extends FunctionalSpec {

    String PROPERTY_KEY = "theme.image";
    String USE_DEFAULT_IMAGE = "USE DEFAULT IMAGE";
    String THEME_IMAGE_FILE_PATH = "themeImageFiles/"

    Boolean directPath = false;

    @Shared
    UUID apiPropertyId

    @Shared
    String apiPropertyResourceEndPoint

    @Shared
    Path resourcesPath

    @Shared
    String resourcePathOverride

    @Override
    String getResourcePath() {
        return (resourcePathOverride) ? resourcePathOverride: "admin/properties/"
    }

    @RunOnce
    def setup() {
        resourcePathOverride = null
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
        assert ApiProperty.count() == 20
        sessionFactory.currentSession.flush()
        UUID userId = CatalogueUser.findByEmailAddress(userEmailAddresses.editor).id
        assert userId
        userId = CatalogueUser.findByEmailAddress(userEmailAddresses.admin).id
        assert userId
        apiPropertyId = ApiProperty.findByKey(PROPERTY_KEY).id
        assert apiPropertyId
        apiPropertyResourceEndPoint = "${apiPropertyId}/image"
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

    String createApiPropertyWithImage() {
        resourcePathOverride = null
        loginAdmin()
        POST(apiPropertyResourceEndPoint, validJson)
        verifyResponse(HttpStatus.CREATED, response)
        String apiPropertyValue = getApiPropertyValue(apiPropertyId)
        logout()
        apiPropertyValue
    }

    void deleteApiProperty() {
        resourcePathOverride = null
        loginUser(true)
        DELETE(apiPropertyResourceEndPoint)
        assert response.status() == HttpStatus.NO_CONTENT
        assertDefaultStatus(apiPropertyId, true)
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

    String getApiPropertyValue(UUID apiPropertyId) {
        GET("admin/properties/${apiPropertyId.toString()}", MAP_ARG, true)
        responseBody().value
    }

    void loginUser(Boolean loginAsAdmin) {
        logout()
        if (loginAsAdmin) {
            loginAdmin()
        }
        else {
            loginEditor()
        }
    }

    @Unroll
    void 'R1 : Test the show action correctly returns #expected if admin login is #loginAsAdmin and no image has been saved using apiPropertyId'() {
        given:
        loginUser(loginAsAdmin)

        when: 'When the show action is called to retrieve a resource'
        GET("${apiPropertyId}/image")

        then: 'The response is correct'
        verifyResponse expected, response

        where:
        loginAsAdmin | expected
        true | HttpStatus.NOT_FOUND
        false | HttpStatus.FORBIDDEN
    }

    @Transactional
    @Unroll
    void 'R2 : Test the save action correctly persists an instance (returns valid response:#expectedValid and invalid response:#expectedInvalid if admin login is #loginAsAdmin)'() {
        given:
        loginUser(loginAsAdmin)

        when: 'The save action is executed with no content'
        POST(apiPropertyResourceEndPoint, [:])

        then: 'The response is correct'
        verifyResponse expectedInvalid, response
        if (loginAsAdmin) {
            response.body().total >= 1
            response.body().errors.size() == response.body().total
            assertDefaultStatus(apiPropertyId, true)
        }

        when: 'The save action is executed with invalid data'
        POST(apiPropertyResourceEndPoint, invalidJson)

        then: 'The response is correct'
        verifyResponse expectedInvalid, response
        if (loginAsAdmin) {
            response.body().total >= 1
            response.body().errors.size() == response.body().total
            assertDefaultStatus(apiPropertyId, true)
        }

        when: 'The save action is executed with valid data'
        POST(apiPropertyResourceEndPoint, validJson)

        then: 'The response is correct'
        verifyResponse expectedValid, response
        if (loginAsAdmin) {
            responseBody().id
            responseBody().domainType == 'ThemeImageFile'
            responseBody().lastUpdated
            responseBody().fileSize == 17510
            responseBody().fileType == 'image/png'
            responseBody().fileName == "${apiPropertyId}-theme".toString()
            !responseBody().fileContents
            assertDefaultStatus(apiPropertyId, false)
        }

        cleanup:
        if (loginAsAdmin) {
            deleteApiProperty()
        }

        where:
        loginAsAdmin | expectedValid | expectedInvalid
        true | HttpStatus.CREATED | HttpStatus.UNPROCESSABLE_ENTITY
        false | HttpStatus.FORBIDDEN | HttpStatus.FORBIDDEN
    }

    @Transactional
    @Unroll
    void 'R3 : Test the update action correctly updates an instance (returns valid response:#expectedValid and invalid response:#expectedInvalid if admin login is #loginAsAdmin)'() {
        given: 'The save action is executed with valid data'
        createApiPropertyWithImage()
        loginUser(loginAsAdmin)

        when: 'The update action is called with invalid data'
        PUT(apiPropertyResourceEndPoint, invalidJson)

        then: 'The response is unprocessable entity'
        verifyResponse(expectedInvalid, response)

        when: 'The update action is called with valid data'
        PUT(apiPropertyResourceEndPoint, validJson)

        then: 'The response is correct'
        verifyResponse(expectedValid, response)
        if (loginAsAdmin) {
            responseBody().id
            responseBody().domainType == 'ThemeImageFile'
            responseBody().lastUpdated
            responseBody().fileSize == 17510
            responseBody().fileType == 'image/png'
            responseBody().fileName == "${apiPropertyId}-theme".toString()
            !responseBody().fileContents
        }

        cleanup:
        deleteApiProperty()

        where:
        loginAsAdmin | expectedValid | expectedInvalid
        true | HttpStatus.OK | HttpStatus.UNPROCESSABLE_ENTITY
        false | HttpStatus.FORBIDDEN | HttpStatus.FORBIDDEN
    }

    @Transactional
    @Unroll
    void 'R4 : Test the show action correctly shows a saved image (returns response:#expected when admin login is #loginAsAdmin and use theme image file path is #useThemeImageFilePath)'() {
        String resourceEndPoint

        given: 'The save action is executed with valid data'
        String apiPropertyValue = createApiPropertyWithImage()
        loginUser(loginAsAdmin)

        if (useThemeImageFilePath) {
            resourcePathOverride = THEME_IMAGE_FILE_PATH
            resourceEndPoint = "${apiPropertyValue}"
        }
        else {
            resourcePathOverride = null;
            resourceEndPoint = apiPropertyResourceEndPoint
        }

        when: 'When the show action is called to retrieve a resource'
        GET(resourceEndPoint)

        then: 'The response is correct'
        verifyResponse expected, response
        if (loginAsAdmin) {
            response.contentLength == 17510
            response.header('Content-Disposition') == "attachment;filename=\"${apiPropertyId}-theme\"".toString()
            response.header('Content-Type') == 'image/png;charset=utf-8'
        }

        cleanup:
        resourcePathOverride = null
        deleteApiProperty()

        where:
        loginAsAdmin | useThemeImageFilePath | expected
        true | false | HttpStatus.OK
        false | false | HttpStatus.FORBIDDEN
        true | true | HttpStatus.OK
        false | true | HttpStatus.OK
    }

    @Transactional
    void 'R5 : Test the delete action correctly deletes an instance (returns response:#expected if admin login is #loginAsAdmin)'() {
        directPath = false
        loginUser(true)
        assertDefaultStatus(apiPropertyId, true)

        given: 'The save action is executed with valid data'
        createApiPropertyWithImage()
        assertDefaultStatus(apiPropertyId, false)
        loginUser(loginAsAdmin)

        when: 'When the delete action is executed on an existing instance'
        DELETE(apiPropertyResourceEndPoint)

        then: 'The response is correct'
        response.status == expected
        assertDefaultStatus(apiPropertyId, loginAsAdmin)

        cleanup:
        if (!loginAsAdmin) {
            deleteApiProperty()
        }

        where:
        loginAsAdmin | expected
        true | HttpStatus.NO_CONTENT
        false | HttpStatus.FORBIDDEN
    }


}
