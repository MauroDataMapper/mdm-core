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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
@Transactional
class CodeSetService extends ModelService<CodeSet> {

    TermRelationshipTypeService termRelationshipTypeService
    TermService termService
    TermRelationshipService termRelationshipService
    AuthorityService authorityService
    PathService pathService

    MessageSource messageSource
    VersionLinkService versionLinkService
    EditService editService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    CodeSet get(Serializable id) {
        CodeSet.get(id)
    }

    @Override
    List<CodeSet> getAll(Collection<UUID> ids) {
        CodeSet.getAll(ids).findAll()
    }

    @Override
    List<CodeSet> list(Map pagination = [:]) {
        CodeSet.list(pagination)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "cs"
    }

    Long count() {
        CodeSet.count()
    }

    int countByLabel(String label) {
        CodeSet.countByLabel(label)
    }

    CodeSet validate(CodeSet codeSet) {
        log.debug('Validating codeSet')
        codeSet.validate()
        codeSet
    }

    @Override
    void deleteAll(Collection<CodeSet> catalogueItems) {
        deleteAll(catalogueItems.id, true)
    }

    @Override
    void delete(CodeSet codeSet) {
        codeSet.deleted = true
    }

    void delete(CodeSet codeSet, boolean permanent, boolean flush = true) {
        if (!codeSet) return
        if (permanent) {
            codeSet.folder = null
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(codeSet, null)
            }
            codeSet.delete(flush: flush)
        } else delete(codeSet)
    }

    @Override
    CodeSet softDeleteModel(CodeSet model) {
        model?.deleted = true
        model
    }

    @Override
    void permanentDeleteModel(CodeSet model) {
        delete(model, true)
    }

    List<CodeSet> deleteAll(List<Serializable> idsToDelete, Boolean permanent) {
        List<CodeSet> updated = []
        idsToDelete.each {
            CodeSet t = get(it)
            delete(t, permanent, false)
            if (!permanent) updated << t
        }
        updated
    }

    @Override
    CodeSet save(CodeSet codeSet) {
        log.debug('Saving {}({}) without batching', codeSet.label, codeSet.ident())
        save(failOnError: true, validate: false, flush: false, codeSet)
    }

    @Override
    CodeSet saveWithBatching(CodeSet model) {
        save(model)
    }

    @Override
    List<CodeSet> findAllReadableByEveryone() {
        CodeSet.findAllByReadableByEveryone(true)
    }

    @Override
    List<CodeSet> findAllReadableByAuthenticatedUsers() {
        CodeSet.findAllByReadableByAuthenticatedUsers(true)
    }

    List<CodeSet> findAllByLabel(String label) {
        CodeSet.findAllByLabel(label)
    }

    List<CodeSet> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        CodeSet.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    List<CodeSet> findAllByMetadataNamespace(String namespace) {
        CodeSet.byMetadataNamespace(namespace).list()
    }

    List<CodeSet> findAllByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService, Map pagination = [:]) {
        findAllByMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name, pagination)
    }

    List<CodeSet> findAllByFolderId(UUID folderId) {
        CodeSet.byFolderId(folderId).list()
    }

    List<UUID> findAllIdsByFolderId(UUID folderId) {
        CodeSet.byFolderId(folderId).id().list() as List<UUID>
    }

    List<CodeSet> findAllDeleted(Map pagination = [:]) {
        CodeSet.byDeleted().list(pagination)
    }

    Number countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        CodeSet.countByLabelAndBranchNameAndFinalised(label, branchName, false)
    }

    CodeSet findLatestByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService) {
        // If we get all the models with the DL metadata then sort by documentation version we should end up with the latest doc version.
        // We should do this rather than using the value of the metadata as its possible someone creates a new documentation version of the model
        // and we should use that one. All DL loaded models have the doc version set to the DL version.
        CodeSet latest = CodeSet
            .byMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name)
            .get([order: 'desc', sort: 'documentationVersion'])
        log.debug('Found CodeSet {}({}) which matches DataLoaderPlugin {}({})', latest.label, latest.documentationVersion,
                  dataLoaderProviderService.name, dataLoaderProviderService.version)
        latest
    }

    @Override
    CodeSet finaliseModel(CodeSet codeSet, User user, Version modelVersion, VersionChangeType versionChangeType,
                          List<Serializable> supersedeModelIds = []) {
        codeSet.finalised = true
        codeSet.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

        codeSet.modelVersion = getNextModelVersion(codeSet, modelVersion, versionChangeType)

        codeSet.addToAnnotations(createdBy: user.emailAddress, label: 'Finalised CodeSet',
                                 description: "CodeSet finalised by ${user.firstName} ${user.lastName} on " +
                                              "${OffsetDateTimeConverter.toString(codeSet.dateFinalised)}")
        editService.createAndSaveEdit(codeSet.id, codeSet.domainType,
                                      "CodeSet finalised by ${user.firstName} ${user.lastName} on " +
                                      "${OffsetDateTimeConverter.toString(codeSet.dateFinalised)}",
                                      user)
        codeSet
    }

    boolean newVersionCreationIsAllowed(CodeSet codeSet) {
        if (!codeSet.finalised) {
            codeSet.errors.reject('invalid.codeset.new.version.not.finalised.message',
                                  [codeSet.label, codeSet.id] as Object[],
                                  'CodeSet [{0}({1})] cannot have a new version as it is not finalised')
            return false
        }

        CodeSet superseding = findCodeSetDocumentationSuperseding(codeSet)
        if (superseding) {
            codeSet.errors.reject('invalid.codeset.new.version.superseded.message',
                                  [codeSet.label, codeSet.id, superseding.label, superseding.id] as Object[],
                                  'CodeSet [{0}({1})] cannot have a new version as it has been superseded by [{2}({3})]')
            return false
        }
        true
    }

    @Override
    CodeSet createNewBranchModelVersion(String branchName, CodeSet codeSet, User user, boolean copyPermissions,
                                        UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments) {
        if (!newVersionCreationIsAllowed(codeSet)) return codeSet

        // Check if the branch name is already being used
        if (countAllByLabelAndBranchNameAndNotFinalised(codeSet.label, branchName) > 0) {
            codeSet.errors.reject('model.label.branch.name.already.exists',
                                  ['branchName', CodeSet, branchName, codeSet.label] as Object[],
                                  'Property [{0}] of class [{1}] with value [{2}] already exists for label [{3}]')
            return codeSet
        }

        // We know at this point the datamodel is finalised which means its branch name == main so we need to check no unfinalised main branch exists
        boolean draftModelOnMainBranchForLabel = countAllByLabelAndBranchNameAndNotFinalised(codeSet.label, 'main') > 0

        CodeSet newMainBranchModelVersion
        if (!draftModelOnMainBranchForLabel) {
            newMainBranchModelVersion = copyCodeSet(codeSet,
                                                    user,
                                                    copyPermissions,
                                                    codeSet.label,
                                                    'main',
                                                    additionalArguments.throwErrors as boolean,
                                                    userSecurityPolicyManager,
                                                    true)
            setCodeSetIsNewBranchModelVersionOfCodeSet(newMainBranchModelVersion, codeSet, user)

            if (additionalArguments.moveDataFlows) {
                throw new ApiNotYetImplementedException('DMSXX', 'CodeSet moving of DataFlows')
                //            moveTargetDataFlows(codeSet, newMainBranchModelVersion)
            }

            if (newMainBranchModelVersion.validate()) save(newMainBranchModelVersion, flush: true, validate: false)
        }
        CodeSet newBranchModelVersion
        if (branchName != 'main') {
            newBranchModelVersion = copyCodeSet(codeSet,
                                                user,
                                                copyPermissions,
                                                codeSet.label,
                                                branchName,
                                                additionalArguments.throwErrors as boolean,
                                                userSecurityPolicyManager,
                                                true)

            setCodeSetIsNewBranchModelVersionOfCodeSet(newBranchModelVersion, codeSet, user)

            if (additionalArguments.moveDataFlows) {
                throw new ApiNotYetImplementedException('DMSXX', 'CodeSet moving of DataFlows')
                //            moveTargetDataFlows(codeSet, newBranchModelVersion)
            }

            if (newBranchModelVersion.validate()) save(newBranchModelVersion, flush: true, validate: false)
        }

        newBranchModelVersion ?: newMainBranchModelVersion
    }

    @Override
    CodeSet createNewDocumentationVersion(CodeSet codeSet, User user, boolean copyPermissions, UserSecurityPolicyManager userSecurityPolicyManager,
                                          Map<String, Object> additionalArguments) {

        if (!newVersionCreationIsAllowed(codeSet)) return codeSet

        CodeSet newDocVersion = copyCodeSet(codeSet, user, copyPermissions, codeSet.label,
                                            Version.nextMajorVersion(codeSet.documentationVersion), codeSet.branchName,
                                            additionalArguments.throwErrors as boolean, userSecurityPolicyManager)
        setCodeSetIsNewDocumentationVersionOfCodeSet(newDocVersion, codeSet, user)

        if (newDocVersion.validate()) newDocVersion.save(flush: true, validate: false)
        newDocVersion
    }

    @Override
    CodeSet createNewForkModel(String label, CodeSet codeSet, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments) {

        if (!newVersionCreationIsAllowed(codeSet)) return codeSet

        CodeSet newForkModel = copyCodeSet(codeSet, user, copyPermissions, label, additionalArguments.throwErrors as boolean,
                                           userSecurityPolicyManager)
        setCodeSetIsNewForkModelOfCodeSet(newForkModel, codeSet, user)

        if (newForkModel.validate()) save(newForkModel)
        newForkModel
    }

    CodeSet copyCodeSet(CodeSet original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                        UserSecurityPolicyManager userSecurityPolicyManager) {
        copyCodeSet(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager)
    }

    CodeSet copyCodeSet(CodeSet original, User copier, boolean copyPermissions, String label, String branchName, boolean throwErrors,
                        UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata) {
        copyCodeSet(original, copier, copyPermissions, label, Version.from('1'), branchName, throwErrors, userSecurityPolicyManager)
    }

    CodeSet copyCodeSet(CodeSet original, User copier, boolean copyPermissions, String label, Version copyVersion, String branchName,
                        boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        CodeSet copy = new CodeSet(author: original.author,
                                   organisation: original.organisation,
                                   finalised: false, deleted: false, documentationVersion: copyVersion,
                                   folder: original.folder, authority: original.authority, branchName: branchName
        )

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('CSSXX', 'CodeSet permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            save(copy, validate: false)
            editService.createAndSaveEdit(copy.id, copy.domainType,
                                          "CodeSet ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('TMS01', 'Copied CodeSet is invalid', copy.errors, messageSource)

        copy.trackChanges()

        // Copy all the terms
        original.terms?.each { term ->
            copy.addToTerms(term)
        }

        copy
    }

    void setCodeSetIsNewForkModelOfCodeSet(CodeSet newModel, CodeSet oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_FORK_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setCodeSetIsNewDocumentationVersionOfCodeSet(CodeSet newModel, CodeSet oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setCodeSetIsNewBranchModelVersionOfCodeSet(CodeSet newModel, CodeSet oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    @Override
    boolean hasTreeTypeModelItems(CodeSet codeSet) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(CodeSet catalogueItem) {
        []
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == CodeSet.simpleName
    }

    @Override
    List<CodeSet> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                             String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet)
        if (!readableIds) return []

        List<CodeSet> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = CodeSet.luceneLabelSearch(CodeSet, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    CodeSet findByIdJoinClassifiers(UUID id) {
        CodeSet.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        removeAllFromContainer(classifier)
    }

    @Override
    List<CodeSet> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        CodeSet.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(CodeSet, it.id) }
    }

    @Override
    Class<CodeSet> getModelClass() {
        CodeSet
    }

    @Override
    List<CodeSet> findAllByContainerId(UUID containerId) {
        // We do not concern ourselves any other types of containers for CodeSets at this time
        findAllByFolderId(containerId)
    }

    @Override
    void deleteAllInContainer(Container container) {
        if (container.instanceOf(Folder)) {
            deleteAll(CodeSet.byFolderId(container.id).id().list() as List<UUID>, true)
        }
        if (container.instanceOf(Classifier)) {
            deleteAll(CodeSet.byClassifierId(container.id).id().list() as List<UUID>, true)
        }
    }

    @Override
    void removeAllFromContainer(Container container) {
        if (container.instanceOf(Folder)) {
            CodeSet.byFolderId(container.id).list().each {
                it.folder = null
            }
        }
        if (container.instanceOf(Classifier)) {
            CodeSet.byClassifierId(container.id).list().each {
                it.removeFromClassifiers(container as Classifier)
            }
        }
    }

    @Override
    List<CodeSet> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                        boolean includeModelSuperseded, boolean includeDeleted) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet)
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
        CodeSet.withReadable(CodeSet.by(), constrainedIds, includeDeleted).list()
    }

    @Override
    List<UUID> findAllModelIdsWithTreeChildren(List<CodeSet> models) {
        // CodeSets should never return a tree structure
        []
    }

    @Override
    void removeVersionLinkFromModel(UUID modelId, VersionLink versionLink) {
        get(modelId).removeFromVersionLinks(versionLink)
    }

    @Override
    List<UUID> findAllSupersededModelIds(List<CodeSet> models) {
        findAllSupersededIds(models.id)
    }

    @Override
    List<CodeSet> findAllDocumentationSupersededModels(Map pagination) {
        List<UUID> ids = findAllDocumentSupersededIds(CodeSet.by().id().list() as List<UUID>)
        findAllSupersededModels(ids, pagination)
    }

    @Override
    List<CodeSet> findAllModelSupersededModels(Map pagination) {
        List<UUID> ids = findAllModelSupersededIds(CodeSet.by().id().list() as List<UUID>)
        findAllSupersededModels(ids, pagination)
    }

    @Override
    List<CodeSet> findAllDeletedModels(Map pagination) {
        CodeSet.byDeleted().list(pagination)
    }

    List<CodeSet> findAllSupersededModels(List<UUID> ids, Map pagination) {
        if (!ids) return []
        CodeSet.byIdInList(ids).list(pagination)
    }

    List<UUID> findAllExcludingDocumentSupersededIds(List<UUID> readableIds) {
        readableIds - findAllDocumentSupersededIds(readableIds)
    }

    List<UUID> findAllExcludingModelSupersededIds(List<UUID> readableIds) {
        readableIds - findAllModelSupersededIds(readableIds)
    }

    List<UUID> findAllExcludingDocumentAndModelSupersededIds(List<UUID> readableIds) {
        readableIds - findAllSupersededIds(readableIds)
    }

    List<UUID> findAllSupersededIds(List<UUID> readableIds) {
        (findAllDocumentSupersededIds(readableIds) + findAllModelSupersededIds(readableIds)).toSet().toList()
    }

    List<UUID> findAllDocumentSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsDocumentSuperseded(CodeSet.simpleName, readableIds)
    }

    List<UUID> findAllModelSupersededIds(List<UUID> readableIds) {
        // All versionLinks which are targets of model version links
        List<VersionLink> modelVersionLinks = versionLinkService.findAllByTargetCatalogueItemIdInListAndIsModelSuperseded(readableIds)

        // However they are only superseded if the source of this link is finalised
        modelVersionLinks.findAll {
            CodeSet sourceModel = get(it.catalogueItemId)
            sourceModel.finalised
        }.collect { it.targetModelId }
    }

    List<CodeSet> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        CodeSet.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet))).list(pagination)
    }

    List<CodeSet> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        CodeSet.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet))).list(pagination)
    }

    CodeSet findCodeSetSuperseding(CodeSet codeSet) {
        VersionLink link = versionLinkService.findLatestLinkSupersedingModelId(CodeSet.simpleName, codeSet.id)
        if (!link) return null
        link.catalogueItemId == codeSet.id ? get(link.targetModelId) : get(link.catalogueItemId)
    }

    CodeSet findCodeSetDocumentationSuperseding(CodeSet codeSet) {
        VersionLink link = versionLinkService.findLatestLinkDocumentationSupersedingModelId(CodeSet.simpleName, codeSet.id)
        if (!link) return null
        link.catalogueItemId == codeSet.id ? get(link.targetModelId) : get(link.catalogueItemId)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<CodeSet> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet)
        ids ? CodeSet.findAllByIdInList(ids, pagination) : []
    }

    void checkDocumentationVersion(CodeSet codeSet, boolean importAsNewDocumentationVersion, User catalogueUser) {
        if (importAsNewDocumentationVersion) {

            if (countByLabel(codeSet.label)) {
                List<CodeSet> existingModels = findAllByLabel(codeSet.label)
                existingModels.each { existing ->
                    log.debug('Setting CodeSet as new documentation version of [{}:{}]', existing.label, existing.documentationVersion)
                    if (!existing.finalised) finaliseModel(existing, catalogueUser, null, null)
                    setCodeSetIsNewDocumentationVersionOfCodeSet(codeSet, existing, catalogueUser)
                }
                Version latestVersion = existingModels.max { it.documentationVersion }.documentationVersion
                codeSet.documentationVersion = Version.nextMajorVersion(latestVersion)

            } else log.info('Marked as importAsNewDocumentationVersion but no existing CodeSets with label [{}]', codeSet.label)
        }
    }

    /**
     * Find a CodeSet by label.
     * @param label
     * @return The found CodeSet
     */
    CodeSet findByLabel(String label) {
        CodeSet.findByLabel(label)
    }

    /**
    * When importing a codeSet, do checks and setting of required values as follows:
    * (1) Set the createdBy of the codeSeT to be the importing user
    * (2) Always set authority to the default authority, overriding any authority that is set in the import data
    * (3) Check facets
    * (4) Check that terms exist
    *
    * @param importingUser The importing user, who will be used to set createdBy
    * @param codeSet   The codeSet to be imported
    * @param bindingMap The binding map, which is necessary for looking up terms
    */
    void checkImportedCodeSetAssociations(User importingUser, CodeSet codeSet, Map bindingMap = [:]) {
        codeSet.createdBy = importingUser.emailAddress

        //At the time of writing, there is, and can only be, one authority. So here we set the authority, overriding any authority provided in the import.
        codeSet.authority = authorityService.getDefaultAuthority()
        
        checkFacetsAfterImportingCatalogueItem(codeSet)

        //Terms are imported by use of a path such as "te:my-terminology-label|tm:my-term-label"
        //Here we check that each path does retrieve a known term.
        if (bindingMap.termPaths) {
            bindingMap.termPaths.each {
                String path = it.termPath
                Map pathParams = [path: path, catalogueItemDomainType: Terminology.simpleName]

                //pathService requires a UserSecurityPolicyManager.
                //Assumption is that if we got this far then it is OK to read the Terms because either (i) we came via a controller in which case
                //the user's ability to import a CodeSet has already been tested, or (ii) we are calling this method from a service test spec in which
                //case it is OK to read. 
                Term term = pathService.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, pathParams)

                if (term) {
                    codeSet.addToTerms(term)
                } else {
                    //Throw an exception
                    throw new ApiBadRequestException('CSS01', "Term retrieval for ${path} failed")
                }
            }
        }
    }     
}
