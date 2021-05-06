/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeObjectDiffData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.VersionLinkAwareService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormValidateable
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset


@Slf4j
abstract class ModelService<K extends Model> extends CatalogueItemService<K> implements SecurableResourceService<K>, VersionLinkAwareService<K> {

    protected static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    @Autowired(required = false)
    AuthorityService authorityService

    @Autowired(required = false)
    Set<ModelItemService> modelItemServices

    @Autowired
    VersionLinkService versionLinkService

    @Autowired
    BreadcrumbTreeService breadcrumbTreeService

    @Autowired
    EditService editService

    @Autowired
    MessageSource messageSource

    @Autowired(required = false)
    Set<ModelImporterProviderService> modelImporterProviderServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    Class<K> getCatalogueItemClass() {
        getModelClass()
    }

    Class<K> getVersionLinkAwareClass() {
        getModelClass()
    }

    abstract Class<K> getModelClass()

    abstract String getUrlResourceName()

    abstract List<K> findAllByContainerId(UUID containerId)

    abstract void deleteAllInContainer(Container container)

    abstract void removeAllFromContainer(Container container)

    abstract List<K> findAllReadableModels(List<UUID> constrainedIds, boolean includeDeleted)

    abstract List<K> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:])

    abstract List<UUID> findAllModelIdsWithTreeChildren(List<K> models)

    abstract List<K> findAllDeletedModels(Map pagination)

    abstract List<K> findAllByFolderId(UUID folderId)

    abstract K validate(K model)

    abstract K saveModelWithContent(K model)

    abstract K saveModelNewContentOnly(K model)

    abstract void delete(K model, boolean permanent)

    abstract void delete(K model, boolean permanent, boolean flush)

    abstract int countByLabel(String label)

    abstract List<K> findAllByLabel(String label)

    abstract int countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName)

    abstract K copyModel(K original,
                         Folder folderToCopyInto,
                         User copier,
                         boolean copyPermissions,
                         String label,
                         Version copyDocVersion,
                         String branchName,
                         boolean throwErrors,
                         UserSecurityPolicyManager userSecurityPolicyManager)

    abstract List<K> findAllByMetadataNamespace(String namespace, Map pagination = [:])

    abstract List<K> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:])

    abstract ModelImporterProviderService<K, ? extends ModelImporterProviderServiceParameters> getJsonModelImporterProviderService()

    void deleteModelAndContent(K model) {
        throw new ApiNotYetImplementedException('MSXX', 'deleteModelAndContent')
    }

    K shallowValidate(K model) {
        log.debug('Shallow validating model')
        long st = System.currentTimeMillis()
        model.validate(deepValidate: false)
        log.debug('Validated Model in {}', Utils.timeTaken(st))
        model
    }

    K softDeleteModel(K model) {
        model?.deleted = true
        model
    }

    void permanentDeleteModel(K model) {
        delete(model, true)
    }

    K undoSoftDeleteModel(K model) {
        model?.deleted = false
        model
    }

    List<K> findAllByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService, Map pagination = [:]) {
        findAllByMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name, pagination)
    }

    List<K> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                  boolean includeModelSuperseded, boolean includeDeleted) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(getModelClass())
        if (!ids) return []
        List<UUID> constrainedIds
        // The list of ids are ALL the readable ids by the user, no matter the model status
        if (includeDocumentSuperseded && includeModelSuperseded) {
            constrainedIds = new ArrayList<>(ids)
        } else if (includeModelSuperseded) {
            constrainedIds = findAllExcludingDocumentSupersededIds(ids)
        } else if (includeDocumentSuperseded) {
            constrainedIds = findAllExcludingModelSupersededIds(ids)
        } else {
            constrainedIds = findAllExcludingDocumentAndModelSupersededIds(ids)
        }
        if (!constrainedIds) return []
        findAllReadableModels(constrainedIds, includeDeleted)
    }

    K finaliseModel(K model, User user, Version modelVersion, VersionChangeType versionChangeType,
                    String versionTag) {
        log.debug('Finalising model')
        long start = System.currentTimeMillis()

        model.finalised = true
        model.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        // No requirement to have a breadcrumbtree
        breadcrumbTreeService.finalise(model.breadcrumbTree)

        model.modelVersion = getNextModelVersion(model, modelVersion, versionChangeType)

        model.modelVersionTag = versionTag

        model.addToAnnotations(createdBy: user.emailAddress, label: 'Finalised Model',
                               description: "${getModelClass().simpleName} finalised by ${user.firstName} ${user.lastName} on " +
                                            "${OffsetDateTimeConverter.toString(model.dateFinalised)}")
        editService.createAndSaveEdit(EditTitle.FINALISE, model.id, model.domainType,
                                      "${getModelClass().simpleName} finalised by ${user.firstName} ${user.lastName} on " +
                                      "${OffsetDateTimeConverter.toString(model.dateFinalised)}",
                                      user)
        log.debug('Model finalised took {}', Utils.timeTaken(start))
        model
    }

    K copyModelAsNewBranchModel(K original, User copier, boolean copyPermissions, String label, String branchName, boolean throwErrors,
                                UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, Version.from('1'), branchName, throwErrors, userSecurityPolicyManager)
    }

    K copyModelAsNewForkModel(K original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                              UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager)
    }

    K copyModelAsNewDocumentationModel(K original, User copier, boolean copyPermissions, String label, Version copyDocVersion, String branchName,
                                       boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager)
    }

    K copyModel(K original,
                User copier,
                boolean copyPermissions,
                String label,
                Version copyDocVersion,
                String branchName,
                boolean throwErrors,
                UserSecurityPolicyManager userSecurityPolicyManager) {
        Folder folder = proxyHandler.unwrapIfProxy(original.folder) as Folder
        copyModel(original, folder, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager)
    }

    K createNewDocumentationVersion(K model, User user, boolean copyPermissions,
                                    UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        if (!newVersionCreationIsAllowed(model)) return model

        K newDocVersion = copyModelAsNewDocumentationModel(model,
                                                           user,
                                                           copyPermissions,
                                                           model.label,
                                                           Version.nextMajorVersion(model.documentationVersion),
                                                           model.branchName,
                                                           additionalArguments.throwErrors as boolean,
                                                           userSecurityPolicyManager,)
        setModelIsNewDocumentationVersionOfModel(newDocVersion, model, user)
        if (additionalArguments.moveDataFlows) {
            throw new ApiNotYetImplementedException('DMSXX', 'DataModel moving of DataFlows')
            //            moveTargetDataFlows(dataModel, newDocVersion)
        }
        newDocVersion
    }

    K createNewForkModel(String label, K model, User user, boolean copyPermissions,
                         UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        if (!newVersionCreationIsAllowed(model)) return model

        K newForkModel = copyModelAsNewForkModel(model, user, copyPermissions, label,
                                                 additionalArguments.throwErrors as boolean,
                                                 userSecurityPolicyManager)
        setModelIsNewForkModelOfModel(newForkModel, model, user)
        if (additionalArguments.copyDataFlows) {
            throw new ApiNotYetImplementedException('DMSXX', 'DataModel copying of DataFlows')
            //copyTargetDataFlows(dataModel, newForkModel, user)
        }
        newForkModel
    }

    K createNewBranchModelVersion(String branchName, K model, User user, boolean copyPermissions,
                                  UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        if (!newVersionCreationIsAllowed(model)) return model

        // Check if the branch name is already being used
        if (countAllByLabelAndBranchNameAndNotFinalised(model.label, branchName) > 0) {
            (model as GormValidateable).errors.reject('version.aware.label.branch.name.already.exists',
                                                      ['branchName', getModelClass(), branchName, model.label] as Object[],
                                                      'Property [{0}] of class [{1}] with value [{2}] already exists for label [{3}]')
            return model
        }

        // We know at this point the datamodel is finalised which means its branch name == main so we need to check no unfinalised main branch exists
        boolean draftModelOnMainBranchForLabel =
            countAllByLabelAndBranchNameAndNotFinalised(model.label, VersionAwareConstraints.DEFAULT_BRANCH_NAME) > 0

        if (!draftModelOnMainBranchForLabel) {
            K newMainBranchModelVersion = copyModelAsNewBranchModel(model,
                                                                    user,
                                                                    copyPermissions,
                                                                    model.label,
                                                                    VersionAwareConstraints.DEFAULT_BRANCH_NAME,
                                                                    additionalArguments.throwErrors as boolean,
                                                                    userSecurityPolicyManager)
            setModelIsNewBranchModelVersionOfModel(newMainBranchModelVersion, model, user)

            if (additionalArguments.moveDataFlows) {
                throw new ApiNotYetImplementedException('DMSXX', 'DataModel moving of DataFlows')
                //            moveTargetDataFlows(dataModel, newMainBranchModelVersion)
            }

            // If the branch name isn't main and the main branch doesnt exist then we need to validate and save it
            // otherwise return the new model
            if (branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME) {
                return newMainBranchModelVersion
            } else {
                if ((newMainBranchModelVersion as GormValidateable).validate()) saveModelWithContent(newMainBranchModelVersion)
                else throw new ApiInvalidModelException('DMSXX', 'Copied (newMainBranchModelVersion) Model is invalid',
                                                        (newMainBranchModelVersion as GormValidateable).errors, messageSource)
            }
        }

        K newBranchModelVersion = copyModelAsNewBranchModel(model,
                                                            user,
                                                            copyPermissions,
                                                            model.label,
                                                            branchName,
                                                            additionalArguments.throwErrors as boolean,
                                                            userSecurityPolicyManager)

        setModelIsNewBranchModelVersionOfModel(newBranchModelVersion, model, user)

        if (additionalArguments.moveDataFlows) {
            throw new ApiNotYetImplementedException('DMSXX', 'DataModel moving of DataFlows')
            //            moveTargetDataFlows(dataModel, newBranchModelVersion)
        }
        newBranchModelVersion
    }

    boolean newVersionCreationIsAllowed(K model) {
        if (!model.finalised) {
            (model as GormValidateable).errors.reject('invalid.version.aware.new.version.not.finalised.message',
                                                      [model.domainType, model.label, model.id] as Object[],
                                                      '{0} [{1}({2})] cannot have a new version as it is not finalised')
            return false
        }
        K superseding = findModelDocumentationSuperseding(model)
        if (superseding) {
            (model as GormValidateable).errors.reject('invalid.version.aware.new.version.superseded.message',
                                                      [model.domainType, model.label, model.id, superseding.label, superseding.id] as Object[],
                                                      '{0} [{1}({2})] cannot have a new version as it has been superseded by [{3}({4})]')
            return false
        }

        true
    }

    K findModelSuperseding(K model) {
        VersionLink link = versionLinkService.findLatestLinkSupersedingModelId(getModelClass().simpleName, model.id)
        if (!link) return null
        link.multiFacetAwareItemId == model.id ? get(link.targetModelId) : get(link.multiFacetAwareItemId)
    }

    K findModelDocumentationSuperseding(K model) {
        VersionLink link = versionLinkService.findLatestLinkDocumentationSupersedingModelId(model.domainType, model.id)
        if (!link) return null
        link.multiFacetAwareItemId == model.id ? get(link.targetModelId) : get(link.multiFacetAwareItemId)
    }

    /**
     * Merges changes made to {@code leftModel} in {@code mergeObjectDiff} into {@code rightModel}. {@code mergeObjectDiff} is based on the return
     * from ObjectDiff.mergeDiff(), customised by the user.
     * @param sourceModel Source model
     * @param targetModel Target model
     * @param modelMergeObjectDiff Differences to merge, based on return from ObjectDiff.mergeDiff(), customised by user
     * @param userSecurityPolicyManager To get user details and permissions when copying "added" items
     * @param domainService Service which handles catalogueItems of the leftModel and rightModel type.
     * @return The model resulting from the merging of changes.
     */
    K mergeObjectDiffIntoModel(MergeObjectDiffData modelMergeObjectDiff, K targetModel,
                               UserSecurityPolicyManager userSecurityPolicyManager) {
        //TODO validation on saving merges
        if (!modelMergeObjectDiff.hasDiffs()) return targetModel
        log.debug('Merging {} diffs into model {}', modelMergeObjectDiff.getValidDiffs().size(), targetModel.label)
        modelMergeObjectDiff.getValidDiffs().each {mergeFieldDiff ->
            log.debug('{}', mergeFieldDiff.summary)

            if (mergeFieldDiff.isFieldChange()) {
                targetModel.setProperty(mergeFieldDiff.fieldName, mergeFieldDiff.value)
            } else if (mergeFieldDiff.isMetadataChange()) {
                mergeMetadataIntoCatalogueItem(mergeFieldDiff, targetModel, userSecurityPolicyManager)
            } else {
                ModelItemService modelItemService = modelItemServices.find {it.handles(mergeFieldDiff.fieldName)}
                if (modelItemService) {
                    modelItemService.processMergeFieldDiff(mergeFieldDiff, targetModel, userSecurityPolicyManager)
                } else {
                    log.error('Unknown ModelItem field to merge [{}]', mergeFieldDiff.fieldName)
                }
            }
        }
        targetModel
    }

    List<VersionTreeModel> buildModelVersionTree(K instance, VersionLinkType versionLinkType, UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!userSecurityPolicyManager.userCanReadSecuredResourceId(instance.class, instance.id)) return []

        List<VersionLink> versionLinks = versionLinkService.findAllByTargetModelId(instance.id)
        VersionTreeModel rootVersionTreeModel = new VersionTreeModel(instance, versionLinkType)
        List<VersionTreeModel> versionTreeModelList = [rootVersionTreeModel]

        for (link in versionLinks) {
            K linkedModel = get(link.multiFacetAwareItemId)
            rootVersionTreeModel.addTarget(linkedModel.id, link.linkType)
            versionTreeModelList.addAll(buildModelVersionTree(linkedModel, link.linkType, userSecurityPolicyManager))
        }
        versionTreeModelList
    }

    ObjectDiff<K> getDiffForModels(K thisModel, K otherModel) {
        thisModel.diff(otherModel)
    }

    K findCommonAncestorBetweenModels(K leftModel, K rightModel) {
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

    K findOldestAncestor(K model) {
        VersionLink versionLink = versionLinkService.findBySourceModel(model)
        if (!versionLink)
            return model
        K parentModel = get(versionLink.targetModelId)
        findOldestAncestor(parentModel)
    }

    K findLatestFinalisedModelByLabel(String label) {
        modelClass.byLabelAndBranchNameAndFinalisedAndLatestModelVersion(label, VersionAwareConstraints.DEFAULT_BRANCH_NAME).get() as K
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

    K findLatestModelByLabel(String label) {
        findCurrentMainBranchByLabel(label) ?: findLatestFinalisedModelByLabel(label)
    }

    /**
     * Use findCurrentMainBranchByLabel instead
     * @param model
     * @return
     */
    @Deprecated
    K findCurrentMainBranchForModel(K model) {
        findCurrentMainBranchByLabel(model.label)
    }

    K findCurrentMainBranchByLabel(String label) {
        modelClass.byLabelAndBranchNameAndNotFinalised(label, VersionAwareConstraints.DEFAULT_BRANCH_NAME).get() as K
    }

    List<K> findAllAvailableBranchesByLabel(String label) {
        modelClass.byLabelAndNotFinalised(label).list() as List<K>
    }

    Version getLatestModelVersionByLabel(String label) {
        findLatestFinalisedModelByLabel(label)?.modelVersion ?: Version.from('0.0.0')
    }

    ObjectDiff<K> getMergeDiffForModels(K leftModel, K rightModel) {
        def commonAncestor = findCommonAncestorBetweenModels(leftModel, rightModel)

        def left = commonAncestor.diff(leftModel)
        def right = commonAncestor.diff(rightModel)
        def top = rightModel.diff(leftModel)

        top.mergeDiff(left, right)
    }

    @Override
    K checkFacetsAfterImportingCatalogueItem(K catalogueItem) {
        super.checkFacetsAfterImportingCatalogueItem(catalogueItem)
        if (catalogueItem.versionLinks) {
            catalogueItem.versionLinks.each {
                it.multiFacetAwareItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        catalogueItem
    }

    /**
     * After importing a model, either force the authority to be the default authority, or otherwise create or use an existing authority
     * @param importingUser
     * @param model
     * @param useDefaultAuthority If true then any imported authority will be overwritten with the default authority
     * @return The model with its authority checked
     */
    K checkAuthority(User importingUser, K model, boolean useDefaultAuthority) {
        if (useDefaultAuthority || !model.authority) {
            model.authority = authorityService.getDefaultAuthority()
        } else {
            //If the authority already exists then use it, otherwise create a new one but set the createdBy property
            Authority exists = authorityService.findByLabel(model.authority.label)
            if (exists) {
                model.authority = exists
            } else {
                model.authority.createdBy = importingUser.emailAddress

                //Save this new authority so that it is available later for ModelLabelValidator
                authorityService.save(model.authority)
            }
        }
        model
    }

    @Override
    K updateFacetsAfterInsertingCatalogueItem(K catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.versionLinks) {
            catalogueItem.versionLinks.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = catalogueItem.getId()
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

    void checkfinaliseModel(K model, Boolean finalised) {
        if (finalised && !model.finalised) {
            model.finalised = finalised
            model.dateFinalised = model.finalised ? OffsetDateTime.now() : null
        }
        if (model.finalised && !model.modelVersion) {
            model.modelVersion = Version.from('1.0.0')
        }
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        versionLinkService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
    }

    void checkDocumentationVersion(K model, boolean importAsNewDocumentationVersion, User catalogueUser) {
        if (importAsNewDocumentationVersion) {

            if (countByLabel(model.label)) {
                List<K> existingModels = findAllByLabel(model.label)
                existingModels.each {existing ->
                    log.debug('Setting Model as new documentation version of [{}:{}]', existing.label, existing.documentationVersion)
                    if (!existing.finalised) finaliseModel(existing, catalogueUser, null, null, null)
                    setModelIsNewDocumentationVersionOfModel(model, existing, catalogueUser)
                }
                Version latestVersion = existingModels.max {it.documentationVersion}.documentationVersion
                model.documentationVersion = Version.nextMajorVersion(latestVersion)

            } else log.info('Marked as importAsNewDocumentationVersion but no existing Models with label [{}]', model.label)
        }
    }

    void checkBranchModelVersion(K model, Boolean importAsNewBranchModelVersion, String branchName, User catalogueUser) {
        if (importAsNewBranchModelVersion) {

            if (countByLabel(model.label)) {
                K latest = findLatestFinalisedModelByLabel(model.label)
                if (latest) {
                    setModelIsNewBranchModelVersionOfModel(model, latest, catalogueUser)
                    model.dateFinalised = null
                    model.finalised = false
                    model.modelVersion = null
                    model.branchName = branchName ?: VersionAwareConstraints.DEFAULT_BRANCH_NAME
                    model.documentationVersion = Version.from('1')
                } else {
                    throw new ApiBadRequestException('MSXX', 'Request to importAsNewBranchModelVersion but no finalised model to use')
                }
            } else log.info('Marked as importAsNewBranchModelVersion but no existing Models with label [{}]', model.label)
        }
    }

    void setModelIsNewForkModelOfModel(K newModel, K oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_FORK_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setModelIsNewDocumentationVersionOfModel(K newModel, K oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setModelIsNewBranchModelVersionOfModel(K newModel, K oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setModelIsFromModel(K source, K target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetMultiFacetAwareItem: target)
    }
}