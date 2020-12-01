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
package uk.ac.ox.softeng.maurodatamapper.security

/**
 * @since 18/11/2019
 */
interface UserSecurityPolicyManager {


    User getUser()

    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass)

    boolean userCanReadResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanCreateResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanEditResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanDeleteResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanChangeFolderOfSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent)

    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action)

    List<String> userAvailableActions(Serializable securableResourceClass, UUID id)

    List<String> userAvailableActions(Class<? extends SecurableResource> securableResourceClass, UUID id)

    List<String> userAvailableActions(String securableResourceDomainType, UUID id)

    List<String> userAvailableActions(String resourceDomainType, UUID id,
                                      Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    List<String> userAvailableActions(Class resourceClass, UUID id,
                                      Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean isApplicationAdministrator()

    boolean isAuthenticated()

    boolean isPending()
}