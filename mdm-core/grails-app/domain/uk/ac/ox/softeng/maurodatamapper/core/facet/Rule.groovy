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

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Rule implements MultiFacetItemAware, Diffable<Rule> {

    UUID id

    String name
    String description

    static hasMany = [
        ruleRepresentations: RuleRepresentation
    ]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.multiFacetAwareItem && !obj.multiFacetAwareItem.ident()) return true
            ['default.null.message']
        }
        name blank: false, nullable: false
        description blank: true, nullable: true
    }

    static mapping = {
        name type: 'text'
        description type: 'text'
        multiFacetAwareItemId index: 'rule_catalogue_item_idx'
        ruleRepresentations cascade: 'all-delete-orphan'
    }

    static search = {
        name searchable: 'yes'
        description searchable: 'yes'
    }

    static transients = ['multiFacetAwareItem']

    Rule() {
    }

    @Override
    String getDomainType() {
        Rule.simpleName
    }

    @Override
    String getPathPrefix() {
        'ru'
    }

    @Override
    String getPathIdentifier() {
        name
    }

    @Override
    String toString() {
        "${getClass().getName()} : ${name}/${description} : ${id ?: '(unsaved)'}"
    }

    @Override
    String getEditLabel() {
        "Rule:${name}"
    }

    @Override
    ObjectDiff<Rule> diff(Rule obj, String context) {
        DiffBuilder.objectDiff(Rule)
            .leftHandSide(id.toString(), this)
            .rightHandSide(obj.id.toString(), obj)
            .appendString('name', this.name, obj.name)
            .appendString('description', this.description, obj.description)
            .appendList(RuleRepresentation, 'ruleRepresentations', this.ruleRepresentations, obj.ruleRepresentations)
    }

    static DetachedCriteria<Rule> by() {
        new DetachedCriteria<Rule>(Rule)
    }

    static DetachedCriteria<Rule> byMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        new DetachedCriteria<Rule>(Rule).eq('multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
    }

    static DetachedCriteria<Rule> byMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        new DetachedCriteria<Rule>(Rule).inList('multiFacetAwareItemId', multiFacetAwareItemIds)
    }

    static DetachedCriteria<Rule> byMultiFacetAwareItemIdAndId(Serializable multiFacetAwareItemId, Serializable resourceId) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<Rule> withFilter(DetachedCriteria<Rule> criteria, Map filters) {
        if (filters.name) criteria = criteria.ilike('name', "%${filters.name}%")
        if (filters.description) criteria = criteria.ilike('description', "%${filters.description}%")
        if (filters.language) criteria = criteria.where {
            ruleRepresentations {
                ilike('language', "%${filters.language}%")
            }
        }
        criteria
    }

    static DetachedCriteria<Rule> byName(String name) {
        new DetachedCriteria<Rule>(Rule).eq('name', name)
    }
}