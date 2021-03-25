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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.JsonWebUnitSpec

import grails.testing.web.controllers.ControllerUnitTest

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.servlet.http.HttpSession

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CONFLICT
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNAUTHORIZED

class AuthenticatingControllerSpec extends BaseUnitSpec implements ControllerUnitTest<AuthenticatingController>, SecurityUsers, JsonWebUnitSpec {
    SessionService sessionService

    def setup() {
        mockDomains(CatalogueUser)
        SessionController sessionController = mockController(SessionController)
        implementSecurityUsers('unitTest')
        sessionService = new SessionService()
        sessionService.initialiseToContext(session.servletContext)
        sessionService.storeSession(session)
        sessionController.sessionService = sessionService
    }

    void 'test login no credentials'() {
        given:
        controller.authenticatingService = Mock(AuthenticatingService) {
            0 * isAuthenticatedSession(_) >> false
            0 * authenticateAndObtainUser(_, _)
            0 * registerUserAsLoggedIn(_, _)
            0 * buildUserSecurityPolicyManager(_)
        }
        when:
        request.method = 'POST'
        request.setJson([:])
        controller.login()

        then:
        response.status == BAD_REQUEST.value()
        response.errorMessage == 'Authentication Information not provided'

        and:
        !sessionService.isAuthenticatedSession(session, session.id)
    }

    void 'test login no such user'() {
        given:
        controller.authenticatingService = Mock(AuthenticatingService) {
            1 * isAuthenticatedSession(_) >> false
            1 * authenticateAndObtainUser(_, null) >> null
            0 * registerUserAsLoggedIn(_, _)
            0 * buildUserSecurityPolicyManager(_)
        }

        when:
        request.method = 'POST'
        request.setJson(username: 'test@test.com', password: 'blah')
        controller.login()

        then:
        response.status == UNAUTHORIZED.value()

        and:
        !sessionService.isAuthenticatedSession(session, session.id)
    }

    void 'test login invalid credentials'() {
        given:
        controller.authenticatingService = Mock(AuthenticatingService) {
            1 * isAuthenticatedSession(_) >> false
            1 * authenticateAndObtainUser(_, null) >> null
            0 * registerUserAsLoggedIn(_, _)
            0 * buildUserSecurityPolicyManager(_)
        }

        when:
        request.method = 'POST'
        request.json = [username: admin.emailAddress, password: 'blah']
        controller.login()

        then:
        response.status == UNAUTHORIZED.value()

        and:
        !sessionService.isAuthenticatedSession(session, session.id)
    }

    void 'test login valid credentials'() {
        given:
        controller.authenticatingService = Mock(AuthenticatingService) {
            1 * isAuthenticatedSession(_) >> false
            1 * authenticateAndObtainUser(_, null) >> admin
            1 * registerUserAsLoggedIn(_, _) >> {u, s ->
                sessionService.setUserEmailAddress(s, u.emailAddress)
                sessionService.setUserName(s, u.firstName, u.lastName)
                sessionService.setUserOrganisation(s, u.organisation)
            }
            1 * buildUserSecurityPolicyManager(_)
        }

        when:
        request.method = 'POST'
        request.json = [username: admin.emailAddress, password: 'password']
        controller.login()
        OffsetDateTime end = OffsetDateTime.now()

        then:
        response.status == OK.value()
        renderModelUsingView()
        response.json.emailAddress == admin.emailAddress
        !response.json.password

        and:
        sessionService.isAuthenticatedSession(session, session.id)
        session.getAttribute('emailAddress') == admin.emailAddress

        when:
        HttpSession sessionInfo = sessionService.retrieveSession(session)
        OffsetDateTime createdTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(sessionInfo.creationTime), ZoneId.systemDefault())

        then:
        sessionInfo.getAttribute('emailAddress') == admin.emailAddress

        and:
        createdTime.isBefore(end)
    }

    void 'test login already logged in'() {
        given:
        controller.authenticatingService = Mock(AuthenticatingService) {
            1 * isAuthenticatedSession(_) >> true
            0 * authenticateAndObtainUser(_, null)
            0 * registerUserAsLoggedIn(_, _)
            0 * buildUserSecurityPolicyManager(_)
        }

        when:
        request.method = 'POST'
        request.json = [username: admin.emailAddress, password: 'password']
        controller.login()

        then:
        response.status == CONFLICT.value()
    }

    void 'test login non-post request'() {

        when:
        request.method = 'PUT'
        request.json = [username: admin.emailAddress, password: 'password']
        controller.login()

        then:
        response.status == METHOD_NOT_ALLOWED.value()
    }

    void 'test logout'() {
        given:
        controller.authenticatingService = Mock(AuthenticatingService) {
            1 * registerUserAsLoggedOut(_) >> {
                hs -> sessionService.destroySession(hs)
            }
        }

        when:
        sessionService.setUserEmailAddress(session, reader.emailAddress)
        sessionService.setUserName(session, reader.firstName, reader.lastName)
        sessionService.setUserOrganisation(session, reader.organisation)
        request.method = 'GET'
        controller.logout()


        then:
        response.status == NO_CONTENT.value()
        session.isNew()

        when:
        sessionService.isAuthenticatedSession(session, session.id)

        then:
        thrown(ApiUnauthorizedException)
    }
}