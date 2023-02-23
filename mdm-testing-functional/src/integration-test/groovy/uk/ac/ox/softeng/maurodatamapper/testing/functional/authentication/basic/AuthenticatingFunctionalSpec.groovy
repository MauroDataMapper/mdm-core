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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.authentication.basic

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNAUTHORIZED

/**
 * <pre>
 * Controller: authenticating
 * |  POST  | /api/admin/activeSessions  | Action: activeSessionsWithCredentials
 * |  *     | /api/authentication/logout | Action: logout
 * |  POST  | /api/authentication/login  | Action: login
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.security.authentication.AuthenticatingController
 */
@Integration
@Slf4j
class AuthenticatingFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        'authentication'
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

    void 'test logout'() {
        when:
        loginAdmin()
        GET('session/isAuthenticated', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().authenticatedSession == true

        when:
        GET('logout')

        then:
        verifyResponse(NO_CONTENT, response)

        when:
        GET('session/isAuthenticated', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().authenticatedSession == false
    }

    void 'post to active sessions endpoint'() {
        when: 'invalid credentials'
        POST('admin/activeSessions', [
            username: 'admin@maurodatamapper.com',
            password: 'not a valid password'
        ], MAP_ARG, true)

        then:
        verifyUnauthorised(response)

        when: 'logged in as admin'
        POST('admin/activeSessions', [
            username: 'admin@maurodatamapper.com',
            password: 'password'
        ], MAP_ARG, true)

        then:
        verifyResponse OK, response

        and:
        response.body().countAuthorised + response.body().countUnauthorised > 0

        when:
        String sessionId = currentCookie.getValue()

        then:
        response.body().items.any {it.id == sessionId}

        when:
        Map info = response.body().items.find {it.id == sessionId}

        then:
        info.id == sessionId
        info.lastAccessedDateTime
        info.creationDateTime
        info.userEmailAddress == 'admin@maurodatamapper.com'
        info.lastAccessedUrl == '/api/admin/activeSessions'
    }
}
