/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.CachedDiffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Transactional
class CodeSetService extends ModelService<CodeSet> {

    TermService termService
    CodeSetJsonImporterService codeSetJsonImporterService
    CodeSetJsonExporterService codeSetJsonExporterService

    @Autowired(required = false)
    Set<CodeSetExporterProviderService> exporterProviderServices

    @Override
    CodeSet get(Serializable id) {
        CodeSet.get(id)
    }

    @Override
    List<CodeSet> getAll(Collection<UUID> ids) {
        CodeSet.getAll(ids).findAll().collect {unwrapIfProxy(it)}
    }

    @Override
    List<CodeSet> list(Map pagination) {
        CodeSet.list(pagination)
    }

    @Override
    List<CodeSet> list() {
        CodeSet.list().collect {unwrapIfProxy(it)}
    }

    @Override
    String getUrlResourceName() {
        'codeSets'
    }

    Long count() {
        CodeSet.count()
    }

    int countByAuthorityAndLabel(Authority authority, String label) {
        CodeSet.countByAuthorityAndLabel(authority, label)
    }

    CodeSet validate(CodeSet codeSet) {
        log.trace('Validating codeSet')
        codeSet.validate()
        codeSet
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
            codeSet.delete(flush: flush)
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(codeSet, null)
            }
        } else delete(codeSet)
    }

    @Override
    CodeSet save(CodeSet codeSet) {
        log.debug('Saving {}', codeSet.label, codeSet.ident())
        long start = System.currentTimeMillis()
        CodeSet saved = save(failOnError: true, validate: false, flush: false, codeSet)
        log.debug('Complete save of CodeSet complete in {}', Utils.timeTaken(start))
        saved
    }

    @Override
    CodeSet saveModelWithContent(CodeSet model) {
        save(model)
    }

    @Override
    CodeSet saveModelNewContentOnly(CodeSet model) {
        save(model)
    }

    @Override
    void deleteModelsAndContent(Set<UUID> idsToDelete) {
        GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)

        log.trace('Removing Term relationships to {} CodeSets', idsToDelete.size())
        sessionFactory.currentSession
            .createSQLQuery('DELETE FROM terminology.join_codeset_to_term WHERE codeset_id IN :ids')
            .setParameter('ids', idsToDelete)
            .executeUpdate()

        log.trace('Removing facets')
        deleteAllFacetsByMultiFacetAwareIds(idsToDelete.toList(), 'delete from terminology.join_codeset_to_facet where codeset_id in :ids')

        log.trace('Content removed')
        sessionFactory.currentSession
            .createSQLQuery('DELETE FROM terminology.code_set WHERE id IN :ids')
            .setParameter('ids', idsToDelete)
            .executeUpdate()

        log.trace('CodeSets removed')

        sessionFactory.currentSession
            .createSQLQuery('DELETE FROM core.breadcrumb_tree WHERE domain_id IN :ids')
            .setParameter('ids', idsToDelete)
            .executeUpdate()

        log.trace('Breadcrumb trees removed')

        GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
    }

    @Override
    List<CodeSet> findAllReadableByEveryone() {
        CodeSet.findAllByReadableByEveryone(true)
    }

    @Override
    List<CodeSet> findAllReadableByAuthenticatedUsers() {
        CodeSet.findAllByReadableByAuthenticatedUsers(true)
    }

    List<CodeSet> findAllByAuthorityAndLabel(Authority authority, String label) {
        CodeSet.findAllByAuthorityAndLabel(authority, label)
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

    @Override
    int countByAuthorityAndLabelAndBranchNameAndNotFinalised(Authority authority, String label, String branchName) {
        CodeSet.countByAuthorityAndLabelAndBranchNameAndFinalised(authority, label, branchName, false)
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

    CodeSet copyModel(CodeSet original, Folder folderToCopyTo, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                      String branchName, boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        long start = System.currentTimeMillis()
        log.debug('Creating a new copy of {} with branch name {}', original.label, branchName)
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

        List<Term> terms = termService.findAllByCodeSetId(original.id)
        terms.each {term ->
            copy.addToTerms(term)
        }
        log.debug('Copy of codeset took {}', Utils.timeTaken(start))
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
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results = CodeSet.labelHibernateSearch(CodeSet, searchTerm, readableIds.toList(), []).results
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
    List<CodeSet> findAllByClassifier(Classifier classifier) {
        CodeSet.byClassifierId(classifier.id).list() as List<CodeSet>
    }

    @Override
    List<CodeSet> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(CodeSet, it.id)}
    }

    @Override
    Integer countByContainerId(UUID containerId) {
        // We do not concern ourselves any other types of containers for CodeSets at this time
        CodeSet.byFolderId(containerId).count()
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

    List<CodeSet> findAllByTermIdAndUser(UUID termId, UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(CodeSet)
        CodeSet.byTermIdAndIdInList(termId, ids).list(pagination)
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
                Path path = Path.from(it.termPath)

                //pathService requires a UserSecurityPolicyManager.
                //Assumption is that if we got this far then it is OK to read the Terms because either (i) we came via a controller in which case
                //the user's ability to import a CodeSet has already been tested, or (ii) we are calling this method from a service test spec in which
                //case it is OK to read.
                Term term = pathService.findResourceByPathFromRootClass(Terminology, path) as Term

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
    CodeSetJsonExporterService getJsonModelExporterProviderService() {
        codeSetJsonExporterService
    }

    @Override
    void updateModelItemPathsAfterFinalisationOfModel(CodeSet model) {
// no-op
    }

    @Override
    List<CodeSet> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        CodeSet.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<CodeSet> findAllByMetadataNamespace(String namespace, Map pagination) {
        CodeSet.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    void processCreationPatchOfModelItem(ModelItem modelItem, Model targetModel, Path pathToCopy,
                                         UserSecurityPolicyManager userSecurityPolicyManager, boolean flush = false) {
        if (!Utils.parentClassIsAssignableFromChild(Term, modelItem.class)) {
            throw new ApiInternalException('CSXX', "Cannot create [${modelItem.domainType}] into a CodeSet")
        }
        log.debug('Creating Term [{}] into CodeSet [{}]', modelItem.getDiffIdentifier(CodeSet.simpleName), Path.from(targetModel))

        (targetModel as CodeSet).addToTerms(modelItem as Term)
        save(targetModel as CodeSet, flush: flush, validate: false)
    }

    @Override
    void processDeletionPatchOfModelItem(ModelItem modelItem, Model targetModel, Path pathToDelete) {
        if (!Utils.parentClassIsAssignableFromChild(Term, modelItem.class)) {
            throw new ApiInternalException('CSXX', "Cannot delete [${modelItem.domainType}] from CodeSet")
        }
        log.debug('Removing Term from CodeSet [{}]', Path.from(targetModel))

        (targetModel as CodeSet).removeFromTerms(modelItem as Term)
        save(targetModel as CodeSet, flush: false, validate: false)
    }

    @Override
    void updateCopiedCrossModelLinks(CodeSet copiedModel, CodeSet originalModel) {
        super.updateCopiedCrossModelLinks(copiedModel, originalModel)
        // Find all Terms which were added to this codeSet
        // These will all point to the same terminology terms as the original model,
        // However this method is designed to repoint them to the branched model which exists inside the same VF as this copied model
        // ie VF-A has CS-B & T-C, VF-D is a branch of A with CS-E & T-F, CS-E points all its terms to those inside T-C,
        // we need to update them to use the terms inside T-F
        // If a T-G exists outside VF-A or D which CS-B/CS-E uses then the terms remain as-is
        List<Term> terms = new ArrayList<>(copiedModel.terms)
        Path copiedCodeSetPath = getFullPathForModel(copiedModel)
        Path originalCodeSetPath = getFullPathForModel(originalModel)
        terms.each {term ->

            Terminology terminology = term.terminology
            Path fullContextTerminologyPath = getFullPathForModel(terminology)
            Path termPath = Path.from(terminology, term)
            // Need to check if the CS is inside the same VF as the terminology
            PathNode terminologyVersionedFolderPathNode = fullContextTerminologyPath.find {it.prefix == 'vf'}
            if (terminologyVersionedFolderPathNode && originalCodeSetPath.any {it == terminologyVersionedFolderPathNode}) {
                log.debug('Original codeset is inside the same context path as terminology for term [{}]', termPath)
                Term branchedTerm = pathService.findResourceByPathFromRootResource(copiedModel, termPath,
                                                                                   copiedCodeSetPath.last().modelIdentifier) as Term
                if (branchedTerm) {
                    copiedModel.removeFromTerms(term)
                    copiedModel.addToTerms(branchedTerm)
                } else {
                    log.error('Branched term not found')
                }
            }
        }
        save(copiedModel, flush: false, validate: false)
    }

    @Override
    CachedDiffable<CodeSet> loadEntireModelIntoDiffCache(UUID modelId) {
        long start = System.currentTimeMillis()
        CodeSet loadedModel = get(modelId)
        log.debug('Loading entire model [{}] into memory', loadedModel.path)

        // Load direct content

        log.trace('Loading Term')
        List<Term> terms = termService.findAllByCodeSetId(modelId)

        log.trace('Loading Facets')
        List<UUID> allIds = Utils.gatherIds(Collections.singleton(modelId),
                                            terms.collect {it.id})

        Map<String, Map<UUID, List<Diffable>>> facetData = loadAllDiffableFacetsIntoMemoryByIds(allIds)

        DiffCache diffCache = createCatalogueItemDiffCache(null, loadedModel, facetData)
        createCatalogueItemDiffCaches(diffCache, 'terms', terms, facetData)

        log.debug('Model loaded into memory, took {}', Utils.timeTaken(start))
        new CachedDiffable(loadedModel, diffCache)
    }
}