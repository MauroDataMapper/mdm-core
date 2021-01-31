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
package uk.ac.ox.softeng.maurodatamapper.security.basic

import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

abstract class AbstractBasicSecurityPolicyManager implements UserSecurityPolicyManager {

    User user

    abstract boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action)

    abstract boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                            UUID owningSecureResourceId, String action)

    @Override
    boolean userCanCreateResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                    UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, 'save')
    }

    @Override
    boolean userCanEditResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, 'update')
    }

    @Override
    boolean userCanDeleteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                    UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, 'softDelete')
    }

    @Override
    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, 'save')
    }

    @Override
    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, 'update')
    }

    @Override
    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent) {
        userCanWriteSecuredResourceId(securableResourceClass, id, permanent ? 'delete' : 'softDelete')
    }

    @Override
    List<String> userAvailableActions(Serializable resourceClass, UUID id) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }

    @Override
    List<String> userAvailableActions(String domainType, UUID id) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }

    @Override
    List<String> userAvailableActions(String resourceDomainType, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                      UUID owningSecureResourceId) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                      UUID owningSecureResourceId) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }

    @Override
    boolean isPending() {
        user.emailAddress == 'pending@test.com'
    }
}
