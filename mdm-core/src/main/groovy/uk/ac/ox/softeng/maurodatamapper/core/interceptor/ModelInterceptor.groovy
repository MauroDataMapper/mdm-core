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
package uk.ac.ox.softeng.maurodatamapper.core.interceptor


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.util.Utils

abstract class ModelInterceptor extends TieredAccessSecurableResourceInterceptor {

    abstract boolean checkExportModelAction()

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'folderId')
        Utils.toUuid(params, 'otherModelId')
    }

    @Override
    List<String> getPublicAccessMethods() {
        ['exporterProviders']
    }

    @Override
    List<String> getAuthenticatedAccessMethods() {
        ['importModel', 'importModels', 'importerProviders']
    }

    @Override
    List<String> getReadAccessMethods() {
        ['exportModel', 'newForkModel', 'latestModelVersion', 'latestFinalisedModel', 'currentMainBranch', 'availableBranches']
    }

    @Override
    List<String> getEditAccessMethods() {
        ['finalise', 'newDocumentationVersion', 'newBranchModelVersion']
    }

    @Override
    List<String> getApplicationAdminAccessMethods() {
        ['deleteAll', 'documentSuperseded', 'modelSuperseded', 'deleted']
    }

    boolean checkModelActionsAuthorised() {

        securableResourceChecks()

        if (params.containsKey('folderId')) {
            boolean canReadFolder = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, params.folderId)

            // We control addition of Terminologys into containers by using container permissions
            if (isSave()) {
                return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(Folder, params.folderId) ?:
                       forbiddenOrNotFound(canReadFolder, Folder, params.folderId)
            }
            if (actionName == 'changeFolder') {
                boolean canReadModel = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), getId())

                if (!currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), getId(), actionName)) {
                    return forbiddenOrNotFound(canReadModel, getSecuredClass(), getId())
                }

                if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, params.folderId)) {
                    return forbiddenOrNotFound(canReadFolder, Folder, params.folderId)
                }
                return true
            }
            if (isIndex() && params.folderId) {
                return canReadFolder ?: notFound(Folder, params.folderId)
            }
        }

        if (actionName in ['diff', 'commonAncestor', 'mergeDiff']) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), getId())) {
                return notFound(getSecuredClass(), getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), params.otherModelId)) {
                return notFound(getSecuredClass(), params.otherModelId)
            }
            return true
        }

        if (actionName == 'mergeInto') {
            //TODO confirm all correct
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), getId())) {
                return notFound(getSecuredClass(), getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), params.otherModelId)) {
                return notFound(getSecuredClass(), params.otherModelId)
            }

            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), getId(), actionName) ?:
                   forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(getSecuredClass(), getId()))
        }

        if (actionName == 'exportModels') {
            return checkExportModelAction()
        }

        if (actionName == 'finalise') {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), getId())) {
                return notFound(getSecuredClass(), getId())
            }
            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), getId(), actionName) ?:
                   forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(getSecuredClass(), getId()))
        }

        checkTieredAccessActionAuthorisationOnSecuredResource(getSecuredClass(), getId(), true)
    }


}
