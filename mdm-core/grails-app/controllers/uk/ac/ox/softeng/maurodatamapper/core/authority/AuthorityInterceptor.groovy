/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils


class AuthorityInterceptor extends SecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        Authority as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'authority')
    }

    @Override
    UUID getId() {
        params.id ?: params.authority
    }

    boolean before() {
        securableResourceChecks()
        checkActionAuthorisationOnSecuredResource(Authority, getId(), true)
    }

    @Override
    boolean forbiddenOrNotFound(boolean canRead, Class resourceClass, UUID resourceId) {
        boolean exists = resourceId ? Authority.get(resourceId) : false
        canRead && exists ? forbiddenDueToPermissions() : notFound(resourceClass, resourceId.toString())
    }
}
