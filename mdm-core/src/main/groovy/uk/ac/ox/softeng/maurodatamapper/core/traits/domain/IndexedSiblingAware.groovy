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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

/**
 * @since 24/09/2020
 */
trait IndexedSiblingAware {

    /**
     * Given a CatalogueItem which has been updated, re-index its siblings.
     *
     * @param CatalogueItem updated         The item which has been updated
     * @param Set<CatalogueItem> siblings   The siblings of the updated item
     */
    void updateSiblingIndexes(CatalogueItem updated, Set<CatalogueItem> siblings) {
        List<CatalogueItem> sorted = siblings.sort()
        int updatedIndex = updated.getOrder()
        int maxIndex = sorted.size() - 1
        sorted.eachWithIndex {CatalogueItem mi, int i ->
            //Don't touch anything whose index is Integer.MAX_VALUE
            if (mi.getOrder() < Integer.MAX_VALUE) {
                //mi is the updated one, skipping any changes
                if (mi == updated) {
                    // Make sure updated value is not ordered larger than the actual size of the collection
                    if (mi.getOrder() > maxIndex) {
                        mi.idx = maxIndex
                    }
                    return
                }

                // Make sure all values have trackChanges turned on
                if (!mi.isDirty()) mi.trackChanges()

                // Reorder the index which matches the one we just added
                if (i == updatedIndex) {
                    if (i == maxIndex) {
                        // If at end of list then move the current value back one to ensure the updated value is at then end of the list
                        mi.idx = i - 1
                    } else {
                        // Otherwise alphabetical sorting has placed the elements in the wrong order so shift the value by 1
                        mi.idx = i + 1
                    }
                } else if (mi.getOrder() != i) {
                    // Sorting has got the order right so make sure the idx is set correctly
                    mi.idx = i
                }
            }
        }
    }

}
