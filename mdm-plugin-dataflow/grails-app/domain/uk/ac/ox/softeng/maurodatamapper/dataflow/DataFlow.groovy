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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
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
import org.grails.datastore.gorm.GormEntity

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
        source index: 'data_flow_source_idx'
        target index: 'data_flow_target_idx'
        dataClassComponents cascade: 'all-delete-orphan'
    }

    DataFlow() {
    }

    @Override
    String getDomainType() {
        DataFlow.simpleName
    }

    @Override
    GormEntity getPathParent() {
        target
    }

    @Override
    def beforeValidate() {
        beforeValidateModelItem()
        dataClassComponents.each {it.beforeValidate()}
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
        "DataFlow:${label}"
    }

    @Override
    DataModel getModel() {
        target
    }

    @Override
    String getDiffIdentifier() {
        this.label
    }

    @Override
    Boolean hasChildren() {
        dataClassComponents
    }

    ObjectDiff<DataFlow> diff(DataFlow otherDataFlow) {
        catalogueItemDiffBuilder(DataFlow, this, otherDataFlow)
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

    static DetachedCriteria<DataFlow> byTargetDataModelIdAndId(UUID dataModelId, UUID id) {
        byTargetDataModelId(dataModelId).idEq(id)
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

    static DetachedCriteria<DataFlow> byDataModelIdInList(List<UUID> dataModelIds) {
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
}