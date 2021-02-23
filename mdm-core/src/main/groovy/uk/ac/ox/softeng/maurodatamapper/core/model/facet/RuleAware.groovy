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
package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 19/11/2020
 */
@SelfType(GormEntity)
@GrailsCompileStatic
trait RuleAware {

    abstract Set<Rule> getRules()

    Rule findRuleByName(String name) {
        rules?.find {it.name == name}
    }

    def addToRules(Rule add) {
        Rule existing = findRuleByName(add.name)
        if (existing) {
            existing.description = add.description
            markDirty('rules', existing)
            this as CatalogueItem
        } else {
            add.setCatalogueItem(this as CatalogueItem)
            addTo('rules', add)
        }
    }

    def addToRules(Map args) {
        addToRules(new Rule(args))
    }

    def addToRules(String name, String description, User createdBy) {
        addToRules(name, description, createdBy.emailAddress)
    }

    def addToRules(String name, String description, String createdBy) {
        addToRules(new Rule(name: name, description: description, createdBy: createdBy))
    }

    def addToRules(String name, String description) {
        addToRules(name: name, description: description)
    }

    def removeFromRules(Rule rule) {
        throw new ApiInternalException('FR01', 'Do not use removeFrom to remove facet from domain')
    }
}