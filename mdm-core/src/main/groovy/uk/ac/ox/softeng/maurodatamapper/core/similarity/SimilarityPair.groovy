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
package uk.ac.ox.softeng.maurodatamapper.core.similarity

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

/**
 * @since 07/04/2020
 */
class SimilarityPair<K extends CatalogueItem> {

    K item
    Float score

    SimilarityPair(K item, Float score) {
        this.item = item
        this.score = score
    }

    String toString() {
        score > 0 ? "Similarity to ${item.label} with score ${score}" : "No similarity to ${item.label}"
    }

    static SimilarityPair create(CatalogueItem item, Float score) {
        new SimilarityPair(item, score)
    }
}
