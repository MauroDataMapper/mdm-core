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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeObjectDiffData
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

abstract class ModelService<K extends Model> extends CatalogueItemService<K> implements SecurableResourceService<K> {

    protected static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    @Autowired(required = false)
    Set<ModelItemService> modelItemServices

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelClass()
    }

    abstract VersionLinkService getVersionLinkService()

    abstract Class<K> getModelClass()

    abstract List<K> findAllByContainerId(UUID containerId)

    abstract void deleteAllInContainer(Container container)

    abstract void removeAllFromContainer(Container container)

    abstract List<K> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                           boolean includeModelSuperseded, boolean includeDeleted)

    abstract List<K> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:])

    abstract List<UUID> findAllModelIdsWithTreeChildren(List<K> models)

    abstract void removeVersionLinkFromModel(UUID modelId, VersionLink versionLink)

    abstract List<UUID> findAllSupersededModelIds(List<K> models)

    abstract List<K> findAllDocumentationSupersededModels(Map pagination)

    abstract List<K> findAllModelSupersededModels(Map pagination)

    abstract List<K> findAllDeletedModels(Map pagination)

    abstract List<K> findAllByFolderId(UUID folderId)

    abstract K validate(K model)

    abstract K saveModelWithContent(K model)

    abstract K saveModelNewContentOnly(K model)

    abstract K softDeleteModel(K model)

    abstract void permanentDeleteModel(K model)

    void deleteModelAndContent(K model) {
        throw new ApiNotYetImplementedException('MSXX', 'deleteModelAndContent')
    }

    /**
     * Merges changes made to {@code leftModel} in {@code mergeObjectDiff} into {@code rightModel}. {@code mergeObjectDiff} is based on the return
     * from ObjectDiff.mergeDiff(), customised by the user.
     * @param leftModel Source model
     * @param rightModel Target model
     * @param mergeObjectDiff Differences to merge, based on return from ObjectDiff.mergeDiff(), customised by user
     * @param userSecurityPolicyManager To get user details and permissions when copying "added" items
     * @param itemService Service which handles catalogueItems of the leftModel and rightModel type.
     * @return The model resulting from the merging of changes.
     */
    K mergeInto(K leftModel, K rightModel, MergeObjectDiffData mergeObjectDiff,
                UserSecurityPolicyManager userSecurityPolicyManager, itemService = this) {

        def item = itemService.get(mergeObjectDiff.leftId)

        mergeObjectDiff.diffs.each {
            diff ->
                diff.each {
                    mergeFieldDiff ->
                        if (mergeFieldDiff.value) {
                            item.setProperty(mergeFieldDiff.fieldName, mergeFieldDiff.value)
                        } else {
                            // if no value, then some combination of created, deleted, and modified may exist
                            if (mergeFieldDiff.fieldName == 'metadata') {
                                // call metadataService version of below
                                mergeFieldDiff.deleted.each {
                                    obj ->
                                        def metadata = metadataService.get(obj.id)
                                        metadataService.delete(metadata)
                                }

                                // copy additions from source to target object
                                mergeFieldDiff.created.each {
                                    obj ->
                                        def metadata = metadataService.get(obj.id)
                                        metadataService.copy(item, metadata, userSecurityPolicyManager)
                                }
                                // for modifications, recursively call this method
                                mergeFieldDiff.modified.each {
                                    obj ->
                                        mergeInto(leftModel, rightModel, obj,
                                                  userSecurityPolicyManager,
                                                  metadataService)
                                }
                            } else {
                                ModelItemService modelItemService = modelItemServices.find { it.handles(mergeFieldDiff.fieldName) }
                                if (modelItemService) {
                                    // apply deletions of children to target object
                                    mergeFieldDiff.deleted.each {
                                        obj ->
                                            def modelItem = modelItemService.get(obj.id) as ModelItem
                                            modelItemService.delete(modelItem)
                                    }

                                    def parentId = modelItemService.class == itemService.class ? mergeObjectDiff.leftId : null

                                    // copy additions from source to target object
                                    mergeFieldDiff.created.each {
                                        obj ->
                                            def modelItem = modelItemService.get(obj.id) as ModelItem
                                            parentId ?
                                            modelItemService.copy(rightModel, modelItem, userSecurityPolicyManager, parentId) :
                                            modelItemService.copy(rightModel, modelItem, userSecurityPolicyManager)
                                    }
                                    // for modifications, recursively call this method
                                    mergeFieldDiff.modified.each {
                                        obj ->
                                            mergeInto(leftModel, rightModel, obj,
                                                      userSecurityPolicyManager,
                                                      modelItemService)
                                    }
                                }
                            }
                        }
                }
        }
        rightModel
    }

    abstract K finaliseModel(K model, User user, Version modelVersion, VersionChangeType versionChangeType, List<Serializable> supersedeModelIds)

    abstract K finaliseModel(K model, User user, Version modelVersion, VersionChangeType versionChangeType)

    abstract K createNewBranchModelVersion(String branchName, K dataModel, User user, boolean copyPermissions,
                                           UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:])

    abstract K createNewDocumentationVersion(K dataModel, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments = [:])

    abstract K createNewForkModel(String label, K dataModel, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments = [:])

    abstract List<K> findAllByMetadataNamespace(String namespace)

    ObjectDiff<K> diff(K thisModel, K otherModel) {
        thisModel.diff(otherModel)
    }

    K commonAncestor(K leftModel, K rightModel) {
        // If left isnt finalised then get it's finalised parent
        if (!leftModel.finalised) {
            leftModel = get(VersionLinkService.findBySourceModelAndLinkType(leftModel, VersionLinkType.NEW_MODEL_VERSION_OF).targetModelId)
        }

        // If right isnt finalised then get it's finalised parent
        if (!rightModel.finalised) {
            rightModel = get(VersionLinkService.findBySourceModelAndLinkType(rightModel, VersionLinkType.NEW_MODEL_VERSION_OF).targetModelId)
        }

        // Choose the finalised parent with the lowest model version
        leftModel.modelVersion < rightModel.modelVersion ? leftModel : rightModel
    }

    K latestFinalisedModel(String label) {
        modelClass.byLabelAndFinalisedAndLatestModelVersion(label).get()
    }

    /*
     * Find latest model, defined as:
     * - branchName == 'main'
     * - AND
     * - if (a non-finalised version exists) then (that model)
     * - else (the latest finalised version)
     *
     * Used by pathService when seeking the latest model by label.
     */

    K latest(String label) {
        Model latestModel = null
        latestModel = modelClass.byLabelAndBranchNameAndNotFinalised(label, "main").get()
        if (!latestModel) {
            latestModel = modelClass.byLabelAndBranchNameAndFinalisedAndLatestModelVersion(label, "main").get()
        }

        latestModel
    }

    Version latestModelVersion(String label) {
        latestFinalisedModel(label)?.modelVersion ?: Version.from('0.0.0')
    }

    ObjectDiff<K> mergeDiff(K leftModel, K rightModel) {
        def commonAncestor = commonAncestor(leftModel, rightModel)

        def left = commonAncestor.diff(leftModel)
        def right = commonAncestor.diff(rightModel)
        def top = rightModel.diff(leftModel)

        top.mergeDiff(left, right)
    }

    K currentMainBranch(K model) {
        modelClass.byLabelAndBranchNameAndNotFinalised(model.label, VersionAwareConstraints.DEFAULT_BRANCH_NAME).get()
    }

    List<K> availableBranches(String label) {
        modelClass.byLabelAndNotFinalised(label).list()
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

    Version getParentModelVersion(K currentModel) {
        VersionLink versionLink = versionLinkService.findBySourceModelIdAndLinkType(currentModel.id, VersionLinkType.NEW_MODEL_VERSION_OF)
        if (!versionLink) return null
        Model parent = get(versionLink.targetModelId)
        parent.modelVersion
    }

    Version getNextModelVersion(K model, Version requestedModelVersion, VersionChangeType requestedVersionChangeType) {
        if (requestedModelVersion) {
            // Prefer requested model version
            return requestedModelVersion
        }
        // We need to get the parent model version first so we can work out what to increment
        Version parentModelVersion = getParentModelVersion(model)

        if (!parentModelVersion) {
            // No parent model then set the current version to 0 to allow the first finalisation to be defined using the versionChangeType
            parentModelVersion = Version.from('0.0.0')
        }

        if (requestedVersionChangeType) {
            // Someone requests a type change
            // Increment the parent version by that amount
            switch (requestedVersionChangeType) {
                case VersionChangeType.MAJOR:
                    return Version.nextMajorVersion(parentModelVersion)
                    break
                case VersionChangeType.MINOR:
                    return Version.nextMinorVersion(parentModelVersion)
                    break
                case VersionChangeType.PATCH:
                    return Version.nextPatchVersion(parentModelVersion)
                    break
            }
        }
        // If no requested version change type then just increment by the next major version
        Version.nextMajorVersion(parentModelVersion)
    }

    void checkfinaliseModel(K model, boolean finalised) {
        if (finalised && !model.finalised) {
            model.finalised = finalised
            model.dateFinalised = model.finalised ? OffsetDateTime.now() : null
        }
        if (model.finalised && !model.modelVersion) {
            model.modelVersion = Version.from('1.0.0')
        }
    }

    @Override
    void deleteAllFacetDataByCatalogueItemIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByCatalogueItemIds(catalogueItemIds)
        versionLinkService.deleteAllByCatalogueItemIds(catalogueItemIds)
    }
}