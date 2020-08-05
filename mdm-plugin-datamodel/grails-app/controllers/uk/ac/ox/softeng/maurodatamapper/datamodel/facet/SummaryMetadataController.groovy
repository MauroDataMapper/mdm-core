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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class SummaryMetadataController extends EditLoggingController<SummaryMetadata> {
    static responseFormats = ['json', 'xml']

    SummaryMetadataService summaryMetadataService

    SummaryMetadataController() {
        super(SummaryMetadata)
    }

    @Override
    protected SummaryMetadata queryForResource(Serializable resourceId) {
        return summaryMetadataService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<SummaryMetadata> listAllReadableResources(Map params) {
        return summaryMetadataService.findAllByCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    void serviceDeleteResource(SummaryMetadata resource) {
        summaryMetadataService.delete(resource)
    }

    @Override
    protected SummaryMetadata createResource() {
        SummaryMetadata resource = super.createResource() as SummaryMetadata
        resource.clearErrors()
        resource.catalogueItem = summaryMetadataService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        if (resource.summaryMetadataReports) {
            for (def report : resource.summaryMetadataReports) {
                if(!report.createdBy) report.createdBy = resource.createdBy
            }
        }
        resource
    }

    @Override
    protected SummaryMetadata saveResource(SummaryMetadata resource) {
        resource.save flush: true, validate: false
        summaryMetadataService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected SummaryMetadata updateResource(SummaryMetadata resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        summaryMetadataService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(SummaryMetadata resource) {
        serviceDeleteResource(resource)
        summaryMetadataService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }
}
