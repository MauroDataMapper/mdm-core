/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

@Slf4j
@Transactional
class TermService extends ModelItemService<Term> {

    TermRelationshipService termRelationshipService
    MessageSource messageSource
    TerminologyService terminologyService
    TreeItemService treeItemService

    @Override
    Term get(Serializable id) {
        Term.get(id)
    }

    @Override
    List<Term> getAll(Collection<UUID> ids) {
        Term.getAll(ids).findAll()
    }

    List<Term> list(Map args) {
        Term.list(args)
    }

    Long count() {
        Term.count()
    }

    void delete(Serializable id) {
        Term term = get(id)
        if (term) delete(term)
    }

    void delete(Term term, boolean flush = false) {
        if (!term) return
        Terminology terminology = proxyHandler.unwrapIfProxy(term.terminology) as Terminology
        term.terminology = terminology

        term.terminology.removeFromTerms(term)
        term.breadcrumbTree.removeFromParent()
        List<TermRelationship> termRelationships = termRelationshipService.findAllByTermId(term.id)
        termRelationshipService.deleteAll(termRelationships)
        term.sourceTermRelationships = []
        term.targetTermRelationships = []
        if (flush) {
            // Discard any latent changes to the Terminology as we dont want them in the flish
            terminology.trackChanges()
            terminology.discard()
        }
        term.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<Term> terms) {
        terms.each {
            delete(it)
        }
    }

    @Override
    void deleteAllByModelIds(Set<UUID> modelIds) {
        List<UUID> termIds = Term.byTerminologyIdInList(modelIds).id().list() as List<UUID>

        if (termIds) {
            log.trace('Removing TermRelationships in {} Terms', termIds.size())
            termRelationshipService.deleteAllByModelIds(modelIds)

            log.trace('Removing CodeSet references for {} Terms', termIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM terminology.join_codeset_to_term WHERE term_id IN :ids')
                .setParameter('ids', termIds)
                .executeUpdate()

            log.trace('Removing facets for {} Terms', termIds.size())
            deleteAllFacetsByMultiFacetAwareIds(termIds,
                                                'delete from terminology.join_term_to_facet where term_id in :ids')

            log.trace('Removing {} Terms', termIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM terminology.term WHERE terminology_id IN :ids')
                .setParameter('ids', modelIds)
                .executeUpdate()

            log.trace('Terms removed')
        }
    }

    @Override
    void preBatchSaveHandling(List<Term> modelItems) {
        modelItems.each {t ->
            t.sourceTermRelationships?.clear()
            t.targetTermRelationships?.clear()
        }
    }

    @Override
    void gatherContents(Map<String, Object> gatheredContents, Term modelItem) {
        List<TermRelationship> termRelationships = gatheredContents.getOrDefault('termRelationships', [])
        termRelationships.addAll(modelItem.sourceTermRelationships ?: [])
        termRelationships.addAll(modelItem.targetTermRelationships ?: [])
        gatheredContents.termRelationships = termRelationships
    }

    @Override
    List<TermRelationship> returnGatheredContents(Map<String, Object> gatheredContents) {
        gatheredContents.getOrDefault('termRelationships', []) as List<TermRelationship>
    }

    Long countByTerminologyId(UUID terminologyId) {
        Term.byTerminologyId(terminologyId).count()
    }

    List<Term> findAllByTerminologyId(UUID terminologyId, Map paginate = [:]) {
        Term.withFilter(Term.byTerminologyId(terminologyId), paginate).list(paginate)
    }

    List<UUID> findAllIdsByTerminologyId(UUID terminologyId) {
        Term.byTerminologyId(terminologyId).id().list() as List<UUID>
    }

    boolean existsByTerminologyIdAndId(UUID terminologyId, Serializable id) {
        Term.byTerminologyIdAndId(terminologyId, Utils.toUuid(id)).count() == 1
    }


    Term findByTerminologyIdAndId(UUID terminologyId, Serializable id) {
        Term.byTerminologyIdAndId(terminologyId, Utils.toUuid(id)).find()
    }

    Long countByCodeSetId(UUID codeSetId) {
        Term.byCodeSetId(codeSetId).count()
    }

    Term findByCodeSetIdAndId(UUID codeSetId, Serializable id) {
        Term.byCodeSetIdAndId(codeSetId, Utils.toUuid(id)).find()
    }

    List<Term> findAllByClassifierId(UUID classifierId, Map pagination = [:]) {
        Term.withFilter(Term.byClassifierId(classifierId), pagination).list(pagination)
    }

    List<Term> findAllByCodeSetId(UUID codeSetId, Map pagination = [:]) {
        Term.withFilter(Term.byCodeSetId(codeSetId), pagination).list(pagination)
    }

    List<Term> findAllByTerminologyIdAndCodeIlikeOrDefinitionIlike(UUID terminologyId, String searchTerm, Map pagination = [:]) {
        Term.withFilter(Term.byTerminologyIdAndCodeIlikeOrDefinitionIlike(terminologyId, searchTerm), pagination).list(pagination)
    }

    List<Term> findAllWhereRootTermOfTerminologyId(UUID terminologyId, Map pagination = [:]) {
        Term.byTerminologyIdAndNotChild(terminologyId).list(pagination)
    }

    Term copy(Model copiedModel, Term original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        Term copy = copyTerm(original, userSecurityPolicyManager.user, userSecurityPolicyManager)
        if (copiedModel.instanceOf(Terminology)) {
            (copiedModel as Terminology).addToTerms(copy)
        } else if (copiedModel.instanceOf(CodeSet)) {
            (copiedModel as CodeSet).addToTerms(copy)
        } else {
            throw new ApiInternalException('TS01', "Cannot add a Term to a model type [${copiedModel.domainType}]")
        }
        copy
    }

    Term copyTerm(Terminology copiedTerminology, Term original, User copier, UserSecurityPolicyManager userSecurityPolicyManager,
                  CopyInformation copyInformation = null) {
        Term copy = copyTerm(original, copier, userSecurityPolicyManager, copyInformation)
        copiedTerminology.addToTerms(copy)
        copy
    }

    Term copyTerm(Term original, User copier, UserSecurityPolicyManager userSecurityPolicyManager, CopyInformation copyInformation = null) {
        if (!original) throw new ApiInternalException('DCSXX', 'Cannot copy non-existent Term')
        Term copy = new Term(createdBy: copier.emailAddress, code: original.code, definition: original.definition, url: original.url,
                             isParent: original.isParent,
                             depth: original.depth)
        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)
        copy
    }

    @Override
    boolean hasTreeTypeModelItems(Term term, boolean fullTreeRender, boolean includeImportedItems) {
        termRelationshipService.countByTermIdIsParent(term.id)
    }

    @Override
    Term findByIdJoinClassifiers(UUID id) {
        Term.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        Term.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<Term> findAllByClassifier(Classifier classifier) {
        Term.byClassifierId(classifier.id).list()
    }

    @Override
    List<Term> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, it.model.id)}
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == Term.simpleName
    }

    @Override
    List<Term> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                          String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Terminology)
        if (!readableIds) return []

        List<Term> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results = Term.labelHibernateSearch(Term, searchTerm, readableIds.toList(),
                                                terminologyService.getAllReadablePathNodes(readableIds)).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    Term updateDepth(Term term, boolean inMemory = false) {
        // If term depth is dirth then its already been calculated and updated
        if (term.isDirty('depth')) return term
        term.depth = calculateTermDepth(term, inMemory)
        term
    }

    Integer calculateTermDepth(Term term, boolean inMemory) {
        // If term depth is already dirty then its been calculated recently
        if (term.isDirty('depth')) return term.depth
        if (isRootTerm(term, inMemory)) return 1

        TermRelationship relationship = term.targetTermRelationships.find { it.sourceIsParentToTarget() }
        if (relationship) return 1 + calculateTermDepth(relationship.sourceTerm, inMemory)

        relationship = term.sourceTermRelationships.find { it.sourceIsChildOfTarget() }
        if (relationship) return 1 + calculateTermDepth(relationship.targetTerm, inMemory)

        Integer.MAX_VALUE
    }

    Boolean isRootTerm(Term term, boolean inMemory) {
        hasNoHierarchy(term, inMemory) || (isParentTerm(term, inMemory) && !isChildTerm(term, inMemory))
    }

    Boolean isParentTerm(Term term, boolean inMemory) {
        inMemory ?
        term.targetTermRelationships.any { it.relationshipType.childRelationship } ||
        term.sourceTermRelationships.any { it.relationshipType.parentalRelationship } :
        termRelationshipService.countByTermIdIsParent(term.id)
    }

    Boolean isChildTerm(Term term, boolean inMemory) {
        inMemory ?
        term.targetTermRelationships.any { it.relationshipType.parentalRelationship } ||
        term.sourceTermRelationships.any { it.relationshipType.childRelationship } :
        termRelationshipService.countByTermIdIsChild(term.id)
    }

    Boolean hasNoHierarchy(Term term, boolean inMemory) {
        inMemory ?
        term.targetTermRelationships.every { !it.relationshipType.childRelationship && !it.relationshipType.parentalRelationship } &&
        term.sourceTermRelationships.every { !it.relationshipType.childRelationship && !it.relationshipType.parentalRelationship } :
        termRelationshipService.countByTermIdHasHierarchy(term.id) == 0
    }

    List<ModelItemTreeItem> buildTermTree(Terminology terminology, Term term = null) {

        Set<ModelItemTreeItem> tree

        long start = System.currentTimeMillis()

        if (term) {
            int depth = term.depth
            log.debug('Term provided, building tree at depth {}', depth)
            List<Term> terms = findAllTreeTypeModelItemsIn(term) as List<Term>
            tree = terms.collect {t -> treeItemService.createModelItemTreeItem(t, t.hasChildren(), [])}.toSet()
        } else {
            log.debug('No term provided so providing top level tree')
            List<Term> terms = terminologyService.findAllTreeTypeModelItemsIn(terminology) as List<Term>
            tree = terms.collect {t -> treeItemService.createModelItemTreeItem(t, t.hasChildren(), [])}.toSet()
        }

        List<ModelItemTreeItem> sortedTree = tree.sort()
        log.debug('Tree build took {}', Utils.timeTaken(start))
        sortedTree
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(Term term, boolean fullTreeRender, boolean includeImportedItems) {
        log.debug('Building source as parent relationships')
        Set<Term> sourceAsParentTerms = TermRelationship.bySourceTermIdAndParental(term.id)
            .join('targetTerm')
            .list()
            .collect { it.targetTerm }.toSet()

        if (sourceAsParentTerms.size() > 100) {
            log.warn('Too many terms found to provide a stable tree {}', sourceAsParentTerms.size())
            return []
        }

        Set<Term> treeTerms = updateChildKnowledge(sourceAsParentTerms)

        log.debug('Building source as child relationships')
        Set<Term> sourceAsChildTerms = TermRelationship.byTargetTermIdAndChild(term.id)
            .join('sourceTerm')
            .list()
            .collect { it.sourceTerm }.toSet()

        if (sourceAsChildTerms.size() + treeTerms.size() > 100) {
            log.warn('Too many terms found to provide a stable tree {}', sourceAsChildTerms.size())
            return []
        }

        treeTerms.addAll(updateChildKnowledge(sourceAsChildTerms))
        treeTerms.toList() as List<ModelItem>
    }

    List<TermRelationship> obtainChildKnowledge(Collection<Term> parents) {
        if (!parents) return []
        TermRelationship.byTermsAreParents(parents.toList()).list()
    }

    Collection<Term> updateChildKnowledge(Collection<Term> terms) {
        List<TermRelationship> childKnowledge = obtainChildKnowledge(terms)
        terms.each { it.isParent = hasChild(it, childKnowledge) }
        terms
    }

    List<Term> findAllByTerminologyIdAndDepth(UUID terminologyId, Integer depth) {
        Term.byTerminologyIdAndDepth(terminologyId, depth).list()
    }

    private boolean hasChild(Term parent, List<TermRelationship> knowledge) {
        knowledge.any {
            (it.targetTerm == parent && it.relationshipType.childRelationship) ||
            (it.sourceTerm == parent && it.relationshipType.parentalRelationship)
        }
    }

    /*
     * Find a Term belonging to parentCatalogueItem and whose label is label.
     * The parentCatalogueItem can be a Terminology or CodeSet.
     * @param parentCatalogueItem The Terminology or CodeSet which is the parent of the Term being sought
     * @param label The label of the Term being sought
     */

    @Override
    Term findByParentIdAndLabel(UUID parentId, String label) {
        Term term = Term.byCodeSetId(parentId).eq('label', label).get()
        if (!term) {
            term = Term.byTerminologyId(parentId).eq('label', label).get()
        }
        term
    }

    @Override
    Term findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        // Older code used the full term label which is not great but we should be able to handle this here
        String legacyHandlingPathIdentifier = pathIdentifier.split(':')[0]

        Term term = Term.byCodeSetId(parentId).eq('code', legacyHandlingPathIdentifier).get()
        if (!term) {
            term = Term.byTerminologyId(parentId).eq('code', legacyHandlingPathIdentifier).get()
        }
        term
    }

    @Override
    List<Term> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        Term.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<Term> findAllByMetadataNamespace(String namespace, Map pagination) {
        Term.byMetadataNamespace(namespace).list(pagination)
    }
}
