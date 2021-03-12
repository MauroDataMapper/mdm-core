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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Transactional
class TerminologyService extends ModelService<Terminology> {

    TermRelationshipTypeService termRelationshipTypeService
    TermService termService
    TermRelationshipService termRelationshipService
    AuthorityService authorityService

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
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(terminology, null)
            }
            log.debug('Deleting Terminology')
            long start = System.currentTimeMillis()
            deleteModelAndContent(terminology)
            log.debug('Terminology deleted. Took {}', Utils.timeTaken(start))
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
    Terminology saveModelWithContent(Terminology terminology) {
        log.debug('Saving {} using batching', terminology.label)

        if (terminology.classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(terminology.classifiers)
        }
        save(failOnError: true, validate: false, flush: true, terminology)
    }

    @Override
    Terminology saveModelNewContentOnly(Terminology model) {
        save(failOnError: true, validate: false, flush: true, model)
    }

    void deleteModelAndContent(Terminology model) {

        GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)

        log.trace('Removing Terms in Terminology')
        termService.deleteAllByModelId(model.id)

        log.trace('Removing TermRelationshipTypes in Terminology')
        termRelationshipTypeService.deleteAllByModelId(model.id)

        log.trace('Removing facets')
        deleteAllFacetsByCatalogueItemId(model.id, 'delete from terminology.join_terminology_to_facet where terminology_id=:id')

        log.trace('Content removed')
        sessionFactory.currentSession
            .createSQLQuery('delete from terminology.terminology where id = :id')
            .setParameter('id', model.id)
            .executeUpdate()

        log.trace('Terminology removed')

        sessionFactory.currentSession
            .createSQLQuery('delete from core.breadcrumb_tree where domain_id = :id')
            .setParameter('id', model.id)
            .executeUpdate()

        log.trace('Breadcrumb tree removed')

        GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
    }

    @Override
    List<Terminology> findAllReadableModels(List<UUID> constrainedIds, boolean includeDeleted) {
        Terminology.withReadable(Terminology.by(), constrainedIds, includeDeleted).list()
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

    @Override
    List<UUID> findAllModelIds() {
        Terminology.by().id().list() as List<UUID>
    }

    @Override
    List<Terminology> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        Terminology.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<Terminology> findAllByMetadataNamespace(String namespace, Map pagination) {
        Terminology.byMetadataNamespace(namespace).list(pagination)
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

    int countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
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

    Terminology copyModel(Terminology original, User copier, boolean copyPermissions, String label, Version copyVersion, String branchName,
                          boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        Folder folder = proxyHandler.unwrapIfProxy(original.folder) as Folder
        Terminology copy = new Terminology(author: original.author,
                                           organisation: original.organisation,
                                           finalised: false, deleted: false, documentationVersion: copyVersion,
                                           folder: folder, authority: original.authority, branchName: branchName
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
    boolean hasTreeTypeModelItems(Terminology terminology, boolean forDiff, boolean includeImported = false) {
        isTreeStructureCapableTerminology(terminology)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(Terminology terminology, boolean forDiff = false, boolean includeImported = false) {
        List<Term> terms = termService.findAllByTerminologyIdAndDepth(terminology.id, 1)
        if (terms.size() > 100) {
            log.warn('Too many terms found to provide a stable tree {}', terms.size())
            return []
        }
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
    List<UUID> findAllModelIdsWithTreeChildren(List<Terminology> models) {
        models.findAll { isTreeStructureCapableTerminology(it) }.collect { it.id }
    }

    @Override
    List<Terminology> findAllDeletedModels(Map pagination) {
        Terminology.byDeleted().list(pagination)
    }

    List<Terminology> findAllSupersededModels(List<UUID> ids, Map pagination) {
        if (!ids) return []
        Terminology.byIdInList(ids).list(pagination)
    }

    List<Terminology> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        Terminology.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology))).list(pagination)
    }

    List<Terminology> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        Terminology.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology))).list(pagination)
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

    /**
     * Find a Terminology by label.
     * @param label
     * @return The found Terminology
     */
    Terminology findByLabel(String label) {
        Terminology.findByLabel(label)
    }

    /**
     * When importing a terminology, do checks and setting of required values as follows:
     * (1) Set the createdBy of the terminology to be the importing user
     * (2) Always set authority to the default authority, overriding any authority that is set in the import data
     * (3) Check facets
     * (4) For each terminologyRelationshipType, set the terminology and if not provided the createdBy
     * (5) For each term, if not provided set the createdBy
     * (6) For each termRelationship, if not provided set the createdBy
     *
     * @param importingUser The importing user, who will be used to set createdBy
     * @param terminology The terminology to be imported
     */
    void checkImportedTerminologyAssociations(User importingUser, Terminology terminology) {
        terminology.createdBy = importingUser.emailAddress

        //At the time of writing, there is, and can only be, one authority. So here we set the authority, overriding any authority provided in the
        // import.
        terminology.authority = authorityService.getDefaultAuthority()

        checkFacetsAfterImportingCatalogueItem(terminology)

        if (terminology.termRelationshipTypes) {
            terminology.termRelationshipTypes.each {
                it.terminology = terminology
                it.createdBy = it.createdBy ?: terminology.createdBy
            }
        }

        if (terminology.terms) {
            terminology.terms.each { term ->
                term.createdBy = term.createdBy ?: terminology.createdBy
            }
        }

        terminology.getAllTermRelationships().each { tr ->
            tr.createdBy = tr.createdBy ?: terminology.createdBy
        }

        log.debug("Terminology associations checked")
    }
}
