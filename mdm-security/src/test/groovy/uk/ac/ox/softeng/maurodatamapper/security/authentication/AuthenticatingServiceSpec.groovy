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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.security.test.TestAuthenticationSchemeService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest

import java.time.OffsetDateTime

class AuthenticatingServiceSpec extends BaseUnitSpec implements ServiceUnitTest<AuthenticatingService>, SecurityUsers, GrailsWebUnitTest {

    def setup() {
        mockDomain(CatalogueUser)
        implementSecurityUsers('unitTest')
        mockArtefact(SessionService)
        mockArtefact(CatalogueUserService)
        mockArtefact(TestAuthenticationSchemeService)

        SessionService sessionService = applicationContext.getBean(SessionService)
        sessionService.servletContext = servletContext
        sessionService.initialiseToContext()
        sessionService.storeSession(session)

        service.groupBasedSecurityPolicyManagerService = Stub(GroupBasedSecurityPolicyManagerService) {
            removeUserSecurityPolicyManager(_) >> null
        }
    }

    void 'test authenticating user and password validation'() {
        expect: 'no user fails'
        !service.authenticateAndObtainUser(username: null, password: 'password')

        and: 'user doesnt exist fails'
        !service.authenticateAndObtainUser(username: 'newtester@email.com', password: 'wobble')

        and: 'user with temp password but wrong password'
        !service.authenticateAndObtainUser(username: reader.emailAddress, password: 'wobble')

        and: 'user with valid temp password'
        service.authenticateAndObtainUser(username: reader.emailAddress, password: reader.tempPassword)

        and: 'user with password but wrong password'
        !service.authenticateAndObtainUser(username: pending.emailAddress, password: 'wobble')

        and: 'user with password and password'
        service.authenticateAndObtainUser(username: pending.emailAddress, password: 'test password')

    }

    void 'test second user authentication'() {
        expect:
        service.authenticateAndObtainUser(username: admin.emailAddress, password: "password")
        !service.authenticateAndObtainUser(username: admin.emailAddress, password: "Password")
        !service.authenticateAndObtainUser(username: admin.emailAddress, password: "password1234")
        service.authenticateAndObtainUser(username: admin.emailAddress, password: "    password")
        service.authenticateAndObtainUser(username: admin.emailAddress, password: "password     ")
        !service.authenticateAndObtainUser(username: admin.emailAddress, password: "pass   word")
    }

    void 'test user authentication by scheme'() {
        when:
        service.authenticateAndObtainUser(username: admin.emailAddress, password: 'password', 'ldap')

        then:
        ApiException ex = thrown(ApiBadRequestException)
        ex.errorCode == 'AS01'

        when:
        CatalogueUser catalogueUser = service.authenticateAndObtainUser(username: admin.emailAddress, password: 'password', 'test')

        then:
        catalogueUser
    }

    void 'test registering user as logged in'() {
        given:
        OffsetDateTime start = OffsetDateTime.now()

        expect:
        !service.isAuthenticatedSession(session)

        when:
        service.registerUserAsLoggedIn(admin, session)

        then:
        service.isAuthenticatedSession(session)

        and:
        CatalogueUser.findByEmailAddress(admin.emailAddress).lastLogin.isAfter(start)
    }

    void 'test registering user as logged out'() {
        expect:
        !service.isAuthenticatedSession(session)

        when:
        service.registerUserAsLoggedIn(admin, session)

        then:
        service.isAuthenticatedSession(session)

        when:
        service.registerUserAsLoggedOut(session)

        then:
        !service.isAuthenticatedSession(session)
    }
}
