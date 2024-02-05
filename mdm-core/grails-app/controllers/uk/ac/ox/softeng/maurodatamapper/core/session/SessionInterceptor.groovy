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

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.util.Environment

class SessionInterceptor implements MdmInterceptor {

    SessionService sessionService
    public static final Integer ORDER = -50000

    SessionInterceptor() {
        match(uri: '/**/api/**/')
        order = ORDER
    }

    boolean before() {

        if (!params.sessionId && sessionService.isInvalidatedSession(session)) {
            if (Environment.developmentMode) {
                sessionService.storeSession(session)
            } else return unauthorised('Session has been invalidated')
        }

        // Every session access will be added to the list by the session service as a sessionlistener
        sessionService.setLastAccessedUrl(session, request.requestURI)

        if (actionName == 'activeSessions') {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: forbiddenDueToNotApplicationAdministrator()
        }

        true
    }
}