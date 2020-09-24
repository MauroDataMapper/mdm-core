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

import org.apache.commons.lang3.NotImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
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

    abstract K saveWithBatching(K model)

    abstract K softDeleteModel(K model)

    abstract void permanentDeleteModel(K model)

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

    Version latestModelVersion(String label) {
        latestFinalisedModel(label)?.modelVersion ?: Version.from('0.0.0')
    }

    ObjectDiff<K> mergeDiff(K leftModel, K rightModel) {
        def commonAncestor = commonAncestor(leftModel, rightModel)

        def left = commonAncestor.diff(leftModel)
        def right = commonAncestor.diff(rightModel)
        def top = rightModel.diff(leftModel)

        def diffs = []

        top.diffs.each {
            def fieldName = it.fieldName

            if (fieldName in left.diffs.fieldName) {
                if (fieldName in right.diffs.fieldName) {
                    if (it.class == FieldDiff) {
                        def commonAncestorValue = right.diffs.find { it.fieldName == fieldName }.left
                        diffs << [diff: it, conflict: true, commonAncestorValue: commonAncestorValue]
//                        true
                    } else if (it.class == ArrayDiff) {
                        def leftArrayDiff = left.diffs.find { it.fieldName == fieldName }
                        def rightArrayDiff = right.diffs.find { it.fieldName == fieldName }
                        it.created.each {
                            def diffIdentifier = it.diffIdentifier
                            if (diffIdentifier in leftArrayDiff.created.diffIdentifier) {
                                // top created, left created
                                diffs << [diff: it, change: 'created']
                            } else if (diffIdentifier in leftArrayDiff.modified.diffIdentifier) {
                                // top created, left modified
                                def commonAncestorValue = left.diffs.find { it.diffIdentifier == diffIdentifier }.left
                                diffs << [diff: it, change: 'created', conflict: true, commonAncestorValue: commonAncestorValue]
                            }
                        }
                        it.deleted.each {
                            def diffIdentifier = it.diffIdentifier
                            if (diffIdentifier in rightArrayDiff.modified.diffIdentifier) {
                                // top deleted, right modified
                                def commonAncestorValue = right.diffs.find { it.diffIdentifier == diffIdentifier }.left
                                diffs << [diff: it, change: 'deleted', conflict: true, commonAncestorValue: commonAncestorValue]
                            } else if (diffIdentifier in leftArrayDiff.deleted.diffIdentifier) {
                                // top deleted, right not modified, left deleted
                                diffs << [diff: it, change: 'deleted']
                            }
                        }
                        it.modified.each {
                            def diffIdentifier = it.diffIdentifier
                            if (diffIdentifier in leftArrayDiff.created.diffIdentifier) {
                                // top modified, right created
                                def commonAncestorValue = left.diffs.find { it.diffIdentifier == diffIdentifier }.left
                                diffs << [diff: it, change: 'modified', conflict: true, commonAncestorValue: commonAncestorValue]
                            } else if (diffIdentifier in leftArrayDiff.modified.diffIdentifier) {
                                if (diffIdentifier in rightArrayDiff.modified.diffIdentifier) {
                                    // top modified, left modified, right modified
                                    def commonAncestorValue = right.diffs.find { it.diffIdentifier == diffIdentifier }.left
                                    diffs << [diff: it, change: 'modified', conflict: true, commonAncestorValue: commonAncestorValue]
                                } else {
                                    // top modified, left modified, right not modified
                                    diffs << [diff: it, change: 'modified']
                                }
                            }
                        }
                    } else {
                        throw new NotImplementedException('ModelService.mergeDiff only implemented for types in  [FieldDiff, ArrayDiff]')
                    }
                } else {
//                        true
                    diffs << [diff: it]
                }
            } else {
//                    false
            }
        }

        top
    }

    K currentMainBranch(K model) {
        modelClass.byLabelAndBranchNameAndNotFinalised(model.label, 'main').get()
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
}