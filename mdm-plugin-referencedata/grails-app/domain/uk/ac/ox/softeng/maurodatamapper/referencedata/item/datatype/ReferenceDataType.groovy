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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

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
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.referencedata.gorm.constraint.validator.ReferenceDataTypeLabelValidator
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
abstract class ReferenceDataType<D> implements ModelItem<D, ReferenceDataModel>, ReferenceSummaryMetadataAware {

    public final static Integer BATCH_SIZE = 1000

    public static final String ENUMERATION_DOMAIN_TYPE = ReferenceEnumerationType.simpleName
    public static final String PRIMITIVE_DOMAIN_TYPE = ReferencePrimitiveType.simpleName

    UUID id
    String domainType

    static hasMany = [
        classifiers    : Classifier,
        metadata       : Metadata,
        annotations    : Annotation,
        semanticLinks  : SemanticLink,
        referenceFiles : ReferenceFile,
        referenceDataElements   : ReferenceDataElement,
        referenceSummaryMetadata: ReferenceSummaryMetadata,
        rules          : Rule
    ]

    static belongsTo = [referenceDataModel: ReferenceDataModel]

    static transients = ['aliases', 'model']

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        label validator: { val, obj -> new ReferenceDataTypeLabelValidator(obj).isValid(val) }
        domainType size: 1..30
    }

    static mapping = {
        referenceDataElements cascade: 'delete,lock,refresh,evict,replicate', cascadeValidate: 'none'
        referenceSummaryMetadata cascade: 'all-delete-orphan'
        referenceDataModel index: 'data_type_reference_data_model_idx', cascade: 'none'
    }

    static mappedBy = [
        referenceDataElements: 'referenceDataType'
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        modelId searchable: 'yes', indexingDependency: [reindexOnUpdate: 'shallow', derivedFrom: 'referenceDataModel']
    }

    ReferenceDataType() {
    }

    @Override
    String getPathPrefix() {
        'rdt'
    }

    UUID getModelId() {
        referenceDataModel.id
    }

    @Override
    ReferenceDataModel getParent() {
        referenceDataModel
    }

    def beforeValidate() {
        beforeValidateModelItem()
        referenceSummaryMetadata?.each {
            if (!it.createdBy) it.createdBy = createdBy
            it.multiFacetAwareItem = this
        }
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    @Override
    ReferenceDataModel getModel() {
        referenceDataModel
    }

    @Override
    Boolean hasChildren() {
        false
    }

    Set<UUID> getDataElementIds() {
        ReferenceDataElement.byReferenceDataTypeId(this.id).id().list() as Set<UUID>
    }

    boolean hasDataElements() {
        ReferenceDataElement.byReferenceDataTypeId(this.id).count()
    }

    @Override
    int compareTo(D that) {
        if (!(that instanceof ModelItem)) throw new ApiInternalException('MI01', 'Cannot compare non-ModelItem')
        int res = this.order <=> ((ModelItem) that).order
        if (res == 0) res = this.domainType <=> that.domainType
        if (res == 0) res = this.label <=> ((ModelItem) that).label
        res
    }

    static DetachedCriteria<ReferenceDataType> byReferenceDataModelId(Serializable referenceDataModelId) {
        new DetachedCriteria<ReferenceDataType>(ReferenceDataType)
        .eq('referenceDataModel.id', Utils.toUuid(referenceDataModelId))
    }

    static DetachedCriteria<ReferenceDataType> byReferenceDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable referenceDataModelId, String searchTerm) {
        byReferenceDataModelId(referenceDataModelId).or {
            ilike('label', "%${searchTerm}%")
            ilike('description', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<ReferenceDataType> byReferenceDataModelIdAndId(Serializable referenceDataModelId, Serializable id) {
        byReferenceDataModelId(referenceDataModelId).idEq(Utils.toUuid(id))
    }

    static <T extends ReferenceDataType> DetachedCriteria<T> byClassifierId(Class<T> clazz, Serializable classifierId) {
        new DetachedCriteria<>(clazz).where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }

    static DetachedCriteria<ReferenceDataType> withFilter(DetachedCriteria<ReferenceDataType> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }

    static DetachedCriteria<ReferenceDataType> byReferenceDataModelIdAndLabel(UUID referenceDataModelId, String label) {
        byReferenceDataModelId(referenceDataModelId)
        .eq('label', label)
    }

    static DetachedCriteria<ReferenceDataType> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ReferenceDataType> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

}