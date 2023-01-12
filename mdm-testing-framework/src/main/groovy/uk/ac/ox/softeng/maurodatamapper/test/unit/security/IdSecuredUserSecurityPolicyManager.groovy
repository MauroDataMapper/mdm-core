/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.test.unit.security

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.AbstractBasicSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser

/**
 * @since 10/02/2020
 */
class IdSecuredUserSecurityPolicyManager extends AbstractBasicSecurityPolicyManager {

    UUID unknownId
    UUID readAccessId
    UUID noAccessId
    UUID writeAccessId
    User user

    IdSecuredUserSecurityPolicyManager(User user, UUID unknownId, UUID noAccessId, UUID readAccessId, UUID writeAccessId) {
        this.unknownId = unknownId
        this.readAccessId = readAccessId
        this.noAccessId = noAccessId
        this.writeAccessId = writeAccessId
        this.user = user
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource>... securableResourceClass) {
        throw new ApiNotYetImplementedException('IDUSPXX', 'Listing of readable secured resource ids')
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        id in [readAccessId, writeAccessId] || owningSecureResourceId in [readAccessId, writeAccessId]
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        id in [readAccessId, writeAccessId]
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        id == writeAccessId
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                   UUID owningSecureResourceId, String action) {
        id == writeAccessId || owningSecureResourceId == writeAccessId
    }

    @Override
    boolean isApplicationAdministrator() {
        user.emailAddress == 'admin@maurodatamapper.com'
    }

    @Override
    boolean isAuthenticated() {
        UnloggedUser.instance.emailAddress != user.emailAddress && !isPending()
    }
}
