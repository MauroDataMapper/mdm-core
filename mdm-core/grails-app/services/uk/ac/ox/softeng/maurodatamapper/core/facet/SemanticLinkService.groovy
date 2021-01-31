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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.util.Pair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.util.function.BiFunction
import javax.transaction.Transactional

@Slf4j
@Transactional
class SemanticLinkService implements CatalogueItemAwareService<SemanticLink> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    SemanticLink get(Serializable id) {
        SemanticLink.get(id)
    }

    List<SemanticLink> list(Map args) {
        SemanticLink.list(args)
    }

    Long count() {
        SemanticLink.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    SemanticLink save(SemanticLink semanticLink) {
        semanticLink.save(flush: true)
    }

    void delete(SemanticLink semanticLink, boolean cleanFromOwner = true) {
        if (!semanticLink) return

        if (cleanFromOwner) {
            CatalogueItemService service = catalogueItemServices.find { it.handles(semanticLink.catalogueItemDomainType) }
            if (!service) throw new ApiBadRequestException('SLS01', 'Semantic link removal for catalogue item with no supporting service')
            service.removeSemanticLinkFromCatalogueItem(semanticLink.catalogueItemId, semanticLink)
        }

        semanticLink.delete()
    }

    void deleteAll(List<SemanticLink> semanticLinks, boolean cleanFromOwner = true) {
        if (cleanFromOwner) {
            semanticLinks.each { delete(it) }
        } else {
            SemanticLink.deleteAll(semanticLinks)
        }
    }

    SemanticLink loadCatalogueItemsIntoSemanticLink(SemanticLink semanticLink) {
        if (!semanticLink) return null
        if (!semanticLink.catalogueItem) {
            semanticLink.catalogueItem = findCatalogueItemByDomainTypeAndId(semanticLink.catalogueItemDomainType, semanticLink.catalogueItemId)
        }
        if (!semanticLink.targetCatalogueItem) {
            semanticLink.targetCatalogueItem = findCatalogueItemByDomainTypeAndId(semanticLink.targetCatalogueItemDomainType,
                                                                                  semanticLink.targetCatalogueItemId)
        }
        semanticLink
    }

    List<SemanticLink> loadCatalogueItemsIntoSemanticLinks(List<SemanticLink> semanticLinks) {
        if (!semanticLinks) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all catalogue items for {} semantic links', semanticLinks.size())
        semanticLinks.each { sl ->

            itemIdsMap.compute(sl.catalogueItemDomainType, [
                apply: { String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(sl.catalogueItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(sl.targetCatalogueItemDomainType, [
                apply: { String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(sl.targetCatalogueItemId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, CatalogueItem> itemMap = [:]
        itemIdsMap.each { domain, ids ->
            CatalogueItemService service = catalogueItemServices.find { it.handles(domain) }
            if (!service) throw new ApiBadRequestException('SLS02', 'Semantic link loading for catalogue item with no supporting service')
            List<CatalogueItem> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries { i -> [new Pair<String, UUID>(domain, i.id), i] })
        }

        log.debug('Loading {} retrieved catalogue items into semantic links', itemMap.size())
        semanticLinks.each { sl ->
            sl.catalogueItem = itemMap.get(new Pair(sl.catalogueItemDomainType, sl.catalogueItemId))
            sl.targetCatalogueItem = itemMap.get(new Pair(sl.targetCatalogueItemDomainType, sl.targetCatalogueItemId))
        }

        semanticLinks
    }

    void deleteBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(CatalogueItem sourceCatalogueItem, CatalogueItem targetCatalogueItem,
                                                                      SemanticLinkType linkType) {
        SemanticLink sl = findBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(sourceCatalogueItem, targetCatalogueItem, linkType)
        if (sl) delete(sl)
    }

    SemanticLink createSemanticLink(User createdBy, CatalogueItem source, CatalogueItem target, SemanticLinkType linkType) {
        new SemanticLink(createdBy: createdBy.emailAddress, linkType: linkType).with {
            setCatalogueItem(source)
            setTargetCatalogueItem(target)
            it
        }
    }

    @Override
    SemanticLink findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        SemanticLink.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<SemanticLink> findAllByCatalogueItemId(UUID catalogueItemId, Map paginate = [:]) {
        findAllBySourceOrTargetCatalogueItemId(catalogueItemId, paginate)
    }

    @Override
    DetachedCriteria<SemanticLink> getBaseDeleteCriteria() {
        SemanticLink.by()
    }

    @Override
    void performDeletion(List<UUID> batch) {
        long start = System.currentTimeMillis()
        SemanticLink.byAnyCatalogueItemIdInList(batch).deleteAll()
        log.trace('{} removed took {}', SemanticLink.simpleName, Utils.timeTaken(start))
    }

    SemanticLink findBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(CatalogueItem sourceCatalogueItem, CatalogueItem targetCatalogueItem,
                                                                            SemanticLinkType linkType) {
        SemanticLink.bySourceCatalogueItemAndTargetCatalogueItemAndLinkType(sourceCatalogueItem, targetCatalogueItem, linkType).get()
    }

    List<SemanticLink> findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(List<UUID> sourceCatalogueItemIds,
                                                                                                     List<UUID> targetCatalogueItemIds,
                                                                                                     SemanticLinkType linkType) {
        SemanticLink.bySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(sourceCatalogueItemIds,
                                                                                            targetCatalogueItemIds, linkType).list()
    }

    List<SemanticLink> findAllBySourceCatalogueItemId(UUID catalogueItemId, Map paginate = [:]) {
        SemanticLink.withFilter(SemanticLink.byCatalogueItemId(catalogueItemId), paginate).list(paginate)
    }

    List<SemanticLink> findAllByTargetCatalogueItemId(Serializable catalogueItemId, Map paginate = [:]) {
        SemanticLink.withFilter(SemanticLink.byTargetCatalogueItemId(catalogueItemId), paginate).list(paginate)
    }

    List<SemanticLink> findAllBySourceOrTargetCatalogueItemId(Serializable catalogueItemId, Map paginate = [:]) {
        SemanticLink.withFilter(SemanticLink.byAnyCatalogueItemId(catalogueItemId), paginate).list(paginate)
    }

    @Deprecated(forRemoval = true)
    List<SemanticLink> findAllByCatalogueItemIdAndType(UUID catalogueItemId, String type, Map paginate = [:]) {
        switch (type) {
            case 'source':
                return findAllBySourceCatalogueItemId(catalogueItemId, paginate)
                break
            case 'target':
                return findAllByTargetCatalogueItemId(catalogueItemId, paginate)
        }
        findAllBySourceOrTargetCatalogueItemId(catalogueItemId, paginate)
    }

}
