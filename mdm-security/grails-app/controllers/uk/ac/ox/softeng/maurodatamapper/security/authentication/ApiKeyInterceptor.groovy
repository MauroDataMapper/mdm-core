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
package uk.ac.ox.softeng.maurodatamapper.security.authentication


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ApiKeyInterceptor implements MdmInterceptor {

    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'apiKeyId')
        Utils.toUuid(params, 'catalogueUserId')
    }

    UUID getId() {
        params.id ?: params.apiKeyId
    }

    boolean before() {
        checkIds()
        if (actionName in ['refreshApiKey', 'enableApiKey', 'disableApiKey']) {
            boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(ApiKey, getId(), CatalogueUser, params.catalogueUserId)
            return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(CatalogueUser, params.catalogueUserId) ?:
                   forbiddenOrNotFound(canRead, getId() ? ApiKey : CatalogueUser, getId() ?: params.catalogueUserId)
        }
        checkActionAuthorisationOnUnsecuredResource(ApiKey, getId(), CatalogueUser, params.catalogueUserId)
    }
}
