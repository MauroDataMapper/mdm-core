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


import uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DeleteAllParams
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult

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
        createNewDocumentationVersion: 'PUT', createNewVersion: 'PUT'
    ]

    DataModelService dataModelService
    SearchService mdmPluginDataModelSearchService

    @Autowired
    Set<DefaultDataTypeProvider> defaultDataTypeProviders

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
        respond providers: defaultDataTypeProviders
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
    @Override
    def update() {
        log.trace('Update')
        if (handleReadOnly()) return

        DataModel instance = queryForResource(params.id)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (instance.finalised) return forbidden('Cannot update a finalised DataModel')

        instance.properties = getObjectToBind()

        instance = checkForAndAddDefaultDataTypes instance

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
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

        PaginatedLuceneResult<ModelItem> result = mdmPluginDataModelSearchService.findAllByDataModelIdByLuceneSearch(params.dataModelId,
                                                                                                                     searchParams, params)
        respond result
    }

    def suggestLinks() {
        DataModel dataModel = queryForResource params.dataModelId
        DataModel otherDataModel = queryForResource params.otherModelId

        if (!dataModel) return notFound(params.dataModelId)
        if (!otherDataModel) return notFound(params.otherModelId)

        int maxResults = params.max ?: 5

        respond dataModelService.suggestLinksBetweenModels(dataModel, otherDataModel, maxResults)
    }

    protected DataModel checkForAndAddDefaultDataTypes(DataModel resource) {
        if (params.defaultDataTypeProvider) {
            DefaultDataTypeProvider provider = defaultDataTypeProviders.find {it.displayName == params.defaultDataTypeProvider}
            if (provider) {
                log.debug("Adding ${provider.displayName} default DataTypes")
                return provider.addDefaultListOfDataTypesToDataModel(resource)
            }
        }
        resource
    }

    /*
        def getProfile() {
            DataModel dataModel = queryForResource params.dataModelId
            System.err.println("Getting dataModel profile: " + dataModel.label)
            Profile p
            if(params.profileName == "dcat") {
                p = new DcatProfile()
            }
            else {
                return errorResponse(UNPROCESSABLE_ENTITY, 'No profile matching that name')
            }
            Object t = p.getProfile(dataModel)
            render contentType: p.getContentType(), text: p.renderProfile(t)
        }

        def setProfile() {
            DataModel dataModel = queryForResource params.dataModelId
            System.err.println("Setting dataModel profile: " + dataModel.label)
            Profile p
            if(params.profileName == "dcat") {
                p = new DcatProfile()
            }
            else {
                return errorResponse(UNPROCESSABLE_ENTITY, 'No profile matching that name')
            }
            System.err.println(request.getJSON())

            def t = p.readProfileFromBody(request.getJSON())

            dataModelService.setProfile((Dcat) t, (DcatProfile) p, dataModel, currentUser)

            render contentType: p.getContentType(), text: p.renderProfile(t)

        }*/


}
