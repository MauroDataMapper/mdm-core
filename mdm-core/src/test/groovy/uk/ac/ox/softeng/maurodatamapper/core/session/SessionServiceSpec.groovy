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
package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest

import javax.servlet.http.HttpSession

/**
 * @since 03/05/2018
 */
class SessionServiceSpec extends BaseUnitSpec implements ServiceUnitTest<SessionService>, GrailsWebUnitTest {

    void 'test servlet context setup'() {
        expect:
        service.getActiveSessionMap(servletContext) == null

        when:
        initialiseContext()

        then:
        service.getActiveSessionMap(servletContext) != null
    }

    void 'create session info'() {
        given:
        initialiseContext()
        service.storeSession(session)

        when:
        service.setUserEmailAddress(session, 'test@test.com')
        service.setUserName(session,'first','last')
        service.setUserOrganisation(session,'Test Organisation')
        HttpSession httpSession = service.retrieveSession(servletContext, session.id)

        then:
        httpSession

        and:
        httpSession.getAttribute('emailAddress') == 'test@test.com'
        !httpSession.getAttribute('lastUrl')

        and:
        !service.isInvalidatedSession(session)
    }

    void 'update session info'() {
        given:
        initialiseContext()
        service.storeSession(session)
        service.setUserEmailAddress(session, 'test@test.com')
        service.setUserName(session,'test','login')
        service.setUserOrganisation('Test Organisation')

        when:
        service.setLastAccessedUrl(session, '/test/url')

        and:
        HttpSession httpSession = service.retrieveSession(servletContext, session.id)

        then:
        httpSession

        and:
        httpSession.getAttribute('emailAddress') == 'test@test.com'
        httpSession.getAttribute('userName') == 'test login'
        httpSession.getAttribute('userOrganisation') == 'Test Organisation'
        httpSession.getAttribute('lastUrl') == '/test/url'

        and:
        !service.isInvalidatedSession(session)
    }

    void 'destroy session info'() {
        given:
        initialiseContext()
        service.storeSession(session)
        service.setUserEmailAddress(session, 'test@test.com')
        service.setUserName(session,'test','login')
        service.setUserOrganisation(session, 'Test Organisation')
        service.setLastAccessedUrl(session, '/test/url')

        when:
        service.destroySession(session)

        and:
        HttpSession httpSession = service.retrieveSession(servletContext, session.id)

        then:
        !httpSession
    }

    private void initialiseContext() {
        try {
            // we have to wrap due to thrown exception from mock servlet
            service.initialiseToContext(servletContext)
        } catch (UnsupportedOperationException ignored) {
            //ignore
        }
    }
}
