/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class FolderInterceptor extends SecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        Folder as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'folderId')
        Utils.toUuid(params, 'versionedFolderId')
        if (params.destinationFolderId != 'root') Utils.toUuid(params, 'destinationFolderId')
        params.folderId = params.folderId ?: params.versionedFolderId
    }

    @Override
    UUID getId() {
        params.id ?: params.folderId
    }

    boolean before() {
        securableResourceChecks()

        if (actionName == 'search') {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, getId()) ?:
                   notFound(Folder, getId())
        }

        if (actionName == 'changeFolder') {
            UUID destinationFolderId = params.destinationFolderId == 'root' ? null : params.destinationFolderId
            boolean canReadDestinationFolder = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, destinationFolderId)
            boolean canReadFolderBeingMoved = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, getId())

            if (!currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(Folder, getId(), actionName)) {
                return forbiddenOrNotFound(canReadFolderBeingMoved, Folder, getId())
            }

            if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, destinationFolderId)) {
                return forbiddenOrNotFound(canReadDestinationFolder, Folder, params.destinationFolderId)
            }
            return true
        }

        if (actionName == 'exportFolder') {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, id)) {
                return forbiddenOrNotFound(false, Folder, id)
            }
            return true
        }

        checkActionAuthorisationOnSecuredResource(Folder, getId(), true)
    }
}
