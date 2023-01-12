/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class RuleRepresentationService implements MdmDomainService<RuleRepresentation> {

    RuleRepresentation get(Serializable id) {
        RuleRepresentation.get(id)
    }

    @Override
    List<RuleRepresentation> getAll(Collection<UUID> resourceIds) {
        RuleRepresentation.getAll(resourceIds)
    }

    List<RuleRepresentation> list(Map args) {
        RuleRepresentation.list(args)
    }

    Long count() {
        RuleRepresentation.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(RuleRepresentation ruleRepresentation, boolean flush = false) {
        if (!ruleRepresentation) return
        ruleRepresentation.delete(flush: flush)
    }

    void deleteAllByRules(List<Rule> rules) {
        RuleRepresentation.byRules(rules).deleteAll()
    }

    RuleRepresentation findByRuleIdAndId(UUID ruleId, UUID ruleRepresentationId) {
        RuleRepresentation.byRuleIdAndId(ruleId, ruleRepresentationId).get()
    }

    List<RuleRepresentation> findAllByRuleId(UUID ruleId, Map pagination = [:]) {
        RuleRepresentation.withFilter(RuleRepresentation.byRuleId(ruleId), pagination).list(pagination)
    }

    RuleRepresentation save(RuleRepresentation ruleRepresentation) {
        save(flush: true, validate: true, ruleRepresentation)
    }

    @Override
    RuleRepresentation findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        RuleRepresentation.byRuleId(parentId).eq('language', pathIdentifier).get()
    }
}