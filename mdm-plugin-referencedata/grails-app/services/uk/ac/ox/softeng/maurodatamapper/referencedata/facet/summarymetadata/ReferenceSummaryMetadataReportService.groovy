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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import javax.transaction.Transactional

@Slf4j
@Transactional
class ReferenceSummaryMetadataReportService {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    ReferenceSummaryMetadataReport get(Serializable id) {
        ReferenceSummaryMetadataReport.get(id)
    }

    List<ReferenceSummaryMetadataReport> list(Map args) {
        ReferenceSummaryMetadataReport.list(args)
    }

    Long count() {
        ReferenceSummaryMetadataReport.count()
    }

    void delete(ReferenceSummaryMetadataReport summaryMetadataReport) {
        if (!summaryMetadataReport) return
        summaryMetadataReport.delete()
    }

    ReferenceSummaryMetadataReport findBySummaryMetadataIdAndId(UUID summaryMetadataId, Serializable id) {
        ReferenceSummaryMetadataReport.bySummaryMetadataIdAndId(summaryMetadataId, id).get()
    }

    List<ReferenceSummaryMetadataReport> findAllBySummaryMetadataId(UUID summaryMetadataId, Map pagination = [:]) {
        ReferenceSummaryMetadataReport.bySummaryMetadataId(summaryMetadataId).list(pagination)
    }

    ReferenceSummaryMetadataReport addCreatedEditToCatalogueItem(User creator, ReferenceSummaryMetadataReport domain, String catalogueItemDomainType,
                                                                 UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    ReferenceSummaryMetadataReport addUpdatedEditToCatalogueItem(User editor, ReferenceSummaryMetadataReport domain, String catalogueItemDomainType,
                                                                 UUID catalogueItemId, List<String> dirtyPropertyNames) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    ReferenceSummaryMetadataReport addDeletedEditToCatalogueItem(User deleter, ReferenceSummaryMetadataReport domain, String catalogueItemDomainType,
                                                                 UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }

    CatalogueItem findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CIAS02', "Facet retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}