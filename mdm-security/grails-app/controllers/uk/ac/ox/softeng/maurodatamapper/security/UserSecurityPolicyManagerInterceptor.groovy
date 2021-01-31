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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.interceptor.SecurityPolicyManagerInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService

import groovy.util.logging.Slf4j
import org.springframework.core.Ordered

/**
 * This should intercept to load the user's security policy manager into the parameters object to allow downstream interceptors and controllers
 * access to it
 */
@Slf4j
class UserSecurityPolicyManagerInterceptor implements SecurityPolicyManagerInterceptor {

    public static final Integer ORDER = Ordered.HIGHEST_PRECEDENCE + 1000

    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    SessionService sessionService

    UserSecurityPolicyManagerInterceptor() {
        match(uri: '/**/api/**/')
        order = ORDER
    }

    boolean before() {
        checkSessionIsValid()

        if (!securityPolicyManagerIsSet()) {
            if (sessionService.isAuthenticatedSession(session, session.id)) {
                UserSecurityPolicyManager userSecurityPolicyManager =
                    groupBasedSecurityPolicyManagerService.retrieveUserSecurityPolicyManager(sessionService.getSessionEmailAddress(session))

                setCurrentUserSecurityPolicyManager(userSecurityPolicyManager)
            }
        }
        true
    }

}
