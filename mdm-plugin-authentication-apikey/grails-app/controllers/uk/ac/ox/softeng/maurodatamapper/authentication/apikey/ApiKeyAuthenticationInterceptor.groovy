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
package uk.ac.ox.softeng.maurodatamapper.authentication.apikey

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManagerInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.interceptor.SecurityPolicyManagerInterceptor

class ApiKeyAuthenticationInterceptor implements SecurityPolicyManagerInterceptor {

    public static final String API_KEY_HEADER = 'apiKey'

    ApiKeyAuthenticatingService apiKeyAuthenticatingService
    SessionService sessionService

    ApiKeyAuthenticationInterceptor() {
        match(uri: '/**/api/**/')
            .excludes(controller: 'authenticating', action: 'login')
            .excludes(controller: 'authenticating', action: 'logout')
        // We want to authenticate before we try to load the usersecuritypolicy manager
        order = UserSecurityPolicyManagerInterceptor.ORDER - 1000
    }

    boolean before() {
        if (!securityPolicyManagerIsSet()) {
            String apikeyHeader = request.getHeader(API_KEY_HEADER)
            // No header then just carry on as if a normal request
            if (!apikeyHeader) return true

            CatalogueUser authenticatedUser = apiKeyAuthenticatingService.authenticateAndObtainUserUsingApiKey(apikeyHeader)

            // Api Key hasn't authenticated against anything so just proceed as if a normal request
            if (!authenticatedUser) return true

            UserSecurityPolicyManager userSecurityPolicyManager =
                apiKeyAuthenticatingService.retrieveOrBuildUserSecurityPolicyManager(authenticatedUser, session)

            setCurrentUserSecurityPolicyManager(userSecurityPolicyManager)
        }
        true
    }
}
