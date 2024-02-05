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

import groovy.transform.CompileStatic
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory

/**
 * @since 25/10/2021
 */
@CompileStatic
class FilterFactory {

    static BooleanPredicateClausesStep startFilter(SearchPredicateFactory factory) {
        factory.bool().constantScore()
    }

    static BooleanPredicateClausesStep mustNot(SearchPredicateFactory factory, PredicateFinalStep filter) {
        startFilter(factory).mustNot(filter)
    }
}
