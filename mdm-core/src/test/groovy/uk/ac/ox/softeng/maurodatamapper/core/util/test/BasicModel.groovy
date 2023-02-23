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
package uk.ac.ox.softeng.maurodatamapper.core.util.test

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria
import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

/**
 * @since 10/12/2019
 */
@Entity
class BasicModel implements Model<BasicModel>, GormEntity<BasicModel> {

    UUID id

    static hasMany = [
        classifiers   : Classifier,
        modelItems    : BasicModelItem,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        versionLinks  : VersionLink,
        referenceFiles: ReferenceFile,
        rules         : Rule
    ]

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
    }

    BasicModel() {
        initialiseVersioning()
        modelType = BasicModel.simpleName
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
        breadcrumbTree = new BreadcrumbTree(this)
    }

    @Override
    String getDomainType() {
        BasicModel.simpleName
    }

    @Override
    String getPathPrefix() {
        'bm'
    }

    Set<BasicModelItem> getAllModelItems() {
        (modelItems ?: []) + modelItems.collect {it.getAllModelItems()}.flatten()
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
    }

    def beforeInsert() {
        id = UUID.randomUUID()
        breadcrumbTree.domainId = id
        breadcrumbTree.save()
    }

    BasicModel addToModelItems(Map map) {
        addToModelItems new BasicModelItem(map)
    }

    BasicModel addToModelItems(BasicModelItem basicModelItem) {
        basicModelItem.model = this
        addTo('modelItems', basicModelItem)
    }

    ObjectDiff<BasicModel> diff(BasicModel obj, String context) {
        diff(obj, context, null, null)
    }

    ObjectDiff<BasicModel> diff(BasicModel obj, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.modelDiffBuilder(BasicModel, this, obj, lhsDiffCache, rhsDiffCache)
    }

    static BasicModel findByIdJoinClassifiers(UUID id) {
        new DetachedCriteria<BasicModel>(BasicModel).idEq(id).join('classifiers').get()
    }


}
