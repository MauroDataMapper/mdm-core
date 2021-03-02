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
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelExtend
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.ModelExtendAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.ModelImportAware
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.IndexedSiblingAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataClassLabelValidator
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
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Index
import org.hibernate.search.bridge.builtin.UUIDBridge

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataClass implements ModelItem<DataClass, DataModel>, MultiplicityAware, SummaryMetadataAware, IndexedSiblingAware, ModelImportAware, ModelExtendAware {

    public final static Integer BATCH_SIZE = 1000

    UUID id
    DataClass parentDataClass
    DataModel dataModel

    static hasMany = [
        classifiers    : Classifier,
        metadata       : Metadata,
        annotations    : Annotation,
        semanticLinks  : SemanticLink,
        referenceFiles : ReferenceFile,
        dataClasses    : DataClass,
        dataElements   : DataElement,
        referenceTypes : ReferenceType,
        summaryMetadata: SummaryMetadata,
        rules          : Rule,
        modelImports   : ModelImport,
        modelExtends   : ModelExtend
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
        summaryMetadata cascade: 'all-delete-orphan'
        dataModel index: 'data_class_data_model_idx' //, cascade: 'none', cascadeValidate: 'none'
        parentDataClass index: 'data_class_parent_data_class_idx', cascade: 'save-update'
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
            it.catalogueItem = this
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

    ObjectDiff<DataClass> diff(DataClass otherDataClass) {
        catalogueItemDiffBuilder(DataClass, this, otherDataClass)
            .appendNumber('minMultiplicity', this.minMultiplicity, otherDataClass.minMultiplicity)
            .appendNumber('maxMultiplicity', this.maxMultiplicity, otherDataClass.maxMultiplicity)
            .appendList(DataClass, 'dataClasses', this.dataClasses, otherDataClass.dataClasses)
            .appendList(DataElement, 'dataElements', this.dataElements, otherDataClass.dataElements)

    }

    @Override
    String getDiffIdentifier() {
        if (!parentDataClass) return this.label
        parentDataClass.getDiffIdentifier() + "/" + this.label
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
        this.dataClasses.find { it.label == label }
    }

    DataElement findDataElement(String label) {
        dataElements.find { it.label == label }
    }

    int countDataElementsByLabel(String label) {
        dataElements?.count { it.label == label } ?: 0
    }

    Set<DataClass> getAllUnsavedChildren() {
        Set<DataClass> children = dataClasses.findAll { !it.id }
        children + children.collectMany { it.getAllUnsavedChildren() }
    }

    static String buildLabelPath(DataClass dataClass) {
        if (!dataClass.parentDataClass) return dataClass.label
        "${buildLabelPath(dataClass.parentDataClass)}|${dataClass.label}"
    }

    /**
     * Where the DataClass has been imported into the specified DataModel. Use this method if you want only those
     * DataClasses which have been imported into a DataModel i.e. excluding DataClasses owned directly by the DataModel.
     *
     * @param ID of DataModel which has imported other catalogue items
     * @return DetachedCriteria<DataClass> for id in IDs of catalogue items which have been imported into the DataModel
     */
    static DetachedCriteria<DataClass> importedByDataModelId(Serializable dataModelId) {
        new DetachedCriteria<DataClass>(DataClass)
        .in('id', ModelImport.importedByCatalogueItemId(dataModelId))
    }

    /**
     * If we want to include imported DataClasses then do a logical OR on imported and directly owned DataClasses.
     *
     * @param dataModelId The ID of the DataModel we are looking at
     * @param includeImported Do we want to retrieve DataClasses which have been imported into the DataModel (in
     *                        addition to DataClasses directly belonging to the DataModel)?
     * @return DetachedCriteria<DataClass>
     */
    static DetachedCriteria<DataClass> byDataModelId(Serializable dataModelId, boolean includeImported = false) {
        DetachedCriteria criteria = new DetachedCriteria<DataClass>(DataClass)

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
     * If we want to include imported DataClasses then do a logical OR on imported and directly owned DataClasses.
     *
     * @param dataClassId The ID of the DataClass we are looking at
     * @param includeImported Do we want to retrieve DataClasses which have been imported into the DataClass (in
     *                        addition to DataClasses directly belonging to the DataClass)?
     * @return DetachedCriteria<DataClass>
     */
    static DetachedCriteria<DataClass> byDataModelIdAndParentDataClassId(UUID dataModelId, UUID dataClassId, boolean includeImported = false, boolean includeExtends = false) {
        DetachedCriteria criteria = new DetachedCriteria<DataClass>(DataClass)

        criteria
        .or {
            if (!includeImported) {
                criteria.eq('dataModel.id', Utils.toUuid(dataModelId))
            }
        }
        .or {
            //DataClasses whose parent DataClass is dataClassId
            criteria.eq('parentDataClass.id', dataClassId)

            //DataClasses which have been imported into dataClassId
            if (includeImported) {
                inList('id', ModelImport.importedByCatalogueItemId(dataClassId))
            }

            //DataClasses whose parent is extended by dataClassId
            if (includeExtends) {
                inList('parentDataClass.id', ModelExtend.extendedByCatalogueItemId(dataClassId))
            }
        }

        criteria
    }

    static DetachedCriteria<DataClass> byRootDataClassOfDataModelId(UUID dataModelId, boolean includeImported = false) {
        byDataModelId(dataModelId, includeImported).isNull('parentDataClass')
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

    /**
     * Get DataClasses for a DataModel by the DataModel id and a search string, including imported data classes if required.
     *
     * @param dataModelId The ID of the DataModel we are looking at
     * @param searchTerm Search string which is applied against the label and description of the DataClass
     * @param includeImported Do we want to retrieve DataClasses which have been imported into the DataModel (in
     *                        addition to DataClasses directly belonging to the DataModel)?
     * @return DetachedCriteria<DataClass>
     */
    static DetachedCriteria<DataClass> byDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable dataModelId, String searchTerm, boolean includeImported = false) {
        byDataModelId(dataModelId, includeImported).or {
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

}