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
package uk.ac.ox.softeng.maurodatamapper.dataflow

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
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.gorm.constraint.validator.DataFlowLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria
import grails.rest.Resource

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataFlow implements ModelItem<DataFlow, DataModel> {

    UUID id
    String diagramLayout
    String definition

    static belongsTo = [
        source: DataModel,
        target: DataModel
    ]

    static hasMany = [
        classifiers        : Classifier,
        metadata           : Metadata,
        annotations        : Annotation,
        semanticLinks      : SemanticLink,
        referenceFiles     : ReferenceFile,
        dataClassComponents: DataClassComponent,
        rules              : Rule
    ]

    static transients = ['aliases', 'model']

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        label validator: {val, obj -> new DataFlowLabelValidator(obj).isValid(val)}
        diagramLayout nullable: true
        target validator: {val ->
            val.modelType == DataModelType.DATA_ASSET.label ?: ['invalid.dataflow.datamodel.type', val.modelType]
        }
        source validator: {val ->
            val.modelType == DataModelType.DATA_ASSET.label ?: ['invalid.dataflow.datamodel.type', val.modelType]
        }
        definition nullable: true, blank: false
    }

    static mapping = {
        definition type: 'text'
        diagramLayout type: 'text'
        source index: 'data_flow_source_idx', cascade: 'none'
        target index: 'data_flow_target_idx', cascade: 'none'
        dataClassComponents cascade: 'all-delete-orphan'
    }

    DataFlow() {
    }

    @Override
    String getDomainType() {
        DataFlow.simpleName
    }

    @Override
    String getPathPrefix() {
        'df'
    }

    @Override
    DataModel getParent() {
        target
    }

    def beforeValidate() {
        beforeValidateModelItem()
        dataClassComponents.each {it.beforeValidate()}
    }

    @Override
    String getEditLabel() {
        "DataFlow:${label}"
    }

    @Override
    DataModel getModel() {
        target
    }

    @Override
    Boolean hasChildren() {
        dataClassComponents
    }

    ObjectDiff<DataFlow> diff(DataFlow otherDataFlow, String context) {
        diff(otherDataFlow, context, null, null)
    }

    ObjectDiff<DataFlow> diff(DataFlow otherDataFlow, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(DataFlow, this, otherDataFlow, lhsDiffCache, rhsDiffCache)
    }

    boolean refersToDataModelId(UUID dataModelId) {
        source.id == dataModelId || target.id == dataModelId
    }

    static DetachedCriteria<DataFlow> by() {
        new DetachedCriteria<DataFlow>(DataFlow)
    }

    static DetachedCriteria<DataFlow> byTargetDataModelId(UUID dataModelId) {
        by().eq('target.id', dataModelId)
    }

    static DetachedCriteria<DataFlow> bySourceDataModelId(UUID dataModelId) {
        by().eq('source.id', dataModelId)
    }

    static DetachedCriteria<DataFlow> byTargetDataModelIdAndId(UUID dataModelId, UUID id) {
        byTargetDataModelId(dataModelId).idEq(id)
    }

    static DetachedCriteria<DataFlow> bySourceDataModelIdAndId(UUID dataModelId, UUID id) {
        bySourceDataModelId(dataModelId).idEq(id)
    }

    static DetachedCriteria<DataFlow> bySourceDataModelIdAndTargetDataModelIdInList(UUID dataModelId, List<UUID> targetDataModelIds) {
        by().eq('source.id', dataModelId)
            .inList('target.id', targetDataModelIds)
    }

    static DetachedCriteria<DataFlow> byTargetDataModelIdAndSourceDataModelIdInList(UUID dataModelId, List<UUID> sourceDataModelIds) {
        byTargetDataModelId(dataModelId).inList('source.id', sourceDataModelIds)
    }

    static DetachedCriteria<DataFlow> byTargetDataModelIdAndLabel(UUID dataModelId, String label) {
        byTargetDataModelId(dataModelId).eq('label', label)
    }

    static DetachedCriteria<DataFlow> byDataModelId(UUID dataModelId) {
        by().or {
            eq('source.id', dataModelId)
            eq('target.id', dataModelId)
        }
    }

    static DetachedCriteria<DataFlow> byDataModelIdAndDataModelIdInList(UUID dataModelId, List<UUID> dataModelIds) {
        byDataModelId(dataModelId).and {
            inList('source.id', dataModelIds)
            inList('target.id', dataModelIds)
        }
    }

    static DetachedCriteria<DataFlow> byDataModelIdInList(Collection<UUID> dataModelIds) {
        by()
            .inList('source.id', dataModelIds)
            .inList('target.id', dataModelIds)
    }


    static DetachedCriteria<DataFlow> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<DataFlow> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataFlow> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

}