/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

class SessionController implements ResourcelessMdmController {

    SessionService sessionService

    static responseFormats = ['json', 'xml']

    def activeSessions() {
        HashMap activeSessions = sessionService.getActiveSessionMap()
        respond view: '/session/activeSessions', [httpSessionCollection: activeSessions.values()]
    }

    def isAuthenticatedSession() {
        respond authenticatedSession: sessionService.isAuthenticatedSession(params.sessionId ?: session.id)
    }

    def isApplicationAdministrationSession() {
        respond applicationAdministrationSession: currentUserSecurityPolicyManager.isApplicationAdministrator()
    }
}
