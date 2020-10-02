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

import uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter.ReferenceDataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.ReferenceDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.rest.transport.DeleteAllParams
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class ReferenceDataModelController extends ModelController<ReferenceDataModel> {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [
        export                       : 'GET', tree: 'GET', types: 'GET', finalise: 'PUT',
        createNewDocumentationVersion: 'PUT', createNewVersion: 'PUT'
    ]

    ReferenceDataModelService referenceDataModelService

    @Autowired
    SearchService mdmPluginReferenceDataModelSearchService

    @Autowired(required = false)
    Set<ReferenceDataModelExporterProviderService> exporterProviderServices

    @Autowired(required = false)
    Set<ReferenceDataModelImporterProviderService> importerProviderServices

    ReferenceDataModelController() {
        super(ReferenceDataModel, 'referenceDataModelId')
    }

    def defaultDataTypeProviders() {
        respond providers: referenceDataModelService.defaultDataTypeProviders
    }

    def hierarchy() {
        params.deep = true
        def resource = queryForResource(params.referenceDataModelId)
        resource ? respond(resource, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'hierarchy']) : notFound(params.id)
    }

    @Override
    protected ModelService<ReferenceDataModel> getModelService() {
        referenceDataModelService
    }

    @Transactional
    @Override
    def update() {
        log.trace('Update')
        if (handleReadOnly()) return

        ReferenceDataModel instance = queryForResource(params.id)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (instance.finalised) return forbidden('Cannot update a finalised ReferenceDataModel')

        instance.properties = getObjectToBind()

        instance = referenceDataModelService.checkForAndAddDefaultDataTypes instance, params.defaultDataTypeProvider

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def deleteAllUnusedDataClasses() {
        if (handleReadOnly()) {
            return
        }

        ReferenceDataModel referenceDataModel = queryForResource params.referenceDataModelId

        if (!referenceDataModel) return notFound(params.referenceDataModelId)

        referenceDataModelService.deleteAllUnusedDataClasses(referenceDataModel)

        render status: NO_CONTENT // NO CONTENT STATUS CODE
    }

    @Transactional
    def deleteAllUnusedDataTypes() {
        if (handleReadOnly()) {
            return
        }

        ReferenceDataModel referenceDataModel = queryForResource params.referenceDataModelId

        if (!referenceDataModel) return notFound(params.referenceDataModelId)

        referenceDataModelService.deleteAllUnusedDataTypes(referenceDataModel)

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

        List<ReferenceDataModel> deleted = referenceDataModelService.deleteAll(deleteAllParams.ids, deleteAllParams.permanent)

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

        PaginatedLuceneResult<ModelItem> result = mdmPluginReferenceDataModelSearchService.findAllByReferenceDataModelIdByLuceneSearch(params.referenceDataModelId,
                                                                                                                     searchParams, params)
        respond result
    }

    def suggestLinks() {
        ReferenceDataModel referenceDataModel = queryForResource params.referenceDataModelId
        ReferenceDataModel otherReferenceDataModel = queryForResource params.otherModelId

        if (!referenceDataModel) return notFound(params.dataModelId)
        if (!otherReferenceDataModel) return notFound(params.otherModelId)

        int maxResults = params.max ?: 5

        respond referenceDataModelService.suggestLinksBetweenModels(referenceDataModel, otherReferenceDataModel, maxResults)
    }

}
