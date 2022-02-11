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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

import groovy.util.logging.Slf4j
import org.hibernate.TransientObjectException

/**
 * @since 24/09/2020
 */
@Slf4j
trait IndexedSiblingAware {

    void fullSortOfChildren(Collection<? extends ModelItem> children) {
        if (!children) return
        //        Map<String, Collection<ModelItem>> idxExists = children.groupBy {(it.idx != null && it.idx != Integer.MAX_VALUE).toString()}
        //        if(!idxExists.false) return
        //        log.debug('Full sort of {} children. {} have idx; {} do not', children.size(), idxExists.true?.size()?:0, idxExists.false?.size()?:0)
        //        Integer offset = idxExists.true?.max {it.idx}?.idx?:0
        //        idxExists.false.sort().eachWithIndex { ModelItem mi, int i ->
        //            if (mi.idx != i+offset) mi.idx = i+offset
        //        }
        if (children.size() == 1) {
            children.first().idx = 0
        } else {
            log.trace('Full sort of {} children', children.size())
            children.sort().eachWithIndex {ModelItem mi, int i ->
                if (mi.idx != i) mi.idx = i
            }
        }
    }

    /**
     * Given a CatalogueItem which has been updated, re-index its siblings.
     *
     * @param ModelItem updated         The item which has been updated
     * @param Set <ModelItem>  siblings   The siblings of the updated item
     */
    void updateSiblingIndexes(ModelItem updated, Collection<ModelItem> siblings) {
        log.trace('Updating sibling indexes {}:{}:{}, siblings size {}, currentIndex {}, original value {}',
                  updated.domainType,
                  updated.label,
                  updated.id,
                  siblings.size(),
                  updated.idx,
                  updated.getOriginalValue('idx'))
        //If the updated item does not currently have an index then add it to the end of the indexed list
        if (updated.idx == null) {
            log.trace('>> No idx')
            updated.idx = siblings ? siblings.size() - 1 : 0
            return
        }

        //If there are no siblings then they do not need to be reordered
        if (!siblings) {
            log.trace('>> No siblings')
            return
        }

        // MI has previously been saved which means we want to be optimised about what we do
        // If not saved then "hasChanged" will always return true, if saved then "hasChanged" may not work.
        // If not saved then "isDirty" will return false, if saved then isDirty will always work
        // If the update did not actually change the index then do nothing
        if (updated.id && !hasIdxChanged(updated)) {
            log.trace('>> idx hasnt changed')
            return
        }

        // If never saved before or has been saved and the idx has changed then we need to make sure all the siblings are ordered
        log.trace('>> Sorting and reordering idxes')
        sortAndReorderAllIndexes(updated, siblings)
    }

    void sortAndReorderAllIndexes(ModelItem updated, Collection<ModelItem> siblings) {
        int updatedIndex = updated.idx
        int maxIndex = siblings.size() - 1
        siblings.sort().eachWithIndex {ModelItem mi, int i ->
            //EV is the updated one, skipping any changes
            if (mi == updated) {
                // Make sure updated value is not ordered larger than the actual size of the collection
                if (mi.idx > maxIndex) {
                    mi.idx = maxIndex
                }
                return
            }

            // Make sure all values have trackChanges turned on if theres no id
            // If id doesnt exist then it will be dirty
            if (mi.id && !mi.isDirty()) mi.trackChanges()

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

    boolean hasIdxChanged(ModelItem modelItem){
        try{
            return modelItem.isDirty('idx')
        }catch(TransientObjectException ignored){
            return modelItem.hasChanged('idx')
        }
    }

}
