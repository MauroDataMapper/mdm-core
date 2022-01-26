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
package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: session
 * |   GET   | /api/session/isApplicationAdministration     | Action: isApplicationAdministrationSession
 * |   GET   | /api/admin/activeSessions       | Action: activeSessions       |
 * |   GET   | /api/sessions/isAuthenticated/$sessionId?       | Action: isAuthenticatedSession       |
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
 */
@Integration
@Slf4j
class SessionFunctionalSpec extends BaseFunctionalSpec {

    String getResourcePath() {
        ''
    }

    void 'get activeSessions endpoint'() {
        when:
        GET('admin/activeSessions')

        and:
        String sessionId = currentCookie.getValue()

        then:
        verifyResponse OK, response

        and:
        response.body().countAuthorised + response.body().countUnauthorised > 0

        when:
        Map info = response.body().items.find {it.id == sessionId}

        then:
        info.id == sessionId
        info.lastAccessedDateTime
        info.creationDateTime
        info.userEmailAddress
        info.lastAccessedUrl
    }

    void 'get is authenticated session endpoint'() {
        when:
        GET('session/isAuthenticated')

        then:
        verifyResponse OK, response

        and:
        response.body().authenticatedSession == false
    }

    void 'get is authenticated session endpoint using different session id'() {
        when:
        GET('session/isAuthenticated')

        then:
        verifyResponse OK, response

        when:
        GET("session/isAuthenticated/${UUID.randomUUID().toString()}")

        then:
        verifyResponse OK, response

        and:
        response.body().authenticatedSession == false
    }

    void 'get is application administration session endpoint'() {
        when:
        GET('session/isApplicationAdministration')

        then:
        verifyResponse OK, response

        and:
        response.body().applicationAdministrationSession == true
    }
}
