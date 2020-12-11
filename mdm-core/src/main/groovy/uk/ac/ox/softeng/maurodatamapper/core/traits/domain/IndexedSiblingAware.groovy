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

    void fullSortOfChildren(Collection<? extends ModelItem> children) {
        if (!children) return
        children.sort().eachWithIndex { ModelItem mi, int i ->
            if (mi.idx != i) mi.idx = i
        }
    }

    /**
     * Given a CatalogueItem which has been updated, re-index its siblings.
     *
     * @param ModelItem updated         The item which has been updated
     * @param Set <ModelItem>  siblings   The siblings of the updated item
     * @param int oldIndex                  The index of the updated item before it was updated     
     */
    @Deprecated
    void updateSiblingIndexes(ModelItem updated, Collection<ModelItem> siblings, Integer oldIndex) {
        updateSiblingIndexes(updated, siblings)
    }

    /**
     * Given a CatalogueItem which has been updated, re-index its siblings.
     *
     * @param ModelItem updated         The item which has been updated
     * @param Set <ModelItem>  siblings   The siblings of the updated item
     */
    void updateSiblingIndexes(ModelItem updated, Collection<ModelItem> siblings) {
        log.trace('Updating sibling indexes {}:{}, siblings size {}, currentIndex {}, indexChanged {}, original value {}',
                  updated.domainType,
                  updated.label,
                  siblings.size(),
                  updated.idx,
                  updated.hasChanged('idx'),
                  updated.getOriginalValue('idx'))
        //If the updated item does not currently have an index then add it to the end of the indexed list
        if (updated.idx == null) {
            log.trace('>> No idx')
            updated.idx = siblings ? siblings.size() - 1 : 0
            return
        }

        //If the update did not actually change the index then do nothing
        if (!updated.hasChanged('idx')) {
            log.trace('>> IDX hasnt changed')
            return
        }

        //If there are no siblings then they do not need to be reordered
        if (!siblings) {
            log.trace('>> No siblings')
            return
        }

        log.trace('>> Sorting and reordering idxes')

        sortAndReorderAllIndexes(updated, siblings)
    }

    void sortAndReorderAllIndexes(ModelItem updated, Collection<ModelItem> siblings) {
        int updatedIndex = updated.idx
        int maxIndex = siblings.size() - 1
        siblings.sort().eachWithIndex { ModelItem mi, int i ->
            //EV is the updated one, skipping any changes
            if (mi == updated) {
                // Make sure updated value is not ordered larger than the actual size of the collection
                if (mi.idx > maxIndex) {
                    mi.idx = maxIndex
                }
                return
            }

            // Make sure all values have trackChanges turned on
            if (!mi.isDirty()) mi.trackChanges()

            log.trace('Before >> MI {} has idx {} sorted to {}', mi.label, mi.idx, i)
            // Reorder the index which matches the one we just added
            if (i == updatedIndex) {
                if (i == maxIndex) {
                    // If at end of list then move the current value back one to ensure the updated value is at then end of the list
                    mi.idx = i - 1
                } else {
                    // Otherwise alphabetical sorting has placed the elements in the wrong order so shift the value by 1
                    mi.idx = i + 1
                }
            } else if (mi.idx != i) {
                // Sorting has got the order right so make sure the idx is set correctly
                mi.idx = i
            }
            // If the idx has changed and the mi has previously been saved then we need to save it into the session
            if (mi.hasChanged('idx') && mi.id) {
                mi.save(flush: false, validate: false)
            }
            log.trace('After >> EV {} has idx {} (Dirty: {})', mi.label, mi.idx, mi.isDirty())
        }
    }

}
