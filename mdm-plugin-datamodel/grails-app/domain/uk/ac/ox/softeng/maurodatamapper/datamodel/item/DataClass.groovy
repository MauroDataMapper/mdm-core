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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.IndexedSiblingAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataClassLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.ImportLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.domain.MultiplicityAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

import javax.persistence.criteria.JoinType

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataClass implements ModelItem<DataClass, DataModel>, MultiplicityAware, SummaryMetadataAware, IndexedSiblingAware {

    public final static Integer BATCH_SIZE = 1000

    UUID id
    DataClass parentDataClass
    DataModel dataModel

    static hasMany = [
        classifiers         : Classifier,
        metadata            : Metadata,
        annotations         : Annotation,
        semanticLinks       : SemanticLink,
        referenceFiles      : ReferenceFile,
        dataClasses         : DataClass,
        dataElements        : DataElement,
        referenceTypes      : ReferenceType,
        summaryMetadata     : SummaryMetadata,
        rules               : Rule,
        extendedDataClasses : DataClass,
        importedDataClasses : DataClass,
        importedDataElements: DataElement,
        importingDataModels : DataModel,
        importingDataClasses: DataClass,
    ]

    static belongsTo = [DataClass, DataModel]

    static transients = ['aliases', 'model']

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        minMultiplicity nullable: true
        maxMultiplicity nullable: true
        parentDataClass nullable: true
        label validator: {val, obj -> new DataClassLabelValidator(obj).isValid(val)}
        dataElements validator: {val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'dataElements').isValid(val)}
        dataClasses validator: {val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'dataClasses').isValid(val)}
        extendedDataClasses validator: {val, obj ->
            if (!val) return true
            Set<DataClass> dataClassesInAnotherModel = val.findAll {it.modelId != obj.modelId}
            if (!dataClassesInAnotherModel) return true
            int count = dataClassesInAnotherModel.count {!it.model.finalised}
            if (count > 0) return ['invalid.extended.dataclasses.model.not.finalised', count]
        }
        importedDataElements validator: {val, obj ->
            new ImportLabelValidator(obj, 'dataElements').isValid(val)
        }
        importedDataClasses validator: {val, obj ->
            new ImportLabelValidator(obj, 'dataClasses').isValid(val)
        }
    }

    static mapping = {
        dataElements cascade: 'all-delete-orphan'
        dataClasses cascade: 'all-delete-orphan'
        referenceTypes cascade: 'none'
        summaryMetadata cascade: 'all-delete-orphan'
        dataModel index: 'data_class_data_model_idx' //, cascade: 'none', cascadeValidate: 'none'
        parentDataClass index: 'data_class_parent_data_class_idx' , cascade: 'save-update'
        extendedDataClasses cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_dataclass_to_extended_data_class',
            key   : 'dataclass_id',
            column: 'extended_dataclass_id'
        ]
        importedDataClasses cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_dataclass_to_imported_data_class',
            key   : 'dataclass_id',
            column: 'imported_dataclass_id'
        ]
        importedDataElements cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_dataclass_to_imported_data_element',
            key   : 'dataclass_id',
            column: 'imported_dataelement_id'
        ]
        importingDataModels cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_datamodel_to_imported_data_class',
            column: 'datamodel_id',
            key   : 'imported_dataclass_id'
        ]
        importingDataClasses cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_dataclass_to_imported_data_class',
            column: 'dataclass_id',
            key   : 'imported_dataclass_id'
        ]
    }

    static mappedBy = [
        dataClasses         : 'parentDataClass',
        referenceTypes      : 'referenceClass',
        dataElements        : 'dataClass',
        extendedDataClasses : 'none',
        importedDataClasses : 'none',
        importedDataElements: 'none',
        importingDataModels : 'none',
        importingDataClasses: 'none'
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        modelType searchable: 'yes', analyze: false, indexingDependency: [reindexOnUpdate: 'shallow', derivedFrom: 'dataModel']
        modelId searchable: 'yes', indexingDependency: [reindexOnUpdate: 'shallow', derivedFrom: 'dataModel']
    }

    DataClass() {
    }

    @Override
    String getDomainType() {
        DataClass.simpleName
    }

    @Override
    String getPathPrefix() {
        'dc'
    }

    UUID getModelId() {
        dataModel.id
    }

    @Override
    GormEntity getPathParent() {
        parentDataClass ?: dataModel
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @Override
    Boolean hasChildren() {
        if (id) {
            DataClass.byDataModelIdAndParentDataClassId(this.dataModel.id, this.id).count() != 0
        } else {
            this.dataClasses == null ? false : !this.dataClasses.isEmpty()
        }
    }

    @Override
    def beforeValidate() {
        long st = System.currentTimeMillis()
        dataModel = dataModel ?: parentDataClass?.getModel()
        beforeValidateModelItem()
        summaryMetadata?.each {
            if (!it.createdBy) it.createdBy = createdBy
            it.multiFacetAwareItem = this
        }
        log.trace('DC before validate {} took {}', this.label, Utils.timeTaken(st))
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
        "DataClass:${label}"
    }

    @Override
    DataModel getModel() {
        dataModel
    }

    ObjectDiff<DataClass> diff(DataClass otherDataClass, String context) {
        catalogueItemDiffBuilder(DataClass, this, otherDataClass)
            .appendNumber('minMultiplicity', this.minMultiplicity, otherDataClass.minMultiplicity)
            .appendNumber('maxMultiplicity', this.maxMultiplicity, otherDataClass.maxMultiplicity)
            .appendList(DataClass, 'dataClasses', this.dataClasses, otherDataClass.dataClasses)
            .appendList(DataElement, 'dataElements', this.dataElements, otherDataClass.dataElements)

    }

    @Override
    String getDiffIdentifier(String context) {
        if (!parentDataClass) return this.pathIdentifier
        "${parentDataClass.getDiffIdentifier(context)}/${this.pathIdentifier}"
    }

    CatalogueItem getParent() {
        parentDataClass ?: dataModel
    }

    /**
     * A DataClass is indexed within its parent, which is either a DataModel or DataClass
     */
    @Override
    CatalogueItem getIndexedWithin() {
        getParent()
    }

    @Override
    void updateChildIndexes(ModelItem updated, Integer oldIndex) {
        if (updated.instanceOf(DataClass)) {
            updateSiblingIndexes(updated, dataClasses ?: [])
        } else if (updated.instanceOf(DataElement)) {
            updateSiblingIndexes(updated, dataElements ?: [])
        }
    }

    DataClass findDataClass(String label) {
        this.dataClasses.find {it.label == label}
    }

    DataElement findDataElement(String label) {
        dataElements.find {it.label == label}
    }

    int countDataElementsByLabel(String label) {
        dataElements?.count {it.label == label} ?: 0
    }

    Set<DataClass> getAllUnsavedChildren() {
        Set<DataClass> children = dataClasses.findAll {!it.id}
        children + children.collectMany {it.getAllUnsavedChildren()}
    }

    static String buildLabelPath(DataClass dataClass) {
        if (!dataClass.parentDataClass) return dataClass.label
        "${buildLabelPath(dataClass.parentDataClass)}|${dataClass.label}"
    }

    static DetachedCriteria<DataClass> byDataModelId(UUID dataModelId) {
        new DetachedCriteria<DataClass>(DataClass).eq('dataModel.id', dataModelId)
    }

    static DetachedCriteria<DataClass> byDataModelIdIncludingImported(UUID dataModelId) {
        new DetachedCriteria<DataClass>(DataClass).or {
            eq('dataModel.id', Utils.toUuid(dataModelId))
            importingDataModels {
                eq 'id', dataModelId
            }
            join('importingDataModels', JoinType.LEFT)
        }
    }

    static DetachedCriteria<DataClass> byDataModelIdAndParentDataClassId(UUID dataModelId, UUID dataClassId) {
        byDataModelId(dataModelId).eq('parentDataClass.id', dataClassId)
    }

    static DetachedCriteria<DataClass> byParentDataClassId(UUID dataClassId) {
        new DetachedCriteria<DataClass>(DataClass).eq('parentDataClass.id', dataClassId)
    }

    static DetachedCriteria<DataClass> byDataModelIdAndParentDataClassIdIncludingImported(UUID dataModelId, UUID dataClassId) {
        new DetachedCriteria<DataClass>(DataClass).or {
            and {
                eq('dataModel.id', dataModelId)
                eq('parentDataClass.id', dataClassId)
            }
            importingDataClasses {
                eq 'id', dataClassId
            }
            join('importingDataClasses', JoinType.LEFT)
        }
    }

    static DetachedCriteria<DataClass> byRootDataClassOfDataModelId(UUID dataModelId) {
        byDataModelId(dataModelId).isNull('parentDataClass')
    }

    static DetachedCriteria<DataClass> byRootDataClassOfDataModelIdIncludingImported(UUID dataModelId) {
        byDataModelIdIncludingImported(dataModelId).isNull('parentDataClass')
    }

    static DetachedCriteria<DataClass> byDataModelIdAndChildOfDataClassId(UUID dataModelId, UUID dataClassId) {
        DetachedCriteria<DataClass> criteria = new DetachedCriteria<>(DataClass)
        criteria.or {
            inList('id', byDataModelIdAndParentDataClassId(dataModelId, dataClassId).id())
            inList('id', DataElement.byDataClassId(dataClassId).id())
        }
    }

    static DetachedCriteria<DataClass> byDataModelIdAndLabel(UUID dataModelId, String label) {
        byDataModelId(dataModelId).eq('label', label)
    }

    static DetachedCriteria<DataClass> byDataModelIdAndLabelIlikeOrDescriptionIlike(UUID dataModelId, String searchTerm) {
        byDataModelId(dataModelId).or {
            ilike('label', "%${searchTerm}%")
            ilike('description', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<DataClass> byDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(UUID dataModelId, String searchTerm) {
        byDataModelIdIncludingImported(dataModelId).or {
            ilike('label', "%${searchTerm}%")
            ilike('description', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<DataClass> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<DataClass> withFilter(DetachedCriteria<DataClass> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }

    static DetachedCriteria<DataClass> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataClass> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<DataClass> byExtendedDataClassId(UUID dataClassId) {
        where {
            extendedDataClasses {
                eq 'id', dataClassId
            }
        }
    }

    static DetachedCriteria<DataClass> byImportedDataClassId(UUID dataClassId) {
        where {
            importedDataClasses {
                eq 'id', dataClassId
            }
        }
    }

}