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
package uk.ac.ox.softeng.maurodatamapper.core.similarity

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

abstract class SimilarityResult<K extends CatalogueItem> {

    K source

    // Use an ArrayList to keep the results in the correct order
    ArrayList<SimilarityPair<K>> results

    SimilarityResult(K source) {
        this.source = source
        results = new ArrayList<>()
    }

    void add(K target, Float f) {
        results.add(new SimilarityPair(target, f))
    }

    int size() {
        results.size()
    }

    def each(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair')
                 Closure closure) {
        results.each(closure)
    }

    SimilarityPair<K> first() {
        results.first()
    }
}
