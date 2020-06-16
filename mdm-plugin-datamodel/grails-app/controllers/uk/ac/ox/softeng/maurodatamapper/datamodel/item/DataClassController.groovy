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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.SearchService
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap

class DataClassController extends CatalogueItemController<DataClass> {

    static responseFormats = ['json', 'xml']

    DataClassService dataClassService

    DataModelService dataModelService

    SearchService searchService

    DataClassController() {
        super(DataClass)
    }

    def all() {
        params.all = true
        index()
    }

    def content(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        params.sort = params.sort ?: 'label'

        respond content: dataClassService.findAllContentOfDataClassId(params.dataClassId, params)
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        if (!dataClassService.existsWhereRootDataClassOfDataModelIdAndId(params.dataModelId, params.dataClassId)) {
            return notFound(params.dataClassId)
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        params.max = params.max ?: searchParams.max ?: 10
        params.offset = params.offset ?: searchParams.offset ?: 0

        PaginatedLuceneResult<ModelItem> result = searchService.findAllByDataClassIdByLuceneSearch(Utils.toUuid(params.dataClassId),
                                                                                                   searchParams, params)
        respond result
    }

    @Transactional
    def copyDataClass() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataClass original = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)

        if (!original) return notFound(params.otherDataClassId)

        DataClass copy
        try {
            copy = dataClassService.copyDataClassMatchingAllReferenceTypes(dataModel, original, currentUser, params.dataClassId)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        dataModelService.validate(dataModel)
        if (dataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    @Override
    protected DataClass queryForResource(Serializable resourceId) {
        if (params.dataClassId) {
            return dataClassService.findByParentDataClassIdAndId(params.dataClassId, resourceId)
        }
        return dataClassService.findWhereRootDataClassOfDataModelIdAndId(params.dataModelId, resourceId)
    }

    @Override
    protected List<DataClass> listAllReadableResources(Map params) {
        if (params.dataClassId) {
            return dataClassService.findAllByParentDataClassId(params.dataClassId, params)
        }
        if (((GrailsParameterMap) params).boolean('all', false)) {
            if (params.search) {
                return dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(params.dataModelId, params.search, params)
            }
            return dataClassService.findAllByDataModelId(params.dataModelId, params)
        }
        return dataClassService.findAllWhereRootDataClassOfDataModelId(params.dataModelId, params)
    }

    @Override
    void serviceDeleteResource(DataClass resource) {
        dataClassService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataClass resource) {
        dataClassService.save(resource)
    }

    @Override
    protected DataClass createResource() {
        DataClass resource = super.createResource() as DataClass
        if (params.dataClassId) {
            dataClassService.get(params.dataClassId)?.addToDataClasses(resource)
        }
        dataModelService.get(params.dataModelId)?.addToDataClasses(resource)

        resource
    }
}
