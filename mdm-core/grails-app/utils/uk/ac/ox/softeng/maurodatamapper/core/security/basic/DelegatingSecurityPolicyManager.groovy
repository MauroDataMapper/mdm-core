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
package uk.ac.ox.softeng.maurodatamapper.core.security.basic

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

/**
 * @since 15/12/2021
 */
class DelegatingSecurityPolicyManager implements UserSecurityPolicyManager {

    UserSecurityPolicyManager delegate

    DelegatingSecurityPolicyManager(Class<UserSecurityPolicyManager> singletonDelegateClass) {
        try {
            singletonDelegateClass.getMethod('getInstance')
            delegate = singletonDelegateClass.getInstance()
        } catch (Exception ignored) {
            throw new ApiInternalException('DSPM', 'Cannot delegate to a non-singleton')
        }
    }

    @Override
    User getUser() {
        return delegate.getUser()
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(
        Class<? extends SecurableResource> securableResourceClass) {
        return delegate.listReadableSecuredResourceIds(securableResourceClass)
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        return delegate.userCanReadResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanCreateResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        return delegate.userCanCreateResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanEditResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        return delegate.userCanEditResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanDeleteResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        return delegate.userCanDeleteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        return delegate.userCanReadSecuredResourceId(securableResourceClass, id)
    }

    @Override
    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        return delegate.userCanCreateSecuredResourceId(securableResourceClass, id)
    }

    @Override
    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        return delegate.userCanEditSecuredResourceId(securableResourceClass, id)
    }

    @Override
    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent) {
        return delegate.userCanDeleteSecuredResourceId(securableResourceClass, id, permanent)
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        return delegate.userCanWriteSecuredResourceId(securableResourceClass, id, action)
    }

    @Override
    List<String> userAvailableActions(Serializable securableResourceClass, UUID id) {
        return delegate.userAvailableActions(securableResourceClass, id)
    }

    @Override
    List<String> userAvailableActions(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        return delegate.userAvailableActions(securableResourceClass, id)
    }

    @Override
    List<String> userAvailableActions(String securableResourceDomainType, UUID id) {
        return delegate.userAvailableActions(securableResourceDomainType, id)
    }

    @Override
    List<String> userAvailableActions(String resourceDomainType, UUID id,
                                      Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        return delegate.userAvailableActions(resourceDomainType, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id,
                                      Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        return delegate.userAvailableActions(resourceClass, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    List<String> userAvailableTreeActions(String securableResourceDomainType, UUID id) {
        return delegate.userAvailableTreeActions(securableResourceDomainType, id)
    }

    @Override
    List<String> userAvailableTreeActions(String resourceDomainType, UUID id, String owningSecureResourceDomainType,
                                          UUID owningSecureResourceId) {
        return delegate.userAvailableTreeActions(resourceDomainType, id, owningSecureResourceDomainType, owningSecureResourceId)
    }

    @Override
    boolean isApplicationAdministrator() {
        return delegate.isApplicationAdministrator()
    }

    @Override
    boolean isAuthenticated() {
        return delegate.isAuthenticated()
    }

    @Override
    boolean isPending() {
        return delegate.isPending()
    }
}
