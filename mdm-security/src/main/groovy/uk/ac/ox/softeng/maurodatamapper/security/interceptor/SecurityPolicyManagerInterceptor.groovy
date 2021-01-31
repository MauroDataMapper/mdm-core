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
package uk.ac.ox.softeng.maurodatamapper.security.interceptor

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import javax.servlet.http.HttpSession

/**
 * @since 13/01/2021
 */
trait SecurityPolicyManagerInterceptor extends MdmInterceptor {

    void checkSessionIsValid() {
        // Get the session without using the grails wrapper
        // If we use 'session' it sets the session variable and we cannot change it
        HttpSession localSession = getRequest().getSession()
        // Check if its invalid (this is only likely to happen when running in dev mode and we shut down the backend and then
        // make another request on startup with the old session in the request
        // Can be tested easily enough with postman)
        // Validity is tested by existence in the known session id map, which is updated whenever tomcat creates a new session
        boolean invalid = sessionService.isInvalidatedSession(localSession)

        // If invalid then invalidate the local session and force the request to create a new one
        // Upon creation of new session the session will be added to the map and all subsequent checks will come back as a valid but new session
        if (invalid) {
            localSession.invalidate()
            getRequest().getSession(true)
        }
    }

    boolean securityPolicyManagerIsSet() {
        params.currentUserSecurityPolicyManager
    }
}