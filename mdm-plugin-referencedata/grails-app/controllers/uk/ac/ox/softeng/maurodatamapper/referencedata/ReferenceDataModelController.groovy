/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter.ReferenceDataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.ReferenceDataModelImporterProviderService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

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

    @Override
    protected ModelService<ReferenceDataModel> getModelService() {
        referenceDataModelService
    }

    def defaultReferenceDataTypeProviders() {
        respond providers: referenceDataModelService.defaultReferenceDataTypeProviders
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params, 'label')

        PaginatedHibernateSearchResult<ModelItem> result = mdmPluginReferenceDataModelSearchService.findAllByReferenceDataModelIdByHibernateSearch(params.referenceDataModelId,
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

    @Override
    protected ReferenceDataModel performAdditionalUpdates(ReferenceDataModel model) {
        referenceDataModelService.checkForAndAddDefaultReferenceDataTypes model, params.defaultReferenceDataTypeProvider
    }
}
