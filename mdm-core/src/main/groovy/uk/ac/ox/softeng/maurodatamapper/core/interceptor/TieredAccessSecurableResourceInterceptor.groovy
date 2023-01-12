/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
            return currentUserSecurityPolicyManager.isAuthenticated() ?: forbiddenDueToNotAuthenticated()
        }

        if (actionName in getApplicationAdminAccessMethods()) {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: forbiddenDueToNotApplicationAdministrator()
        }

        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(securableResourceClass, id)

        if (actionName in getReadAccessMethods()) {
            return canRead ?: notFound(securableResourceClass, id.toString())
        }

        if (actionName in getCreateAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(securableResourceClass, id) ?:
                   forbiddenOrNotFound(canRead, securableResourceClass, id)
        }

        if (actionName in getEditAccessMethods()) {
            // Edit actions are becoming more "powerful" so we will now check the exact write ability on the action
            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(securableResourceClass, id, actionName) ?:
                   forbiddenOrNotFound(canRead, securableResourceClass, id)
        }

        if (actionName in getDeleteAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanDeleteSecuredResourceId(
                securableResourceClass, id, params.boolean('permanent', false)) ?:
                   forbiddenOrNotFound(canRead, securableResourceClass, id)
        }

        checkActionAuthorisationOnSecuredResource(securableResourceClass, id, directCallsToIndexAllowed)
    }
}
