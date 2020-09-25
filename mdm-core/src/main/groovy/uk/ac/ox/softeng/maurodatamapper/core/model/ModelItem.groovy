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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.Breadcrumb
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import groovy.transform.SelfType
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.annotations.Field
import org.springframework.core.Ordered

/**
 * Base class for all items which are contained inside a model. These items are securable by the model they are contained in.
 * D is always the class extending ModelItem, however due to compilation issues we have to use Diffable as the constraint
 * @since 04/11/2019
 */
@SelfType(GormEntity)
@Slf4j
trait ModelItem<D extends Diffable, T extends Model> extends CatalogueItem<D> implements PathAware, Ordered, Comparable<D> {

    abstract T getModel()

    abstract Boolean hasChildren()

    Integer idx

    int getOrder() {
        idx != null ? idx : Ordered.LOWEST_PRECEDENCE
    }

    void setIndex(int index) {
        log.debug("ModelItem.setIndex ${index}")
        idx = index
        markDirty('idx')
        if (ident()) updateIndices(index)
    }

    void updateIndices(int index) {
        log.debug("ModelItem.updateIndices ${index}")
        CatalogueItem parent = getParent()
        log.debug("parent ${parent.toString()}")
        if (parent) {
            log.debug("I have a parent")
            parent.updateChildIndexes(this)
        }
    }

    CatalogueItem getParent() {
        //no-op
    }

    List<Breadcrumb> getBreadcrumbs() {
        breadcrumbTree.getBreadcrumbs()
    }

    @Override
    String buildPath() {
        String path = super.buildPath()
        if (!breadcrumbTree) {
            breadcrumbTree = new BreadcrumbTree(this)
        } else {
            if (!breadcrumbTree.matchesPath(path)) {
                breadcrumbTree.update(this)
            }
        }
        path
    }

    def beforeValidateModelItem() {
        if (idx == null) idx = Ordered.LOWEST_PRECEDENCE
        buildPath()
        beforeValidateCatalogueItem()
    }

    @Field
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
