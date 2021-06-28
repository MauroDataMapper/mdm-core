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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeObjectDiffData
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class CodeSetService extends ModelService<CodeSet> {

    TermRelationshipTypeService termRelationshipTypeService
    TermService termService
    TermRelationshipService termRelationshipService
    PathService pathService
    CodeSetJsonImporterService codeSetJsonImporterService

    @Override
    CodeSet get(Serializable id) {
        CodeSet.get(id)
    }

    @Override
    List<CodeSet> getAll(Collection<UUID> ids) {
        CodeSet.getAll(ids).findAll().collect {unwrapIfProxy(it)}
    }

    @Override
    List<CodeSet> list(Map pagination = [:]) {
        CodeSet.list(pagination)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "cs"
    }

    @Override
    String getUrlResourceName() {
        "codeSets"
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

    @Override
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
    CodeSet saveModelWithContent(CodeSet model) {
        save(failOnError: true, validate: false, flush: true, model)
    }

    @Override
    CodeSet saveModelNewContentOnly(CodeSet model) {
        save(failOnError: true, validate: false, flush: true, model)
    }

    @Override
    List<CodeSet> findAllReadableModels(List<UUID> constrainedIds, boolean includeDeleted) {
        CodeSet.withReadable(CodeSet.by(), constrainedIds, includeDeleted).list()
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

    @Override
    List<UUID> getAllModelIds() {
        CodeSet.by().id().list() as List<UUID>
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

    int countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
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
    CodeSet mergeObjectDiffIntoModel(MergeObjectDiffData modelMergeObjectDiff, CodeSet targetModel,
                                     UserSecurityPolicyManager userSecurityPolicyManager) {


        if (!modelMergeObjectDiff.hasDiffs()) return targetModel

        modelMergeObjectDiff.getValidDiffs().each { mergeFieldDiff ->

            if (mergeFieldDiff.isFieldChange()) {
                targetModel.setProperty(mergeFieldDiff.fieldName, mergeFieldDiff.value)
            } else if (mergeFieldDiff.isMetadataChange()) {
                mergeMetadataIntoCatalogueItem(mergeFieldDiff, targetModel, userSecurityPolicyManager)
            } else {
                ModelItemService modelItemService = modelItemServices.find { it.handles(mergeFieldDiff.fieldName) }

                if (modelItemService) {

                    // Special handling for terms as CodeSets dont own terms
                    if (mergeFieldDiff.fieldName == 'terms') {
                        // apply deletions of children to target object
                        mergeFieldDiff.deleted.each { mergeItemData ->
                            Term modelItem = modelItemService.get(mergeItemData.id) as Term
                            targetModel.removeFromTerms(modelItem)
                        }

                        // copy additions from source to target object
                        mergeFieldDiff.created.each { mergeItemData ->
                            Term modelItem = modelItemService.get(mergeItemData.id) as Term
                            targetModel.addToTerms(modelItem)
                        }
                        // for modifications, recursively call this method
                        mergeFieldDiff.modified.each { mergeObjectDiffData ->
                            Term termToRemove = modelItemService.get(mergeObjectDiffData.leftId) as Term
                            Term termToAdd = modelItemService.get(mergeObjectDiffData.rightId) as Term
                            targetModel.removeFromTerms(termToRemove)
                            targetModel.addToTerms(termToAdd)
                        }
                    } else {
                        // apply deletions of children to target object
                        mergeFieldDiff.deleted.each { mergeItemData ->
                            ModelItem modelItem = modelItemService.get(mergeItemData.id) as ModelItem
                            modelItemService.delete(modelItem)
                        }

                        // copy additions from source to target object
                        mergeFieldDiff.created.each { mergeItemData ->
                            ModelItem modelItem = modelItemService.get(mergeItemData.id) as ModelItem
                            modelItemService.copy(targetModel, modelItem, userSecurityPolicyManager)
                        }
                        // for modifications, recursively call this method
                        mergeFieldDiff.modified.each { mergeObjectDiffData ->
                            ModelItem modelItem = modelItemService.get(mergeObjectDiffData.leftId) as ModelItem
                            modelItemService.mergeObjectDiffIntoModelItem(mergeObjectDiffData, modelItem, targetModel, userSecurityPolicyManager)
                        }
                    }
                } else {
                    log.error('Unknown ModelItem field to merge [{}]', mergeFieldDiff.fieldName)
                }
            }
        }
        targetModel
    }

    CodeSet copyModel(CodeSet original, Folder folderToCopyTo, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                      String branchName, boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        CodeSet copy = new CodeSet(author: original.author,
                                   organisation: original.organisation,
                                   finalised: false, deleted: false, documentationVersion: copyDocVersion,
                                   folder: folderToCopyTo, authority: authorityService.defaultAuthority, branchName: branchName
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
            editService.createAndSaveEdit(EditTitle.COPY, copy.id, copy.domainType,
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
    List<UUID> findAllModelIdsWithTreeChildren(List<CodeSet> models) {
        // CodeSets should never return a tree structure
        []
    }

    @Override
    List<CodeSet> findAllDeletedModels(Map pagination) {
        CodeSet.byDeleted().list(pagination)
    }

    List<CodeSet> findAllModelsByIdInList(List<UUID> ids, Map pagination) {
        if (!ids) return []
        CodeSet.byIdInList(ids).list(pagination)
    }

    List<CodeSet> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        CodeSet.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet))).list(pagination)
    }

    List<CodeSet> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        CodeSet.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet))).list(pagination)
    }

    @Override
    int countByAuthorityAndLabelAndVersion(Authority authority, String label, Version modelVersion) {
        CodeSet.countByAuthorityAndLabelAndModelVersion(authority, label, modelVersion)
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
     * (2) Check the authority
     * (3) Check facets
     * (4) Check that terms exist
     *
     * @param importingUser The importing user, who will be used to set createdBy
     * @param codeSet The codeSet to be imported
     * @param bindingMap The binding map, which is necessary for looking up terms
     */
    void checkImportedCodeSetAssociations(User importingUser, CodeSet codeSet, Map bindingMap = [:]) {
        codeSet.createdBy = importingUser.emailAddress

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

    @Override
    ModelImporterProviderService<CodeSet, ? extends ModelImporterProviderServiceParameters> getJsonModelImporterProviderService() {
        codeSetJsonImporterService
    }

    @Override
    List<CodeSet> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        CodeSet.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<CodeSet> findAllByMetadataNamespace(String namespace, Map pagination) {
        CodeSet.byMetadataNamespace(namespace).list(pagination)
    }

}
