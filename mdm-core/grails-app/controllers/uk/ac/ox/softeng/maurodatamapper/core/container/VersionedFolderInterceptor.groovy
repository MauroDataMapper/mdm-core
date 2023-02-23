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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.TieredAccessSecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class VersionedFolderInterceptor extends TieredAccessSecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        VersionedFolder as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'folderId')
        Utils.toUuid(params, 'versionedFolderId')
        Utils.toUuid(params, 'otherVersionedFolderId')
    }

    @Override
    UUID getId() {
        params.id ?: params.versionedFolderId ?: params.folderId
    }

    @Override
    List<String> getReadAccessMethods() {
        ['search', 'newForkModel', 'latestModelVersion', 'latestFinalisedModel', 'currentMainBranch', 'availableBranches',
        'modelVersionTree', 'simpleModelVersionTree']
    }

    @Override
    List<String> getEditAccessMethods() {
        ['finalise', 'newDocumentationVersion', 'newBranchModelVersion']
    }

    boolean before() {
        securableResourceChecks()

        if (actionName in ['commonAncestor', 'diff', 'mergeDiff']) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(VersionedFolder, getId())) {
                return notFound(VersionedFolder, getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(VersionedFolder, params.otherVersionedFolderId)) {
                return notFound(VersionedFolder, params.otherVersionedFolderId)
            }
            return true
        }
        if (actionName == 'mergeInto') {
            //TODO confirm all correct
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), getId())) {
                return notFound(getSecuredClass(), getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), params.otherVersionedFolderId)) {
                return notFound(getSecuredClass(), params.otherVersionedFolderId)
            }

            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), params.otherVersionedFolderId, actionName) ?:
                   forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(getSecuredClass(), params.otherVersionedFolderId))
        }

        if (params.id || params.versionedFolderId) {
            return checkTieredAccessActionAuthorisationOnSecuredResource(VersionedFolder, getId(), true)
        }
        return checkActionAuthorisationOnSecuredResource(Folder, params.folderId, true)
    }
}
