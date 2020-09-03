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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

abstract class ModelService<K extends Model> extends CatalogueItemService<K> implements SecurableResourceService<K> {

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelClass()
    }

    abstract Class<K> getModelClass()

    abstract List<K> findAllByContainerId(UUID containerId)

    abstract void deleteAllInContainer(Container container)

    abstract void removeAllFromContainer(Container container)

    abstract List<K> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                           boolean includeModelSuperseded, boolean includeDeleted)

    abstract List<K> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:])

    abstract List<UUID> findAllModelIdsWithChildren(List<K> models)

    abstract void removeVersionLinkFromModel(UUID modelId, VersionLink versionLink)

    abstract List<UUID> findAllSupersededModelIds(List<K> models)

    abstract List<K> findAllDocumentationSupersededModels(Map pagination)

    abstract List<K> findAllModelSupersededModels(Map pagination)

    abstract List<K> findAllDeletedModels(Map pagination)

    abstract List<K> findAllByFolderId(UUID folderId)

    abstract K validate(K model)

    abstract K saveWithBatching(K model)

    abstract K softDeleteModel(K model)

    abstract void permanentDeleteModel(K model)

    abstract K finaliseModel(K model, User user, Version modelVersion, List<Serializable> supersedeModelIds, Version version, VersionChangeType versionChangeType)

    abstract K finaliseModel(K model, User user, Version modelVersion, Version version, VersionChangeType versionChangeType)

    @Deprecated(forRemoval = true)
    abstract K finaliseModel(K model, User user, List<Serializable> supersedeModelIds, Version version, VersionChangeType versionChangeType)

    abstract K createNewBranchModelVersion(String branchName, K dataModel, User user, boolean copyPermissions,
                                           UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:])

    abstract K createNewDocumentationVersion(K dataModel, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments = [:])

    abstract K createNewForkModel(String label, K dataModel, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments = [:])

    ObjectDiff<K> diff(K thisModel, K otherModel) {
        thisModel.diff(otherModel)
    }

    @Override
    K checkFacetsAfterImportingCatalogueItem(K catalogueItem) {
        super.checkFacetsAfterImportingCatalogueItem(catalogueItem)
        if (catalogueItem.versionLinks) {
            catalogueItem.versionLinks.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        catalogueItem
    }

    @Override
    K updateFacetsAfterInsertingCatalogueItem(K catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.versionLinks) {
            catalogueItem.versionLinks.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            VersionLink.saveAll(catalogueItem.versionLinks)
        }
        catalogueItem
    }
}