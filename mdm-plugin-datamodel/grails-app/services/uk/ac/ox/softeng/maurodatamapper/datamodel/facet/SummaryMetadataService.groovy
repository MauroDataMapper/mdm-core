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
    SummaryMetadata findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        SummaryMetadata.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<SummaryMetadata> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination = [:]) {
        SummaryMetadata.byMultiFacetAwareItemId(multiFacetAwareItemId).list(pagination)
    }

    @Override
    DetachedCriteria<SummaryMetadata> getBaseDeleteCriteria() {
        SummaryMetadata.by()
    }
}