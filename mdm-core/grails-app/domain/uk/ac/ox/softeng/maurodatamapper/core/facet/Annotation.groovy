/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.PathAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Annotation implements MultiFacetItemAware, PathAware, InformationAware, Diffable<Annotation> {

    UUID id
    Annotation parentAnnotation
    List<Annotation> childAnnotations
    User user

    static belongsTo = [Annotation]

    static hasMany = [
        childAnnotations: Annotation
    ]

    static mappedBy = [
        childAnnotations: 'parentAnnotation'
    ]

    static transients = ['multiFacetAwareItem', 'user']

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.multiFacetAwareItem && !obj.multiFacetAwareItem.ident()) return true
            ['default.null.message']
        }
        description validator: {String val, Annotation obj ->
            obj.parentAnnotation && !val ? ['annotation.description.required.message',] : true
        }
        user nullable: true
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
    String getPathPrefix() {
        'ann'
    }

    @Override
    String getPathIdentifier() {
        label
    }

    @Override
    Annotation getPathParent() {
        parentAnnotation
    }

    def beforeValidate() {
        buildPath()
        childAnnotations.eachWithIndex { ann, i ->
            if (!ann.label) ann.label = "$label [$i]"
            if (multiFacetAwareItem) {
                ann.setMultiFacetAwareItem(this.multiFacetAwareItem)
            } else {
                ann.multiFacetAwareItemId = this.multiFacetAwareItemId
                ann.multiFacetAwareItemDomainType = this.multiFacetAwareItemDomainType
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

    void setCreatedByUser(Map map) {
        // ignore, this action is just to protect against anyone submitting the createdByUser map
    }

    @Override
    ObjectDiff<Annotation> diff(Annotation otherAnnotation, String context) {
        DiffBuilder.objectDiff(Annotation)
            .leftHandSide(this.id.toString(), this)
            .rightHandSide(otherAnnotation.id.toString(), otherAnnotation)
            .appendString('description', this.description, otherAnnotation.description)
            .appendList(Annotation, 'childAnnotations', this.childAnnotations, otherAnnotation.childAnnotations)

    }

    static DetachedCriteria<Annotation> by() {
        new DetachedCriteria<Annotation>(Annotation)
    }

    static DetachedCriteria<Annotation> byMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        new DetachedCriteria<Annotation>(Annotation).eq('multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
    }

    static DetachedCriteria<Annotation> byyMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        new DetachedCriteria<Annotation>(Annotation).inList('multiFacetAwareItemId', multiFacetAwareItemIds)
    }

    static DetachedCriteria<Annotation> byyMultiFacetAwareItemIdAndId(Serializable multiFacetAwareItemId, Serializable resourceId) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<Annotation> whereRootAnnotationOfMultiFacetAwareItemId(Serializable catalogueItemId) {
        byMultiFacetAwareItemId(catalogueItemId).isNull('parentAnnotation')
    }

    static DetachedCriteria<Annotation> byParentAnnotationId(Serializable parentAnnotationId) {
        new DetachedCriteria<Annotation>(Annotation).eq('parentAnnotation.id', Utils.toUuid(parentAnnotationId))
    }
}