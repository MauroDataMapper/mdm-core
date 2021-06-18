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
package uk.ac.ox.softeng.maurodatamapper.core.traits.service

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 31/01/2020
 */
@Slf4j
trait MultiFacetItemAwareService<K> extends DomainService<K> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List<ContainerService> containerServices

    abstract K findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id)

    abstract List<K> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination)

    abstract DetachedCriteria<K> getBaseDeleteCriteria()

    abstract void saveMultiFacetAwareItem(K facet)

    abstract void delete(K facet, boolean flush)

    abstract void addFacetToDomain(K facet, String domainType, UUID domainId)

    K addCreatedEditToMultiFacetAwareItem(User creator, K domain, String multiFacetAwareItemDomainType, UUID multiFacetAwareItemId) {
        EditHistoryAware multiFacetAwareItem =
            findMultiFacetAwareItemByDomainTypeAndId(multiFacetAwareItemDomainType, multiFacetAwareItemId) as EditHistoryAware
        multiFacetAwareItem.addToEditsTransactionally EditTitle.CREATE, creator, "[$domain.editLabel] added to component " +
                                                                                 "[${multiFacetAwareItem.editLabel}]"
        domain
    }

    K addUpdatedEditToMultiFacetAwareItem(User editor, K domain, String multiFacetAwareItemDomainType, UUID multiFacetAwareItemId,
                                          List<String> dirtyPropertyNames) {
        EditHistoryAware multiFacetAwareItem =
            findMultiFacetAwareItemByDomainTypeAndId(multiFacetAwareItemDomainType, multiFacetAwareItemId) as EditHistoryAware
        multiFacetAwareItem.addToEditsTransactionally EditTitle.UPDATE,editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    K addDeletedEditToMultiFacetAwareItem(User deleter, K domain, String multiFacetAwareItemDomainType, UUID multiFacetAwareItemId) {
        EditHistoryAware multiFacetAwareItem =
            findMultiFacetAwareItemByDomainTypeAndId(multiFacetAwareItemDomainType, multiFacetAwareItemId) as EditHistoryAware
        multiFacetAwareItem.addToEditsTransactionally EditTitle.DELETE, deleter, "[$domain.editLabel] removed from component " +
                                                                                 "[${multiFacetAwareItem.editLabel}]"
        domain
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        findServiceForMultiFacetAwareDomainType(domainType).get(multiFacetAwareItemId)
    }

    void deleteAllByMultiFacetAwareItemIds(List<UUID> multiFacetAwareItemIds) {
        // Too large a batch will throw an error as too many bind variables in the query
        if (multiFacetAwareItemIds.size() > GormUtils.POSTGRES_MAX_BIND_VARIABLES) {
            batchDeleteAllByMultiFacetAwareItemIds(multiFacetAwareItemIds)
        } else {
            performDeletion(multiFacetAwareItemIds)
        }
    }

    void batchDeleteAllByMultiFacetAwareItemIds(List<UUID> multiFacetAwareItemIds) {
        int batchSize = GormUtils.POSTGRES_MAX_BIND_VARIABLES
        List<UUID> batch = new ArrayList<>(batchSize)
        multiFacetAwareItemIds.each {UUID id ->
            batch << id
            if (batch.size() % batchSize == 0) {
                performDeletion(batch)
                batch.clear()
            }
        }
        if (batch) {
            performDeletion(batch)
            batch.clear()
        }
    }

    void performDeletion(List<UUID> batch) {
        long start = System.currentTimeMillis()
        getBaseDeleteCriteria().inList('multiFacetAwareItemId', batch).deleteAll()
        log.trace('{} removed took {}', getBaseDeleteCriteria().getPersistentClass().simpleName, Utils.timeTaken(start))
    }

    void addFacetAndSaveMultiFacetAware(String facetType, MultiFacetItemAware multiFacetAwareItemAware) {
        if (!multiFacetAwareItemAware) return
        MultiFacetAware multiFacetAware = multiFacetAwareItemAware.multiFacetAwareItem
        multiFacetAware.addTo(facetType, multiFacetAwareItemAware)
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(multiFacetAwareItemAware.multiFacetAwareItemDomainType)
        service.save(multiFacetAware)
    }

    MultiFacetAwareService findServiceForMultiFacetAwareDomainType(String domainType) {
        MultiFacetAwareService service = findCatalogueItemService(domainType) ?: findContainerService(domainType)
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${domainType}")
        return service
    }

    CatalogueItemService findCatalogueItemService(String catalogueItemDomainType) {
        catalogueItemServices.find {it.handles(catalogueItemDomainType)}

    }

    private ContainerService findContainerService(String containerDomainType) {
        containerServices.find {it.handles(containerDomainType)}
    }
}
