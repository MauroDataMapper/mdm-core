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


import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

import groovy.util.logging.Slf4j

/**
 * @since 24/09/2020
 */
@Slf4j
trait IndexedSiblingAware {

    /**
     * Given a CatalogueItem which has been updated, re-index its siblings.
     *
     * @param ModelItem updated         The item which has been updated
     * @param Set <ModelItem>  siblings   The siblings of the updated item
     * @param int oldIndex                  The index of the updated item before it was updated     
     */
    void updateSiblingIndexes(ModelItem updated, Collection<ModelItem> siblings, Integer oldIndex) {
        log.trace('Updating sibling indexes {}:{}, siblings size {}, oldIndex {}, currentIndex {}', updated.domainType, updated.label,
                  siblings.size(),
                  oldIndex,
                  updated.idx)
        //If the updated item does not currently have an index then add it to the end of the indexed list
        if (updated.idx == null) {
            if (siblings) {
                updated.idx = siblings.size() - 1
            } else {
                updated.idx = 0
            }
            return
        }

        //If the update did not actually change the index then do nothing
        if (updated.idx == oldIndex) {
            return
        }

        //If there are no siblings then they do not need to be reordered
        if (!siblings) {
            return
        }

        log.trace('>> Sorting and reordering idxes')

        //Imagine a list (vertical) of all the items. If the new value of the updated.idx is less than
        //the old value then updated is being moved up, in which case it pushes down the item that was
        //previously at position updated.idx
        boolean movingUp = updated.idx < oldIndex

        List<ModelItem> sorted = siblings.sort()
        sorted.eachWithIndex { ModelItem mi, int i ->
            if (mi == updated) {
                //do nothing
            } else {
                // Make sure all values have trackChanges turned on
                if (!mi.isDirty()) mi.trackChanges()

                if (mi.idx == updated.idx) {
                    if (movingUp) {
                        mi.idx += 1
                    } else {
                        mi.idx -= 1
                    }
                } else {
                    mi.idx = i
                }
            }
        }
    }

    void fullSortOfChildren(Collection<? extends ModelItem> children) {
        if (!children) return
        children.sort().eachWithIndex { ModelItem mi, int i ->
            if (mi.idx != i) mi.idx = i
        }
    }
}
