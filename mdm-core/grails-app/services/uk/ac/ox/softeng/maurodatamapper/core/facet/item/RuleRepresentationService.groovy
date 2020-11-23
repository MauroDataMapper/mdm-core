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
package uk.ac.ox.softeng.maurodatamapper.core.facet.item


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import javax.transaction.Transactional

@Slf4j
@Transactional
class RuleRepresentationService {

    RuleRepresentation get(Serializable id) {
        RuleRepresentation.get(id)
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

    RuleRepresentation findByRuleIdAndId(UUID ruleId, UUID ruleRepresentationId) {
        RuleRepresentation.byRuleIdAndId(ruleId, ruleRepresentationId).get()
    }

    List<RuleRepresentation> findAllByRuleId(UUID ruleId, Map pagination = [:]) {
        RuleRepresentation.withFilter(RuleRepresentation.byRuleId(ruleId), pagination).list(pagination)
    }

    RuleRepresentation save(RuleRepresentation ruleRepresentation) {
        save(flush: true, validate: true, ruleRepresentation)
    }  
}