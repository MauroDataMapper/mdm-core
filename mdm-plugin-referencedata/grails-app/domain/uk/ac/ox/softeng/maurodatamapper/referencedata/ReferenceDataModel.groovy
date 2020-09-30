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
package uk.ac.ox.softeng.maurodatamapper.referencedata

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
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.DataType
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
class ReferenceDataModel implements Model<ReferenceDataModel> {

    UUID id

    Boolean hasChild

    static hasMany = [
        dataTypes      : DataType,
        dataElements   : DataElement,
        classifiers    : Classifier,
        metadata       : Metadata,
        semanticLinks  : SemanticLink,
        annotations    : Annotation,
        versionLinks   : VersionLink,
        referenceFiles : ReferenceFile
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        dataTypes validator: { val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'dataTypes').isValid(val) }
    }

    static mapping = {
        documentationVersion type: VersionUserType
        modelVersion type: VersionUserType
        folder cascade: 'none'
        dataTypes cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        metadata   : 'none',
        dataTypes  : 'referenceDataModel'
    ]

    static search = {
        CallableSearch.call(ReferenceModelSearch, delegate)
    }

    DataModel() {
        modelType = ReferenceDataModel.simpleName
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
        ReferenceDataModel.simpleName
    }

    ObjectDiff<ReferenceDataModel> diff(ReferenceDataModel otherDataModel) {
        modelDiffBuilder(ReferenceDataModel, this, otherDataModel)
            .appendList(DataType, 'dataTypes', this.dataTypes, otherDataModel.dataTypes)
            .appendList(DataType, 'dataElements', this.getAllDataElements(), otherDataModel.getAllDataElements())
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
        dataTypes?.each { it.beforeValidate() }
    }

    DataType findDataTypeByLabel(String label) {
        dataTypes?.find { it.label == label }
    }

    DataType findDataTypeByLabelAndType(String label, String type) {
        dataTypes?.find { it.domainType == type && it.label == label }
    }

    int countDataTypesByLabel(String label) {
        dataTypes?.count { it.label == label } ?: 0
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
        Errors deErrors = null
        Errors dtErrors = null
        boolean dmValid = true
        long start = null

        if (dataElements) {
            if (!fields || fields.contains('dataElements')) {
                start = System.currentTimeMillis()
                deErrors = validateDataElements()
                log.trace('DE validate {} (valid: {})', Utils.timeTaken(start), deErrors)
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
        Collection<DataElement> storedDataElements = []
        Collection<DataType> storedDataTypes = []
        if (dataElements) {
            storedDataElements.addAll(dataElements ?: [])
            dataElements?.clear()
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

        if (storedDataElements) {
            dataElements.addAll(storedDataElements)
        }
        if (storedDataTypes) {
            dataTypes.addAll(storedDataTypes)
        }

        log.trace('Final validate {} (valid: {})', Utils.timeTaken(start), dmValid)

        if (dtErrors) errors.addAllErrors(dtErrors)
        if (deErrors) errors.addAllErrors(deErrors)

        !hasErrors()
    }

    /**
     * There is an issue with abstract collections and mappingcontexts when validating, basically the DataType class has no mapping context validator
     * which means no validation occurs, therefore we have to do it manually.
     */
    protected Errors validateDataTypes() {
        def dataTypeErrors = new org.grails.datastore.mapping.validation.ValidationErrors(this)
        if (!dataTypes) return dataTypeErrors

        dataTypes.eachWithIndex { DataType dataType, int index ->
            if (!dataType.validate()) {
                for (FieldError error : dataType.getErrors().getFieldErrors()) {
                    String path = "dataTypes[$index].${error.getField()}"

                    Object[] args = new Object[Math.max(error.arguments.size(), 3)]
                    args[0] = path
                    args[1] = ReferenceDataModel
                    if (error.arguments.size() >= 2) {
                        System.arraycopy(error.arguments, 2, args, 2, error.arguments.size() - 2)
                    } else if (error.arguments.size() == 1) {
                        System.arraycopy(error.arguments, 0, args, 2, 1)
                    }
                    dataTypeErrors.rejectValue(path, error.code, args, error.defaultMessage)
                }
            }
        }
        List<String> duplicates = dataTypes.groupBy { it.label }.findAll { it.value.size() > 1 }.collect { it.key }
        if (duplicates) {
            Object[] args = new Object[5]
            args[0] = 'dataTypes'
            args[1] = ReferenceDataModel
            args[2] = ReferenceDataModel
            args[3] = duplicates.sort().join(',')
            args[4] = 'label'

            dataTypeErrors.
                rejectValue('dataTypes', 'invalid.unique.values.message', args, 'Property [{0}] has non-unique values [{3}] on property [{4}]')
            //return ['invalid.unique.values.message', duplicates.sort().join(','), collectionName]
        }

        dataTypeErrors
    }

    protected Errors validateDataElements() {
        def dataElementErrors = new ValidationErrors(this)
        if (!dataElements) return dataElementErrors

        dataElements.eachWithIndex { DataElement dataElement, int index ->
            if (!dataElement.validate()) {
                for (FieldError error : dataElement.getErrors().getFieldErrors()) {
                    String path = "dataElements[$index].${error.getField()}"

                    Object[] args = new Object[Math.max(error.arguments.size(), 3)]
                    args[0] = path
                    args[1] = ReferenceDataModel
                    if (error.arguments.size() >= 2) {
                        System.arraycopy(error.arguments, 2, args, 2, error.arguments.size() - 2)
                    } else if (error.arguments.size() == 1) {
                        System.arraycopy(error.arguments, 0, args, 2, 1)
                    }
                    dataElementErrors.rejectValue(path, error.code, args, error.defaultMessage)
                }
            }
        }


        List<String> duplicates = dataElements
            .findAll { !it.parentReferenceDataModel }
            .groupBy { it.label }
            .findAll { it.value.size() > 1 }
            .collect { it.key }

        if (duplicates) {
            Object[] args = new Object[5]
            args[0] = 'dataElements'
            args[1] = ReferenceDataModel
            args[2] = ReferenceDataModel
            args[3] = duplicates.sort().join(',')
            args[4] = 'label'
            dataElementErrors.rejectValue('childDataClasses', 'invalid.unique.values.message', args,
                                        'Property [{0}] has non-unique values [{3}] on property[{4}]')
        }
        dataElementErrors
    }

    private GormValidationApi currentGormValidationApi() {
        GormEnhancer.findValidationApi(ReferenceDataModel)
    }

    static DetachedCriteria<ReferenceDataModel> by() {
        new DetachedCriteria<ReferenceDataModel>(ReferenceDataModel)
    }

    static DetachedCriteria<ReferenceDataModel> byFolderId(UUID folderId) {
        by().eq('folder.id', folderId)
    }

    static DetachedCriteria<ReferenceDataModel> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<ReferenceDataModel> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        by().in('id', Metadata.byNamespaceAndKey(metadataNamespace, metadataKey).property('catalogueItem'))
    }

    static DetachedCriteria<ReferenceDataModel> byMetadataNamespace(String metadataNamespace) {
        by().in('id', Metadata.byNamespace(metadataNamespace).property('catalogueItem'))
    }

    static DetachedCriteria<ReferenceDataModel> byDeleted() {
        by().eq('deleted', true)
    }

    static DetachedCriteria<ReferenceDataModel> byIdInList(Collection<UUID> ids) {
        by().inList('id', ids.toList())
    }

    static DetachedCriteria<ReferenceDataModel> byLabel(String label) {
        by().eq('label', label)
    }

    static DetachedCriteria<ReferenceDataModel> byLabelAndFinalisedAndLatestModelVersion(String label) {
        byLabel(label).eq('finalised', true).order('modelVersion', 'desc')
    }

    static DetachedCriteria<ReferenceDataModel> byLabelAndNotFinalised(String label) {
        byLabel(label).eq('finalised', false)
    }

    static DetachedCriteria<ReferenceDataModel> byLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        byLabelAndNotFinalised(label).eq('branchName', branchName)
    }

    static DetachedCriteria<ReferenceDataModel> withFilter(DetachedCriteria<ReferenceDataModel> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }
}