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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ModelImportInterceptor extends FacetInterceptor {

    @Override
    Class getFacetClass() {
        ModelImport
    }


    UUID getId() {
        params.modelImportId ?: params.id
    }

    @Override
    void checkAdditionalIds() {
        Utils.toUuid(params,"modelImportId")
    }

    boolean before() {
        facetResourceChecks()
        if (actionName == 'save') {

            if (Utils.parentClassIsAssignableFromChild(SecurableResource, getOwningClass())) {
                boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(getFacetClass(), getId(), getOwningClass(), getOwningId())
                return currentUserSecurityPolicyManager.userCanEditResourceId(getFacetClass(), getId(),
                        getOwningClass(),getOwningId()) ?:
                        forbiddenOrNotFound(canRead, getId() ? getFacetClass() : getOwningClass(), getId() ?: getOwningId())

            }

            Model model = proxyHandler.unwrapIfProxy(getOwningModel())
            boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(getFacetClass(), getId(), getOwningClass(), getOwningId())
            return currentUserSecurityPolicyManager.userCanEditResourceId(getFacetClass(), getId(),
                    model.class,getOwningId()) ?:
                    forbiddenOrNotFound(canRead, getId() ? getFacetClass() : model.class, getId() ?: getOwningId())
        }

        checkActionAllowedOnFacet()

    }
}
