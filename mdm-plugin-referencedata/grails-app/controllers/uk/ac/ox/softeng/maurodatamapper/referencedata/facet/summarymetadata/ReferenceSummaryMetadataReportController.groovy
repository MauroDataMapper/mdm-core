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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService

class ReferenceSummaryMetadataReportController extends EditLoggingController<ReferenceSummaryMetadataReport> {

    static responseFormats = ['json', 'xml']

    ReferenceSummaryMetadataReportService referenceSummaryMetadataReportService
    ReferenceSummaryMetadataService referenceSummaryMetadataService

    ReferenceSummaryMetadataReportController() {
        super(ReferenceSummaryMetadataReport)
    }

    @Override
    protected ReferenceSummaryMetadataReport queryForResource(Serializable resourceId) {
        return referenceSummaryMetadataReportService.findByReferenceSummaryMetadataIdAndId(params.referenceSummaryMetadataId, resourceId)

    }

    @Override
    protected List<ReferenceSummaryMetadataReport> listAllReadableResources(Map params) {
        return referenceSummaryMetadataReportService.findAllByReferenceSummaryMetadataId(params.referenceSummaryMetadataId, params)
    }

    @Override
    void serviceDeleteResource(ReferenceSummaryMetadataReport resource) {
        referenceSummaryMetadataReportService.delete(resource)
    }

    @Override
    protected ReferenceSummaryMetadataReport createResource() {
        ReferenceSummaryMetadataReport resource = super.createResource() as ReferenceSummaryMetadataReport
        resource.clearErrors()
        resource.summaryMetadata = referenceSummaryMetadataService.findByMultiFacetAwareItemIdAndId(params.multiFacetAwareItemId, params.referenceSummaryMetadataId)
        resource
    }

    @Override
    protected ReferenceSummaryMetadataReport saveResource(ReferenceSummaryMetadataReport resource) {
        resource.save flush: true, validate: false
        referenceSummaryMetadataReportService.
            addCreatedEditToCatalogueItem(currentUser, resource, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
    }

    @Override
    protected ReferenceSummaryMetadataReport updateResource(ReferenceSummaryMetadataReport resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        referenceSummaryMetadataReportService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId,
                                          dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(ReferenceSummaryMetadataReport resource) {
        serviceDeleteResource(resource)
        referenceSummaryMetadataReportService.
            addDeletedEditToCatalogueItem(currentUser, resource, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
    }
}
