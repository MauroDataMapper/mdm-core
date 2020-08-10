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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.hibernate.SessionFactory
import org.springframework.context.MessageSource

@Slf4j
@Transactional
class TermService extends ModelItemService<Term> {

    TermRelationshipService termRelationshipService
    MessageSource messageSource
    SessionFactory sessionFactory

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

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

    @Override
    Term save(Map args = [flush: true], Term term) {
        term.save(args)
        updateFacetsAfterInsertingCatalogueItem(term)
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
        terminology.trackChanges() // Discard any latent changes to the Terminology as we dont want them
        term.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<Term> terms) {
        terms.each {
            delete(it)
        }
    }

    void batchSave(Collection<Term> terms) {
        log.trace('Batch saving terms')
        long start = System.currentTimeMillis()
        List batch = []
        int count = 0
        terms.each {term ->
            term.sourceTermRelationships?.clear()
            term.targetTermRelationships?.clear()
            batch += term
            count++
            if (count % Term.BATCH_SIZE == 0) {
                singleBatchSave(batch)
                batch.clear()
            }

        }
        // Save final batch of terms
        singleBatchSave(batch)
        batch.clear()
        log.debug('{} terms batch saved, took {}', terms.size(), Utils.timeTaken(start))
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

    Term copyTerm(Terminology copiedTerminology, Term original, User copier) {
        copyTermIntoTerminologyOrCodeSet(copiedTerminology, original, copier)
    }

    Term copyTerm(CodeSet copiedCodeSet, Term original, User copier) {
        copyTermIntoTerminologyOrCodeSet(copiedCodeSet, original, copier)
    }

    private Term copyTermIntoTerminologyOrCodeSet(copiedTerminologyOrCodeSet, Term original, User copier) {

        if (!original) throw new ApiInternalException('DCSXX', 'Cannot copy non-existent Term')

        Term copy = new Term(createdBy: copier.emailAddress, code: original.code, definition: original.definition)

        copy = copyCatalogueItemInformation(original, copy, copier)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedTerminologyOrCodeSet.addToTerms(copy)

        if (copy.validate()) save(validate: false, copy)
        else throw new ApiInvalidModelException('DC01', 'Copied Term is invalid', copy.errors, messageSource)

        copy.trackChanges()
        copy
    }

    @Override
    Class<Term> getModelItemClass() {
        Term
    }

    @Override
    boolean hasTreeTypeModelItems(Term term) {
        termRelationshipService.countByTermIdIsParent(term.id)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(Term term) {
        TermRelationship.byTermIdIsParent(term.id)
            .join('sourceTerm')
            .join('targetTerm')
            .list()
            .collect {tr ->
                if (tr.relationshipType.parentalRelationship) tr.sourceTerm
                else if (tr.relationshipType.childRelationship) tr.targetTerm
                else null
            }.findAll()
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
    List<Term> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        Term.byClassifierId(classifier.id).list().findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, it.model.id)}
    }

    @Override
    Term updateIndexForModelItemInParent(Term modelItem, CatalogueItem parent, int newIndex) {
        throw new ApiNotYetImplementedException('TSXX', 'Term Ordering')
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
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = Term.luceneLabelSearch(Term, searchTerm, readableIds.toList()).results
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

        TermRelationship relationship = term.targetTermRelationships.find {it.sourceIsParentToTarget()}
        if (relationship) return 1 + calculateTermDepth(relationship.sourceTerm, inMemory)

        relationship = term.sourceTermRelationships.find {it.sourceIsChildOfTarget()}
        if (relationship) return 1 + calculateTermDepth(relationship.targetTerm, inMemory)

        Integer.MAX_VALUE
    }

    Boolean isRootTerm(Term term, boolean inMemory) {
        hasNoHierarchy(term, inMemory) || (isParentTerm(term, inMemory) && !isChildTerm(term, inMemory))
    }

    Boolean isParentTerm(Term term, boolean inMemory) {
        inMemory ?
        term.targetTermRelationships.any {it.relationshipType.childRelationship} ||
        term.sourceTermRelationships.any {it.relationshipType.parentalRelationship} :
        termRelationshipService.countByTermIdIsParent(term.id)
    }

    Boolean isChildTerm(Term term, boolean inMemory) {
        inMemory ?
        term.targetTermRelationships.any {it.relationshipType.parentalRelationship} ||
        term.sourceTermRelationships.any {it.relationshipType.childRelationship} :
        termRelationshipService.countByTermIdIsChild(term.id)
    }

    Boolean hasNoHierarchy(Term term, boolean inMemory) {
        inMemory ?
        term.targetTermRelationships.every {!it.relationshipType.childRelationship && !it.relationshipType.parentalRelationship} &&
        term.sourceTermRelationships.every {!it.relationshipType.childRelationship && !it.relationshipType.parentalRelationship} :
        termRelationshipService.countByTermIdHasHierarchy(term.id) == 0
    }

    List<ModelItemTreeItem> buildTermTree(Terminology terminology, Term term = null) {

        Set<ModelItemTreeItem> tree

        long start = System.currentTimeMillis()

        if (!term) {
            log.debug('No term provided so providing top level tree')

            List<Term> terms = Term.findAllByTerminologyAndDepth(terminology, 1)
            List<TermRelationship> childKnowledge = obtainChildKnowledge(terms)
            Set<String> hasChildren = []
            childKnowledge.each {relationship ->
                if (relationship.relationshipType.childRelationship) {
                    hasChildren += relationship.targetTerm.code
                } else if (relationship.relationshipType.parentalRelationship) {
                    hasChildren += relationship.sourceTerm.code
                }
            }
            tree = terms.collect {root ->
                new ModelItemTreeItem(root, hasChildren.contains(root.code))
            }.toSet()
        } else {
            int depth = term.depth
            log.debug('Term provided, building tree at depth {}', depth)

            log.debug('Building source as parent relationships')
            Set<Term> terms = TermRelationship.bySourceTermIdAndParental(term.id)
                .join('targetTerm')
                .list()
                .collect {it.targetTerm}.toSet()

            List<TermRelationship> childKnowledge = obtainChildKnowledge(terms.asList())
            Set<String> hasChildren = []
            childKnowledge.each {relationship ->
                if (relationship.relationshipType.childRelationship) {
                    hasChildren += relationship.targetTerm.code
                } else if (relationship.relationshipType.parentalRelationship) {
                    hasChildren += relationship.sourceTerm.code
                }
            }
            tree = terms.collect {parent ->
                new ModelItemTreeItem(parent, hasChildren.contains(parent.code))
            }.toSet()

            log.debug('Building source as child relationships')
            terms = TermRelationship.byTargetTermIdAndChild(term.id)
                .join('sourceTerm')
                .list()
                .collect {it.sourceTerm}
            childKnowledge = obtainChildKnowledge(terms.asList())

            terms.each {parent ->
                if (!hasParentToRelationship(parent, term)) {
                    tree.add(new ModelItemTreeItem(parent, hasChild(parent, childKnowledge)))
                }
            }
        }

        List<ModelItemTreeItem> sortedTree = tree.sort()
        log.debug('Tree build took {}', Utils.timeTaken(start))
        sortedTree
    }

    private Boolean hasParentToRelationship(Term parent, Term child) {
        parent.sourceTermRelationships.any {it.sourceIsParentToTarget() && it.targetTerm == child}
    }

    private List<TermRelationship> obtainChildKnowledge(List<Term> parents) {
        if (!parents) return []
        TermRelationship.byTermsAreParents(parents).list()
    }

    private boolean hasChild(Term parent, List<TermRelationship> knowledge) {
        knowledge.any {
            (it.targetTerm == parent && it.relationshipType.childRelationship) ||
            (it.sourceTerm == parent && it.relationshipType.parentalRelationship)
        }
    }

    private void singleBatchSave(Collection<Term> terms) {
        if (!terms) {
            return
        }
        long start = System.currentTimeMillis()
        log.trace('Batch saving {} terms', terms.size())

        Term.saveAll(terms)

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }
}
