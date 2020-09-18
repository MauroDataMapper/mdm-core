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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.session

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: session
 *  |  GET  | /api/session/isApplicationAdministration     | Action: isApplicationAdministrationSession
 *  |  GET  | /api/session/isAuthenticated  | Action: isAuthenticatedSession
 *  |  GET  | /api/admin/activeSessions     | Action: activeSessions
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
 */
@Integration
@Slf4j
class SessionFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        ''
    }

    void 'get activeSessions endpoint'() {
        when: 'not logged in'
        GET('admin/activeSessions')

        then:
        verifyForbidden response

        when: 'not logged in as admin'
        loginAuthenticated()
        GET('admin/activeSessions')

        then:
        verifyForbidden response

        when: 'logged in as admin'
        loginAdmin()
        GET('admin/activeSessions')
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
        when: 'not logged in'
        GET('session/isAuthenticated')

        then:
        verifyResponse OK, response

        and:
        response.body().authenticatedSession == false

        when:
        loginAuthenticated()
        GET('session/isAuthenticated')

        then:
        verifyResponse OK, response

        and:
        response.body().authenticatedSession == true
    }

    void 'get is application administration session endpoint'() {
        when: 'not logged in'
        GET('session/isApplicationAdministration')

        then:
        verifyResponse OK, response

        and:
        response.body().applicationAdministrationSession == false

        when:
        loginAuthenticated()
        GET('session/isApplicationAdministration')

        then:
        verifyResponse OK, response

        and:
        response.body().applicationAdministrationSession == false

        when:
        loginAdmin()
        GET('session/isApplicationAdministration')

        then:
        verifyResponse OK, response

        and:
        response.body().applicationAdministrationSession == true
    }
}
