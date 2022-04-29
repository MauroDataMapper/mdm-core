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
package uk.ac.ox.softeng.maurodatamapper.core.util.test

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria
import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

/**
 * @since 10/12/2019
 */
@Entity
class BasicModelItem implements ModelItem<BasicModelItem, BasicModel>, GormEntity<BasicModelItem> {

    UUID id
    BasicModel model
    BasicModelItem parentItem

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        parentItem nullable: true
    }

    static belongsTo = [BasicModel, BasicModelItem]

    static hasMany = [
        classifiers    : Classifier,
        childModelItems: BasicModelItem,
        metadata       : Metadata,
        annotations    : Annotation,
        semanticLinks  : SemanticLink,
        referenceFiles : ReferenceFile,
        rules          : Rule
    ]

    static mappedBy = [
        childModelItems: 'parentItem'
    ]

    static mapping = {
        breadcrumbTree fetch: 'join'
    }

    BasicModelItem() {
        id = UUID.randomUUID()
    }

    @Override
    String getDomainType() {
        BasicModelItem.simpleName
    }

    @Override
    String getPathPrefix() {
        'bmi'
    }

    @Override
    String getEditLabel() {
        null
    }

    def beforeValidate() {
        idx = 0
        if (!model) this.model = parentItem?.model
        beforeValidateModelItem()
    }


    CatalogueItem getParent() {
        parentItem ?: model
    }

    BasicModelItem addToChildModelItems(Map map) {
        addToChildModelItems new BasicModelItem(map)
    }

    BasicModelItem addToChildModelItems(BasicModelItem basicModelItem) {
        basicModelItem.parentItem = this
        basicModelItem.model = this.model
        addTo('childModelItems', basicModelItem)
    }

    Boolean hasChildren() {
        childModelItems == null ? false : !childModelItems.isEmpty()
    }

    Set<BasicModelItem> getAllModelItems() {
        (childModelItems ?: []) + childModelItems.collect {it.getAllModelItems()}.flatten()
    }

    static BasicModel findByIdJoinClassifiers(UUID id) {
        new DetachedCriteria<BasicModel>(BasicModel).idEq(id).join('classifiers').get()
    }

    ObjectDiff<BasicModelItem> diff(BasicModelItem obj, String context) {
       diff(obj, context, null, null)
    }

    ObjectDiff<BasicModelItem> diff(BasicModelItem obj, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(BasicModelItem, this, obj, lhsDiffCache, rhsDiffCache)
    }
}
