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

        // Allows for direct calls to secured resources but stops indexing on nested secured resources
        if (directCallsToIndexAllowed && isIndex() && !id) return true

        // The 3 tiers of writing are deleting, updating/editing and saving/creation
        // We will have to handle certain controls inside the controller
        if (isDelete()) {
            return currentUserSecurityPolicyManager.userCanDeleteSecuredResourceId(
                securableResourceClass, id, params.boolean('permanent', false)) ?: unauthorised()
        }
        if (isUpdate()) {
            return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }
        if (isSave()) {
            return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }
        // If index or show then if user can read then they can see it
        if (isIndex() || isShow()) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        unauthorised()
    }
}
