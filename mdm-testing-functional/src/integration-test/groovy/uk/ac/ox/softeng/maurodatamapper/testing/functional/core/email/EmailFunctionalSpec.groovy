/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.email

import uk.ac.ox.softeng.maurodatamapper.core.email.Email
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: email
 *  | GET | /api/admin/emails | Action: index |
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.email.EmailController
 */
@Integration
@Slf4j
class EmailFunctionalSpec extends FunctionalSpec {

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        cleanUpResource(Email)
    }

    @Transactional
    def cleanupSpec() {
        cleanUpResource(Email)
    }

    String getResourcePath() {
        'admin/emails'
    }

    Map getValidJson() {
        [
            sentToEmailAddress: 'anewuser@test.com',
            subject           : 'test email',
            body              : 'this is just a test email',
            emailServiceUsed  : 'BasicEmailPluginService',
            dateTimeSent      : OffsetDateTime.now(),
            successfullySent  : true,
        ]
    }

    void selfRegister() {
        logout()
        POST("catalogueUsers", [
            emailAddress: 'user@functional-test.com',
            password    : 'password',
            firstName   : 'a',
            lastName    : 'new user'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        sleep(2000)
    }

    @Transactional
    void cleanupFunctionalSpecUser() {
        CatalogueUser user = getUserByEmailAddress('user@functional-test.com')
        user.delete(flush: true)
    }

    void "Test the index action"() {
        when: "The index action is requested unlogged in"
        GET('')

        then: "The response is unauth"
        verifyForbidden response

        when: "The index action is requested as normal user"
        loginAuthenticated()
        GET('')

        then: "The response is unauth"
        verifyForbidden response

        when: "The index action is requested as admin user"
        logout()
        loginAdmin()
        GET('', STRING_ARG)

        then: "The response is correct but no emails"
        verifyJsonResponse OK, '{"count":0,"items":[]}'
    }

    void 'test index action with emails present'() {
        given: 'self registering a user '
        selfRegister()

        when:
        loginAdmin()
        GET('', STRING_ARG)

        then:
        verifyJsonResponse OK, '''{"count":1,"items":[
  {
    "successfullySent": false,
    "subject": "Mauro Data Mapper Registration",
    "failureReason": "No email provider service configured",
    "dateTimeSent": null,
    "sentToEmailAddress": "user@functional-test.com",
    "id": "${json-unit.matches:id}",
    "body": "${json-unit.any-string}",
    "emailServiceUsed": null
  }
]
}'''

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the save action isnt found"() {
        when: "The save action is executed with no content"
        loginAdmin()
        POST('', [:])

        then: "The response is correct"
        verifyResponse(NOT_FOUND, response)
    }

    void "Test the update action isnt found"() {
        given: 'self registering a user'
        selfRegister()

        when: 'getting email id'
        loginAdmin()
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].id

        when:
        def id = response.body().items[0].id
        PUT("$id", validJson)

        then: "The response is correct"
        verifyResponse(NOT_FOUND, response)

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the show action isnt found"() {
        given: 'self registering a user'
        selfRegister()

        when: 'getting email id'
        loginAdmin()
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].id

        when:
        def id = response.body().items[0].id
        GET("$id")

        then: "The response is correct"
        verifyResponse(NOT_FOUND, response)

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the delete action isnt found"() {
        given: 'self registering a user'
        selfRegister()

        when: 'getting email id'
        loginAdmin()
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].id

        when:
        def id = response.body().items[0].id
        DELETE("$id")

        then:
        verifyResponse(NOT_FOUND, response)

        cleanup:
        cleanupFunctionalSpecUser()
    }
}