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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser

import grails.databinding.DataBindingSource
import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CONFLICT
import static org.springframework.http.HttpStatus.NO_CONTENT

@Slf4j
class AuthenticatingController implements ResourcelessMdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [login: 'POST', logout: ['POST', 'GET', 'PUT'], isValidSession: 'GET']

    AuthenticatingService authenticatingService

    @Autowired
    SessionController sessionController

    def login() {
        Map authenticationInformation = extractAuthenticationInformation()
        def loginResponse = loginProcess(authenticationInformation)

        // Any errors will be commited into the response so dont continue any further
        if (!loginResponse) return

        respond loginResponse
    }

    def logout() {
        authenticatingService.registerUserAsLoggedOut(session)
        render status: NO_CONTENT
    }

    def activeSessionsWithCredentials() {
        // Any errors will be commited into the response so dont continue any further
        if (!loginProcess(extractAuthenticationInformation())) return

        if (currentUserSecurityPolicyManager.isApplicationAdministrator()) {
            // Once logged in call to the normal active sessions call
            return sessionController.activeSessions()
        }
        forbiddenDueToNotApplicationAdministrator()
    }

    private def loginProcess(Map<String, Object> authenticationInformation) {
        if (!authenticationInformation) {
            log.warn('Login attempt with authentication information')
            errorResponse(BAD_REQUEST, 'Authentication Information not provided')
            return false
        }

       String alreadyLoggedInEmailAddress
        if (authenticatingService.isAuthenticatedSession(session)) {
            log.warn('Login attempt while a user currently logged in')
            alreadyLoggedInEmailAddress = authenticatingService.getEmailAddressForSession(session)
        }

        authenticationInformation.session = session
        CatalogueUser user = authenticatingService.authenticateAndObtainUser(authenticationInformation, params.scheme)

        if (!user) {
            log.warn('Authentication attempt with invalid authentication information',)
            return unauthorised('Invalid credentials')
        }

        if (!user) {
            log.warn('Authentication attempt with invalid authentication information',)
            return unauthorised('Invalid credentials')
        }

        if (alreadyLoggedInEmailAddress && alreadyLoggedInEmailAddress != user.emailAddress) {
            errorResponse(CONFLICT, 'A user is already logged in, logout first')
            return false
        }
        setCurrentUserSecurityPolicyManager(authenticatingService.buildUserSecurityPolicyManager(user))

        authenticatingService.registerUserAsLoggedIn(user, session)
        user
    }

    private Map<String, Object> extractAuthenticationInformation() {
        DataBindingSource dataBindingSource = DataBindingUtils.createDataBindingSource(grailsApplication, Map, request)
        dataBindingSource.propertyNames.collectEntries {key -> [key, dataBindingSource.getPropertyValue(key)]}
    }
}
