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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentationService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class RuleService implements MultiFacetItemAwareService<Rule> {

    RuleRepresentationService ruleRepresentationService

    Rule get(Serializable id) {
        Rule.get(id)
    }

    @Override
    List<Rule> getAll(Collection<UUID> resourceIds) {
        Rule.getAll(resourceIds)
    }

    List<Rule> list(Map args) {
        Rule.list(args)
    }

    Long count() {
        Rule.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(Rule rule, boolean flush = false) {
        if (!rule) return
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(rule.multiFacetAwareItemDomainType)
        service.removeRuleFromMultiFacetAware(rule.multiFacetAwareItemId, rule)
        rule.delete(flush: flush)
    }

    @Override
    Rule copy(Rule facetToCopy, MultiFacetAware multiFacetAwareItemToCopyInto) {
        Rule copy = new Rule(name: facetToCopy.name, description: facetToCopy.description, createdBy: facetToCopy.createdBy)
        facetToCopy.ruleRepresentations.each {rr ->
            copy.addToRuleRepresentations(language: rr.language, representation: rr.representation, createdBy: rr.createdBy)
        }
        multiFacetAwareItemToCopyInto.addToRules(copy)
        copy
    }

    @Override
    void saveMultiFacetAwareItem(Rule facet) {
        if (!facet) return
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(facet.multiFacetAwareItemDomainType)
        service.save(facet.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(Rule facet, String domainType, UUID domainId) {
        if (!facet) return
        MultiFacetAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId)
        facet.multiFacetAwareItem = domain
        domain.addToRules(facet)
    }

    boolean validate(Rule rule) {
        boolean valid = rule.validate()
        if (!valid) return false

        MultiFacetAware multiFacetAwareItem = rule.multiFacetAwareItem ?: findMultiFacetAwareItemByDomainTypeAndId(rule.multiFacetAwareItemDomainType,
                                                                                                                   rule.multiFacetAwareItemId)

        //Ensure name is unique within multiFacetAwareItem
        if (multiFacetAwareItem.rules.any {r ->
            log.debug("${r} ${rule}")
            r != rule && r.name == rule.name
        }) {
            rule.errors.rejectValue('name', 'default.not.unique.message', ['name', Rule, rule.name].toArray(),
                                    'Property [{0}] of class [{1}] with value [{2}] must be unique')
            return false
        }
        true
    }

    @Override
    Rule findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        Rule.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<Rule> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination = [:]) {
        Rule.withFilter(Rule.byMultiFacetAwareItemId(multiFacetAwareItemId), pagination).list(pagination)
    }

    @Override
    List<Rule> findAllByMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        Rule.byMultiFacetAwareItemIdInList(multiFacetAwareItemIds).list()
    }

    @Override
    void performDeletion(List<UUID> batch) {
        long start = System.currentTimeMillis()
        List<Rule> rules = Rule.byMultiFacetAwareItemIdInList(batch).list()
        if (rules) {
            ruleRepresentationService.deleteAllByRules(rules)
            getBaseDeleteCriteria().inList('multiFacetAwareItemId', batch).deleteAll()
        }
        log.trace('{} removed took {}', Rule.simpleName, Utils.timeTaken(start))
    }

    @Override
    DetachedCriteria<Rule> getBaseDeleteCriteria() {
        Rule.by()
    }

    //This works around the fact the RuleRepresentation is not MultiFacetAwareItemAware
    RuleRepresentation addCreatedEditToMultiFacetAwareItemOfRule(User creator, RuleRepresentation domain, String multiFacetAwareItemDomainType,
                                                                 UUID multiFacetAwareItemId) {
        EditHistoryAware multiFacetAwareItem =
            findMultiFacetAwareItemByDomainTypeAndId(multiFacetAwareItemDomainType, multiFacetAwareItemId) as EditHistoryAware
        multiFacetAwareItem.addToEditsTransactionally EditTitle.CREATE,creator, "[$domain.editLabel] added to component [${multiFacetAwareItem.editLabel}]"
        domain
    }

    //This works around the fact the RuleRepresentation is not MultiFacetAwareItemAware
    RuleRepresentation addUpdatedEditToMultiFacetAwareItemOfRule(User editor, RuleRepresentation domain, String multiFacetAwareItemDomainType,
                                                                 UUID multiFacetAwareItemId, List<String> dirtyPropertyNames) {
        EditHistoryAware multiFacetAwareItem =
            findMultiFacetAwareItemByDomainTypeAndId(multiFacetAwareItemDomainType, multiFacetAwareItemId) as EditHistoryAware
        multiFacetAwareItem.addToEditsTransactionally EditTitle.UPDATE,editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    //This works around the fact the RuleRepresentation is not MultiFacetAwareItemAware
    RuleRepresentation addDeletedEditToMultiFacetAwareItemOfRule(User deleter, RuleRepresentation domain, String multiFacetAwareItemDomainType,
                                                                 UUID multiFacetAwareItemId) {
        EditHistoryAware multiFacetAwareItem =
            findMultiFacetAwareItemByDomainTypeAndId(multiFacetAwareItemDomainType, multiFacetAwareItemId) as EditHistoryAware
        multiFacetAwareItem.addToEditsTransactionally(EditTitle.DELETE, deleter, "[$domain.editLabel] removed from component " +
                                                                                 "[${multiFacetAwareItem.editLabel}]")
        domain
    }

    @Override
    Rule findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        Rule.byMultiFacetAwareItemId(parentId).eq('name', pathIdentifier).get()
    }
}