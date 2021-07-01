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

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import grails.util.Pair
import groovy.util.logging.Slf4j

import java.util.function.BiFunction

@Slf4j
@Transactional
class SemanticLinkService implements MultiFacetItemAwareService<SemanticLink> {

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

    @Override
    SemanticLink findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        String[] split = pathIdentifier
        SemanticLink.byMultiFacetAwareItemId(parentId)
            .eq('linkType', SemanticLinkType.findForLabel(split[0]))
            .eq('targetMultiFacetAwareItemId', Utils.toUuid(split[1]))
            .get()
    }

    void delete(SemanticLink semanticLink, boolean flush = false) {
        if (!semanticLink) return
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(semanticLink.multiFacetAwareItemDomainType)
        service.removeSemanticLinkFromMultiFacetAware(semanticLink.multiFacetAwareItemId, semanticLink)
        semanticLink.delete(flush: flush)
    }

    void deleteAll(List<SemanticLink> semanticLinks, boolean cleanFromOwner = true) {
        if (cleanFromOwner) {
            semanticLinks.each {delete(it)}
        } else {
            SemanticLink.deleteAll(semanticLinks)
        }
    }

    @Override
    void saveMultiFacetAwareItem(SemanticLink facet) {
        if (!facet) return
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(facet.multiFacetAwareItemDomainType)
        service.save(facet.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(SemanticLink facet, String domainType, UUID domainId) {
        if (!facet) return
        MultiFacetAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId)
        facet.multiFacetAwareItem = domain
        domain.addToSemanticLinks(facet)
    }

    SemanticLink loadMultiFacetAwareItemsIntoSemanticLink(SemanticLink semanticLink) {
        if (!semanticLink) return null
        if (!semanticLink.multiFacetAwareItem) {
            semanticLink.multiFacetAwareItem =
                findMultiFacetAwareItemByDomainTypeAndId(semanticLink.multiFacetAwareItemDomainType, semanticLink.multiFacetAwareItemId)
        }
        if (!semanticLink.targetMultiFacetAwareItem) {
            semanticLink.targetMultiFacetAwareItem = findMultiFacetAwareItemByDomainTypeAndId(semanticLink.targetMultiFacetAwareItemDomainType,
                                                                                              semanticLink.targetMultiFacetAwareItemId)
        }
        semanticLink
    }

    List<SemanticLink> loadMultiFacetAwareItemsIntoSemanticLinks(List<SemanticLink> semanticLinks) {
        if (!semanticLinks) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all catalogue items for {} semantic links', semanticLinks.size())
        semanticLinks.each {sl ->

            itemIdsMap.compute(sl.multiFacetAwareItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(sl.multiFacetAwareItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(sl.targetMultiFacetAwareItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(sl.targetMultiFacetAwareItemId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, MultiFacetAware> itemMap = [:]
        itemIdsMap.each {domain, ids ->
            MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(domain)
            List<MultiFacetAware> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries {i -> [new Pair<String, UUID>(domain, i.id), i]})
        }

        log.debug('Loading {} retrieved catalogue items into semantic links', itemMap.size())
        semanticLinks.each {sl ->
            sl.multiFacetAwareItem = itemMap[new Pair(sl.multiFacetAwareItemDomainType, sl.multiFacetAwareItemId)]
            sl.targetMultiFacetAwareItem = itemMap[new Pair(sl.targetMultiFacetAwareItemDomainType, sl.targetMultiFacetAwareItemId)]
        }

        semanticLinks
    }

    void deleteBySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(MultiFacetAware sourceMultiFacetAwareItem,
                                                                                  MultiFacetAware targetMultiFacetAwareItem,
                                                                                  SemanticLinkType linkType) {
        SemanticLink sl =
            findBySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(sourceMultiFacetAwareItem, targetMultiFacetAwareItem, linkType)
        if (sl) delete(sl)
    }

    SemanticLink createSemanticLink(User createdBy, MultiFacetAware source, MultiFacetAware target, SemanticLinkType linkType) {
        new SemanticLink(createdBy: createdBy.emailAddress, linkType: linkType).with {
            setMultiFacetAwareItem(source)
            setTargetMultiFacetAwareItem(target)
            it
        }
    }

    @Override
    SemanticLink findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        SemanticLink.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<SemanticLink> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map paginate = [:]) {
        findAllBySourceOrTargetMultiFacetAwareItemId(multiFacetAwareItemId, paginate)
    }

    @Override
    DetachedCriteria<SemanticLink> getBaseDeleteCriteria() {
        SemanticLink.by()
    }

    @Override
    void performDeletion(List<UUID> batch) {
        long start = System.currentTimeMillis()
        SemanticLink.byAnyMultiFacetAwareItemIdInList(batch).deleteAll()
        log.trace('{} removed took {}', SemanticLink.simpleName, Utils.timeTaken(start))
    }

    SemanticLink findBySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(MultiFacetAware sourceMultiFacetAwareItem,
                                                                                        MultiFacetAware targetMultiFacetAwareItem,
                                                                                        SemanticLinkType linkType) {
        SemanticLink.
            bySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(sourceMultiFacetAwareItem, targetMultiFacetAwareItem, linkType).get()
    }

    List<SemanticLink> findAllBySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
        List<UUID> sourceMultiFacetAwareItemIds,
        List<UUID> targetMultiFacetAwareItemIds,
        SemanticLinkType linkType) {
        SemanticLink.bySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(sourceMultiFacetAwareItemIds,
                                                                                                        targetMultiFacetAwareItemIds, linkType).list()
    }

    List<SemanticLink> findAllBySourceMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map paginate = [:]) {
        SemanticLink.withFilter(SemanticLink.byMultiFacetAwareItemId(multiFacetAwareItemId), paginate).list(paginate)
    }

    List<SemanticLink> findAllByTargetMultiFacetAwareItemId(Serializable multiFacetAwareItemId, Map paginate = [:]) {
        SemanticLink.withFilter(SemanticLink.byTargetMultiFacetAwareItemId(multiFacetAwareItemId), paginate).list(paginate)
    }

    List<SemanticLink> findAllBySourceOrTargetMultiFacetAwareItemId(Serializable multiFacetAwareItemId, Map paginate = [:]) {
        SemanticLink.withFilter(SemanticLink.byAnyMultiFacetAwareItemId(multiFacetAwareItemId), paginate).list(paginate)
    }

    @Deprecated(forRemoval = true)
    List<SemanticLink> findAllByMultiFacetAwareItemIdAndType(UUID multiFacetAwareItemId, String type, Map paginate = [:]) {
        switch (type) {
            case 'source':
                return findAllBySourceMultiFacetAwareItemId(multiFacetAwareItemId, paginate)
                break
            case 'target':
                return findAllByTargetMultiFacetAwareItemId(multiFacetAwareItemId, paginate)
        }
        findAllBySourceOrTargetMultiFacetAwareItemId(multiFacetAwareItemId, paginate)
    }

}
