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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataClassLabelValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.hibernate.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.domain.MultiplicityAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Index
import org.hibernate.search.bridge.builtin.UUIDBridge

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataClass implements ModelItem<DataClass, DataModel>, MultiplicityAware {

    public final static Integer BATCH_SIZE = 1000

    UUID id
    DataClass parentDataClass
    DataModel dataModel

    static hasMany = [
        classifiers   : Classifier,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        referenceFiles: ReferenceFile,
        dataClasses   : DataClass,
        dataElements  : DataElement,
        referenceTypes: ReferenceType,
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
    }

    static mapping = {
        dataElements cascade: 'all-delete-orphan'
        dataClasses cascade: 'all-delete-orphan'
        referenceTypes cascade: 'none'
        semanticLinks cascade: 'all-delete-orphan'
        dataModel index: 'data_class_data_model_idx', cascade: 'none'
        parentDataClass index: 'data_class_parent_data_class_idx', cascade: 'save-update'
        breadcrumbTree cascade: 'all-delete-orphan', fetch: 'join'
    }

    static mappedBy = [
        dataClasses   : 'parentDataClass',
        referenceTypes: 'referenceClass',
        dataElements  : 'dataClass',
    ]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
    }

    DataClass() {
    }

    @Override
    String getDomainType() {
        DataClass.simpleName
    }


    @Field(index = Index.YES, bridge = @FieldBridge(impl = UUIDBridge))
    UUID getModelId() {
        dataModel.id
    }

    @Override
    GormEntity getPathParent() {
        parentDataClass ?: dataModel
    }

    @Override
    Boolean hasChildren() {
        this.dataClasses == null ? false : !this.dataClasses.isEmpty()
    }

    @Override
    def beforeValidate() {
        dataModel = dataModel ?: parentDataClass?.getModel()
        beforeValidateModelItem()
        this.dataClasses.each {it.beforeValidate()}
        dataElements.each {it.beforeValidate()}
        referenceTypes.each {it.beforeValidate()}
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

    ObjectDiff<DataClass> diff(DataClass otherDataClass) {
        catalogueItemDiffBuilder(DataClass, this, otherDataClass)
            .appendNumber('minMultiplicity', this.minMultiplicity, otherDataClass.minMultiplicity)
            .appendNumber('maxMultiplicity', this.maxMultiplicity, otherDataClass.maxMultiplicity)
            .appendList(DataClass, 'childDataClasses', this.dataClasses, otherDataClass.dataClasses)
            .appendList(DataElement, "dataElements", this.dataElements, otherDataClass.dataElements)

    }

    @Override
    String getDiffIdentifier() {
        if (!parentDataClass) return this.label
        parentDataClass.getDiffIdentifier() + "/" + this.label
    }

    CatalogueItem getParent() {
        parentDataClass ?: dataModel
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

    static String buildLabelPath(DataClass dataClass) {
        if (!dataClass.parentDataClass) return dataClass.label
        "${buildLabelPath(dataClass.parentDataClass)}|${dataClass.label}"
    }


    static DetachedCriteria<DataClass> byDataModelId(UUID dataModelId) {
        new DetachedCriteria<DataClass>(DataClass).eq('dataModel.id', dataModelId)
    }

    static DetachedCriteria<DataClass> byParentDataClassId(UUID dataClassId) {
        new DetachedCriteria<DataClass>(DataClass).eq('parentDataClass.id', dataClassId)
    }

    static DetachedCriteria<DataClass> byRootDataClassOfDataModelId(UUID dataModelId) {
        byDataModelId(dataModelId).isNull('parentDataClass')
    }

    static DetachedCriteria<DataClass> byChildOfDataClassId(UUID dataClassId) {
        DetachedCriteria<DataClass> criteria = new DetachedCriteria<>(DataClass)
        criteria.or {
            inList('id', byParentDataClassId(dataClassId).id())
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
}