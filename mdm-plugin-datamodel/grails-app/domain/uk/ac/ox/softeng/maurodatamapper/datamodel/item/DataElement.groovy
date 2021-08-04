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
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.databinding.DataTypeBindingHelper
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataElementLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.domain.MultiplicityAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Index
import org.hibernate.search.bridge.builtin.UUIDBridge

import javax.persistence.criteria.JoinType

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
@Slf4j
class DataElement implements ModelItem<DataElement, DataModel>, MultiplicityAware, SummaryMetadataAware {

    public final static Integer BATCH_SIZE = 1000

    DataClass dataClass
    @BindUsing({obj, source ->
        new DataTypeBindingHelper().getPropertyValue(obj, 'dataType', source)
    })
    DataType dataType

    UUID id

    static belongsTo = [DataClass, DataType]

    static transients = ['aliases']

    static hasMany = [
        classifiers         : Classifier,
        metadata            : Metadata,
        annotations         : Annotation,
        semanticLinks       : SemanticLink,
        referenceFiles      : ReferenceFile,
        summaryMetadata     : SummaryMetadata,
        rules               : Rule,
        importingDataClasses: DataClass,
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        minMultiplicity nullable: true
        maxMultiplicity nullable: true
        label validator: {val, obj -> new DataElementLabelValidator(obj).isValid(val)}
        dataType validator: {val, obj ->
            if (val && val.model && obj.model) {
                // In the same model is okay
                if (val.model.id == obj.model.id) return true
                // Imported into model is okay
                if (obj.model.importedDataTypes.any {it.id == val.id}) return true
                ['invalid.dataelement.datatype.model']
            }
        }
    }

    static mapping = {
        summaryMetadata cascade: 'all-delete-orphan'
        dataClass index: 'data_element_data_class_idx', cascade: 'none'
        dataType index: 'data_element_data_type_idx', cascade: 'none', fetch: 'join', cascadeValidate: 'dirty'
        model cascade: 'none'
        importingDataClasses cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_dataclass_to_imported_data_element',
            column: 'dataclass_id',
            key   : 'imported_dataelement_id'
        ]
    }

    static mappedBy = [
        importingDataClasses: 'none'
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        dataClass indexEmbedded: true
        dataType indexEmbedded: true
    }

    DataElement() {
    }

    @Override
    String getDomainType() {
        DataElement.simpleName
    }

    @Override
    String getPathPrefix() {
        'de'
    }

    @Field(index = Index.YES, bridge = @FieldBridge(impl = UUIDBridge))
    UUID getModelId() {
        model.id
    }

    @Override
    GormEntity getPathParent() {
        dataClass
    }

    @Override
    def beforeValidate() {
        long st = System.currentTimeMillis()
        beforeValidateModelItem()
        summaryMetadata?.each {
            if (!it.createdBy) it.createdBy = createdBy
            it.multiFacetAwareItem = this
        }
        // If datatype is newly created with dataelement and the datamodel is not new
        // If the DM is new then DT validation will happen at the DM level
        if (dataType && !dataType.ident() && getModel().id) {
            dataType.dataModel = model
            dataType.createdBy = createdBy
            dataType.beforeValidate()
        }
        log.trace('DE before validate {} took {}', this.label, Utils.timeTaken(st))
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
        "${domainType}:${label}"
    }

    @Override
    DataModel getModel() {
        dataClass?.model
    }

    @Override
    String getDiffIdentifier(String context) {
        "${dataClass.getDiffIdentifier(context)}/${pathIdentifier}"
    }

    @Override
    Boolean hasChildren() {
        false
    }

    /**
     * A DataElement is indexed within the DataClass to which it belongs
     */
    @Override
    DataClass getIndexedWithin() {
        dataClass
    }

    ObjectDiff<DataElement> diff(DataElement otherDataElement, String context) {
        catalogueItemDiffBuilder(DataElement, this, otherDataElement)
            .appendString('dataType.label', this.dataType.label, otherDataElement.dataType.label)
            .appendNumber('minMultiplicity', this.minMultiplicity, otherDataElement.minMultiplicity)
            .appendNumber('maxMultiplicity', this.maxMultiplicity, otherDataElement.maxMultiplicity)


    }

    static DetachedCriteria<DataElement> by() {
        new DetachedCriteria<DataElement>(DataElement)
    }

    static DetachedCriteria<DataElement> byDataClassIdAndId(Serializable dataClassId, Serializable resourceId) {
        byDataClassId(dataClassId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<DataElement> byDataClassIdAndLabel(Serializable dataClassId, String label) {
        byDataClassId(dataClassId).eq('label', label)
    }

    static DetachedCriteria<DataElement> byDataTypeIdAndId(Serializable dataTypeId, Serializable resourceId) {
        byDataTypeId(dataTypeId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<DataElement> byDataClassId(Serializable dataClassId) {
        new DetachedCriteria<DataElement>(DataElement).eq('dataClass.id', Utils.toUuid(dataClassId))
    }

    static DetachedCriteria<DataElement> byDataClassIdIncludingImported(UUID dataClassId) {
        new DetachedCriteria<DataElement>(DataElement).or {
            eq('dataClass.id', dataClassId)
            importingDataClasses {
                eq 'id', dataClassId
            }
            join('importingDataClasses', JoinType.LEFT)
        }
    }

    static DetachedCriteria<DataElement> byDataClass(DataClass dataClass) {
        new DetachedCriteria<DataElement>(DataElement).eq('dataClass', dataClass)
    }

    static DetachedCriteria<DataElement> byDataTypeId(Serializable dataTypeId) {
        new DetachedCriteria<DataElement>(DataElement).eq('dataType.id', Utils.toUuid(dataTypeId))
    }

    static DetachedCriteria<DataElement> byDataType(DataType dataType) {
        new DetachedCriteria<DataElement>(DataElement).eq('dataType', dataType)
    }

    static DetachedCriteria<DataElement> byMetadataNamespaceAndKeyAndValue(String namespace, String key, String value) {
        new DetachedCriteria<DataElement>(DataElement).in('id', Metadata.byNamespaceAndKeyAndValue(namespace, key, value))
    }

    static DetachedCriteria<DataElement> byDataModelId(Serializable dataModelId) {
        new DetachedCriteria<DataElement>(DataElement).in('dataClass', DataClass.byDataModelId(dataModelId).id())
    }

    static DetachedCriteria<DataElement> byDataModelIdAndLabelIlike(Serializable dataModelId, String labelSearch) {
        byDataModelId(dataModelId).ilike('label', "%${labelSearch}%")
    }

    static DetachedCriteria<DataElement> byClassifierId(Serializable classifierId) {
        where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }

    static DetachedCriteria<DataElement> withFilter(DetachedCriteria<DataElement> criteria, Map filters) {
        criteria = withCatalogueItemFilter(criteria, filters)
        if (filters.dataType) criteria = criteria.and {
            dataType {
                ilike 'label', "%${filters.dataType}%"
            }
        }
        criteria
    }

    static DetachedCriteria<DataElement> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataElement> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}