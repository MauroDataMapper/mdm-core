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
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.gorm.GormEntity

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceDataValue implements ModelItem<ReferenceDataValue, ReferenceDataModel> {

    UUID id

    int rowNumber
    String value

    static belongsTo = [referenceDataModel: ReferenceDataModel, referenceDataElement: ReferenceDataElement]

    static transients = ['aliases']

    static hasMany = [
        classifiers   : Classifier,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        referenceFiles: ReferenceFile,
    ]    

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        value blank: true, nullable: true
    }

    static mapping = {
        rowNumber type: 'integer'
        value type: 'text'
        referenceDataModel index: 'reference_data_value_reference_data_model_idx'
        referenceDataElement index: 'reference_data_value_reference_data_element_idx', cascade: 'save-update', fetch: 'join'
        model cascade: 'none'
    }  

    static mappedBy = [:]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
    }

    ReferenceDataValue() {
    }

    @Override
    String getDomainType() {
        ReferenceDataValue.simpleName
    }

    @Override
    GormEntity getPathParent() {
        referenceDataModel
    }

    @Override
    def beforeValidate() {
        buildLabel()
        description = value     
        beforeValidateModelItem()
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
        this.label
    }

    @Override
    Boolean hasChildren() {
        false
    }

    ObjectDiff<ReferenceDataValue> diff(ReferenceDataValue otherValue) {
        catalogueItemDiffBuilder(ReferenceDataValue, this, otherValue)
            .appendInteger('rowNumber', this.rowNumber, otherValue.rowNumber)
            .appendString('value', this.value, otherValue.value)
    }

    void setValue(String value) {
        this.value = value
        this.description = value
    }

    private String buildLabel() {
        this.label = "Model: ${this.referenceDataModel.label}, Row: ${this.rowNumber}, Element: ${this.referenceDataElement.label}"
    }

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelId(Serializable referenceDataModelId) {
        new DetachedCriteria<ReferenceDataValue>(ReferenceDataValue).eq('referenceDataModel.id', Utils.toUuid(referenceDataModelId))
    }

    /*static DetachedCriteria<ReferenceEnumerationValue> byClassifierId(Serializable classifierId) {
        where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }*/

    static DetachedCriteria<ReferenceDataValue> withFilter(DetachedCriteria<ReferenceDataValue> criteria, Map filters) {
        criteria = withCatalogueItemFilter(criteria, filters)
        criteria
    }    

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelIdAndRowNumber(Serializable referenceDataModelId, Integer fromRowNumber, Integer toRowNumber) {
        new DetachedCriteria<ReferenceDataValue>(ReferenceDataValue)
        .eq('referenceDataModel.id', Utils.toUuid(referenceDataModelId))
        .ge('rowNumber', fromRowNumber)
        .lt('rowNumber', toRowNumber)
    }

    static DetachedCriteria<ReferenceDataValue> countByReferenceDataModelId(Serializable referenceDataModelId) {
        new DetachedCriteria<ReferenceDataValue>(ReferenceDataValue)
        .eq('referenceDataModel.id', Utils.toUuid(referenceDataModelId))
        .projections {
            countDistinct("rowNumber")
        }
    }
}
