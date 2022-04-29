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
package uk.ac.ox.softeng.maurodatamapper.core.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class RuleRepresentation implements MdmDomain, Diffable<RuleRepresentation>, EditHistoryAware {

    UUID id

    String language
    String representation

    static belongsTo = [rule: Rule]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        rule nullable: false
        language blank: false, nullable: false
        representation blank: false, nullable: false
    }

    static mapping = {
        language type: 'text'
        representation type: 'text'
        rule index: 'rule_representation_rule_idx'
    }

    static search = {
        language searchable: 'yes'
        representation searchable: 'yes'
    }

    RuleRepresentation() {
    }

    /**
     * Force language to be trimmed and lower case so that e.g. 'SQL' and ' sql' are treated as the same.
     */
    void setLanguage(String language) {
        this.language = language?.trim()?.toLowerCase()
    }

    @Override
    String getDomainType() {
        RuleRepresentation.simpleName
    }

    @Override
    String getPathPrefix() {
        'rr'
    }

    @Override
    String getPathIdentifier() {
        language
    }

    @Override
    String toString() {
        "${getClass().getName()} : ${language}/${representation} : ${id ?: '(unsaved)'}"
    }

    @Override
    String getEditLabel() {
        "RuleRepresentation:${language} on Rule ${rule.getEditLabel()}"
    }

    @Override
    ObjectDiff<RuleRepresentation> diff(RuleRepresentation that, String context) {
        diff(that, context, null,null)
    }

    @Override
    ObjectDiff<RuleRepresentation> diff(RuleRepresentation that, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.objectDiff(RuleRepresentation)
            .leftHandSide(id.toString(), this)
            .rightHandSide(that.id.toString(), that)
            .withLeftHandSideCache(lhsDiffCache)
            .withRightHandSideCache(rhsDiffCache)
            .appendString('language', this.language, that.language)
    }

    static DetachedCriteria<RuleRepresentation> by() {
        new DetachedCriteria<RuleRepresentation>(RuleRepresentation)
    }

    static DetachedCriteria<RuleRepresentation> byRuleId(Serializable ruleId) {
        by().eq('rule.id', Utils.toUuid(ruleId))
    }

    static DetachedCriteria<RuleRepresentation> byRules(List<Rule> rules) {
        by().inList('rule', rules)
    }

    static DetachedCriteria<RuleRepresentation> byRuleIdAndId(Serializable ruleId, Serializable resourceId) {
        byRuleId(ruleId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<RuleRepresentation> withFilter(DetachedCriteria<RuleRepresentation> criteria, Map filters) {
        if (filters.language) criteria = criteria.ilike('language', "%${filters.language}%")
        if (filters.representation) criteria = criteria.ilike('representation', "%${filters.representation}%")
        criteria
    }
}