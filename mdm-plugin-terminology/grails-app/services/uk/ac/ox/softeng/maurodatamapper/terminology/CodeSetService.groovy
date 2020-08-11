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
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
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
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
@Transactional
class CodeSetService extends ModelService<CodeSet> {

    TermRelationshipTypeService termRelationshipTypeService
    TermService termService
    TermRelationshipService termRelationshipService

    MessageSource messageSource
    VersionLinkService versionLinkService
    EditService editService
    ClassifierService classifierService

    SessionFactory sessionFactory

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
            delete(t, permanent)
            if (!permanent) updated << t
        }
        updated
    }

    @Override
    CodeSet save(CodeSet codeSet) {
        log.debug('Saving {}({}) without batching', codeSet.label, codeSet.ident())
        codeSet.save(failOnError: true, validate: false)
        updateFacetsAfterInsertingCatalogueItem(codeSet)
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

    @Deprecated
    CodeSet finaliseCodeSet(CodeSet codeSet, User user, List<Serializable> supersedeModelIds = []) {
        finaliseModel(codeSet, user, supersedeModelIds)
    }

    @Override
    CodeSet finaliseModel(CodeSet codeSet, User user, List<Serializable> supersedeModelIds = []) {
        codeSet.finalised = true
        codeSet.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
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

    @Deprecated
    CodeSet createNewDocumentationVersion(CodeSet codeSet, User user, boolean copyPermissions,
                                          boolean throwErrors = false) {
        createNewDocumentationVersion(codeSet, user, copyPermissions, [throwErrors: throwErrors])
    }

    @Override
    CodeSet createNewDocumentationVersion(CodeSet codeSet, User user, boolean copyPermissions,
                                          Map<String, Object> additionalArguments) {

        if (!newVersionCreationIsAllowed(codeSet)) return codeSet

        CodeSet newDocVersion = copyCodeSet(codeSet, user, copyPermissions, codeSet.label,
                                            Version.nextMajorVersion(codeSet.documentationVersion),
                                            additionalArguments.throwErrors as boolean)
        setCodeSetIsNewDocumentationVersionOfCodeSet(newDocVersion, codeSet, user)

        if (newDocVersion.validate()) newDocVersion.save(flush: true, validate: false)
        newDocVersion
    }

    @Deprecated
    CodeSet createNewModelVersion(String label, CodeSet codeSet, User user, boolean copyPermissions,
                                  boolean throwErrors = false) {
        createNewModelVersion(label, codeSet, user, copyPermissions, [throwErrors: throwErrors])
    }

    @Override
    CodeSet createNewModelVersion(String label, CodeSet codeSet, User user, boolean copyPermissions,
                                  Map<String, Object> additionalArguments) {

        if (!newVersionCreationIsAllowed(codeSet)) return codeSet

        CodeSet newModelVersion = copyCodeSet(codeSet, user, copyPermissions, label, additionalArguments.throwErrors as boolean)
        setCodeSetIsNewModelVersionOfCodeSet(newModelVersion, codeSet, user)

        if (newModelVersion.validate()) save(newModelVersion)
        newModelVersion
    }

    CodeSet copyCodeSet(CodeSet original, User copier, boolean copyPermissions, String label, boolean throwErrors) {
        copyCodeSet(original, copier, copyPermissions, label, Version.from('1'), throwErrors)
    }

    CodeSet copyCodeSet(CodeSet original, User copier, boolean copyPermissions, String label,
                        Version copyVersion, boolean throwErrors) {
        CodeSet copy = new CodeSet(author: original.author,
                                   organisation: original.organisation,
                                   finalised: false, deleted: false, documentationVersion: copyVersion,
                                   folder: original.folder
        )

        copy = copyCatalogueItemInformation(original, copy, copier)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('CSSXX', 'CodeSet permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            copy.save(validate: false)
            editService.createAndSaveEdit(copy.id, copy.domainType,
                                          "CodeSet ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('TMS01', 'Copied CodeSet is invalid', copy.errors, messageSource)

        copy.trackChanges()

        // Copy all the terms
        original.terms?.each {term ->
            copy.addToTerms(term)
        }

        if (original.semanticLinks) {
            original.semanticLinks.each {link ->
                copy.addToSemanticLinks(createdBy: copier.emailAddress,
                                        linkType: link.linkType,
                                        targetCatalogueItemId: link.targetCatalogueItemId,
                                        targetCatalogueItemDomainType: link.targetCatalogueItemDomainType)
            }
        }

        if (original.versionLinks) {
            original.versionLinks.each {link ->
                copy.addToVersionLinks(createdBy: copier.emailAddress,
                                       linkType: link.linkType,
                                       targetModelId: link.targetModelId,
                                       targetModelDomainType: link.targetModelDomainType)
            }
        }

        copy
    }

    void setCodeSetIsNewModelVersionOfCodeSet(CodeSet newModel, CodeSet oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
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
        CodeSet.byClassifierId(classifier.id).list().findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(CodeSet, it.id)}
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
    List<UUID> findAllModelIdsWithChildren(List<CodeSet> models) {
        models.findAll {it.hasChildren()}.collect {it.id}
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
        CodeSet.byIdInList(ids).list(pagination)
    }

    @Override
    List<CodeSet> findAllModelSupersededModels(Map pagination) {
        List<UUID> ids = findAllModelSupersededIds(CodeSet.by().id().list() as List<UUID>)
        CodeSet.byIdInList(ids).list(pagination)
    }

    @Override
    List<CodeSet> findAllDeletedModels(Map pagination) {
        CodeSet.byDeleted().list(pagination)
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
        versionLinkService.filterModelIdsWhereModelIdIsSuperseded(CodeSet.simpleName, readableIds)
    }

    List<UUID> findAllDocumentSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsDocumentSuperseded(CodeSet.simpleName, readableIds)
    }

    List<UUID> findAllModelSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsModelSuperseded(CodeSet.simpleName, readableIds)
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
                existingModels.each {existing ->
                    log.debug('Setting CodeSet as new documentation version of [{}:{}]', existing.label, existing.documentationVersion)
                    if (!existing.finalised) finaliseCodeSet(existing, catalogueUser)
                    setCodeSetIsNewDocumentationVersionOfCodeSet(codeSet, existing, catalogueUser)
                }
                Version latestVersion = existingModels.max {it.documentationVersion}.documentationVersion
                codeSet.documentationVersion = Version.nextMajorVersion(latestVersion)

            } else log.info('Marked as importAsNewDocumentationVersion but no existing CodeSets with label [{}]', codeSet.label)
        }
    }

    void checkFinaliseCodeSet(CodeSet codeSet, boolean finalised) {
        if (finalised && !codeSet.finalised) {
            codeSet.finalised = finalised
            codeSet.dateFinalised = codeSet.finalised ? OffsetDateTime.now() : null
        }
    }
}
