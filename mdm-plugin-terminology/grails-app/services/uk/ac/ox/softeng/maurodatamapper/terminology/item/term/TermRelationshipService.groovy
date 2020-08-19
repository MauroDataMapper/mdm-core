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
package uk.ac.ox.softeng.maurodatamapper.terminology.item.term

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory

@Slf4j
@Transactional
class TermRelationshipService extends ModelItemService<TermRelationship> {

    SessionFactory sessionFactory

    TermRelationship get(Serializable id) {
        TermRelationship.get(id)
    }

    List<TermRelationship> list(Map args) {
        TermRelationship.list(args)
    }

    Long count() {
        TermRelationship.count()
    }

    void delete(Serializable id) {
        TermRelationship termRelationship = get(id)
        if (termRelationship) delete(termRelationship)
    }

    void delete(TermRelationship termRelationship, boolean flush = false) {
        if (!termRelationship) return

        termRelationship.breadcrumbTree.removeFromParent()
        termRelationship.sourceTerm.removeFromSourceTermRelationships(termRelationship)
        termRelationship.targetTerm.removeFromTargetTermRelationships(termRelationship)

        termRelationship.delete(flush: flush)
    }

    void deleteAll(Collection<TermRelationship> termRelationships) {
        termRelationships.each {
            delete(it)
        }
    }

    void batchSave(Collection<TermRelationship> relationships) {
        log.trace('Batch saving relationships')
        long start = System.currentTimeMillis()
        List batch = []
        int count = 0
        relationships.each { relationship ->
            batch += relationship
            count++
            if (count % TermRelationship.BATCH_SIZE == 0) {
                singleBatchSave(batch)
                batch.clear()
            }
        }
        // Save final batch of terms
        singleBatchSave(batch)
        batch.clear()
        log.debug('{} relationships batch saved, took {}', relationships.size(), Utils.timeTaken(start))
    }

    List<TermRelationship> findAllByTermId(UUID termId, Map paginate = [:]) {
        TermRelationship.withFilter(TermRelationship.byTermId(termId), paginate).list(paginate)
    }

    List<TermRelationship> findAllByTermIdAndType(UUID id, String type, Map paginate = [:]) {
        TermRelationship.withFilter(TermRelationship.byTermIdAndTermType(id, type), paginate).list(paginate)
    }

    List<TermRelationship> findAllByRelationshipTypeId(UUID relationshipTypeId, Map paginate = [:]) {
        TermRelationship.byRelationshipTypeId(relationshipTypeId).list(paginate)
    }

    TermRelationship findByTermIdAndId(UUID termId, Serializable id) {
        TermRelationship.byTermIdAndId(termId, Utils.toUuid(id)).get()
    }

    TermRelationship findByRelationshipTypeIdAndId(UUID relationshipTypeId, Serializable id) {
        TermRelationship.byRelationshipTypeIdAndId(relationshipTypeId, Utils.toUuid(id)).get()
    }

    Long countByTermIdIsParent(UUID termId) {
        TermRelationship.byTermIdIsParent(termId).count()
    }

    Long countByTermIdIsChild(UUID termId) {
        TermRelationship.byTermIdIsChild(termId).count()
    }

    Long countByTermIdHasHierarchy(UUID termId) {
        TermRelationship.byTermIdHasHierarchy(termId).count()
    }

    TermRelationship copyTermRelationship(Terminology terminology, TermRelationship original, User copier) {

        Term source = terminology.findTermByCode(original.sourceTerm.code)
        Term target = terminology.findTermByCode(original.targetTerm.code)
        TermRelationshipType termRelationshipType = terminology.findRelationshipTypeByLabel(original.relationshipType.label)

        TermRelationship copy = new TermRelationship(createdBy: copier.emailAddress, relationshipType: termRelationshipType, sourceTerm: source,
                                                     targetTerm: target)
        source.addToSourceTermRelationships(copy)
        target.addToTargetTermRelationships(copy)
        copy
    }

    private void singleBatchSave(Collection<TermRelationship> termRelationships) {
        long start = System.currentTimeMillis()
        log.trace('Batch saving {} termRelationships', termRelationships.size())

        TermRelationship.saveAll(termRelationships)

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    @Override
    Class<TermRelationship> getModelItemClass() {
        TermRelationship
    }

    @Override
    TermRelationship findByIdJoinClassifiers(UUID id) {
        TermRelationship.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        TermRelationship.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<TermRelationship> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        TermRelationship.byClassifierId(classifier.id).list().
            findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, it.model.id) }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        false
    }

    @Override
    List<TermRelationship> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                      String searchTerm, String domainType) {
        []
    }

    @Override
    TermRelationship updateIndexForModelItemInParent(TermRelationship modelItem, CatalogueItem parent, int newIndex) {
        throw new ApiNotYetImplementedException('TRSXX', 'TermRelationshipType Ordering')
    }

    @Override
    TermRelationship save(Map args = [flush: true], TermRelationship catalogueItem) {
        catalogueItem.save(args)
        updateFacetsAfterInsertingCatalogueItem(catalogueItem)
    }

    @Override
    List<TermRelationship> getAll(Collection<UUID> ids) {
        TermRelationship.getAll(ids).findAll()
    }

    @Override
    boolean hasTreeTypeModelItems(TermRelationship catalogueItem) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(TermRelationship catalogueItem) {
        []
    }
}