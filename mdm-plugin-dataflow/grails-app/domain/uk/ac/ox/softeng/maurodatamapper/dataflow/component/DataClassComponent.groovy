/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

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
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria

class DataClassComponent implements ModelItem<DataClassComponent, DataModel> {

    UUID id
    DataFlow dataFlow
    String definition

    static belongsTo = [
        DataFlow
    ]

    static hasMany = [
        classifiers          : Classifier,
        metadata             : Metadata,
        annotations          : Annotation,
        semanticLinks        : SemanticLink,
        referenceFiles       : ReferenceFile,
        sourceDataClasses    : DataClass,
        targetDataClasses    : DataClass,
        dataElementComponents: DataElementComponent,
        rules                : Rule
    ]

    static transients = ['aliases', 'model']

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        sourceDataClasses minSize: 1
        targetDataClasses minSize: 1
        definition nullable: true, blank: false
    }

    static mapping = {
        definition type: 'text'
        sourceDataClasses joinTable: [name: 'join_data_class_component_to_source_data_class', key: 'data_class_component_id'],
                          index: 'jdcctsdc_data_class_component_idx', cascade: 'none'
        targetDataClasses joinTable: [name: 'join_data_class_component_to_target_data_class', key: 'data_class_component_id'],
                          index: 'jdccttdc_data_class_component_idx', cascade: 'none'
        dataElementComponents cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        dataElementComponents: 'dataClassComponent',
        sourceDataClasses    : 'none',
        targetDataClasses    : 'none'
    ]

    DataClassComponent() {
    }

    @Override
    String getDomainType() {
        DataClassComponent.simpleName
    }

    @Override
    String getPathPrefix() {
        'dcc'
    }

    @Override
    DataFlow getParent() {
        dataFlow
    }

    def beforeValidate() {
        beforeValidateModelItem()
        dataElementComponents.each {it.beforeValidate()}
    }

    @Override
    String getEditLabel() {
        "DataClassComponent:${label}"
    }

    @Override
    DataModel getModel() {
        dataFlow.model
    }

    @Override
    Boolean hasChildren() {
        dataElementComponents
    }

    ObjectDiff<DataClassComponent> diff(DataClassComponent that, String context) {
        diff(that, context, null,null)
    }

    ObjectDiff<DataClassComponent> diff(DataClassComponent that, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(DataClassComponent, this, that, lhsDiffCache, rhsDiffCache)
    }

    static DetachedCriteria<DataClassComponent> by() {
        new DetachedCriteria<DataClassComponent>(DataClassComponent)
    }

    static DetachedCriteria<DataClassComponent> byDataFlowId(UUID dataFlowId) {
        by().eq('dataFlow.id', dataFlowId)
    }

    static DetachedCriteria<DataClassComponent> byDataFlowIdAndId(UUID dataFlowId, UUID id) {
        byDataFlowId(dataFlowId).eq('id', id)
    }

    static DetachedCriteria<DataClassComponent> byDataFlowIdAndDataClassId(UUID dataFlowId, UUID dataClassId) {
        byDataFlowId(dataFlowId).or {
            eq('sourceDataClasses.id', dataClassId)
            eq('targetDataClasses.id', dataClassId)
        }
    }

    static DetachedCriteria<DataClassComponent> bySourceDataClassId(UUID dataClassId) {
        by().eq('sourceDataClasses.id', dataClassId)
    }

    static DetachedCriteria<DataClassComponent> byTargetDataClassId(UUID dataClassId) {
        by().eq('targetDataClasses.id', dataClassId)
    }

    static DetachedCriteria<DataClassComponent> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<DataClassComponent> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataClassComponent> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}
