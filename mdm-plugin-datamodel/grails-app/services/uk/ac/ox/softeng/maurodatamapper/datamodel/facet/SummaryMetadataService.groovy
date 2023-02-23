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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet


import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class SummaryMetadataService implements MultiFacetItemAwareService<SummaryMetadata> {

    SummaryMetadata get(Serializable id) {
        SummaryMetadata.get(id)
    }

    @Override
    List<SummaryMetadata> getAll(Collection<UUID> resourceIds) {
        SummaryMetadata.getAll(resourceIds)
    }

    List<SummaryMetadata> list(Map args) {
        SummaryMetadata.list(args)
    }

    Long count() {
        SummaryMetadata.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(SummaryMetadata summaryMetadata, boolean flush = false) {
        if (!summaryMetadata) return

        SummaryMetadataAwareService service = findCatalogueItemService(summaryMetadata.multiFacetAwareItemDomainType) as SummaryMetadataAwareService
        service.removeSummaryMetadataFromMultiFacetAware(summaryMetadata.multiFacetAwareItemId, summaryMetadata)

        List<SummaryMetadataReport> reports = new ArrayList<>(summaryMetadata.summaryMetadataReports)
        reports.each {
            it.delete(flush: false)
        }
        summaryMetadata.delete(flush: flush)
    }

    void saveMultiFacetAwareItem(SummaryMetadata facet) {
        if (!facet) return
        MultiFacetAwareService multiFacetAwareItemService = findServiceForMultiFacetAwareDomainType(facet.multiFacetAwareItemDomainType)
        multiFacetAwareItemService.save(facet.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(SummaryMetadata facet, String domainType, UUID domainId) {
        if (!facet) return
        SummaryMetadataAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId) as SummaryMetadataAware
        facet.multiFacetAwareItem = domain as MultiFacetAware
        domain.addToSummaryMetadata(facet)
    }

    @Override
    SummaryMetadata copy(SummaryMetadata facetToCopy, MultiFacetAware multiFacetAwareItemToCopyInto) {
        SummaryMetadata copy = new SummaryMetadata(summaryMetadataType: facetToCopy.summaryMetadataType, createdBy: facetToCopy.createdBy)
        facetToCopy.summaryMetadataReports.each {smr ->
            copy.addToSummaryMetadataReports(reportDate: smr.reportDate, reportValue: smr.reportValue)
        }
        (multiFacetAwareItemToCopyInto as SummaryMetadataAware).addToSummaryMetadata(copy)
        copy
    }

    boolean existsByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        SummaryMetadata.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).count() == 1
    }

    @Override
    SummaryMetadata findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        SummaryMetadata.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<SummaryMetadata> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination = [:]) {
        SummaryMetadata.withFilter(SummaryMetadata.byMultiFacetAwareItemId(multiFacetAwareItemId), pagination).list(pagination)
    }

    @Override
    List<SummaryMetadata> findAllByMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        SummaryMetadata.byMultiFacetAwareItemIdInList(multiFacetAwareItemIds).list()
    }

    @Override
    DetachedCriteria<SummaryMetadata> getBaseDeleteCriteria() {
        SummaryMetadata.by()
    }

    @Override
    SummaryMetadata findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        SummaryMetadata.byMultiFacetAwareItemId(parentId).eq('label', pathIdentifier).get()
    }
}