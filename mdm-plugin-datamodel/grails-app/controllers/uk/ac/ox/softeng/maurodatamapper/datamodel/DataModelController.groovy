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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DeleteAllParams
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class DataModelController extends ModelController<DataModel> {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [
        export                       : 'GET', tree: 'GET', types: 'GET', finalise: 'PUT',
        createNewDocumentationVersion: 'PUT', createNewVersion: 'PUT', modelVersionTree: 'GET'
    ]

    DataModelService dataModelService

    DataTypeService dataTypeService

    DataClassService dataClassService

    @Autowired
    SearchService mdmPluginDataModelSearchService

    @Autowired(required = false)
    Set<DataModelExporterProviderService> exporterProviderServices

    @Autowired(required = false)
    Set<DataModelImporterProviderService> importerProviderServices

    DataModelController() {
        super(DataModel, 'dataModelId')
    }

    def types() {
        respond DataModelType.labels()
    }

    def defaultDataTypeProviders() {
        respond providers: dataModelService.defaultDataTypeProviders
    }

    def hierarchy() {
        params.deep = true
        def resource = queryForResource(params.dataModelId)
        resource ? respond(resource, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'hierarchy']) : notFound(params.id)
    }

    @Override
    protected ModelService<DataModel> getModelService() {
        dataModelService
    }

    @Transactional
    def deleteAllUnusedDataClasses() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = queryForResource params.dataModelId

        if (!dataModel) return notFound(params.dataModelId)

        dataModelService.deleteAllUnusedDataClasses(dataModel)

        render status: NO_CONTENT // NO CONTENT STATUS CODE
    }

    @Transactional
    def deleteAllUnusedDataTypes() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = queryForResource params.dataModelId

        if (!dataModel) return notFound(params.dataModelId)

        dataModelService.deleteAllUnusedDataTypes(dataModel)

        render status: NO_CONTENT // NO CONTENT STATUS CODE
    }

    @Transactional
    def deleteAll() {
        // Command object binding is not performed when the HTTP method id DELETE
        DeleteAllParams deleteAllParams = new DeleteAllParams()
        bindData deleteAllParams, request.inputStream
        deleteAllParams.validate()

        if (deleteAllParams.hasErrors()) {
            respond deleteAllParams.errors, status: UNPROCESSABLE_ENTITY
            return
        }

        List<DataModel> deleted = dataModelService.deleteAll(deleteAllParams.ids, deleteAllParams.permanent)

        if (deleted) {
            deleted.each {
                updateResource(it)
            }
            respond deleted, status: HttpStatus.OK, view: 'index'
        } else {
            render status: NO_CONTENT
        }
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        params.max = params.max ?: searchParams.max ?: 10
        params.offset = params.offset ?: searchParams.offset ?: 0
        params.sort = params.sort ?: searchParams.sort ?: 'label'
        if (searchParams.order) {
            params.order = searchParams.order
        }

        PaginatedHibernateSearchResult<ModelItem> result = mdmPluginDataModelSearchService.findAllByDataModelIdByHibernateSearch(params.dataModelId,
                                                                                                                                 searchParams, params)
        respond result
    }

    def suggestLinks() {
        DataModel dataModel = queryForResource params.dataModelId
        DataModel otherDataModel = queryForResource params.otherDataModelId

        if (!dataModel) return notFound(params.dataModelId)
        if (!otherDataModel) return notFound(params.otherDataModelId)

        int maxResults = params.max ?: 5

        respond dataModelService.suggestLinksBetweenModels(dataModel, otherDataModel, maxResults)
    }

    @Transactional
    def importDataType() {
        if (handleReadOnly()) {
            return
        }
        log.debug('Importing DataType')
        DataModel instance = dataModelService.get(params.dataModelId)
        if (!instance) return notFound(params.dataModelId)

        DataType dataTypeToBeImported = dataTypeService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataTypeId)
        if (!dataTypeToBeImported) return notFound(DataType, params.otherDataTypeId)

        if (request.method == 'PUT') {
            if (!validateImportAddition(instance, dataTypeToBeImported)) return
            log.debug('Importing DataType {} from {}', params.otherDataTypeId, params.otherDataModelId)
            instance.addToImportedDataTypes(dataTypeToBeImported)
        } else {
            if (!validateImportRemoval(instance, dataTypeToBeImported)) return
            log.debug('Removing import of DataType {} from {}', params.otherDataTypeId, params.otherDataModelId)
            instance.removeFromImportedDataTypes(dataTypeToBeImported)
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
        log.debug('Importing DataClass')
        DataModel instance = dataModelService.get(params.dataModelId)
        if (!instance) return notFound(params.dataModelId)

        DataClass dataClassToBeImported = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)
        if (!dataClassToBeImported) return notFound(DataClass, params.otherDataClassId)

        if (request.method == 'PUT') {
            if (!validateImportAddition(instance, dataClassToBeImported)) return
            log.debug('Importing DataClass {} from {}', params.otherDataClassId, params.otherDataModelId)
            instance.addToImportedDataClasses(dataClassToBeImported)
        } else {
            if (!validateImportRemoval(instance, dataClassToBeImported)) return
            log.debug('Removing import of DataClass {} from {}', params.otherDataClassId, params.otherDataModelId)
            instance.removeFromImportedDataClasses(dataClassToBeImported)
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    protected DataModel createResource() {
        DataModel model = super.createResource() as DataModel
        dataModelService.checkForAndAddDefaultDataTypes(model, params.defaultDataTypeProvider)
        model
    }

    @Override
    protected DataModel performAdditionalUpdates(DataModel model) {
        dataModelService.checkForAndAddDefaultDataTypes model, params.defaultDataTypeProvider
    }

    protected boolean validateImportAddition(DataModel instance, ModelItem importingItem) {
        if (importingItem.model.id == instance.id) {
            instance.errors.reject('invalid.imported.modelitem.same.datamodel',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] to be imported belongs to the DataModel already')
            respond instance.errors, view: 'update' // STATUS CODE 422
            return false
        }
        if (!importingItem.model.finalised) {
            instance.errors.reject('invalid.imported.modelitem.model.not.finalised',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] to be imported does not belong to a finalised DataModel')
            respond instance.errors, view: 'update' // STATUS CODE 422
            return false
        }
        return true
    }

    protected boolean validateImportRemoval(DataModel instance, ModelItem importingItem) {
        if (importingItem.model.id == instance.id) {
            instance.errors.reject('invalid.imported.deletion.modelitem.same.datamodel',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] belongs to the DataModel and cannot be removed as an import')
            respond instance.errors, view: 'update' // STATUS CODE 422
            return false
        }
        return true
    }
}
