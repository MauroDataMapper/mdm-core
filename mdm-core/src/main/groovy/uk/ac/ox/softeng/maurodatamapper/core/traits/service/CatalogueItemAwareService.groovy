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
package uk.ac.ox.softeng.maurodatamapper.core.traits.service

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.security.User

/**
 * @since 31/01/2020
 */
trait CatalogueItemAwareService<K> {

    abstract List<CatalogueItemService> getCatalogueItemServices()

    abstract K findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id)

    abstract List<K> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination)

    K addCreatedEditToCatalogueItem(User creator, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    K addUpdatedEditToCatalogueItem(User editor, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally editor, domain.editLabel, domain.dirtyPropertyNames
        domain
    }

    K addDeletedEditToCatalogueItem(User deleter, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }


    CatalogueItem findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CIAS02', "Metadata retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}
