/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.similarity

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.hibernate.search.engine.search.query.SearchResult

import java.time.Duration

abstract class SimilarityResult<K extends CatalogueItem> {

    K source

    SearchResult<SimilarityPair<K>> searchResult

    SimilarityResult(K source, SearchResult<SimilarityPair<K>> searchResult) {
        this.source = source
        this.searchResult = searchResult
    }

    List<SimilarityPair<K>> getAllResults() {
        searchResult.hits()
    }

    List<SimilarityPair<K>> getSimilarResults() {
        searchResult.hits().findAll {it.score > 0}
    }

    int hits() {
        searchResult.total().hitCount()
    }

    int totalSimilar() {
        similarResults.size()
    }

    Duration took() {
        searchResult.took()
    }

    def each(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair') Closure closure) {
        getAllResults().each(closure)
    }

    def find(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair') Closure closure) {
        getAllResults().find(closure)
    }

    def any(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair') Closure closure) {
        getAllResults().any(closure)
    }


    SimilarityPair<K> first() {
        getSimilarResults().first()
    }

    String toString() {
        "Similarity Result for ${source.label}\n  ${allResults.join('\n  ')}"
    }
}
