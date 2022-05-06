/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.SearchService
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DataClassController extends CatalogueItemController<DataClass> {

    static responseFormats = ['json', 'xml']

    DataClassService dataClassService

    DataModelService dataModelService

    DataElementService dataElementService

    @Autowired
    SearchService mdmPluginDataModelSearchService

    DataClassController() {
        super(DataClass)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, [
            model: [
                owningDataModelId        : params.dataModelId,
                owningDataClassId        : params.dataClassId,
                userSecurityPolicyManager: currentUserSecurityPolicyManager,
            ],
            view : 'index'
        ]
    }

    def all() {
        params.all = true
        index()
    }

    def content(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        params.sort = params.sort ?: 'label'

        respond content: dataClassService.findAllContentOfDataClassIdInDataModelId(params.dataModelId, params.dataClassId, params)
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        if (!dataClassService.existsWhereRootDataClassOfDataModelIdAndId(params.dataModelId, params.dataClassId)) {
            return notFound(params.dataClassId)
        }

        searchParams.crossValuesIntoParametersMap(params, 'label')

        PaginatedHibernateSearchResult<ModelItem> result = mdmPluginDataModelSearchService.findAllByDataClassIdByHibernateSearch(params.dataClassId,
                                                                                                                                 searchParams,
                                                                                                                                 params)
        respond result
    }

    @Transactional
    def copyDataClass(CopyInformation copyInformation) {

        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataClass original = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)

        if (!original) return notFound(params.otherDataClassId)

        DataClass copy
        try {
            copy = dataClassService.copyDataClassMatchingAllReferenceTypes(dataModel, original, currentUser, currentUserSecurityPolicyManager,
                                                                           params.dataClassId, copyInformation)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        if (!validateResource(copy, 'create')) return

        // Validate the whole datamodel as copying may add new bits
        dataModelService.validate(dataModel)
        if (dataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    @Transactional
    def extendDataClass() {
        if (handleReadOnly()) {
            return
        }

        DataClass instance = dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)
        if (!instance) return notFound(params.dataClassId)

        DataClass dataClassToExtend = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)
        if (!dataClassToExtend) return notFound(params.otherDataClassId)


        if (request.method == 'PUT') {
            if (!dataClassService.isExtendableDataClassInSameModelOrInFinalisedModel(dataClassToExtend, instance)) {
                instance.errors.reject('invalid.extended.dataclass.model.not.finalised',
                                       [params.otherDataClassId].toArray(),
                                       'DataClass [{0}] to be extended does not belong to a finalised DataModel')
                respond instance.errors, view: 'update' // STATUS CODE 422
                return false
            }
            log.debug('Extending DataClass {} from {}', params.dataClassId, params.otherDataClassId)
            instance.addToExtendedDataClasses(dataClassToExtend)
        } else {
            log.debug('Removing extension of DataClass {} from {}', params.dataClassId, params.otherDataClassId)
            instance.removeFromExtendedDataClasses(dataClassToExtend)
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def importDataClass() {
        if (handleReadOnly()) {
            return
        }

        DataClass instance = dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)
        if (!instance) return notFound(params.dataClassId)

        DataClass dataClassToBeImported = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)
        if (!dataClassToBeImported) return notFound(DataClass, params.otherDataClassId)

        if (request.method == 'PUT') {
            if (dataClassService.validateImportAddition(instance, dataClassToBeImported)) {
                log.debug('Importing DataClass {} from {}', params.otherDataClassId, params.otherDataModelId)
                instance.addToImportedDataClasses(dataClassToBeImported)
            }
        } else {
            if (dataClassService.validateImportRemoval(instance, dataClassToBeImported)) {
                log.debug('Removing import of DataClass {} from {}', params.otherDataClassId, params.otherDataModelId)
                instance.removeFromImportedDataClasses(dataClassToBeImported)
            }
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def importDataElement() {
        if (handleReadOnly()) {
            return
        }

        DataClass instance = dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)
        if (!instance) return notFound(params.dataClassId)

        DataElement dataElementToBeImported = dataElementService.findByDataClassIdAndId(params.otherDataClassId, params.otherDataElementId)
        if (!dataElementToBeImported) return notFound(DataClass, params.otherDataClassId)

        if (request.method == 'PUT') {
            if (dataClassService.validateImportAddition(instance, dataElementToBeImported)) {
                log.debug('Importing DataElement {} from {}', params.otherDataElementId, params.otherDataClassId)
                instance.addToImportedDataElements(dataElementToBeImported)
            }
        } else {
            if (dataClassService.validateImportRemoval(instance, dataElementToBeImported)) {
                log.debug('Removing import of DataElement {} from {}', params.otherDataElementId, params.otherDataClassId)
                instance.removeFromImportedDataElements(dataElementToBeImported)
            }
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    protected DataClass queryForResource(Serializable resourceId) {
        if (params.dataClassId) {
            return dataClassService.findByDataModelIdAndParentDataClassIdAndId(params.dataModelId, params.dataClassId, resourceId)
        }
        return dataClassService.findWhereRootDataClassOfDataModelIdAndId(params.dataModelId, resourceId)
    }

    @Override
    protected List<DataClass> listAllReadableResources(Map params) {
        params.sort = params.sort ?: ['label': 'asc']
        // Cannot sort DCs including imported using idx combined with any other field
        if (params.sort instanceof Map && (params.sort as Map).size() > 1) (params.sort as Map).remove('idx')
        if (params.dataClassId) {
            if (!dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)) {
                notFound(params.dataClassId)
                return null
            }
            return dataClassService.findAllByDataModelIdAndParentDataClassIdIncludingImported(params.dataModelId, params.dataClassId, params)
        }
        if (((GrailsParameterMap) params).boolean('all', false)) {
            if (params.search) {
                return dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(params.dataModelId,
                                                                                                             params.search,
                                                                                                             params)
            }
            return dataClassService.findAllByDataModelIdIncludingImported(params.dataModelId, params)
        }
        return dataClassService.findAllWhereRootDataClassOfDataModelIdIncludingImported(params.dataModelId, params)
    }

    @Override
    protected void serviceDeleteResource(DataClass resource) {
        dataClassService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataClass resource) {
        dataClassService.save([flush        : true, validate: false, insert: actionName == 'copyDataClass',
                               deepSave     : actionName == 'copyDataClass',
                               saveDataTypes: actionName == 'copyDataClass'],
                              resource)
    }

    @Override
    protected DataClass createResource() {
        DataClass resource = super.createResource() as DataClass
        if (params.dataClassId) {
            dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)?.addToDataClasses(resource)
        }
        dataModelService.get(params.dataModelId)?.addToDataClasses(resource)

        resource
    }

    @Override
    @Transactional
    protected boolean validateResourceDeletion(DataClass resource, String view) {
        if (dataClassService.isDataClassBeingUsedAsExtension(resource)) {
            transactionStatus.setRollbackOnly()
            resource.errors.reject('invalid.deletion.dataclass.used.as.extension',
                                   [resource.id].toArray(),
                                   'Cannot delete DataClass [{0}] as it is still used as an extension in another DataClass')
            respond resource.errors, view: view // STATUS CODE 422
            return false
        }

        if (dataClassService.isDataClassBeingUsedAsImport(resource)) {
            transactionStatus.setRollbackOnly()
            resource.errors.reject('invalid.deletion.modelitem.used.as.import',
                                   ['DataClass', resource.id].toArray(),
                                   'Cannot delete {0} [{1}] as it is still used as an import')
            respond resource.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
