/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode

import groovy.transform.CompileStatic
import org.hibernate.search.engine.search.predicate.SearchPredicate
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory

/**
 * @since 27/04/2018
 * Secures a hibernate search so that the data is filtered so only data which includes an allowed id or its path contains one of the supplied path
 * nodes.
 * Path is tokenised on the | character so if you have a resource with path "mo:model|mi:model item 1|mi:model item 2" and the allowedPathNodes
 * contains "mo:model" then the
 * resource will be allowed.
 */
@CompileStatic
class IdPathSecureFilterFactory extends FilterFactory {

    static PredicateFinalStep createFilter(SearchPredicateFactory factory, Collection<UUID> allowedIds, Collection<Path> allowedPaths) {

        BooleanPredicateClausesStep step = startFilter(factory)

        allowedIds.each {id ->
            step = step.should(factory.id().matching(id))
        }
        allowedPaths?.each {p ->
            step = step.should(factory.phrase().field('path').matching(p.toString()))
        }
        step
    }

    static SearchPredicate createFilterPredicate(SearchPredicateFactory factory, Collection<UUID> allowedIds, Collection<Path> allowedPaths) {
        createFilter(factory, allowedIds, allowedPaths).toPredicate()
    }
}
