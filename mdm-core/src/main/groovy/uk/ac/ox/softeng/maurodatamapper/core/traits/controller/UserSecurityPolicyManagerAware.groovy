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
package uk.ac.ox.softeng.maurodatamapper.core.traits.controller

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import static org.springframework.http.HttpStatus.UNAUTHORIZED

/**
 * @since 25/11/2019
 */
//@SelfType([ResponseRenderer, ServletAttributes])
//@CompileStatic
trait UserSecurityPolicyManagerAware {

    User getCurrentUser() {
        currentUserSecurityPolicyManager.getUser()
    }

    Boolean isAuthenticated() {
        currentUserSecurityPolicyManager.isLoggedIn()
    }

    UserSecurityPolicyManager getCurrentUserSecurityPolicyManager() {
        // If no uspm stored in the params then return the default uspm bean
        params.currentUserSecurityPolicyManager ?:
        applicationContext.getBean(MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME) as UserSecurityPolicyManager
    }

    boolean unauthorised() {
        render status: UNAUTHORIZED
        false
    }

    void setCurrentUserSecurityPolicyManager(UserSecurityPolicyManager userSecurityPolicyManager) {
        params.currentUserSecurityPolicyManager = userSecurityPolicyManager
    }
}
