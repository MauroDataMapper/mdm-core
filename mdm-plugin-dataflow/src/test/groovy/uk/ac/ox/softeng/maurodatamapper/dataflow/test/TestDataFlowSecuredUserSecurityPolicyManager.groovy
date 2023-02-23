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
package uk.ac.ox.softeng.maurodatamapper.dataflow.test


import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.AbstractBasicSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser

class TestDataFlowSecuredUserSecurityPolicyManager extends AbstractBasicSecurityPolicyManager {

    List<UUID> unknownIds
    List<UUID> noAccessIds
    List<UUID> readAccessIds
    List<UUID> writeAccessIds
    User user

    TestDataFlowSecuredUserSecurityPolicyManager(User user, List<UUID> unknownIds, List<UUID> noAccessIds, List<UUID> readAccessIds,
                                                 List<UUID> writeAccessIds) {
        this.user = user
        this.unknownIds = unknownIds
        this.noAccessIds = noAccessIds
        this.readAccessIds = readAccessIds
        this.writeAccessIds = writeAccessIds
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource>... securableResourceClass) {
        (readAccessIds + writeAccessIds).toSet().toList()
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        id in ([readAccessIds, writeAccessIds].flatten() as List<UUID>) || owningSecureResourceId in
        ([readAccessIds, writeAccessIds].flatten() as List<UUID>)
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        id in ([readAccessIds, writeAccessIds].flatten() as List<UUID>)
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        id in writeAccessIds
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                   UUID owningSecureResourceId, String action) {
        id in writeAccessIds || owningSecureResourceId in writeAccessIds
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
