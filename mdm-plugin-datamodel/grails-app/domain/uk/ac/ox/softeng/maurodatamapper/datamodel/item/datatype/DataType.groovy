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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.IndexedSiblingAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataTypeLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

import javax.persistence.criteria.JoinType

@Resource(readOnly = false, formats = ['json', 'xml'])
@Slf4j
abstract class DataType<D> implements ModelItem<D, DataModel>, SummaryMetadataAware, IndexedSiblingAware {

    public final static Integer BATCH_SIZE = 1000

    public static final String REFERENCE_DOMAIN_TYPE = ReferenceType.simpleName
    public static final String ENUMERATION_DOMAIN_TYPE = EnumerationType.simpleName
    public static final String PRIMITIVE_DOMAIN_TYPE = PrimitiveType.simpleName
    public static final String MODEL_DATA_DOMAIN_TYPE = ModelDataType.simpleName

    UUID id
    String domainType

    static hasMany = [
        classifiers        : Classifier,
        metadata           : Metadata,
        annotations        : Annotation,
        semanticLinks      : SemanticLink,
        referenceFiles     : ReferenceFile,
        dataElements       : DataElement,
        summaryMetadata    : SummaryMetadata,
        rules              : Rule,
        importingDataModels: DataModel,
    ]

    static belongsTo = [dataModel: DataModel]

    static transients = ['aliases', 'model']

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        label validator: {val, obj -> new DataTypeLabelValidator(obj).isValid(val)}
        domainType size: 13..15
    }

    static mapping = {
        dataElements cascade: 'delete,lock,refresh,evict,replicate', cascadeValidate: 'none'
        summaryMetadata cascade: 'all-delete-orphan'
        dataModel index: 'data_type_data_model_idx', cascade: 'none'
        importingDataModels cascade: 'none', cascadeValidate: 'none', joinTable: [
            name  : 'join_datamodel_to_imported_data_type',
            column: 'datamodel_id',
            key   : 'imported_datatype_id'
        ]
    }

    static mappedBy = [
        dataElements       : 'dataType',
        metadata           : 'none',
        importingDataModels: 'none'
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        modelType searchable: 'yes', analyze: false, indexingDependency: [reindexOnUpdate: 'shallow', derivedFrom: 'dataModel']
        modelId searchable: 'yes', indexingDependency: [reindexOnUpdate: 'shallow', derivedFrom: 'dataModel']
    }

    DataType() {
    }

    @Override
    String getPathPrefix() {
        'dt'
    }

    UUID getModelId() {
        dataModel.id
    }

    @Override
    DataModel getParent() {
        dataModel
    }

    def beforeValidate() {
        long st = System.currentTimeMillis()
        beforeValidateModelItem()
        summaryMetadata?.each {
            if (!it.createdBy) it.createdBy = createdBy
            it.multiFacetAwareItem = this
        }
        if (domainType != ENUMERATION_DOMAIN_TYPE) log.trace('DT before validate {} took {}', this.label, Utils.timeTaken(st))
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    @Override
    DataModel getModel() {
        dataModel
    }

    @Override
    Boolean hasChildren() {
        false
    }

    /**
     * A DataType is indexed within the DataModel to which it belongs
     */
    @Override
    DataModel getIndexedWithin() {
        // Hack around the code, its indexed with the DC but if the DC has no id then the DE will be sorted and idx'd as part of the DC beforeValidate
        dataModel?.id ? dataModel : null
    }

    Set<UUID> getDataElementIds() {
        DataElement.byDataTypeId(this.id).id().list() as Set<UUID>
    }

    boolean hasDataElements() {
        DataElement.byDataTypeId(this.id).count()
    }

    @Override
    int compareTo(D that) {
        if (!(that instanceof ModelItem)) throw new ApiInternalException('MI01', 'Cannot compare non-ModelItem')
        int res = this.order <=> ((ModelItem) that).order
        if (res == 0) res = this.domainType <=> that.domainType
        if (res == 0) res = this.label <=> ((ModelItem) that).label
        res
    }

    static DetachedCriteria<DataType> byDataModelId(Serializable dataModelId) {
        new DetachedCriteria<DataType>(DataType).eq('dataModel.id', Utils.toUuid(dataModelId))
    }

    static DetachedCriteria<DataType> byDataModelIdInList(Collection<UUID> dataModelIds) {
        new DetachedCriteria<DataType>(DataType).inList('dataModel.id', dataModelIds)
    }

    static DetachedCriteria<DataType> byDataModelIdIncludingImported(Serializable dataModelId) {
        new DetachedCriteria<DataType>(DataType).or {
            eq('dataModel.id', Utils.toUuid(dataModelId))
            importingDataModels {
                eq 'id', dataModelId
            }
            join('importingDataModels', JoinType.LEFT)
        }
    }

    static DetachedCriteria<DataType> byDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable dataModelId, String searchTerm) {
        byDataModelId(dataModelId).or {
            ilike('label', "%${searchTerm}%")
            ilike('description', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<DataType> byDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(Serializable dataModelId, String searchTerm) {
        byDataModelIdIncludingImported(dataModelId).or {
            ilike('label', "%${searchTerm}%")
            ilike('description', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<DataType> byDataModelIdAndId(Serializable dataModelId, Serializable id) {
        byDataModelId(dataModelId).idEq(Utils.toUuid(id))
    }

    static <T extends DataType> DetachedCriteria<T> byClassifierId(Class<T> clazz, Serializable classifierId) {
        new DetachedCriteria<>(clazz).where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }

    static DetachedCriteria<DataType> withFilter(DetachedCriteria<DataType> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }

    static DetachedCriteria<DataType> byDataModelIdAndLabel(UUID dataModelId, String label) {
        byDataModelId(dataModelId).eq('label', label)
    }


    static DetachedCriteria<DataType> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataType> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}