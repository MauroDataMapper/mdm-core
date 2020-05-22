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
package uk.ac.ox.softeng.maurodatamapper.core.interceptor


import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

abstract class TieredAccessSecurableResourceInterceptor extends SecurableResourceInterceptor {

    List<String> getPublicAccessMethods() {
        []
    }

    List<String> getAuthenticatedAccessMethods() {
        []
    }

    List<String> getReadAccessMethods() {
        []
    }

    List<String> getCreateAccessMethods() {
        []
    }

    List<String> getEditAccessMethods() {
        []
    }

    List<String> getDeleteAccessMethods() {
        []
    }

    List<String> getApplicationAdminAccessMethods() {
        []
    }

    boolean checkTieredAccessActionAuthorisationOnSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id,
                                                                  boolean directCallsToIndexAllowed = false) {

        if (actionName in getPublicAccessMethods()) {
            return true
        }

        if (actionName in getAuthenticatedAccessMethods()) {
            return currentUserSecurityPolicyManager.isLoggedIn() ?: unauthorised()
        }

        if (actionName in getReadAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        if (actionName in getCreateAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        if (actionName in getEditAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        if (actionName in getDeleteAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanDeleteSecuredResourceId(
                securableResourceClass, id, params.boolean('permanent', false)) ?: unauthorised()
        }

        if (actionName in getApplicationAdminAccessMethods()) {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
        }

        checkActionAuthorisationOnSecuredResource(securableResourceClass, id, directCallsToIndexAllowed)
    }
}
