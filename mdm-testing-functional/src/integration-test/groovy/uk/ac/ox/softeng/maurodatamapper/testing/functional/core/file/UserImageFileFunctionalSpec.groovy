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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.file

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Integration
@Slf4j
class UserImageFileFunctionalSpec extends FunctionalSpec {

    @Shared
    UUID userId

    @Shared
    Path resourcesPath

    @Override
    String getResourcePath() {
        "catalogueUsers/${userId}/image"
    }

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources').toAbsolutePath()
        assert Files.exists(resourcesPath.resolve('image_data_file.txt'))
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        userId = CatalogueUser.findByEmailAddress(userEmailAddresses.editor).id
        assert userId
    }

    void getValidId() {
        loginEditor()
        POST('', validJson)
        verifyResponse CREATED, response
        logout()
    }

    void removeValidIdObject() {
        loginAdmin()
        DELETE('')
        verifyResponse NO_CONTENT, response
        logout()
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

    Map getValidUpdateJson() {
        getValidJson()
    }

    void verifyShowDefaultImage() {
        verifyResponse OK, response
        assert response.contentLength == 4297
        assert response.header('Content-Disposition') == 'attachment;filename="no_profile_image.png"'
        assert response.header('Content-Type') == 'image/png;charset=utf-8'
    }

    void verifyShow() {
        verifyResponse OK, response
        assert response.contentLength == 17510
        assert response.header('Content-Disposition') == "attachment;filename=\"${userId}-profile\"".toString()
        assert response.header('Content-Type') == 'image/png;charset=utf-8'
    }

    /*
      * Logged in as editor testing
      */

    void 'E01 : Test the show action correctly renders the default instance for set user (as editor)'() {
        given:
        loginEditor()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then:
        verifyShowDefaultImage()
    }

    void 'E02 : Test the show action correctly renders an instance for set user (as editor)'() {
        given:
        getValidId()
        loginEditor()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then:
        verifyShow()

        cleanup:
        removeValidIdObject()
    }

    /*
     * Logged out testing
     */

    void 'L01 : Test the show action correctly renders the default instance for set user (not logged in)'() {
        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShowDefaultImage()
    }

    void 'L02 : Test the show action correctly renders an instance for set user (not logged in)'() {
        given:
        getValidId()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShow()

        cleanup:
        removeValidIdObject()
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N01 : Test the show action correctly renders the default instance for set user (as no access/authenticated)'() {
        given:
        loginAuthenticated()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShowDefaultImage()
    }

    void 'N02 : Test the show action correctly renders an instance for set user (as no access/authenticated)'() {
        given:
        getValidId()
        loginAuthenticated()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShow()

        cleanup:
        removeValidIdObject()
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R01 : Test the show action correctly renders the default instance for set user (as reader)'() {
        given:
        loginReader()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShowDefaultImage()
    }

    void 'R02 : Test the show action correctly renders an instance for set user (as reader)'() {
        given:
        getValidId()
        loginReader()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShow()

        cleanup:
        removeValidIdObject()
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A01 : Test the show action correctly renders the default instance for set user (as admin)'() {
        given:
        loginAdmin()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShowDefaultImage()
    }

    void 'A02 : Test the show action correctly renders an instance for set user (as admin)'() {
        given:
        getValidId()
        loginAdmin()

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyShow()

        cleanup:
        removeValidIdObject()
    }

    /*
   * Logged in as editor testing
   */

    void 'E03a : Test the save action correctly persists an instance (as editor)'() {
        given:
        loginEditor()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        removeValidIdObject()

    }

    void 'E03b : Test the save action correctly persists an instance when using PUT/update (as editor)'() {
        given:
        loginEditor()

        when: 'The save action is executed with no content'
        PUT('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        PUT('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        PUT('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        removeValidIdObject()

    }

    void 'E04 : Test the delete action correctly deletes an instance (as editor)'() {
        given:
        getValidId()
        loginEditor()

        when: 'When the delete action is executed on an existing instance'
        DELETE('')

        then: 'The response is correct'
        verifyResponse NO_CONTENT, response
    }

    /*
     * Logged out testing
     */

    void 'L03 : Test the save action correctly persists an instance (not logged in)'() {
        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyForbidden response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyForbidden response
    }

    void 'L04 : Test the delete action correctly deletes an instance (not logged in)'() {
        given:
        getValidId()

        when: 'When the delete action is executed on an existing instance'
        DELETE('')

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject()
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */

    void 'N03 : Test the save action correctly persists an instance (as no access/authenticated)'() {
        given:
        loginAuthenticated()

        when: 'The save action is executed with no content'
        POST('', [:])

        then:
        verifyForbidden response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then:
        verifyForbidden response

    }

    void 'N04 : Test the delete action correctly deletes an instance (as no access/authenticated)'() {
        given:
        getValidId()
        loginAuthenticated()

        when: 'When the delete action is executed on an existing instance'
        DELETE('')

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject()
    }

    /**
     * Testing when logged in as a reader only user
     */

    void 'R03 : Test the save action correctly persists an instance (as reader)'() {
        given:
        loginReader()

        when: 'The save action is executed with no content'
        POST('', [:])

        then:
        verifyForbidden response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then:
        verifyForbidden response
    }

    void 'R04 : Test the delete action correctly deletes an instance (as reader)'() {
        given:
        getValidId()
        loginReader()

        when: 'When the delete action is executed on an existing instance'
        DELETE('')

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject()
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A03 : Test the save action correctly persists an instance (as admin)'() {
        given:
        loginAdmin()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        removeValidIdObject()

    }

    void 'A04 : Test the delete action correctly deletes an instance (as admin)'() {
        given:
        getValidId()
        loginAdmin()

        when: 'When the delete action is executed on an existing instance'
        DELETE('')

        then: 'The response is correct'
        verifyResponse NO_CONTENT, response
    }

    /*
    * Logged in as editor testing
    */

    void 'E05 : Test the update action correctly updates an instance (as editor)'() {
        given:
        getValidId()
        loginEditor()

        when: 'The update action is called with invalid data'
        PUT('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The update action is called with valid data'
        PUT('', validUpdateJson)

        then: 'The response is correct'
        verifyResponse OK, response
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        removeValidIdObject()
    }

    /*
     * Logged out testing
     */

    void 'L05 : Test the update action correctly updates an instance (not logged in)'() {
        given:
        getValidId()

        when: 'The update action is called with invalid data'
        PUT('', invalidJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The update action is called with valid data'
        PUT('', validUpdateJson)

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject()
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N05 : Test the update action correctly updates an instance (as no access/authenticated)'() {
        given:
        getValidId()
        loginAuthenticated()

        when: 'The update action is called with invalid data'
        PUT('', invalidJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The update action is called with valid data'
        PUT('', validUpdateJson)

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject()
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R05 : Test the update action correctly updates an instance (as reader)'() {
        given:
        getValidId()
        loginReader()

        when: 'The update action is called with invalid data'
        PUT('', invalidJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The update action is called with valid data'
        PUT('', validUpdateJson)

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject()
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A05 : Test the update action correctly updates an instance (as admin)'() {
        given:
        getValidId()
        loginAdmin()

        when: 'The update action is called with invalid data'
        PUT('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The update action is called with valid data'
        PUT('', validUpdateJson)

        then: 'The response is correct'
        verifyResponse OK, response
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        removeValidIdObject()
    }


    void 'X01 : Test getting edits after creation (as editor)'() {
        given:
        getValidId()
        loginEditor()

        when: 'getting initial edits'
        GET("catalogueUsers/$userId/edits?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().count >= 1
        responseBody().items.first().createdBy == userEmailAddresses.editor
        (responseBody().items.first().description as String).matches(getExpectedCreatedEditRegex())

        cleanup:
        removeValidIdObject()
    }

    Pattern getExpectedCreatedEditRegex() {
        ~/\[\w+:.+?] added to component \[CatalogueUser:.+?]/
    }
}
