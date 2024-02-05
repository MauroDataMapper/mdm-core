/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class TermRelationshipService extends ModelItemService<TermRelationship> {

    TermService termService

    TermRelationship get(Serializable id) {
        TermRelationship.get(id)
    }

    List<TermRelationship> list(Map args) {
        TermRelationship.list(args)
    }

    Long count() {
        TermRelationship.count()
    }

    @Override
    TermRelationship validate(TermRelationship modelItem) {
        modelItem.validate()
        modelItem
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

    @Override
    void deleteAllByModelIds(Set<UUID> modelIds) {
        List<UUID> termRelationshipIds = TermRelationship.by().or {
            sourceTerm {
                inList('terminology.id', modelIds)
            }
            targetTerm {
                inList('terminology.id', modelIds)
            }
        }.id().list() as List<UUID>

        if (termRelationshipIds) {

            log.trace('Removing facets for {} TermRelationships', termRelationshipIds.size())
            deleteAllFacetsByMultiFacetAwareIds(termRelationshipIds,
                                                'delete from terminology.join_term_to_facet where term_id in :ids')

            log.trace('Removing {} TermRelationships', termRelationshipIds.size())
            Utils.executeInBatches(termRelationshipIds, {ids ->
                sessionFactory.currentSession
                    .createSQLQuery('DELETE FROM terminology.term_relationship WHERE id IN :ids')
                    .setParameter('ids', ids)
                    .executeUpdate()
            })

            log.trace('TermRelationships removed')
        }
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

    List<TermRelationship> findAllBySourceTermIdInList(Collection<UUID> termIds) {
        if (!termIds) return []
        TermRelationship.by().inList('sourceTerm.id', termIds).list()
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

    @Override
    TermRelationship copy(Model terminology, TermRelationship original, CatalogueItem nonModelParent,
                          UserSecurityPolicyManager userSecurityPolicyManager) {
        copyTermRelationship(terminology as Terminology, original, userSecurityPolicyManager.user)
    }

    TermRelationship copyTermRelationship(Terminology terminology, TermRelationship original, User copier) {
        copyTermRelationship(terminology, original, new TreeMap(terminology.terms.collectEntries {[it.code, it]}), copier)
    }

    TermRelationship copyTermRelationship(Terminology terminology, TermRelationship original, TreeMap<String, Term> terms, User copier) {

        Term source = terms[original.sourceTerm.code]
        Term target = terms[original.targetTerm.code]
        TermRelationshipType termRelationshipType = terminology.findRelationshipTypeByLabel(original.relationshipType.label)

        TermRelationship copy = new TermRelationship(createdBy: copier.emailAddress, relationshipType: termRelationshipType, sourceTerm: source,
                                                     targetTerm: target)
        source.addToSourceTermRelationships(copy)
        target.addToTargetTermRelationships(copy)
        copy
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
    List<TermRelationship> findAllByClassifier(Classifier classifier) {
        TermRelationship.byClassifierId(classifier.id).list()
    }

    @Override
    List<TermRelationship> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, it.model.id)}
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
    List<TermRelationship> getAll(Collection<UUID> ids) {
        TermRelationship.getAll(ids).findAll()
    }

    @Override
    List<TermRelationship> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        TermRelationship.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<TermRelationship> findAllByMetadataNamespace(String namespace, Map pagination) {
        TermRelationship.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    TermRelationship findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        String[] split = pathIdentifier.split(/\./)
        if (split.size() != 3) throw new ApiBadRequestException('TRS01', "TermRelationship Path identifier is invalid [${pathIdentifier}]")

        if (termService.get(parentId)) {
            TermRelationship.byTermIdAndPathIdentifierFields(parentId, split[0], split[1], split[2]).get()
        } else {
            TermRelationship.byTerminologyIdAndPathIdentifierFields(parentId, split[0], split[1], split[2]).get()
        }
    }
}