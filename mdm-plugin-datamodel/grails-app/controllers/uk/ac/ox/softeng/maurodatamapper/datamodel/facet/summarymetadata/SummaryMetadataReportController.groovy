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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReportService

class SummaryMetadataReportController extends EditLoggingController<SummaryMetadataReport> {

    static responseFormats = ['json', 'xml']

    SummaryMetadataReportService summaryMetadataReportService
    SummaryMetadataService summaryMetadataService

    SummaryMetadataReportController() {
        super(SummaryMetadataReport)
    }

    @Override
    protected SummaryMetadataReport queryForResource(Serializable resourceId) {
        return summaryMetadataReportService.findBySummaryMetadataIdAndId(params.summaryMetadataId, resourceId)

    }

    @Override
    protected List<SummaryMetadataReport> listAllReadableResources(Map params) {
        return summaryMetadataReportService.findAllBySummaryMetadataId(params.summaryMetadataId, params)
    }

    @Override
    void serviceDeleteResource(SummaryMetadataReport resource) {
        summaryMetadataReportService.delete(resource)
    }

    @Override
    protected SummaryMetadataReport createResource() {
        SummaryMetadataReport resource = super.createResource() as SummaryMetadataReport
        resource.clearErrors()
        resource.summaryMetadata = summaryMetadataService.get(params.summaryMetadataId)
        resource
    }

    @Override
    protected SummaryMetadataReport saveResource(SummaryMetadataReport resource) {
        resource.save flush: true, validate: false
        summaryMetadataReportService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected SummaryMetadataReport updateResource(SummaryMetadataReport resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        summaryMetadataReportService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(SummaryMetadataReport resource) {
        serviceDeleteResource(resource)
        summaryMetadataReportService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }
}
