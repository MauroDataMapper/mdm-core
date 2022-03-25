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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.Breadcrumb
import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.transform.SelfType
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.core.Ordered

/**
 * Base class for all items which are contained inside a model. These items are securable by the model they are contained in.
 * D is always the class extending ModelItem, however due to compilation issues we have to use Diffable as the constraint
 * @since 04/11/2019
 */
@SelfType(GormEntity)
@Slf4j
trait ModelItem<D extends Diffable, T extends Model> extends CatalogueItem<D> implements Ordered, Comparable<D> {

    abstract T getModel()

    abstract Boolean hasChildren()

    abstract CatalogueItem getParent()

    Integer idx

    int getOrder() {
        idx != null ? idx : Ordered.LOWEST_PRECEDENCE
    }

    /**
     * On setting the index, update the indices of siblings.
     */
    void setIndex(Integer index) {
        Integer oldIndex = idx
        idx = index
        markDirty('idx')
        updateIndices(oldIndex)
    }

    void updateIndices(Integer oldIndex) {
        CatalogueItem indexedWithin = getIndexedWithin()
        if (indexedWithin) {
            indexedWithin.updateChildIndexes(this, oldIndex)
        } else if (idx == null) {
            // If idx is not set and there's no parent object then make sure its set
            idx = Ordered.LOWEST_PRECEDENCE
        }
    }

    CatalogueItem getIndexedWithin() {
        //no-op
        null
    }

    List<Breadcrumb> getBreadcrumbs() {
        breadcrumbTree.getBreadcrumbs()
    }

    @Override
    Path buildPath() {
        // We only want to call the getpath method once
        Path parentPath = parent?.getPath()
        parentPath ? Path.from(parentPath, pathPrefix, pathIdentifier) : null
    }

    def beforeValidateModelItem() {
        //Update indices.
        //If index is null and this is a thing whose siblings are ordered, add this to the end of the list.
        //If this is a thing which is not ordered, then no action will be taken.
        updateIndices(idx)
        beforeValidateCatalogueItem()
    }

    String getModelType() {
        model.modelType
    }

    @Override
    int compareTo(D that) {
        if (!(that instanceof ModelItem)) throw new ApiInternalException('MI01', 'Cannot compare non-ModelItem')
        int res = this.order <=> ((ModelItem) that).order
        res == 0 ? this.label <=> ((ModelItem) that).label : res
    }
}
