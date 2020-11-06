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
package uk.ac.ox.softeng.maurodatamapper.authentication.basic

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNAUTHORIZED


/**
 * @see uk.ac.ox.softeng.maurodatamapper.security.authentication.AuthenticatingController* Controller: authentication
 *  | *    | /api/authentication/logout                 | Action: logout         |
 *  | POST | /api/authentication/login                  | Action: login          |
 *  | GET  | /api/authentication/isAuthenticatedSession | Action: isAuthenticatedSession |
 */
@Integration
@Slf4j
class AuthenticatingFunctionalSpec extends BaseFunctionalSpec implements SecurityDefinition {

    @Override
    String getResourcePath() {
        'authentication'
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        createModernSecurityUsers('functionalTest')
        checkAndSave(admin)
        checkAndSave(editor)
        checkAndSave(pending)
        checkAndSave(reader)
        checkAndSave(authenticated)
    }

    @Transactional
    def cleanupSpec() {
        CatalogueUser.list().findAll {
            !(it.emailAddress in [UnloggedUser.UNLOGGED_EMAIL_ADDRESS, StandardEmailAddress.ADMIN])
        }.each {it.delete(flush: true)}
    }

    void 'test logging in'() {
        when: 'invalid call made to login'
        POST('login', [
            username: 'admin@maurodatamapper.com',
            password: 'not a valid password'
        ])

        then:
        verifyResponse(UNAUTHORIZED, response)

        when: 'valid call made to login'
        POST('login', [
            username: 'admin@maurodatamapper.com',
            password: 'password'
        ], STRING_ARG)

        then:
        verifyJsonResponse(OK, '''{
  "id": "${json-unit.matches:id}",
  "emailAddress": "admin@maurodatamapper.com",
  "firstName": "Admin",
  "lastName": "User",
  "pending": false,
  "disabled": false,
  "createdBy": "admin@maurodatamapper.com"
}''')

        when:
        GET('session/isAuthenticated', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().authenticatedSession == true
    }

    void 'test case insensitive username'() {
        when: 'invalid call made to login'
        POST('login', [
            username: 'ADMIN@MAURODATAMAPPER.COM',
            password: 'not a valid password'
        ])

        then:
        verifyResponse(UNAUTHORIZED, response)

        when: 'valid call made to login'
        POST('login', [
            username: 'ADMIN@MAURODATAMAPPER.COM',
            password: 'password'
        ], STRING_ARG)

        then:
        verifyJsonResponse(OK, '''{
  "id": "${json-unit.matches:id}",
  "emailAddress": "admin@maurodatamapper.com",
  "firstName": "Admin",
  "lastName": "User",
  "pending": false,
  "disabled": false,
  "createdBy": "admin@maurodatamapper.com"
}''')

        when:
        GET('session/isAuthenticated', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().authenticatedSession == true
    }

    void "test isAuthenticatedSession"() {
        when: "Unlogged in call to check"
        GET('session/isAuthenticated', MAP_ARG, true)

        then: "The response is OK but false"
        verifyResponse(OK, response)
        response.body().authenticatedSession == false

        when: "logged in"
        POST('login', [
            username: 'admin@maurodatamapper.com',
            password: 'password'
        ])
        verifyResponse(OK, response)
        GET('session/isAuthenticated', MAP_ARG, true)

        then: "The response is OK and true"
        verifyResponse(OK, response)
        response.body().authenticatedSession == true
    }

    void "test logout"() {
        given:
        POST('login', [
            username: 'admin@maurodatamapper.com',
            password: 'password'
        ])
        verifyResponse(OK, response)
        GET('session/isAuthenticated', MAP_ARG, true)
        verifyResponse(OK, response)

        expect:
        response.body().authenticatedSession == true

        when:
        GET('logout')

        then:
        verifyResponse(NO_CONTENT, response)

        and:
        GET('session/isAuthenticated', MAP_ARG, true)
        verifyResponse(OK, response)
        response.body().authenticatedSession == false
    }
}
