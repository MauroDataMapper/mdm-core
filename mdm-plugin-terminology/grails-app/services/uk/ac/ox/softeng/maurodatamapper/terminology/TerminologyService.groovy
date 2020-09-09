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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
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
class TerminologyService extends ModelService<Terminology> {

    TermRelationshipTypeService termRelationshipTypeService
    TermService termService
    TermRelationshipService termRelationshipService

    MessageSource messageSource
    VersionLinkService versionLinkService
    EditService editService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    Terminology get(Serializable id) {
        Terminology.get(id)
    }

    @Override
    List<Terminology> getAll(Collection<UUID> ids) {
        Terminology.getAll(ids).findAll()
    }

    @Override
    List<Terminology> list(Map pagination = [:]) {
        Terminology.list(pagination)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "te"
    }

    Long count() {
        Terminology.count()
    }

    int countByLabel(String label) {
        Terminology.countByLabel(label)
    }

    Terminology validate(Terminology terminology) {
        log.debug('Validating Terminology')
        terminology.validate()
        terminology
    }

    @Override
    void deleteAll(Collection<Terminology> catalogueItems) {
        deleteAll(catalogueItems.id, true)
    }

    @Override
    void delete(Terminology terminology) {
        terminology.deleted = true
    }

    void delete(Terminology terminology, boolean permanent, boolean flush = true) {
        if (!terminology) return
        if (permanent) {
            terminology.folder = null
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(terminology, null)
            }
            terminology.delete(flush: flush)
        } else delete(terminology)
    }

    @Override
    Terminology softDeleteModel(Terminology model) {
        model?.deleted = true
        model
    }

    @Override
    void permanentDeleteModel(Terminology model) {
        delete(model, true)
    }

    List<Terminology> deleteAll(List<Serializable> idsToDelete, Boolean permanent) {
        List<Terminology> updated = []
        idsToDelete.each {
            Terminology t = get(it)
            delete(t, permanent, false)
            if (!permanent) updated << t
        }
        updated
    }

    @Override
    Terminology save(Terminology terminology) {
        log.debug('Saving {}({}) without batching', terminology.label, terminology.ident())
        save(failOnError: true, validate: false, flush: false, terminology)
    }

    @Override
    Terminology saveWithBatching(Terminology model) {
        save(model)
    }

    @Override
    List<Terminology> findAllReadableByEveryone() {
        Terminology.findAllByReadableByEveryone(true)
    }

    @Override
    List<Terminology> findAllReadableByAuthenticatedUsers() {
        Terminology.findAllByReadableByAuthenticatedUsers(true)
    }

    List<Terminology> findAllByLabel(String label) {
        Terminology.findAllByLabel(label)
    }

    List<Terminology> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        Terminology.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    List<Terminology> findAllByMetadataNamespace(String namespace) {
        Terminology.byMetadataNamespace(namespace).list()
    }

    List<Terminology> findAllByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService, Map pagination = [:]) {
        findAllByMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name, pagination)
    }

    List<Terminology> findAllByFolderId(UUID folderId) {
        Terminology.byFolderId(folderId).list()
    }

    List<UUID> findAllIdsByFolderId(UUID folderId) {
        Terminology.byFolderId(folderId).id().list() as List<UUID>
    }

    List<Terminology> findAllDeleted(Map pagination = [:]) {
        Terminology.byDeleted().list(pagination)
    }

    Number countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        Terminology.countByLabelAndBranchNameAndFinalised(label, branchName, false)
    }

    Terminology findLatestByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService) {
        // If we get all the models with the DL metadata then sort by documentation version we should end up with the latest doc version.
        // We should do this rather than using the value of the metadata as its possible someone creates a new documentation version of the model
        // and we should use that one. All DL loaded models have the doc version set to the DL version.
        Terminology latest = Terminology
            .byMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name)
            .get([order: 'desc', sort: 'documentationVersion'])
        log.debug('Found Terminology {}({}) which matches DataLoaderPlugin {}({})', latest.label, latest.documentationVersion,
                  dataLoaderProviderService.name, dataLoaderProviderService.version)
        latest
    }

    @Override
    Terminology finaliseModel(Terminology terminology, User user, Version modelVersion, VersionChangeType versionChangeType,
                              List<Serializable> supersedeModelIds = []) {
        terminology.finalised = true
        terminology.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology.breadcrumbTree.finalise()

        terminology.modelVersion = getNextModelVersion(terminology, modelVersion, versionChangeType)

        terminology.addToAnnotations(createdBy: user.emailAddress, label: 'Finalised Terminology',
                                     description: "Terminology finalised by ${user.firstName} ${user.lastName} on " +
                                                  "${OffsetDateTimeConverter.toString(terminology.dateFinalised)}")
        editService.createAndSaveEdit(terminology.id, terminology.domainType,
                                      "Terminology finalised by ${user.firstName} ${user.lastName} on " +
                                      "${OffsetDateTimeConverter.toString(terminology.dateFinalised)}",
                                      user)
        terminology
    }

    boolean newVersionCreationIsAllowed(Terminology terminology) {
        if (!terminology.finalised) {
            terminology.errors.reject('invalid.terminology.new.version.not.finalised.message',
                                      [terminology.label, terminology.id] as Object[],
                                      'Terminology [{0}({1})] cannot have a new version as it is not finalised')
            return false
        }
        Terminology superseding = findTerminologyDocumentationSuperseding(terminology)
        if (superseding) {
            terminology.errors.reject('invalid.terminology.new.version.superseded.message',
                                      [terminology.label, terminology.id, superseding.label, superseding.id] as Object[],
                                      'Terminology [{0}({1})] cannot have a new version as it has been superseded by [{2}({3})]')
            return false
        }
        true
    }

    @Override
    Terminology createNewBranchModelVersion(String branchName, Terminology terminology, User user, boolean copyPermissions,
                                            UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments) {
        if (!newVersionCreationIsAllowed(terminology)) return terminology


        // Check if the branch name is already being used
        if (countAllByLabelAndBranchNameAndNotFinalised(terminology.label, branchName) > 0) {
            terminology.errors.reject('model.label.branch.name.already.exists',
                                      ['branchName', Terminology, branchName, terminology.label] as Object[],
                                      'Property [{0}] of class [{1}] with value [{2}] already exists for label [{3}]')
            return terminology
        }

        // We know at this point the datamodel is finalised which means its branch name == main so we need to check no unfinalised main branch exists
        boolean mainBranchExistsForLabel = countAllByLabelAndBranchNameAndNotFinalised(terminology.label, 'main') > 0

        Terminology newMainBranchModelVersion
        if (!mainBranchExistsForLabel) {
            newMainBranchModelVersion = copyTerminology(terminology,
                                                        user,
                                                        copyPermissions,
                                                        terminology.label,
                                                        'main',
                                                        additionalArguments.throwErrors as boolean,
                                                        userSecurityPolicyManager,
                                                        true)
            setTerminologyIsNewBranchModelVersionOfTerminology(newMainBranchModelVersion, terminology, user)

            if (additionalArguments.moveDataFlows) {
                throw new ApiNotYetImplementedException('DMSXX', 'Terminology moving of DataFlows')
                //            moveTargetDataFlows(terminology, newMainBranchModelVersion)
            }

            if (newMainBranchModelVersion.validate()) save(newMainBranchModelVersion, flush: true, validate: false)
        }
        Terminology newBranchModelVersion
        if (branchName != 'main') {
            newBranchModelVersion = copyTerminology(terminology,
                                                    user,
                                                    copyPermissions,
                                                    terminology.label,
                                                    branchName,
                                                    additionalArguments.throwErrors as boolean,
                                                    userSecurityPolicyManager,
                                                    true)

            setTerminologyIsNewBranchModelVersionOfTerminology(newBranchModelVersion, terminology, user)

            if (additionalArguments.moveDataFlows) {
                throw new ApiNotYetImplementedException('DMSXX', 'Terminology moving of DataFlows')
                //            moveTargetDataFlows(terminology, newBranchModelVersion)
            }

            if (newBranchModelVersion.validate()) save(newBranchModelVersion, flush: true, validate: false)
        }

        newBranchModelVersion ?: newMainBranchModelVersion
    }

    @Override
    Terminology createNewDocumentationVersion(Terminology terminology, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments) {

        if (!newVersionCreationIsAllowed(terminology)) return terminology

        Terminology newDocVersion = copyTerminology(terminology, user, copyPermissions, terminology.label,
                                                    Version.nextMajorVersion(terminology.documentationVersion), terminology.branchName,
                                                    additionalArguments.throwErrors as boolean, userSecurityPolicyManager)
        setTerminologyIsNewDocumentationVersionOfTerminology(newDocVersion, terminology, user)

        if (newDocVersion.validate()) newDocVersion.save(flush: true, validate: false)
        newDocVersion
    }

    @Override
    Terminology createNewForkModel(String label, Terminology terminology, User user, boolean copyPermissions, UserSecurityPolicyManager
        userSecurityPolicyManager, Map<String, Object> additionalArguments) {

        if (!newVersionCreationIsAllowed(terminology)) return terminology

        Terminology newForkModel =
            copyTerminology(terminology, user, copyPermissions, label, additionalArguments.throwErrors as boolean, userSecurityPolicyManager)
        setTerminologyIsNewForkModelOfTerminology(newForkModel, terminology, user)

        if (newForkModel.validate()) newForkModel.save(flush: true, validate: false)
        newForkModel
    }

    Terminology copyTerminology(Terminology original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                                UserSecurityPolicyManager userSecurityPolicyManager) {
        copyTerminology(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager)
    }

    Terminology copyTerminology(Terminology original, User copier, boolean copyPermissions, String label, String branchName, boolean throwErrors,
                                UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata) {
        copyTerminology(original, copier, copyPermissions, label, Version.from('1'), branchName, throwErrors, userSecurityPolicyManager)
    }

    Terminology copyTerminology(Terminology original, User copier, boolean copyPermissions, String label, Version copyVersion, String branchName,
                                boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        Terminology copy = new Terminology(author: original.author,
                                           organisation: original.organisation,
                                           finalised: false, deleted: false, documentationVersion: copyVersion,
                                           folder: original.folder, authority: original.authority, branchName: branchName
        )
        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('TSXX', 'Terminology permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            save(copy, validate: false)
            editService.createAndSaveEdit(copy.id, copy.domainType,
                                          "Terminology ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('TMS01', 'Copied Terminology is invalid', copy.errors, messageSource)

        copy.trackChanges()

        // Copy all the TermRelationshipType
        original.termRelationshipTypes?.each { trt ->
            termRelationshipTypeService.copyTermRelationshipType(copy, trt, copier)
        }

        // Copy all the terms
        original.terms?.each { term ->
            termService.copyTerm(copy, term, copier, userSecurityPolicyManager)
        }

        // Copy all the term relationships
        // We need all the terms to exist so we can create the links
        // Only copy source relationships as this will propgate the target relationships
        original.terms?.each { term ->
            term.sourceTermRelationships.each { relationship ->
                termRelationshipService.copyTermRelationship(copy, relationship, copier)
            }
        }

        copy
    }

    void setTerminologyIsNewForkModelOfTerminology(Terminology newModel, Terminology oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_FORK_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setTerminologyIsNewDocumentationVersionOfTerminology(Terminology newModel, Terminology oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setTerminologyIsNewBranchModelVersionOfTerminology(Terminology newModel, Terminology oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    Terminology updateTermDepths(Terminology terminology, boolean inMemory = false) {
        log.debug('Updating terminology term depths in memory {}', inMemory)

        boolean hasNoValidRelationships
        if (inMemory) {
            hasNoValidRelationships = terminology.termRelationshipTypes.every {
                !it.parentalRelationship && !it.childRelationship
            }
        } else {
            hasNoValidRelationships = TermRelationshipType.byTerminologyIdAndParentalRelationshipOrChildRelationship(terminology.id).count() == 0
        }

        // No parental or child relationships then ensure all depths are 1
        if (hasNoValidRelationships) {
            log.debug('No parent/child relationships so all terms are depth 1')
            terminology.terms.each { it.depth = 1 }
            return terminology
        }

        log.debug('Updating all term depths')
        // Reset all track changes, as this whole process needs to be done AFTER insert into database
        // the only changes here should be depths
        terminology.terms.each { it.trackChanges() }
        terminology.terms.each {
            termService.updateDepth(it, inMemory)
        }
        terminology
    }

    boolean isTreeStructureCapableTerminology(Terminology terminology) {
        if (terminology.hasChild == null) {
            terminology.hasChild = TermRelationshipType.byTerminologyIdAndParentalRelationshipOrChildRelationship(terminology.id).count() &&
                                   Term.byTerminologyIdAndHasChildDepth(terminology.id).count()
        }
        terminology.hasChild
    }

    @Override
    boolean hasTreeTypeModelItems(Terminology terminology) {
        isTreeStructureCapableTerminology(terminology)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(Terminology terminology) {
        List<Term> terms = termService.findAllByTerminologyIdAndDepth(terminology.id, 1)
        termService.updateChildKnowledge(terms)
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == Terminology.simpleName
    }

    @Override
    List<Terminology> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                 String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology)
        if (!readableIds) return []

        List<Terminology> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = Terminology.luceneLabelSearch(Terminology, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    Terminology findByIdJoinClassifiers(UUID id) {
        Terminology.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        removeAllFromContainer(classifier)
    }

    @Override
    List<Terminology> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        Terminology.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, it.id) }
    }

    @Override
    Class<Terminology> getModelClass() {
        Terminology
    }

    @Override
    List<Terminology> findAllByContainerId(UUID containerId) {
        // We do not concern ourselves any other types of containers for Terminologys at this time
        findAllByFolderId(containerId)
    }

    @Override
    void deleteAllInContainer(Container container) {
        if (container.instanceOf(Folder)) {
            deleteAll(Terminology.byFolderId(container.id).id().list() as List<UUID>, true)
        }
        if (container.instanceOf(Classifier)) {
            deleteAll(Terminology.byClassifierId(container.id).id().list() as List<UUID>, true)
        }
    }

    @Override
    void removeAllFromContainer(Container container) {
        if (container.instanceOf(Folder)) {
            Terminology.byFolderId(container.id).list().each {
                it.folder = null
            }
        }
        if (container.instanceOf(Classifier)) {
            Terminology.byClassifierId(container.id).list().each {
                it.removeFromClassifiers(container as Classifier)
            }
        }
    }

    @Override
    List<Terminology> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                            boolean includeModelSuperseded, boolean includeDeleted) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology)
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
        Terminology.withReadable(Terminology.by(), constrainedIds, includeDeleted).list()
    }

    @Override
    List<UUID> findAllModelIdsWithTreeChildren(List<Terminology> models) {
        models.findAll { isTreeStructureCapableTerminology(it) }.collect { it.id }
    }

    @Override
    void removeVersionLinkFromModel(UUID modelId, VersionLink versionLink) {
        get(modelId).removeFromVersionLinks(versionLink)
    }

    @Override
    List<UUID> findAllSupersededModelIds(List<Terminology> models) {
        findAllSupersededIds(models.id)
    }

    @Override
    List<Terminology> findAllDocumentationSupersededModels(Map pagination) {
        List<UUID> ids = findAllDocumentSupersededIds(Terminology.by().id().list() as List<UUID>)
        findAllSupersededModels(ids, pagination)
    }

    @Override
    List<Terminology> findAllModelSupersededModels(Map pagination) {
        List<UUID> ids = findAllModelSupersededIds(Terminology.by().id().list() as List<UUID>)
        findAllSupersededModels(ids, pagination)
    }

    @Override
    List<Terminology> findAllDeletedModels(Map pagination) {
        Terminology.byDeleted().list(pagination)
    }

    List<Terminology> findAllSupersededModels(List<UUID> ids, Map pagination) {
        if (!ids) return []
        Terminology.byIdInList(ids).list(pagination)
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
        versionLinkService.filterModelIdsWhereModelIdIsDocumentSuperseded(Terminology.simpleName, readableIds)
    }

    List<UUID> findAllModelSupersededIds(List<UUID> readableIds) {
        // All versionLinks which are targets of model version links
        List<VersionLink> modelVersionLinks = versionLinkService.findAllByTargetCatalogueItemIdInListAndIsModelSuperseded(readableIds)

        // However they are only superseded if the source of this link is finalised
        modelVersionLinks.findAll {
            Terminology sourceModel = get(it.catalogueItemId)
            sourceModel.finalised
        }.collect { it.targetModelId }
    }

    List<Terminology> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        Terminology.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology))).list(pagination)
    }

    List<Terminology> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        Terminology.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology))).list(pagination)
    }

    Terminology findTerminologySuperseding(Terminology terminology) {
        VersionLink link = versionLinkService.findLatestLinkSupersedingModelId(Terminology.simpleName, terminology.id)
        if (!link) return null
        link.catalogueItemId == terminology.id ? get(link.targetModelId) : get(link.catalogueItemId)
    }

    Terminology findTerminologyDocumentationSuperseding(Terminology terminology) {
        VersionLink link = versionLinkService.findLatestLinkDocumentationSupersedingModelId(Terminology.simpleName, terminology.id)
        if (!link) return null
        link.catalogueItemId == terminology.id ? get(link.targetModelId) : get(link.catalogueItemId)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<Terminology> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology)
        ids ? Terminology.findAllByIdInList(ids, pagination) : []
    }

    void checkDocumentationVersion(Terminology terminology, boolean importAsNewDocumentationVersion, User catalogueUser) {
        if (importAsNewDocumentationVersion) {

            if (countByLabel(terminology.label)) {
                List<Terminology> existingModels = findAllByLabel(terminology.label)
                existingModels.each { existing ->
                    log.debug('Setting Terminology as new documentation version of [{}:{}]', existing.label, existing.documentationVersion)
                    if (!existing.finalised) finaliseModel(existing, catalogueUser, null, null)
                    setTerminologyIsNewDocumentationVersionOfTerminology(terminology, existing, catalogueUser)
                }
                Version latestVersion = existingModels.max { it.documentationVersion }.documentationVersion
                terminology.documentationVersion = Version.nextMajorVersion(latestVersion)

            } else log.info('Marked as importAsNewDocumentationVersion but no existing Terminologys with label [{}]', terminology.label)
        }
    }

    void checkfinaliseModel(Terminology terminology, boolean finalised) {
        if (finalised && !terminology.finalised) {
            terminology.finalised = finalised
            terminology.dateFinalised = terminology.finalised ? OffsetDateTime.now() : null
        }
    }
}
