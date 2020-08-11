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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Annotation implements CatalogueItemAware, PathAware, InformationAware, CreatorAware, Diffable<Annotation> {

    UUID id
    Annotation parentAnnotation
    List<Annotation> childAnnotations

    static belongsTo = [Annotation]

    static hasMany = [
        childAnnotations: Annotation
    ]

    static mappedBy = [
        childAnnotations: 'parentAnnotation'
    ]

    static transients = ['catalogueItem']

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        description validator: {String val, Annotation obj ->
            obj.parentAnnotation && !val ? ['annotation.description.required.message',] : true
        }
    }

    static mapping = {
        parentAnnotation index: 'annotation_parent_annotation_idx'
        childAnnotations sort: 'dateCreated', order: 'asc', cascade: 'all-delete-orphan'
    }

    Annotation() {
    }

    @Override
    String getDomainType() {
        Annotation.simpleName
    }

    @Override
    Annotation getPathParent() {
        parentAnnotation
    }

    def beforeValidate() {
        buildPath()
        childAnnotations.eachWithIndex {ann, i ->
            if (!ann.label) ann.label = "$label [$i]"
            if (catalogueItem) {
                ann.setCatalogueItem(this.catalogueItem)
            } else {
                ann.catalogueItemId = this.catalogueItemId
                ann.catalogueItemDomainType = this.catalogueItemDomainType
            }
            ann.beforeValidate()
        }
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    String getEditLabel() {
        "Annotation:${label}"
    }

    @Override
    ObjectDiff<Annotation> diff(Annotation otherAnnotation) {
        ObjectDiff.builder(Annotation)
            .leftHandSide(this.id.toString(), this)
            .rightHandSide(otherAnnotation.id.toString(), otherAnnotation)
            .appendString('description', this.description, otherAnnotation.description)
            .appendList(Annotation, 'childAnnotations', this.childAnnotations, otherAnnotation.childAnnotations)

    }

    @Override
    String getDiffIdentifier() {
        this.label
    }

    static DetachedCriteria<Annotation> byCatalogueItemId(Serializable catalogueItemId) {
        new DetachedCriteria<Annotation>(Annotation).eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<Annotation> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<Annotation> whereRootAnnotationOfCatalogueItemId(Serializable catalogueItemId) {
        byCatalogueItemId(catalogueItemId).isNull('parentAnnotation')
    }

    static DetachedCriteria<Annotation> byParentAnnotationId(Serializable parentAnnotationId) {
        new DetachedCriteria<Annotation>(Annotation).eq('parentAnnotation.id', Utils.toUuid(parentAnnotationId))
    }
}