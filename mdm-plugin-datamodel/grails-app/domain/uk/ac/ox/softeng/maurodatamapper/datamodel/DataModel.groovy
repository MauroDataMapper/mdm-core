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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator.DataModelDataClassCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.datamodel.hibernate.search.DataModelSearch
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import grails.validation.ValidationErrors
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormValidationApi
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataModel implements Model<DataModel>, SummaryMetadataAware {

    UUID id

    Boolean hasChild

    static hasMany = [
        dataClasses    : DataClass,
        dataTypes      : DataType,
        classifiers    : Classifier,
        metadata       : Metadata,
        annotations    : Annotation,
        semanticLinks  : SemanticLink,
        versionLinks   : VersionLink,
        referenceFiles : ReferenceFile,
        summaryMetadata: SummaryMetadata
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        dataTypes validator: {val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'dataTypes').isValid(val)}
        dataClasses validator: {val, obj -> new DataModelDataClassCollectionValidator(obj).isValid(val)}
    }

    static mapping = {
        documentationVersion type: VersionUserType
        modelVersion type: VersionUserType
        folder cascade: 'none'
        dataClasses cascade: 'all-delete-orphan'
        dataTypes cascade: 'all-delete-orphan'
        summaryMetadata cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        metadata   : 'none',
        dataClasses: 'dataModel',
        dataTypes  : 'dataModel'
    ]

    static search = {
        CallableSearch.call(DataModelSearch, delegate)
    }

    DataModel() {
        modelType = DataModelType.DATA_STANDARD.label
        documentationVersion = Version.from('1')
        finalised = false
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
        breadcrumbTree = new BreadcrumbTree(this)
        branchName = ModelConstraints.DEFAULT_BRANCH_NAME
    }

    @Override
    String getDomainType() {
        DataModel.simpleName
    }

    void setType(DataModelType type) {
        modelType = type.label
    }

    void setType(String type) {
        modelType = DataModelType.findForLabel(type)?.label
    }

    List<DataClass> getChildDataClasses() {
        dataClasses?.findAll { !it.parentDataClass }?.sort() ?: [] as List<DataClass>
    }

    ObjectDiff<DataModel> diff(DataModel otherDataModel) {
        modelDiffBuilder(DataModel, this, otherDataModel)
            .appendList(DataType, 'dataTypes', this.dataTypes, otherDataModel.dataTypes)
            .appendList(DataClass, 'dataClasses', this.dataClasses, otherDataModel.dataClasses)
            .appendList(DataType, 'dataElements', this.getAllDataElements(), otherDataModel.getAllDataElements())
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
        dataTypes?.each {it.beforeValidate()}
        dataClasses?.each {it.beforeValidate()}
    }

    DataType findDataTypeByLabel(String label) {
        dataTypes?.find {it.label == label}
    }

    DataType findDataTypeByLabelAndType(String label, String type) {
        dataTypes?.find {it.domainType == type && it.label == label}
    }

    int countDataTypesByLabel(String label) {
        dataTypes?.count { it.label == label } ?: 0
    }

    Set<DataElement> getAllDataElements() {
        dataClasses.collect { it.dataElements }.findAll().flatten().toSet() as Set<DataElement>
    }

    List<DataType> getSortedDataTypes() {
        dataTypes?.sort() ?: []
    }

    @Override
    String getEditLabel() {
        "${modelType}:${label}"
    }


    @Override
    boolean validate(Map arguments) {
        validate(arguments, [])
    }

    @Override
    boolean validate(List fields) {
        validate([:], fields)
    }

    @Override
    boolean validate() {
        validate([:], [])
    }

    boolean validate(Map args, List<String> fields) {

        if ((args || fields) && shouldSkipValidation()) return true
        Errors dcErrors = null
        Errors dtErrors = null
        boolean dmValid = true
        long start = System.currentTimeMillis()

        if (dataClasses) {
            if (!fields || fields.contains('dataClasses') || fields.contains('childDataClasses')) {
                start = System.currentTimeMillis()
                dcErrors = validateDataClasses()
                log.trace('DC validate {} (valid: {})', Utils.timeTaken(start), dcErrors)
            }
        }

        if (dataTypes) {
            if (!fields || fields.contains('dataTypes')) {
                start = System.currentTimeMillis()
                dtErrors = validateDataTypes()
                log.trace('DT validate {} (valid: {})', Utils.timeTaken(start), dtErrors)
            }
        }

        start = System.currentTimeMillis()
        Collection<DataClass> storedDataClasses = []
        Collection<DataType> storedDataTypes = []
        if (dataClasses) {
            storedDataClasses.addAll(dataClasses ?: [])
            dataClasses?.clear()
        }
        if (dataTypes) {
            storedDataTypes.addAll(dataTypes ?: [])
            dataTypes?.clear()
        }

        if (args) {
            dmValid = currentGormValidationApi().validate(this, args)
        } else if (fields) {
            dmValid = currentGormValidationApi().validate(this, fields)
        } else {
            dmValid = currentGormValidationApi().validate(this)
        }

        if (storedDataClasses) {
            dataClasses.addAll(storedDataClasses)
        }
        if (storedDataTypes) {
            dataTypes.addAll(storedDataTypes)
        }

        log.trace('Final validate {} (valid: {})', Utils.timeTaken(start), dmValid)

        if (dtErrors) errors.addAllErrors(dtErrors)
        if (dcErrors) errors.addAllErrors(dcErrors)

        !hasErrors()
    }

    /**
     * There is an issue with abstract collections and mappingcontexts when validating, basically the DataType class has no mapping context validator
     * which means no validation occurs, therefore we have to do it manually.
     */
    protected Errors validateDataTypes() {
        def dataTypeErrors = new org.grails.datastore.mapping.validation.ValidationErrors(this)
        if (!dataTypes) return dataTypeErrors

        dataTypes.eachWithIndex {DataType dataType, int index ->
            if (!dataType.validate()) {
                for (FieldError error : dataType.getErrors().getFieldErrors()) {
                    String path = "dataTypes[$index].${error.getField()}"

                    Object[] args = new Object[Math.max(error.arguments.size(), 3)]
                    args[0] = path
                    args[1] = DataModel
                    if (error.arguments.size() >= 2) {
                        System.arraycopy(error.arguments, 2, args, 2, error.arguments.size() - 2)
                    } else if (error.arguments.size() == 1) {
                        System.arraycopy(error.arguments, 0, args, 2, 1)
                    }
                    dataTypeErrors.rejectValue(path, error.code, args, error.defaultMessage)
                }
            }
        }
        List<String> duplicates = dataTypes.groupBy {it.label}.findAll {it.value.size() > 1}.collect {it.key}
        if (duplicates) {
            Object[] args = new Object[5]
            args[0] = 'dataTypes'
            args[1] = DataModel
            args[2] = DataModel
            args[3] = duplicates.sort().join(',')
            args[4] = 'label'

            dataTypeErrors.
                rejectValue('dataTypes', 'invalid.unique.values.message', args, 'Property [{0}] has non-unique values [{3}] on property [{4}]')
            //return ['invalid.unique.values.message', duplicates.sort().join(','), collectionName]
        }

        dataTypeErrors
    }

    /**
     * Standard validation will validate dataClasses field which will iterate through all the dataClasses and all their child dataclasses therefore
     * duplicating validation. This method just calls the validation on each child dataclass which will cascade validation down to their children
     **/
    protected Errors validateDataClasses() {
        def dataClassErrors = new ValidationErrors(this)
        if (!childDataClasses) return dataClassErrors

        childDataClasses.eachWithIndex {DataClass dataClass, int index ->
            if (!dataClass.validate()) {
                for (FieldError error : dataClass.getErrors().getFieldErrors()) {
                    String path = "childDataClasses[$index].${error.getField()}"

                    Object[] args = new Object[Math.max(error.arguments.size(), 3)]
                    args[0] = path
                    args[1] = DataModel
                    if (error.arguments.size() >= 2) {
                        System.arraycopy(error.arguments, 2, args, 2, error.arguments.size() - 2)
                    } else if (error.arguments.size() == 1) {
                        System.arraycopy(error.arguments, 0, args, 2, 1)
                    }
                    dataClassErrors.rejectValue(path, error.code, args, error.defaultMessage)
                }
            }
        }
        List<String> duplicates = childDataClasses
            .findAll {!it.parentDataClass}
            .groupBy {it.label}
            .findAll {it.value.size() > 1}
            .collect {it.key}

        if (duplicates) {
            Object[] args = new Object[5]
            args[0] = 'childDataClasses'
            args[1] = DataModel
            args[2] = DataModel
            args[3] = duplicates.sort().join(',')
            args[4] = 'label'
            dataClassErrors.rejectValue('childDataClasses', 'invalid.unique.values.message', args,
                                        'Property [{0}] has non-unique values [{3}] on property[{4}]')
        }
        dataClassErrors
    }

    private GormValidationApi currentGormValidationApi() {
        GormEnhancer.findValidationApi(DataModel)
    }

    static DetachedCriteria<DataModel> by() {
        new DetachedCriteria<DataModel>(DataModel)
    }

    static DetachedCriteria<DataModel> byFolderId(UUID folderId) {
        by().eq('folder.id', folderId)
    }

    static DetachedCriteria<DataModel> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<DataModel> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        by().in('id', Metadata.byNamespaceAndKey(metadataNamespace, metadataKey).property('catalogueItem'))
    }

    static DetachedCriteria<DataModel> byMetadataNamespace(String metadataNamespace) {
        by().in('id', Metadata.byNamespace(metadataNamespace).property('catalogueItem'))
    }

    static DetachedCriteria<DataModel> byDeleted() {
        by().eq('deleted', true)
    }

    static DetachedCriteria<DataModel> byIdInList(Collection<UUID> ids) {
        by().inList('id', ids.toList())
    }

    static DetachedCriteria<DataModel> withFilter(DetachedCriteria<DataModel> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }
}