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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.ObjectPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.DomainService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.VersionLinkAwareService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormValidateable
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.function.Predicate

@Slf4j
abstract class ModelService<K extends Model> extends CatalogueItemService<K> implements SecurableResourceService<K>, VersionLinkAwareService<K> {

    protected static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    @Autowired(required = false)
    AuthorityService authorityService

    @Autowired(required = false)
    Set<ModelItemService> modelItemServices

    @Autowired(required = false)
    Set<DomainService> domainServices

    @Autowired(required = false)
    Set<MultiFacetItemAwareService> multiFacetItemAwareServices

    @Autowired
    VersionLinkService versionLinkService

    @Autowired
    BreadcrumbTreeService breadcrumbTreeService

    @Autowired
    EditService editService

    @Autowired
    PathService pathService

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

    abstract int countByAuthorityAndLabelAndBranchNameAndNotFinalised(Authority authority, String label, String branchName)

    abstract int countByAuthorityAndLabelAndVersion(Authority authority, String label, Version modelVersion)

    abstract int countByAuthorityAndLabel(Authority authority, String label)

    abstract List<K> findAllByAuthorityAndLabel(Authority authority, String label)

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

    Set<DomainService> getDomainServices() {
        domainServices.add(this)
        domainServices
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

    @Override
    K findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        String[] split = pathIdentifier.split(PathNode.ESCAPED_MODEL_PATH_IDENTIFIER_SEPARATOR)
        String label = split[0]

        // A specific identity of the model has been requested so make sure we limit to that
        if (split.size() == 2) {
            String identity = split[1]
            DetachedCriteria criteria = parentId ? modelClass.byFolderId(parentId) : modelClass.by()

            criteria.eq('label', label)

            // Try the search by modelVersion or branchName and no modelVersion
            // This will return the requested model or the latest non-finalised main branch
            if (Version.isVersionable(identity)) {
                criteria.eq('modelVersion', Version.from(identity))
            } else {
                // Need to make sure that if the main branch is requested we return the one without a modelVersion
                criteria.eq('branchName', identity)
                    .isNull('modelVersion')
            }
            return criteria.get() as K
        }

        // If no identity part then we can just get the latest model by the label
        findLatestModelByLabel(label)
    }

    K finaliseModel(K model, User user, Version requestedModelVersion, VersionChangeType versionChangeType,
                    String versionTag) {
        log.debug('Finalising model')
        long start = System.currentTimeMillis()

        model.finalised = true
        model.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        // No requirement to have a breadcrumbtree
        breadcrumbTreeService.finalise(model.breadcrumbTree)

        model.modelVersion = getNextModelVersion(model, requestedModelVersion, versionChangeType)

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
        if (countByAuthorityAndLabelAndBranchNameAndNotFinalised(model.authority, model.label, branchName) > 0) {
            (model as GormValidateable).errors.reject('version.aware.label.branch.name.already.exists',
                                                      ['branchName', getModelClass(), branchName, model.label] as Object[],
                                                      'Property [{0}] of class [{1}] with value [{2}] already exists for label [{3}]')
            return model
        }

        // We know at this point the datamodel is finalised which means its branch name == main so we need to check no unfinalised main branch exists
        boolean draftModelOnMainBranchForLabel = countByAuthorityAndLabelAndBranchNameAndNotFinalised(model.authority, model.label,
                                                                                                      VersionAwareConstraints.DEFAULT_BRANCH_NAME) > 0

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
                if ((newMainBranchModelVersion as GormValidateable).validate()) {
                    saveModelWithContent(newMainBranchModelVersion)
                    if (securityPolicyManagerService) {
                        userSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(newMainBranchModelVersion, user,
                                                                                                                 newMainBranchModelVersion.label)
                    }
                } else throw new ApiInvalidModelException('DMSXX', 'Copied (newMainBranchModelVersion) Model is invalid',
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
     * @param objectPatchData Differences to merge, based on return from ObjectDiff.mergeDiff(), customised by user
     * @param userSecurityPolicyManager To get user details and permissions when copying "added" items
     * @param domainService Service which handles catalogueItems of the leftModel and rightModel type.
     * @return The model resulting from the merging of changes.
     */
    K mergeObjectPatchDataIntoModel(ObjectPatchData objectPatchData, K targetModel, K sourceModel, boolean isLegacy,
                                    UserSecurityPolicyManager userSecurityPolicyManager) {


        if (!objectPatchData.hasPatches()) {
            log.debug('No patch data to merge into {}', targetModel.id)
            return targetModel
        }
        log.debug('Merging patch data into {}', targetModel.id)
        if (isLegacy) return mergeLegacyObjectPatchDataIntoModel(objectPatchData, targetModel, userSecurityPolicyManager)

        objectPatchData.patches.each {fieldPatch ->
            switch (fieldPatch.type) {
                case 'creation':
                    return processCreationPatchIntoModel(fieldPatch, targetModel, sourceModel, userSecurityPolicyManager)
                case 'deletion':
                    return processDeletionPatchIntoModel(fieldPatch, targetModel)
                case 'modification':
                    return processModificationPatchIntoModel(fieldPatch, targetModel)
                default:
                    log.warn('Unknown field patch type [{}]', fieldPatch.type)
            }
        }
        targetModel
    }


    void processCreationPatchIntoModel(FieldPatchData creationPatch, K targetModel, K sourceModel, UserSecurityPolicyManager userSecurityPolicyManager) {
        CreatorAware domainToCopy = pathService.findResourceByPathFromRootResource(sourceModel, creationPatch.path)
        if (!domainToCopy) {
            log.warn('Could not process creation patch into model at path [{}] as no such path exists in the source', creationPatch.path)
            return
        }
        log.debug('Creating {} into {}', creationPatch.path, creationPatch.relativePathToRoot.parent)
        // Potential deletions are modelitems or facets from model or modelitem
        if (Utils.parentClassIsAssignableFromChild(ModelItem, domainToCopy.class)) {
            processCreationPatchOfModelItem(domainToCopy as ModelItem, targetModel, creationPatch.relativePathToRoot.parent, userSecurityPolicyManager)
        }
        if (Utils.parentClassIsAssignableFromChild(MultiFacetItemAware, domainToCopy.class)) {
            processCreationPatchOfFacet(domainToCopy as MultiFacetItemAware, targetModel, creationPatch.relativePathToRoot.parent)
        }
    }

    void processDeletionPatchIntoModel(FieldPatchData deletionPatch, K targetModel) {
        CreatorAware domain = pathService.findResourceByPathFromRootResource(targetModel, deletionPatch.relativePathToRoot)
        if (!domain) {
            log.warn('Could not process deletion patch into model at path [{}] as no such path exists in the target', deletionPatch.relativePathToRoot)
            return
        }
        log.debug('Deleting [{}]', deletionPatch.relativePathToRoot)

        // Potential deletions are modelitems or facets from model or modelitem
        if (Utils.parentClassIsAssignableFromChild(ModelItem, domain.class)) {
            processDeletionPatchOfModelItem(domain as ModelItem)
        }
        if (Utils.parentClassIsAssignableFromChild(MultiFacetItemAware, domain.class)) {
            processDeletionPatchOfFacet(domain as MultiFacetItemAware, targetModel, deletionPatch.relativePathToRoot)
        }
    }

    void processModificationPatchIntoModel(FieldPatchData modificationPatch, K targetModel) {
        CreatorAware domain = pathService.findResourceByPathFromRootResource(targetModel, modificationPatch.relativePathToRoot)
        if (!domain) {
            log.warn('Could not process modifiation patch into model at path [{}] as no such path exists in the target', modificationPatch.relativePathToRoot)
            return
        }
        String fieldName = modificationPatch.fieldName
        log.debug('Modifying [{}] in [{}]', fieldName, modificationPatch.relativePathToRoot)
        domain."${fieldName}" = modificationPatch.sourceValue
        DomainService domainService = getDomainServices().find {it.handles(domain.class)}
        if (!domainService) throw new ApiInternalException('MSXX', "No domain service to handle modification of [${domain.domainType}]")

        if (!domain.validate())
            throw new ApiInvalidModelException('MS01', 'Modified domain is invalid', domain.errors, messageSource)
        domainService.save(domain, flush: false, validate: false)
    }

    void processDeletionPatchOfModelItem(ModelItem modelItem) {
        ModelItemService modelItemService = modelItemServices.find {it.handles(modelItem.class)}
        if (!modelItemService) throw new ApiInternalException('MSXX', "No domain service to handle deletion of [${modelItem.domainType}]")
        log.debug('Deleting ModelItem from Model')
        modelItemService.delete(modelItem)
    }

    CatalogueItem processDeletionPatchOfFacet(MultiFacetItemAware multiFacetItemAware, Model targetModel, Path path) {
        MultiFacetItemAwareService multiFacetItemAwareService = multiFacetItemAwareServices.find {it.handles(multiFacetItemAware.class)}
        if (!multiFacetItemAwareService) throw new ApiInternalException('MSXX', "No domain service to handle deletion of [${multiFacetItemAware.domainType}]")
        log.debug('Deleting Facet from path [{}]', path)
        multiFacetItemAwareService.delete(multiFacetItemAware)

        CatalogueItem catalogueItem = pathService.findResourceByPathFromRootResource(targetModel, path.getParent()) as CatalogueItem
        switch (multiFacetItemAware.domainType) {
            case Metadata.simpleName:
                catalogueItem.metadata.remove(multiFacetItemAware)
                break
            case Annotation.simpleName:
                catalogueItem.annotations.remove(multiFacetItemAware)
                break
            case Rule.simpleName:
                catalogueItem.rules.remove(multiFacetItemAware)
                break
            case SemanticLink.simpleName:
                catalogueItem.semanticLinks.remove(multiFacetItemAware)
                break
            case ReferenceFile.simpleName:
                catalogueItem.referenceFiles.remove(multiFacetItemAware)
                break
            case VersionLink.simpleName:
                (catalogueItem as Model).versionLinks.remove(multiFacetItemAware)
                break
        }
        catalogueItem
    }

    void processCreationPatchOfModelItem(ModelItem modelItemToCopy, Model targetModel, Path parentPathToCopyTo, UserSecurityPolicyManager userSecurityPolicyManager) {
        ModelItemService modelItemService = modelItemServices.find {it.handles(modelItemToCopy.class)}
        if (!modelItemService) throw new ApiInternalException('MSXX', "No domain service to handle creation of [${modelItemToCopy.domainType}]")
        log.debug('Creating ModelItem into Model at [{}]', parentPathToCopyTo)
        CatalogueItem parentToCopyInto = pathService.findResourceByPathFromRootResource(targetModel, parentPathToCopyTo) as CatalogueItem
        if (Utils.parentClassIsAssignableFromChild(Model, parentToCopyInto.class)) parentToCopyInto = null
        ModelItem copy = modelItemService.copy(targetModel, modelItemToCopy, parentToCopyInto, userSecurityPolicyManager)

        if (!copy.validate())
            throw new ApiInvalidModelException('MS01', 'Copied ModelItem is invalid', copy.errors, messageSource)

        modelItemService.save(copy, flush: false, validate: false)
    }

    void processCreationPatchOfFacet(MultiFacetItemAware multiFacetItemAwareToCopy, Model targetModel, Path parentPathToCopyTo) {
        MultiFacetItemAwareService multiFacetItemAwareService = multiFacetItemAwareServices.find {it.handles(multiFacetItemAwareToCopy.class)}
        if (!multiFacetItemAwareService) throw new ApiInternalException('MSXX', "No domain service to handle creation of [${multiFacetItemAwareToCopy.domainType}]")
        log.debug('Creating Facet into Model at [{}]', parentPathToCopyTo)

        CatalogueItem parentToCopyInto = pathService.findResourceByPathFromRootResource(targetModel, parentPathToCopyTo) as CatalogueItem
        MultiFacetItemAware copy = multiFacetItemAwareService.copy(multiFacetItemAwareToCopy, parentToCopyInto)

        if (!copy.validate())
            throw new ApiInvalidModelException('MS01', 'Copied Facet is invalid', copy.errors, messageSource)

        multiFacetItemAwareService.save(copy, flush: false, validate: false)
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    @Deprecated
    K mergeLegacyObjectPatchDataIntoModel(ObjectPatchData objectPatchData, K targetModel, UserSecurityPolicyManager userSecurityPolicyManager) {

        log.debug('Merging legacy {} diffs into model {}', objectPatchData.getDiffsWithContent().size(), targetModel.label)
        objectPatchData.getDiffsWithContent().each {mergeFieldDiff ->
            log.debug('{}', mergeFieldDiff.summary)

            if (mergeFieldDiff.isFieldChange()) {
                targetModel.setProperty(mergeFieldDiff.fieldName, mergeFieldDiff.value)
            } else if (mergeFieldDiff.isMetadataChange()) {
                mergeLegacyMetadataIntoCatalogueItem(mergeFieldDiff, targetModel, userSecurityPolicyManager)
            } else {
                ModelItemService modelItemService = modelItemServices.find {it.handles(mergeFieldDiff.fieldName)}
                if (modelItemService) {
                    modelItemService.processLegacyFieldPatchData(mergeFieldDiff, targetModel, userSecurityPolicyManager)
                } else {
                    log.error('Unknown ModelItem field to merge [{}]', mergeFieldDiff.fieldName)
                }
            }
        }
        targetModel
    }

    List<VersionTreeModel> buildModelVersionTree(K instance, VersionLinkType versionLinkType,
                                                 VersionTreeModel parentVersionTreeModel,
                                                 boolean includeForks, boolean branchesOnly,
                                                 UserSecurityPolicyManager userSecurityPolicyManager) {

        if (!userSecurityPolicyManager.userCanReadSecuredResourceId(instance.class, instance.id)) return []

        VersionTreeModel rootVersionTreeModel = new VersionTreeModel(instance, versionLinkType, parentVersionTreeModel)
        List<VersionTreeModel> versionTreeModelList = instance.finalised && branchesOnly ? [] : [rootVersionTreeModel]

        // If fork then add to the list but dont proceed any further into that tree
        if (versionLinkType == VersionLinkType.NEW_FORK_OF) return includeForks ? versionTreeModelList : []

        List<VersionLink> versionLinks = versionLinkService.findAllByTargetModelId(instance.id)
        versionLinks.each {link ->
            K linkedModel = get(link.multiFacetAwareItemId)
            versionTreeModelList.
                addAll(buildModelVersionTree(linkedModel, link.linkType, rootVersionTreeModel, includeForks, branchesOnly, userSecurityPolicyManager))
        }
        versionTreeModelList.sort()
    }

    ObjectDiff<K> getDiffForModels(K thisModel, K otherModel) {
        thisModel.diff(otherModel)
    }

    K findCommonAncestorBetweenModels(K leftModel, K rightModel) {

        if (leftModel.label != rightModel.label) {
            throw new ApiBadRequestException('MS03',
                                             "Model [${leftModel.id}] does not share its label with [${leftModel.id}] therefore they cannot have a " +
                                             "common ancestor")
        }

        K finalisedLeftParent = getFinalisedParent(leftModel)
        K finalisedRightParent = getFinalisedParent(rightModel)

        if (!finalisedLeftParent) {
            throw new ApiBadRequestException('MS01', "Model [${leftModel.id}] has no finalised parent therefore cannot have a " +
                                                     "common ancestor with Model [${rightModel.id}]")
        }

        if (!finalisedRightParent) {
            throw new ApiBadRequestException('MS02', "Model [${rightModel.id}] has no finalised parent therefore cannot have a " +
                                                     "common ancestor with Model [${leftModel.id}]")
        }

        // Choose the finalised parent with the lowest model version
        finalisedLeftParent.modelVersion < finalisedRightParent.modelVersion ? finalisedLeftParent : finalisedRightParent
    }

    K getFinalisedParent(K model) {
        if (model.finalised) return model
        get(VersionLinkService.findBySourceModelAndLinkType(model, VersionLinkType.NEW_MODEL_VERSION_OF)?.targetModelId)
    }

    K findOldestAncestor(K model) {
        // Look for model version or doc version only
        VersionLink versionLink = versionLinkService.findBySourceModelIdAndLinkType(model.id, VersionLinkType.NEW_MODEL_VERSION_OF)
        versionLink = versionLink ?: versionLinkService.findBySourceModelIdAndLinkType(model.id, VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)

        // If no versionlink then we're at the oldest ancestor
        if (!versionLink) {
            return model
        }
        // Check the parent for oldest ancestor
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

    K findCurrentMainBranchByLabel(String label) {
        modelClass.byLabelAndBranchNameAndNotFinalised(label, VersionAwareConstraints.DEFAULT_BRANCH_NAME).get() as K
    }

    List<K> findAllAvailableBranchesByLabel(String label) {
        modelClass.byLabelAndNotFinalised(label).list() as List<K>
    }

    Version getLatestModelVersionByLabel(String label) {
        findLatestFinalisedModelByLabel(label)?.modelVersion ?: Version.from('0.0.0')
    }

    MergeDiff<K> getMergeDiffForModels(K sourceModel, K targetModel) {
        K commonAncestor = findCommonAncestorBetweenModels(sourceModel, targetModel)

        ObjectDiff<K> caDiffSource = commonAncestor.diff(sourceModel)
        ObjectDiff<K> caDiffTarget = commonAncestor.diff(targetModel)

        // Remove the branchname as  diff as we know its a diff and for merging we dont want it
        Predicate branchNamePredicate = [test: {FieldDiff fieldDiff ->
            fieldDiff.fieldName == 'branchName'
        },] as Predicate

        caDiffSource.diffs.removeIf(branchNamePredicate)
        caDiffTarget.diffs.removeIf(branchNamePredicate)

        DiffBuilder
            .mergeDiff(sourceModel.class as Class<K>)
            .forMergingDiffable(sourceModel)
            .intoDiffable(targetModel)
            .havingCommonAncestor(commonAncestor)
            .withCommonAncestorDiffedAgainstSource(caDiffSource)
            .withCommonAncestorDiffedAgainstTarget(caDiffTarget)
            .generate()
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

    Version getNextModelVersion(K model, Version requestedModelVersion, VersionChangeType requestedVersionChangeType) {
        if (requestedModelVersion) {
            // Prefer requested model version
            return requestedModelVersion
        }
        // We need to get the parent model version first so we can work out what to increment
        Version parentModelVersion = getLatestModelVersionByLabel(model.label)

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

    void checkFinaliseModel(K model, Boolean finalise, Boolean importAsNewBranchModelVersion = false) {
        if (finalise && (!model.finalised || !model.modelVersion)) {
            // Parameter update will have set the model as finalised, but it wont have set the model version
            // If the actual import data includes finalised data then it will also containt the model version
            // If the model hasnt been imported as a new branch model version then we need to check if any existing models
            // If existing models then we cant finalise as we need to link the imported model
            if (!importAsNewBranchModelVersion && countByAuthorityAndLabel(model.authority, model.label)) {
                throw new ApiBadRequestException('MSXX', 'Request to finalise import without creating newBranchModelVersion to existing models')
            }
            model.finalised = true
        }
        if (model.finalised) {
            model.dateFinalised = model.dateFinalised ?: OffsetDateTime.now()
            model.modelVersion = model.modelVersion ?: getNextModelVersion(model, null, VersionChangeType.MAJOR)
        }
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        versionLinkService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
    }

    void checkDocumentationVersion(K model, boolean importAsNewDocumentationVersion, User catalogueUser) {
        if (importAsNewDocumentationVersion) {

            if (countByAuthorityAndLabel(model.authority, model.label)) {
                List<K> existingModels = findAllByAuthorityAndLabel(model.authority, model.label)
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

            if (countByAuthorityAndLabel(model.authority, model.label)) {
                K latest = findLatestFinalisedModelByLabel(model.label)

                if (!latest) {
                    log.info('No finalised model to create branch from so finalising existing main branch')
                    latest = findCurrentMainBranchByLabel(model.label)
                    finaliseModel(latest, catalogueUser, Version.from('1'), null, null)
                    save(latest, flush: true, validate: false)
                }

                // Now we have a finalised model to work from
                if (latest) {
                    setModelIsNewBranchModelVersionOfModel(model, latest, catalogueUser)
                    model.dateFinalised = null
                    model.finalised = false
                    model.modelVersion = null
                    model.branchName = branchName ?: VersionAwareConstraints.DEFAULT_BRANCH_NAME
                    model.documentationVersion = Version.from('1')
                } else {
                    throw new ApiBadRequestException('MSXX', 'Request to importAsNewBranchModelVersion but no finalised model or main branch available')
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

    Model copyModelAndValidateAndSave(K original,
                                      Folder folderToCopyInto,
                                      User copier,
                                      boolean copyPermissions,
                                      String label,
                                      Version copyDocVersion,
                                      String branchName,
                                      boolean throwErrors,
                                      UserSecurityPolicyManager userSecurityPolicyManager) {
        Model copiedModel = copyModel(original, folderToCopyInto, copier, true, original.label, original.documentationVersion,
                                      branchName, false, userSecurityPolicyManager)

        if ((copiedModel as GormValidateable).validate()) {
            saveModelWithContent(copiedModel)
            if (securityPolicyManagerService) {
                userSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(copiedModel, userSecurityPolicyManager.user,
                                                                                                         copiedModel.label)
            }
        } else throw new ApiInvalidModelException('DMSXX', 'Copied Model is invalid',
                                                  (copiedModel as GormValidateable).errors, messageSource)
        copiedModel
    }
}