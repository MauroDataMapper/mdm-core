/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import grails.gorm.DetachedCriteria

/**
 * @since 20/11/2019
 */
@Singleton
class PublicAccessSecurityPolicyManager extends AbstractBasicSecurityPolicyManager {

    User user

    @Override
    User getUser() {
        user ?: UnloggedUser.instance
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource>... securableResourceClasses) {
        securableResourceClasses.collectMany {securableResourceClass -> new DetachedCriteria(securableResourceClass).id().list()} as List<UUID>
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        true
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        true
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        true
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                   UUID owningSecureResourceId, String action) {
        true
    }

    @Override
    boolean isApplicationAdministrator() {
        true
    }

    @Override
    boolean isAuthenticated() {
        true
    }
}
