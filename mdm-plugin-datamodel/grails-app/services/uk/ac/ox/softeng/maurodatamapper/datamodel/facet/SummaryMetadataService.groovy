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


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j

import javax.transaction.Transactional

@Slf4j
@Transactional
class SummaryMetadataService implements CatalogueItemAwareService<SummaryMetadata> {

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

        CatalogueItemService service = findCatalogueItemService(summaryMetadata.catalogueItemDomainType)
        service.removeSummaryMetadataFromCatalogueItem(summaryMetadata.catalogueItemId, summaryMetadata)

        List<SummaryMetadataReport> reports = new ArrayList<>(summaryMetadata.summaryMetadataReports)
        reports.each {
            it.delete(flush: false)
        }
        summaryMetadata.delete(flush: flush)
    }

    void saveCatalogueItem(SummaryMetadata facet) {
        if (!facet) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(facet.catalogueItemDomainType)
        catalogueItemService.save(facet.catalogueItem)
    }

    @Override
    void addFacetToDomain(SummaryMetadata facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
        domain.addToSummaryMetadata(facet)
    }

    @Override
    SummaryMetadata findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        SummaryMetadata.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<SummaryMetadata> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination = [:]) {
        SummaryMetadata.byCatalogueItemId(catalogueItemId).list(pagination)
    }

    @Override
    DetachedCriteria<SummaryMetadata> getBaseDeleteCriteria() {
        SummaryMetadata.by()
    }

}