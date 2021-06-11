/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.service.ReferenceSummaryMetadataAwareService

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class ReferenceSummaryMetadataService implements MultiFacetItemAwareService<ReferenceSummaryMetadata> {

    ReferenceSummaryMetadata get(Serializable id) {
        ReferenceSummaryMetadata.get(id)
    }

    List<ReferenceSummaryMetadata> list(Map args) {
        ReferenceSummaryMetadata.list(args)
    }

    Long count() {
        ReferenceSummaryMetadata.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(ReferenceSummaryMetadata summaryMetadata, boolean flush = false) {
        if (!summaryMetadata) return
        ReferenceSummaryMetadataAwareService service =
            findCatalogueItemService(summaryMetadata.multiFacetAwareItemDomainType) as ReferenceSummaryMetadataAwareService
        service.removeReferenceSummaryMetadataFromMultiFacetAware(summaryMetadata.multiFacetAwareItemId, summaryMetadata)

        List<ReferenceSummaryMetadataReport> reports = new ArrayList<>(summaryMetadata.summaryMetadataReports)
        reports.each {
            it.delete(flush: false)
        }
        summaryMetadata.delete(flush: flush)
    }

    @Override
    void saveMultiFacetAwareItem(ReferenceSummaryMetadata facet) {
        if (!facet) return
        MultiFacetAwareService multiFacetAwareItemService = findServiceForMultiFacetAwareDomainType(facet.multiFacetAwareItemDomainType)
        multiFacetAwareItemService.save(facet.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(ReferenceSummaryMetadata facet, String domainType, UUID domainId) {
        if (!facet) return
        ReferenceSummaryMetadataAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId)
        facet.multiFacetAwareItem = domain as MultiFacetAware
        domain.addToReferenceSummaryMetadata(facet)
    }

    @Override
    ReferenceSummaryMetadata findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        ReferenceSummaryMetadata.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<ReferenceSummaryMetadata> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination = [:]) {
        ReferenceSummaryMetadata.withFilter(ReferenceSummaryMetadata.byMultiFacetAwareItemId(multiFacetAwareItemId), pagination).list(pagination)
    }

    @Override
    DetachedCriteria<ReferenceSummaryMetadata> getBaseDeleteCriteria() {
        ReferenceSummaryMetadata.by()
    }
}