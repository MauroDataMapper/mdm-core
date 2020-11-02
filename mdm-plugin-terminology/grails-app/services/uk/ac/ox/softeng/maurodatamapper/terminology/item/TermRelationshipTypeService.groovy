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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class TermRelationshipTypeService extends ModelItemService<TermRelationshipType> {

    TerminologyService terminologyService
    TermRelationshipService termRelationshipService

    @Override
    TermRelationshipType get(Serializable id) {
        TermRelationshipType.get(id)
    }

    @Override
    List<TermRelationshipType> getAll(Collection<UUID> ids) {
        TermRelationshipType.getAll(ids).findAll()
    }

    @Override
    List<TermRelationshipType> list(Map args) {
        TermRelationshipType.list(args)
    }

    Long count() {
        TermRelationshipType.count()
    }

    void delete(Serializable id) {
        TermRelationshipType termRelationshipType = get(id)
        if (termRelationshipType) delete(termRelationshipType)
    }

    void delete(TermRelationshipType termRelationshipType, boolean flush = false) {
        if (!termRelationshipType) return
        termRelationshipType.terminology.removeFromTermRelationshipTypes(termRelationshipType)
        termRelationshipType.breadcrumbTree.removeFromParent()
        List<TermRelationship> termRelationships = termRelationshipService.findAllByRelationshipTypeId(termRelationshipType.id)
        termRelationshipService.deleteAll(termRelationships)

        termRelationshipType.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<TermRelationshipType> termRelationshipTypes) {
        termRelationshipTypes.each {
            delete(it)
        }
    }

    def findAllByTerminologyId(UUID terminologyId, Map paginate = [:]) {
        TermRelationshipType.withFilter(TermRelationshipType.byTerminologyId(terminologyId), paginate).list(paginate)
    }

    def findAllByTerminologyIdAndLabelIlikeOrDescriptionIlike(UUID terminologyId, String searchTerm, Map paginate = [:]) {
        TermRelationshipType.byTerminologyIdAndLabelIlikeOrDescriptionIlike(terminologyId, searchTerm).list(paginate)
    }

    def findByTerminologyIdAndId(UUID terminologyId, Serializable id) {
        TermRelationshipType.byTerminologyIdAndId(terminologyId, Utils.toUuid(id)).find()
    }

    TermRelationshipType copy(Terminology copiedTerminology, TermRelationshipType original, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyTermRelationshipType(copiedTerminology, original, userSecurityPolicyManager.user)
    }

    TermRelationshipType copyTermRelationshipType(Terminology copiedTerminology, TermRelationshipType original, User copier) {
        TermRelationshipType copy = new TermRelationshipType(createdBy: copier.emailAddress, label: original.label, description: original.description,
                                                             displayLabel: original.displayLabel)
        copiedTerminology.addToTermRelationshipTypes(copy)
        copy
    }

    TermRelationshipType addCreatedEditToTerminology(User creator, TermRelationshipType domain, UUID terminologyId) {
        Terminology terminology = terminologyService.get(terminologyId)
        terminology.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${terminology.editLabel}]"
        domain
    }

    TermRelationshipType addUpdatedEditToTerminology(User editor, TermRelationshipType domain, UUID terminologyId, List<String> dirtyPropertyNames) {
        Terminology terminology = terminologyService.get(terminologyId)
        terminology.addToEditsTransactionally editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    TermRelationshipType addDeletedEditToTerminology(User deleter, TermRelationshipType domain, UUID terminologyId) {
        Terminology terminology = terminologyService.get(terminologyId)
        terminology.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${terminology.editLabel}]"
        domain
    }

    @Override
    Class<TermRelationshipType> getModelItemClass() {
        TermRelationshipType
    }

    @Override
    boolean hasTreeTypeModelItems(TermRelationshipType term) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(TermRelationshipType term) {
        []
    }

    @Override
    TermRelationshipType findByIdJoinClassifiers(UUID id) {
        TermRelationshipType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        TermRelationshipType.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<TermRelationshipType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        TermRelationshipType.byClassifierId(classifier.id).list().
            findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, it.model.id) }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        false
    }

    @Override
    List<TermRelationshipType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                          String searchTerm, String domainType) {
        []
    }
}