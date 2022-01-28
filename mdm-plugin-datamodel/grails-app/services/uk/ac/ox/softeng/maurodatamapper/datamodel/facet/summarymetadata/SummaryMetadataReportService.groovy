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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

@Slf4j
@Transactional
class SummaryMetadataReportService implements MdmDomainService<SummaryMetadataReport> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    SummaryMetadataReport get(Serializable id) {
        SummaryMetadataReport.get(id)
    }

    @Override
    List<SummaryMetadataReport> getAll(Collection<UUID> resourceIds) {
        SummaryMetadataReport.getAll(resourceIds)
    }

    List<SummaryMetadataReport> list(Map args) {
        SummaryMetadataReport.list(args)
    }

    Long count() {
        SummaryMetadataReport.count()
    }

    void delete(SummaryMetadataReport summaryMetadataReport) {
        if (!summaryMetadataReport) return
        summaryMetadataReport.delete()
    }

    @Override
    SummaryMetadataReport findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        OffsetDateTime reportDate = OffsetDateTime.parse(pathIdentifier, SummaryMetadataReport.PATH_FORMATTER)
        SummaryMetadataReport.bySummaryMetadataId(parentId).eq('reportDate', reportDate).get()
    }

    SummaryMetadataReport findBySummaryMetadataIdAndId(UUID summaryMetadataId, Serializable id) {
        SummaryMetadataReport.bySummaryMetadataIdAndId(summaryMetadataId, id).get()
    }

    List<SummaryMetadataReport> findAllBySummaryMetadataId(UUID summaryMetadataId, Map pagination = [:]) {
        SummaryMetadataReport.bySummaryMetadataId(summaryMetadataId).list(pagination)
    }

    SummaryMetadataReport addCreatedEditToCatalogueItem(User creator, SummaryMetadataReport domain, String catalogueItemDomainType,
                                                        UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally EditTitle.CREATE, creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    SummaryMetadataReport addUpdatedEditToCatalogueItem(User editor, SummaryMetadataReport domain, String catalogueItemDomainType,
                                                        UUID catalogueItemId, List<String> dirtyPropertyNames) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally EditTitle.UPDATE, editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    SummaryMetadataReport addDeletedEditToCatalogueItem(User deleter, SummaryMetadataReport domain, String catalogueItemDomainType,
                                                        UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally EditTitle.DELETE, deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }

    CatalogueItem findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CIAS02', "Facet retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}