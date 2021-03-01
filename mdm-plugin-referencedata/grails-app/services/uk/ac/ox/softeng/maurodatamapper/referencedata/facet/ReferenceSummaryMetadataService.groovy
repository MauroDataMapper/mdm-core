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


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReport

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import javax.transaction.Transactional

@Slf4j
@Transactional
class ReferenceSummaryMetadataService implements CatalogueItemAwareService<ReferenceSummaryMetadata> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

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
        CatalogueItemService service = findCatalogueItemService(summaryMetadata.catalogueItemDomainType)
        service.removeReferenceSummaryMetadataFromCatalogueItem(summaryMetadata.catalogueItemId, summaryMetadata)

        List<ReferenceSummaryMetadataReport> reports = new ArrayList<>(summaryMetadata.summaryMetadataReports)
        reports.each {
            it.delete(flush: false)
        }
        summaryMetadata.delete(flush: flush)
    }

    @Override
    void saveCatalogueItem(ReferenceSummaryMetadata facet) {
        if (!facet) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(facet.catalogueItemDomainType)
        catalogueItemService.save(facet.catalogueItem)
    }

    @Override
    void addFacetToDomain(ReferenceSummaryMetadata facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
        domain.addToReferenceSummaryMetadata(facet)
    }

    @Override
    ReferenceSummaryMetadata findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        ReferenceSummaryMetadata.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<ReferenceSummaryMetadata> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination = [:]) {
        ReferenceSummaryMetadata.byCatalogueItemId(catalogueItemId).list(pagination)
    }

    @Override
    DetachedCriteria<ReferenceSummaryMetadata> getBaseDeleteCriteria() {
        ReferenceSummaryMetadata.by()
    }
}