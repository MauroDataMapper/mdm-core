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

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Rule implements CatalogueItemAware, CreatorAware, Diffable<Rule> {

    UUID id

    String name
    String description

    static hasMany = [
        ruleRepresentations: RuleRepresentation
    ]    

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        name blank: false, nullable: false
        description blank: true, nullable: true
    }

    static mapping = {
        name type: 'text'
        description type: 'text'
        catalogueItemId index: 'rule_catalogue_item_idx'
        ruleRepresentations cascade: 'all-delete-orphan'
    }

    static search = {
        name index: 'yes'
        description index: 'yes'
    }

    static transients = ['catalogueItem']

    Rule() {
    }

    @Override
    String getDomainType() {
        Rule.simpleName
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
    ObjectDiff<Rule> diff(Rule obj) {
        ObjectDiff.builder(Rule)
            .leftHandSide(id.toString(), this)
            .rightHandSide(obj.id.toString(), obj)
            .appendString('name', this.name, obj.name)
            .appendString('description', this.description, obj.description)
    }

    @Override
    String getDiffIdentifier() {
        "${this.name}.${this.description}"
    }

    static DetachedCriteria<Rule> byCatalogueItemId(Serializable catalogueItemId) {
        new DetachedCriteria<Rule>(Rule).eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<Rule> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<Metadata> withFilter(DetachedCriteria<Rule> criteria, Map filters) {
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