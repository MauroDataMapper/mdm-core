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
package uk.ac.ox.softeng.maurodatamapper.security.test

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
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
        breadcrumbTree = new BreadcrumbTree(this)
    }

    @Override
    String getDomainType() {
        BasicModel.simpleName
    }

    Set<BasicModelItem> getAllModelItems() {
        (modelItems ?: []) + modelItems.collect { it.getAllModelItems() }.flatten()
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
        breadcrumbTree = new BreadcrumbTree(this)
    }

    BasicModel addToModelItems(Map map) {
        addToModelItems new BasicModelItem(map)
    }

    BasicModel addToModelItems(BasicModelItem basicModelItem) {
        basicModelItem.model = this
        addTo('modelItems', basicModelItem)
    }

    ObjectDiff<BasicModel> diff(BasicModel obj, String context) {
        modelDiffBuilder(BasicModel, this, obj)
    }

    @Override
    String getPathPrefix() {
        'bm'
    }

    static BasicModel findByIdJoinClassifiers(UUID id) {
        new DetachedCriteria<BasicModel>(BasicModel).idEq(id).join('classifiers').get()
    }


}
