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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class ReferenceSummaryMetadataController extends EditLoggingController<ReferenceSummaryMetadata> {
    static responseFormats = ['json', 'xml']

    ReferenceSummaryMetadataService referenceSummaryMetadataService

    ReferenceSummaryMetadataController() {
        super(ReferenceSummaryMetadata)
    }

    @Override
    protected ReferenceSummaryMetadata queryForResource(Serializable resourceId) {
        return referenceSummaryMetadataService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<ReferenceSummaryMetadata> listAllReadableResources(Map params) {
        return referenceSummaryMetadataService.findAllByCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    void serviceDeleteResource(ReferenceSummaryMetadata resource) {
        referenceSummaryMetadataService.delete(resource)
    }

    @Override
    protected ReferenceSummaryMetadata createResource() {
        ReferenceSummaryMetadata resource = super.createResource() as ReferenceSummaryMetadata
        resource.clearErrors()
        resource.catalogueItem = referenceSummaryMetadataService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        if (resource.summaryMetadataReports) {
            for (def report : resource.summaryMetadataReports) {
                if (!report.createdBy) report.createdBy = resource.createdBy
            }
        }
        resource
    }

    @Override
    protected ReferenceSummaryMetadata saveResource(ReferenceSummaryMetadata resource) {
        resource.save flush: true, validate: false
        referenceSummaryMetadataService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ReferenceSummaryMetadata updateResource(ReferenceSummaryMetadata resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        referenceSummaryMetadataService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(ReferenceSummaryMetadata resource) {
        serviceDeleteResource(resource)
        referenceSummaryMetadataService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }
}
