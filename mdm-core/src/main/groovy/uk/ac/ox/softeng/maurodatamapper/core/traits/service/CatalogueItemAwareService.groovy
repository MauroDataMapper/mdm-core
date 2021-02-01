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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j

/**
 * @since 31/01/2020
 */
@Slf4j
trait CatalogueItemAwareService<K> extends DomainService<K> {

    abstract List<CatalogueItemService> getCatalogueItemServices()

    abstract List<ContainerService> getContainerServices()

    abstract K findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id)

    abstract List<K> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination)

    abstract DetachedCriteria<K> getBaseDeleteCriteria()

    abstract void saveCatalogueItem(K facet)

    abstract void delete(K facet, boolean flush)

    abstract void addFacetToDomain(K facet, String domainType, UUID domainId)

    K addCreatedEditToCatalogueItem(User creator, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally EditTitle.CREATE, creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    K addUpdatedEditToCatalogueItem(User editor, K domain, String catalogueItemDomainType, UUID catalogueItemId, List<String> dirtyPropertyNames) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally EditTitle.UPDATE, editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    K addDeletedEditToCatalogueItem(User deleter, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally EditTitle.DELETE, deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }

    MultiFacetAware findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        findCatalogueItemService(domainType).get(catalogueItemId)?:findContainerService(domainType)
    }

    void deleteAllByCatalogueItemIds(List<UUID> catalogueItemIds) {
        // Too large a batch will throw an error as too many bind variables in the query
        if (catalogueItemIds.size() > GormUtils.POSTGRES_MAX_BIND_VARIABLES) {
            batchDeleteAllByCatalogueItemIds(catalogueItemIds)
        } else {
            performDeletion(catalogueItemIds)
        }
    }

    void batchDeleteAllByCatalogueItemIds(List<UUID> catalogueItemIds) {
        int batchSize = GormUtils.POSTGRES_MAX_BIND_VARIABLES
        List<UUID> batch = new ArrayList<>(batchSize)
        catalogueItemIds.each { UUID id ->
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
        getBaseDeleteCriteria().inList('catalogueItemId', batch).deleteAll()
        log.trace('{} removed took {}', getBaseDeleteCriteria().getPersistentClass().simpleName, Utils.timeTaken(start))
    }

    CatalogueItemService findCatalogueItemService(String catalogueItemDomainType) {
        CatalogueItemService service = catalogueItemServices.find { it.handles(catalogueItemDomainType) }
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${catalogueItemDomainType}")
        return service
    }

    ContainerService findContainerService(String containerDomainType) {
        ContainerService service = containerServices.find {it.handles(containerDomainType)}
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${containerDomainType}")
        return service
    }
}
