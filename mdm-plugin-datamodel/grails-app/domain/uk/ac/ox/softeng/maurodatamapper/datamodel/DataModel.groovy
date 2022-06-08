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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
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
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelSearch
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.IndexedSiblingAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.databinding.DataTypeCollectionBindingHelper
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataModelDataClassCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.ImportLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j

@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataModel implements Model<DataModel>, SummaryMetadataAware, IndexedSiblingAware {

    UUID id

    Boolean hasChild

    /*
    Required for binding during importing, this allows the import code to send a dataTypes collection object which is then bound to the correct
    datatype collections. This object should only be accessed using the getter method as the collection itself is transient and will not be populated
     */
    @BindUsing({ obj, source -> new DataTypeCollectionBindingHelper().getPropertyValue(obj, 'dataTypes', source) })
    private Set<DataType> dataTypes

    static hasMany = [
        dataClasses        : DataClass,
        classifiers        : Classifier,
        metadata           : Metadata,
        annotations        : Annotation,
        semanticLinks      : SemanticLink,
        versionLinks       : VersionLink,
        referenceFiles     : ReferenceFile,
        summaryMetadata    : SummaryMetadata,
        referenceTypes     : ReferenceType,
        enumerationTypes   : EnumerationType,
        primitiveTypes     : PrimitiveType,
        modelDataTypes     : ModelDataType,
        rules              : Rule,
        importedDataTypes  : DataType,
        importedDataClasses: DataClass,
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases', 'dataTypes']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        dataTypes validator: { val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'dataTypes').isValid(val) }
        dataClasses validator: { val, obj -> new DataModelDataClassCollectionValidator(obj).isValid(val) }
        importedDataTypes validator: { val, obj ->
            new ImportLabelValidator(obj, 'dataTypes').isValid(val)
        }
        importedDataClasses validator: { val, obj ->
            new ImportLabelValidator(obj, 'childDataClasses').isValid(val)
        }
    }

    static mapping = {
        documentationVersion type: VersionUserType
        modelVersion type: VersionUserType
        folder cascade: 'none'
        authority fetch: 'join'
        dataClasses cascade: 'all-delete-orphan'
        //        dataTypes cascade: 'all-delete-orphan', cascadeValidate: 'none'
        referenceTypes cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
        enumerationTypes cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
        primitiveTypes cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
        modelDataTypes cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
        summaryMetadata cascade: 'all-delete-orphan'
        importedDataTypes cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_datamodel_to_imported_data_type',
            key   : 'datamodel_id',
            column: 'imported_datatype_id'
        ]
        importedDataClasses cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_datamodel_to_imported_data_class',
            key   : 'datamodel_id',
            column: 'imported_dataclass_id'
        ]
    }

    static mappedBy = [
        metadata           : 'none',
        dataClasses        : 'dataModel',
        referenceTypes     : 'dataModel',
        enumerationTypes   : 'dataModel',
        primitiveTypes     : 'dataModel',
        modelDataTypes     : 'dataModel',
        importedDataTypes  : 'none',
        importedDataClasses: 'none',
    ]

    static search = {
        CallableSearch.call(ModelSearch, delegate)
    }

    /**
     * Constructor, sets various default and initial values.
     */
    DataModel() {
        initialiseVersioning()
        modelType = DataModelType.DATA_STANDARD.label
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
        breadcrumbTree = new BreadcrumbTree(this)
        referenceTypes = []
        primitiveTypes = []
        enumerationTypes = []
        modelDataTypes = []
    }

    @Override
    String getDomainType() {
        DataModel.simpleName
    }

    @Override
    String getPathPrefix() {
        'dm'
    }

    void setType(DataModelType type) {
        modelType = type.label
    }

    void setType(String type) {
        modelType = DataModelType.findForLabel(type)?.label
    }

    /**
     * Gets all DataClasses which are immediate children of the DataModel,
     * not including DataClasses nested within other DataClasses.
     * @return List<DataClass>                  of immediate children of the DataModel
     */
    List<DataClass> getChildDataClasses() {
        dataClasses?.findAll { !it.parentDataClass }?.sort() ?: [] as List<DataClass>
    }

    /**
     * Generate differences between {@code this} DataModel and some {@code otherDataModel}
     * @param otherDataModel DataModel to compare to {@code this} DataModel
     * @return ObjectDiff<DataModel>                  containing field differences and arrays of child differences
     */
    ObjectDiff<DataModel> diff(DataModel otherDataModel, String context) {
        diff(otherDataModel, context, null, null)
    }

    ObjectDiff<DataModel> diff(DataModel otherDataModel, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        ObjectDiff<DataModel> base = DiffBuilder.modelDiffBuilder(DataModel, this, otherDataModel, lhsDiffCache, rhsDiffCache)

        if (!lhsDiffCache || !rhsDiffCache) {
            base.appendCollection(DataType, 'dataTypes', this.getDataTypes(), otherDataModel.getDataTypes())
                .appendCollection(DataClass, 'dataClasses', this.childDataClasses, otherDataModel.childDataClasses)
        } else {
            base.appendCollection(DataType, 'dataTypes')
                .appendCollection(DataClass, 'dataClasses')
        }
        base
    }

    def beforeValidate() {
        modelType = DataModelType.findFor(modelType)?.label
        beforeValidateCatalogueItem()
        summaryMetadata?.each {
            it.beforeValidateCheck(this)
        }
        // New save/validate so all DEs and DCs are also new so sort the indexes now
        // This avoids repeated calls to the individual DE or DC during their beforeValidate
        if (!id) {
            fullSortOfChildren(getDataTypes())
            fullSortOfChildren(childDataClasses)
        }
    }

    DataType findDataTypeByLabel(String label) {
        getDataTypes()?.find { it.label == label }
    }

    DataType findDataTypeByLabelAndType(String label, String type) {
        getDataTypes()?.find { it.domainType == type && it.label == label }
    }

    int countDataTypesByLabel(String label) {
        getDataTypes()?.count { it.label == label } ?: 0
    }

    Set<DataElement> getAllDataElements() {
        dataClasses.collect { it.dataElements }.findAll().flatten().toSet() as Set<DataElement>
    }

    List<DataType> getSortedDataTypes() {
        getDataTypes()?.sort() ?: []
    }

    @Override
    String getEditLabel() {
        "${modelType}:${label}"
    }

    DataModel addToReferenceTypes(Map dataType) {
        addToDataTypes(new ReferenceType(dataType))
    }

    DataModel addToEnumerationTypes(Map dataType) {
        addToDataTypes(new EnumerationType(dataType))
    }

    DataModel addToPrimitiveTypes(Map dataType) {
        addToDataTypes(new PrimitiveType(dataType))
    }

    DataModel addToModelDataTypes(Map dataType) {
        addToDataTypes(new ModelDataType(dataType))
    }

    DataModel addToReferenceTypes(ReferenceType dataType) {
        addToDataTypes(dataType)
    }

    DataModel addToEnumerationTypes(EnumerationType dataType) {
        addToDataTypes(dataType)
    }

    DataModel addToPrimitiveTypes(PrimitiveType dataType) {
        addToDataTypes(dataType)
    }

    DataModel addToModelDataTypes(ModelDataType dataType) {
        addToDataTypes(dataType)
    }

    DataModel removeFromReferenceTypes(ReferenceType dataType) {
        removeFromDataTypes(dataType)
    }

    DataModel removeFromEnumerationTypes(EnumerationType dataType) {
        removeFromDataTypes(dataType)
    }

    DataModel removeFromPrimitiveTypes(PrimitiveType dataType) {
        removeFromDataTypes(dataType)
    }

    DataModel removeFromModelDataTypes(ModelDataType dataType) {
        removeFromDataTypes(dataType)
    }

    DataModel addToDataTypes(DataType dataType) {
        addTo('dataTypes', dataType)
        if (dataType.instanceOf(ReferenceType)) {
            addTo('referenceTypes', dataType)
        } else if (dataType.instanceOf(PrimitiveType)) {
            addTo('primitiveTypes', dataType)
        } else if (dataType.instanceOf(EnumerationType)) {
            addTo('enumerationTypes', dataType)
        } else if (dataType.instanceOf(ModelDataType)) {
            addTo('modelDataTypes', dataType)
        }
        this
    }

    DataModel removeFromDataTypes(DataType dataType) {
        removeFrom('dataTypes', dataType)
        if (dataType.instanceOf(ReferenceType)) {
            removeFrom('referenceTypes', dataType)
        } else if (dataType.instanceOf(PrimitiveType)) {
            removeFrom('primitiveTypes', dataType)
        } else if (dataType.instanceOf(EnumerationType)) {
            removeFrom('enumerationTypes', dataType)
        } else if (dataType.instanceOf(ModelDataType)) {
            removeFrom('modelDataTypes', dataType)
        }
        this
    }

    Set<DataType> getDataTypes() {
        (primitiveTypes + enumerationTypes + referenceTypes + modelDataTypes).asUnmodifiable() as Set<DataType>
    }

    Set<DataType> getDataTypesAndImportedDataTypes() {
        getDataTypes() + importedDataTypes
    }

    Set<DataClass> getChildDataClassesAndImportedDataClasses() {
        getChildDataClasses() + importedDataClasses
    }

    DataModel addToImportedDataClasses(DataClass dataClass) {
        dataClass.addToImportingDataModels(this)
        addTo('importedDataClasses', dataClass)
    }

    DataModel addToImportedDataTypes(DataType dataType) {
        dataType.addToImportingDataModels(this)
        addTo('importedDataTypes', dataType)
    }

    DataModel removeFromImportedDataClasses(DataClass dataClass) {
        dataClass.removeFromImportingDataModels(this)
        removeFrom('importedDataClasses', dataClass)
    }

    DataModel removeFromImportedDataTypes(DataType dataType) {
        dataType.removeFromImportingDataModels(this)
        removeFrom('importedDataTypes', dataType)
    }

    @Override
    void updateChildIndexes(ModelItem updated, Integer oldIndex) {
        if (updated.instanceOf(DataClass)) {
            updateSiblingIndexes(updated, getChildDataClasses())
            return
        }
        if (updated.instanceOf(DataType)) {
            updateSiblingIndexes(updated, getDataTypes() ?: [])
            return
        }
        log.warn('Unknown model item type cannot update child indexes: {}', updated.domainType)
    }

    static DetachedCriteria<DataModel> by() {
        new DetachedCriteria<DataModel>(DataModel)
    }

    static DetachedCriteria<DataModel> byIds(List<UUID> ids) {
        by().inList('id', ids)
    }

    static DetachedCriteria<DataModel> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataModel> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<DataModel> withFilter(DetachedCriteria<DataModel> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }

    DataType findEnumerationTypeByLabel(String label) {
        getEnumerationTypes()?.find { it.label == label }
    }

    static DetachedCriteria<DataClass> byImportedDataClassId(UUID dataClassId) {
        where {
            importedDataClasses {
                eq 'id', dataClassId
            }
        }
    }

    static DetachedCriteria<DataClass> byImportedDataTypeId(UUID dataTypeId) {
        where {
            importedDataTypes {
                eq 'id', dataTypeId
            }
        }
    }
}