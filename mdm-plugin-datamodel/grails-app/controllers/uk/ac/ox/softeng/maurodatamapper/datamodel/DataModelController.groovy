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
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.Intersects
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.Subset
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

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

    def search() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params)

        PaginatedHibernateSearchResult<ModelItem> result = mdmPluginDataModelSearchService.findAllByDataModelIdByHibernateSearch(params.dataModelId, searchParams, params)
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
            if (dataModelService.validateImportAddition(instance, dataTypeToBeImported)) {
                log.debug('Importing DataType {} from {}', params.otherDataTypeId, params.otherDataModelId)
                instance.addToImportedDataTypes(dataTypeToBeImported)
            }
        } else {
            if (dataModelService.validateImportRemoval(instance, dataTypeToBeImported)) {
                log.debug('Removing import of DataType {} from {}', params.otherDataTypeId, params.otherDataModelId)
                instance.removeFromImportedDataTypes(dataTypeToBeImported)
            }
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
            if (dataModelService.validateImportAddition(instance, dataClassToBeImported)) {
                log.debug('Importing DataClass {} from {}', params.otherDataClassId, params.otherDataModelId)
                instance.addToImportedDataClasses(dataClassToBeImported)
            }
        } else {
            if (dataModelService.validateImportRemoval(instance, dataClassToBeImported)) {
                log.debug('Removing import of DataClass {} from {}', params.otherDataClassId, params.otherDataModelId)
                instance.removeFromImportedDataClasses(dataClassToBeImported)
            }
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

    @Transactional
    def subset(Subset subsetData) {
        if (!subsetData.validate()) {
            respond subsetData.errors
            return
        }

        DataModel sourceModel = queryForResource params[alternateParamsIdKey]
        if (!sourceModel) return notFound(params[alternateParamsIdKey])

        DataModel targetModel = queryForResource params.otherDataModelId
        if (!targetModel) return notFound(params.otherDataModelId)

        dataModelService.subset(sourceModel, targetModel, subsetData, currentUserSecurityPolicyManager)

        if (!validateResource(targetModel, 'update')) return
        updateResource(targetModel)
        updateResponse(targetModel)
    }

    def intersects() {

        DataModel sourceModel = queryForResource params[alternateParamsIdKey]
        if (!sourceModel) return notFound(params[alternateParamsIdKey])

        DataModel targetModel = queryForResource params.otherDataModelId
        if (!targetModel) return notFound(params.otherDataModelId)

        respond(intersection: dataModelService.intersects(sourceModel, targetModel))
    }

    def intersectsMany(Intersects intersects) {

        DataModel sourceModel = queryForResource params[alternateParamsIdKey]
        if (!sourceModel) return notFound(params[alternateParamsIdKey])

        respond(intersectionMany: dataModelService.intersectsMany(currentUserSecurityPolicyManager, sourceModel, intersects))
    }
}
