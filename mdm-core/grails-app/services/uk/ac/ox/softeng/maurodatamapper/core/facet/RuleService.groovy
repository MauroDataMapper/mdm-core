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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService

import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import javax.transaction.Transactional

@Slf4j
@Transactional
class RuleService implements CatalogueItemAwareService<Rule> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    SessionFactory sessionFactory
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

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
        CatalogueItemService service = catalogueItemServices.find { it.handles(rule.catalogueItemDomainType) }
        if (!service) throw new ApiBadRequestException('RS01', 'Rule removal for catalogue item with no supporting service')
        service.removeRuleFromCatalogueItem(rule.catalogueItemId, rule)
        rule.delete(flush: flush)
    }

    //Ensure a row is inserted into the _facet table
    void addRuleToCatalogueItem(Rule rule, CatalogueItem catalogueItem) {
        catalogueItem.addToRules(rule)
    }

    boolean validate(Rule rule) {
        boolean valid = rule.validate()
        if (!valid) return false

        CatalogueItem catalogueItem = rule.catalogueItem ?: findCatalogueItemByDomainTypeAndId(rule.catalogueItemDomainType,
                                                                                                   rule.catalogueItemId)

        //Ensure name is unique within catalogueItem
        if (catalogueItem.rules.any {r ->
            log.debug("${r} ${rule}")
            r != rule && r.name == rule.name}) {
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
}