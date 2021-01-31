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

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.UserCredentials
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser

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

    def login(UserCredentials credentials) {
        def loginResponse = loginProcess(credentials)

        // Any errors will be commited into the response so dont continue any further
        if (!loginResponse) return

        respond loginResponse
    }

    def logout() {
        authenticatingService.registerUserAsLoggedOut(session)
        render status: NO_CONTENT
    }

    def activeSessionsWithCredentials(UserCredentials credentials) {
        // Any errors will be commited into the response so dont continue any further
        if (!loginProcess(credentials)) return

        if (currentUserSecurityPolicyManager.isApplicationAdministrator()) {
            // Once logged in call to the normal active sessions call
            return sessionController.activeSessions()
        }
        forbiddenDueToNotApplicationAdministrator()
    }

    private def loginProcess(UserCredentials credentials) {
        if (!credentials || !credentials.username) {
            log.warn("Login attempt with no username or password")
            errorResponse(BAD_REQUEST, "Username and/or password not provided")
            return false
        }

        String username = credentials.getUsername()?.trim()
        String password = credentials.getPassword()?.trim()

        if (authenticatingService.isAuthenticatedSession(session)) {
            log.warn("Login attempt for '${username}' while a user currently logged in")
            errorResponse(CONFLICT, 'A user is already logged in, logout first')
            return false
        }

        CatalogueUser user = authenticatingService.authenticateAndObtainUser(username, password, params.scheme)

        if (!user) {
            log.warn("Authentication attempt with username '${username}' with invalid username or password",)
            return unauthorised('Invalid credentials')
        }

        if (!user) {
            log.warn("Authentication attempt with username '${username}' with invalid username or password",)
            return unauthorised('Invalid credentials')
        }

        setCurrentUserSecurityPolicyManager(authenticatingService.buildUserSecurityPolicyManager(user))

        authenticatingService.registerUserAsLoggedIn(user, session)
        user
    }
}
