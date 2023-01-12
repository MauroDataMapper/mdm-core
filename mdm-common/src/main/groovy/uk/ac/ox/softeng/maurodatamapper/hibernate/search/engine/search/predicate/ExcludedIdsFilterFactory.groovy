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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.search.engine.search.predicate.SearchPredicate
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory

/**
 * @since 27/04/2018
 */
@Slf4j
@CompileStatic
class ExcludedIdsFilterFactory extends FilterFactory {

    static PredicateFinalStep createFilter(SearchPredicateFactory factory, Collection<UUID> excludedIds) {

        BooleanPredicateClausesStep step = startFilter(factory)

        excludedIds.each {id ->
            step = step.mustNot(factory.id().matching(id))
        }

        step
    }

    static SearchPredicate createFilterPredicate(SearchPredicateFactory factory, Collection<UUID> excludedIds) {
        createFilter(factory, excludedIds).toPredicate()
    }
}
