/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.databinding.DataTypeBindingHelper
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.gorm.constraint.validator.ReferenceDataElementLabelValidator
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.domain.MultiplicityAware
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

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
@Slf4j
class ReferenceDataElement implements ModelItem<ReferenceDataElement, ReferenceDataModel>, MultiplicityAware {

    public final static Integer BATCH_SIZE = 1000

    ReferenceDataModel referenceDataModel
    @BindUsing({obj, source ->
        new DataTypeBindingHelper().getPropertyValue(obj, 'referenceDataType', source)
    })
    ReferenceDataType referenceDataType

    UUID id

    static belongsTo = [ReferenceDataModel, ReferenceDataType]

    static transients = ['aliases']

    static hasMany = [
            classifiers    : Classifier,
            metadata       : Metadata,
            annotations    : Annotation,
            semanticLinks  : SemanticLink,
            referenceFiles : ReferenceFile,
            referenceSummaryMetadata: ReferenceSummaryMetadata,
            referenceDataValues: ReferenceDataValue
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        minMultiplicity nullable: true
        maxMultiplicity nullable: true
        label validator: {val, obj -> new ReferenceDataElementLabelValidator(obj).isValid(val)}
    }

    static mapping = {
        referenceSummaryMetadata cascade: 'all-delete-orphan'
        referenceDataModel index: 'data_element_reference_data_model_idx' //, cascade: 'none'
        referenceDataType index: 'data_element_data_type_idx', cascade: 'save-update', fetch: 'join'
        model cascade: 'none'
    }

    static mappedBy = [
        :
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        referenceDataModel indexEmbedded: true
        referenceDataType indexEmbedded: true
    }

    ReferenceDataElement() {
    }

    @Override
    String getDomainType() {
        ReferenceDataElement.simpleName
    }


    @Field(index = Index.YES, bridge = @FieldBridge(impl = UUIDBridge))
    UUID getModelId() {
        model.id
    }

    @Override
    GormEntity getPathParent() {
        referenceDataModel
    }

    @Override
    def beforeValidate() {
        beforeValidateModelItem()
        this.referenceSummaryMetadata?.each {
            if (!it.createdBy) it.createdBy = createdBy
            it.catalogueItem = this
        }
        if (referenceDataType && !referenceDataType.ident()) {
            referenceDataType.referenceDataModel = model
            referenceDataType.createdBy = createdBy
            referenceDataType.beforeValidate()
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
        "${domainType}:${label}"
    }

    @Override
    ReferenceDataModel getModel() {
        referenceDataModel
    }

    @Override
    String getDiffIdentifier() {
        "${referenceDataModel.getDiffIdentifier()}/${label}"
    }

    @Override
    Boolean hasChildren() {
        false
    }

    ObjectDiff<ReferenceDataElement> diff(ReferenceDataElement otherDataElement) {
        catalogueItemDiffBuilder(ReferenceDataElement, this, otherDataElement)
            .appendString('referenceDataType.label', this.referenceDataType.label, otherDataElement.referenceDataType.label)
            .appendNumber('minMultiplicity', this.minMultiplicity, otherDataElement.minMultiplicity)
            .appendNumber('maxMultiplicity', this.maxMultiplicity, otherDataElement.maxMultiplicity)


    }

    static DetachedCriteria<ReferenceDataElement> byReferenceDataModelIdAndId(Serializable referenceDataModelId, Serializable resourceId) {
        byReferenceDataModelId(referenceDataModelId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ReferenceDataElement> byReferenceDataModelIdAndLabel(Serializable referenceDataModelId, String label) {
        byReferenceDataModelId(referenceDataModelId).eq('label', label)
    }

    static DetachedCriteria<ReferenceDataElement> byDataTypeIdAndId(Serializable dataTypeId, Serializable resourceId) {
        byDataTypeId(dataTypeId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ReferenceDataElement> byReferenceDataModelId(Serializable referenceDataModelId) {
        new DetachedCriteria<ReferenceDataElement>(ReferenceDataElement).eq('referenceDataModel.id', Utils.toUuid(referenceDataModelId))
    }

    static DetachedCriteria<ReferenceDataElement> byReferenceDataModel(ReferenceDataModel referenceDataModel) {
        new DetachedCriteria<ReferenceDataElement>(ReferenceDataElement).eq('referenceDataModel', referenceDataModel)
    }

    static DetachedCriteria<ReferenceDataElement> byDataTypeId(Serializable dataTypeId) {
        new DetachedCriteria<ReferenceDataElement>(ReferenceDataElement).eq('referenceDataType.id', Utils.toUuid(dataTypeId))
    }

    static DetachedCriteria<ReferenceDataElement> byDataType(ReferenceDataType dataType) {
        new DetachedCriteria<ReferenceDataElement>(ReferenceDataElement).eq('referenceDataType', dataType)
    }

    static DetachedCriteria<ReferenceDataElement> byMetadataNamespaceAndKeyAndValue(String namespace, String key, String value) {
        new DetachedCriteria<ReferenceDataElement>(ReferenceDataElement).in('id', Metadata.byNamespaceAndKeyAndValue(namespace, key, value).catalogueItemId)
    }

    static DetachedCriteria<ReferenceDataElement> byDataModelIdAndLabelIlike(Serializable dataModelId, String labelSearch) {
        byReferenceDataModelId(dataModelId).ilike('label', "%${labelSearch}%")
    }

    static DetachedCriteria<ReferenceDataElement> byClassifierId(Serializable classifierId) {
        where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }

    static DetachedCriteria<ReferenceDataElement> withFilter(DetachedCriteria<ReferenceDataElement> criteria, Map filters) {
        criteria = withCatalogueItemFilter(criteria, filters)
        if (filters.dataType) criteria = criteria.and {
            referenceDataType {
                ilike 'label', "%${filters.referenceDataType}%"
            }
        }
        criteria
    }


}