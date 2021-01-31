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
package uk.ac.ox.softeng.maurodatamapper.core.interceptor

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

import groovy.util.logging.Slf4j

@Slf4j
abstract class SecurableResourceInterceptor implements MdmInterceptor {

    public static final String CLASS_PARAMS_KEY = 'securableResourceClass'
    public static final String ID_PARAMS_KEY = 'securableResourceId'

    abstract <S extends SecurableResource> Class<S> getSecuredClass()

    void setSecurableResourceParams() {
        if (!params.containsKey(CLASS_PARAMS_KEY)) params[CLASS_PARAMS_KEY] = getSecuredClass()
        params[ID_PARAMS_KEY] = getSecuredId()
    }

    abstract void checkIds()

    UUID getSecuredId() {
        getId()
    }

    abstract UUID getId()

    void securableResourceChecks() {
        checkIds()
        setSecurableResourceParams()
    }

    /**
     * Check the current action is allowed against the {@link SecurableResource} and id.
     *
     * @param securableResourceClass
     * @param id
     * @return
     */
    boolean checkActionAuthorisationOnSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id,
                                                      boolean directCallsToIndexAllowed = false) {

        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(securableResourceClass, id)

        // Allows for direct calls to secured resources but stops indexing on nested secured resources
        if (directCallsToIndexAllowed && isIndex() && !id) return true

        // The 3 tiers of writing are deleting, updating/editing and saving/creation
        // We will have to handle certain controls inside the controller
        if (isDelete()) {
            return currentUserSecurityPolicyManager.userCanDeleteSecuredResourceId(
                securableResourceClass, id, params.boolean('permanent', false)) ?:
                   forbiddenOrNotFound(canRead, securableResourceClass, id)
        }
        if (isUpdate() || actionName in ['readByEveryone', 'readByAuthenticated']) {
            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(securableResourceClass, id, actionName) ?:
                   forbiddenOrNotFound(canRead, securableResourceClass, id)
        }
        if (isSave()) {
            // A save wont have an id on a securable resource so it will always be forbidden or allowed
            return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(securableResourceClass, id) ?: forbiddenDueToPermissions()
        }
        // If index on an id or show then if user can read then they can see it otherwise the id is notFound
        if ((isIndex() && id) || isShow()) {
            return canRead ?: notFound(securableResourceClass, id.toString())
        }
        // If index and no id then assume policy manager manages this and we know they can can read or not,
        // But return forbidden rather than not found
        if (isIndex() && !id) {
            return canRead ?: forbiddenDueToPermissions()
        }

        unauthorised("Unknown action [${actionName}]")
    }
}
