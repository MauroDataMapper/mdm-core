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
package uk.ac.ox.softeng.maurodatamapper.security.role


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

/**
 * This is a temporary interceptor until the UI can be updated to handle the new {@link SecurableResourceGroupRole}
 * @Deprecated (forRemoval = true)
 */
@Deprecated(forRemoval = true)
class PermissionsInterceptor implements MdmInterceptor {

    boolean before() {
        mapDomainTypeToClass('securableResource', true)

        currentUserSecurityPolicyManager.userCanReadSecuredResourceId(params.securableResourceClass, params.securableResourceId) ?:
        notFound(params.securableResourceClass, params.securableResourceId)
    }
}
