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
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
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
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Index
import org.hibernate.search.bridge.builtin.UUIDBridge

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
        classifiers    : Classifier,
        metadata       : Metadata,
        annotations    : Annotation,
        semanticLinks  : SemanticLink,
        referenceFiles : ReferenceFile,
        dataElements   : DataElement,
        summaryMetadata: SummaryMetadata,
        rules          : Rule
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
    }

    static mappedBy = [
        dataElements: 'dataType',
        metadata    : 'none',
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
    }

    DataType() {
    }

    @Field(index = Index.YES, bridge = @FieldBridge(impl = UUIDBridge))
    UUID getModelId() {
        dataModel.id
    }

    @Override
    GormEntity getPathParent() {
        dataModel
    }

    @Override
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
        dataModel
    }

    @Override
    String getDiffIdentifier() {
        this.label
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
        dataModel
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

    /**
     * Where the DataType has been imported into the specified DataModel. Use this method if you want only those 
     * DataTypes which have been imported into a DataModel i.e. excluding DataTypes owned directly by the DataModel.
     *
     * @param ID of DataModel which has imported other catalogue items
     * @return DetachedCriteria<DataType> for id in IDs of catalogue items which have been imported into the DataModel
     */
    static DetachedCriteria<DataType> importedByDataModelId(Serializable dataModelId) {
        new DetachedCriteria<DataType>(DataType)
        .in('id', ModelImport.importedByCatalogueItemId(dataModelId))
    }

    /**
     * If we want to include imported DataTypes then do a logical OR on imported and directly owned DataTypes.
     *
     * @param dataModelId The ID of the DataModel we are looking at
     * @param includeImported Do we want to retrieve DataTypes which have been imported into the DataModel (in 
     *                        addition to DataTypes directly belonging to the DataModel)?
     * @return DetachedCriteria<DataType>    
     */
    static DetachedCriteria<DataType> byDataModelId(Serializable dataModelId, boolean includeImported = false) {
        DetachedCriteria criteria = new DetachedCriteria<DataType>(DataType)

        if (includeImported) {
            criteria
            .or {
                inList('id', ModelImport.importedByCatalogueItemId(dataModelId))
                eq('dataModel.id', Utils.toUuid(dataModelId))                
            }
        } else {
            criteria
            .eq('dataModel.id', Utils.toUuid(dataModelId))
        }
        
        criteria
    }

    /**
     * Get DataTypes for a DataModel by the DataModel id and a search string, including imported data types if required.
     *
     * @param dataModelId The ID of the DataModel we are looking at
     * @param searchTerm Search string which is applied against the label and description of the DataType
     * @param includeImported Do we want to retrieve DataTypes which have been imported into the DataModel (in 
     *                        addition to DataTypes directly belonging to the DataModel)?
     * @return DetachedCriteria<DataType>      
     */
    static DetachedCriteria<DataType> byDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable dataModelId, String searchTerm, boolean includeImported = false) {
        byDataModelId(dataModelId, includeImported).or {
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