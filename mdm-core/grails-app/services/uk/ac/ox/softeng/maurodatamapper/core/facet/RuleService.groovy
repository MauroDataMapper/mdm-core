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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentationService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import javax.transaction.Transactional

@Slf4j
@Transactional
class RuleService implements CatalogueItemAwareService<Rule> {

    RuleRepresentationService ruleRepresentationService

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    Rule get(Serializable id) {
        Rule.get(id)
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
        CatalogueItemService service = findCatalogueItemService(rule.catalogueItemDomainType)
        service.removeRuleFromCatalogueItem(rule.catalogueItemId, rule)
        rule.delete(flush: flush)
    }

    @Override
    void saveCatalogueItem(Rule facet) {
        if (!facet) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(facet.catalogueItemDomainType)
        catalogueItemService.save(facet.catalogueItem)
    }

    @Override
    void addFacetToDomain(Rule facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
        domain.addToRules(facet)
    }

    boolean validate(Rule rule) {
        boolean valid = rule.validate()
        if (!valid) return false

        CatalogueItem catalogueItem = rule.catalogueItem ?: findCatalogueItemByDomainTypeAndId(rule.catalogueItemDomainType,
                                                                                               rule.catalogueItemId)

        //Ensure name is unique within catalogueItem
        if (catalogueItem.rules.any { r ->
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
    Rule findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        Rule.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<Rule> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination = [:]) {
        Rule.withFilter(Rule.byCatalogueItemId(catalogueItemId), pagination).list(pagination)
    }

    @Override
    void performDeletion(List<UUID> batch) {
        long start = System.currentTimeMillis()
        List<Rule> rules = Rule.byCatalogueItemIdInList(batch).list()
        if (rules) {
            ruleRepresentationService.deleteAllByRules(rules)
            getBaseDeleteCriteria().inList('catalogueItemId', batch).deleteAll()
        }
        log.trace('{} removed took {}', Rule.simpleName, Utils.timeTaken(start))
    }

    @Override
    DetachedCriteria<Rule> getBaseDeleteCriteria() {
        Rule.by()
    }

    //This works around the fact the RuleRepresentation is not CatalogueItemAware
    RuleRepresentation addCreatedEditToCatalogueItemOfRule(User creator, RuleRepresentation domain, String catalogueItemDomainType,
                                                           UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    //This works around the fact the RuleRepresentation is not CatalogueItemAware
    RuleRepresentation addUpdatedEditToCatalogueItemOfRule(User editor, RuleRepresentation domain, String catalogueItemDomainType,
                                                           UUID catalogueItemId, List<String> dirtyPropertyNames) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    //This works around the fact the RuleRepresentation is not CatalogueItemAware
    RuleRepresentation addDeletedEditToCatalogueItemOfRule(User deleter, RuleRepresentation domain, String catalogueItemDomainType,
                                                           UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }
}