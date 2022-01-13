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
package uk.ac.ox.softeng.maurodatamapper.security.file

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.spockframework.util.Assert
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class UserImageFileFunctionalSpec extends BaseFunctionalSpec implements SecurityUsers {

    @Shared
    UUID userId

    @Shared
    Path resourcesPath

    @Override
    String getResourcePath() {
        "catalogueUsers/${userId}/image"
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
        implementSecurityUsers('functionalTest')
        assert CatalogueUser.count() == 9
        sessionFactory.currentSession.flush()
        userId = CatalogueUser.findByEmailAddress(UnloggedUser.UNLOGGED_EMAIL_ADDRESS).id
        assert userId
    }

    @Transactional
    def cleanupSpec() {
        CatalogueUser.list().findAll {
            !(it.emailAddress in [UnloggedUser.UNLOGGED_EMAIL_ADDRESS, StandardEmailAddress.ADMIN])
        }.each { it.delete(flush: true) }
        if (CatalogueUser.count() != 2) {
            Assert.fail("Resource Class ${CatalogueUser.simpleName} has not been emptied")
        }
    }

    void createNewItem(Map model) {
        POST('', model)
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

    Map getValidUpdateJson() {
        getValidJson()
    }

    int getExpectedInitialResourceCount() {
        0
    }

    void 'R1 : Test the show action correctly returns the no profile image if none has been saved'() {

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyResponse OK, response
        response.contentLength == 4297
        response.header('Content-Disposition') == 'attachment;filename="no_profile_image.png"'
        response.header('Content-Type') == 'image/png;charset=utf-8'
    }

    @Transactional
    void 'R2 : Test the save action correctly persists an instance'() {
        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
        response.body().total >= 1
        response.body().errors.size() == response.body().total

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
        response.body().total >= 1
        response.body().errors.size() == response.body().total

        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        DELETE('')
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R3 : Test the update action correctly updates an instance'() {
        given: 'The save action is executed with valid data'
        createNewItem(validJson)

        when: 'The update action is called with invalid data'
        PUT('', invalidJson)

        then: 'The response is unprocessable entity'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'The update action is called with valid data'
        PUT('', validJson)

        then: 'The response is correct'
        responseBody().id
        responseBody().domainType == 'UserImageFile'
        responseBody().lastUpdated
        responseBody().fileSize == 17510
        responseBody().fileType == 'image/png'
        responseBody().fileName == "${userId}-profile".toString()
        responseBody().userId == userId.toString()
        !responseBody().fileContents

        cleanup:
        DELETE('')
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R4 : Test the show action correctly a saved image'() {
        given: 'The save action is executed with valid data'
        createNewItem(validJson)

        when: 'When the show action is called to retrieve a resource'
        GET('')

        then: 'The response is correct'
        verifyResponse OK, response
        response.contentLength == 17510
        response.header('Content-Disposition') == "attachment;filename=\"${userId}-profile\"".toString()
        response.header('Content-Type') == 'image/png;charset=utf-8'

        cleanup:
        DELETE('')
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R5 : Test the delete action correctly deletes an instance'() {
        given: 'The save action is executed with valid data'
        createNewItem(validJson)

        when: 'When the delete action is executed on an existing instance'
        DELETE('')

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT
    }
}
