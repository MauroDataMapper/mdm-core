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
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.VersionLinkAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import grails.util.Pair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.util.function.BiFunction

@Slf4j
@Transactional
class VersionLinkService implements MultiFacetItemAwareService<VersionLink> {

    @Autowired(required = false)
    List<VersionLinkAwareService> versionLinkAwareServices

    VersionLink get(Serializable id) {
        VersionLink.get(id)
    }

    List<VersionLink> list(Map args) {
        VersionLink.list(args)
    }

    Long count() {
        VersionLink.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(VersionLink versionLink, boolean flush = false) {
        if (!versionLink) return

        VersionLinkAwareService service = findServiceForVersionLinkAwareDomainType(versionLink.modelDomainType)
        service.removeVersionLinkFromModel(versionLink.multiFacetAwareItemId, versionLink)

        versionLink.delete(flush: flush)
    }

    @Override
    VersionLink copy(VersionLink facetToCopy, MultiFacetAware multiFacetAwareItemToCopyInto) {
        VersionLink copy = new VersionLink(linkType: facetToCopy.linkType,
                                           targetModelDomainType: facetToCopy.targetModelDomainType,
                                           targetModelId: facetToCopy.targetModelId,
                                           createdBy: facetToCopy.createdBy)
        (multiFacetAwareItemToCopyInto as Model).addToVersionLinks(copy)
        copy
    }

    void deleteBySourceModelAndTargetModelAndLinkType(Model sourceModel, Model targetModel,
                                                      VersionLinkType linkType) {
        VersionLink sl = findBySourceModelAndTargetModelAndLinkType(sourceModel, targetModel, linkType)
        if (sl) delete(sl)
    }

    @Override
    void saveMultiFacetAwareItem(VersionLink versionLink) {
        if (!versionLink) return
        VersionLinkAwareService service = findServiceForVersionLinkAwareDomainType(versionLink.modelDomainType)
        service.save(versionLink.model)
    }

    @Override
    VersionLink findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        String[] split = pathIdentifier
        VersionLink.byModelId(parentId)
            .eq('linkType', SemanticLinkType.findForLabel(split[0]))
            .eq('targetModelId', Utils.toUuid(split[1]))
            .get()
    }

    @Override
    void addFacetToDomain(VersionLink facet, String domainType, UUID domainId) {
        if (!facet) return
        VersionLinkAware domain = findVersionLinkAwareByDomainTypeAndId(domainType, domainId)
        facet.model = domain
        domain.addToVersionLinks(facet)
    }

    VersionLink loadModelsIntoVersionLink(VersionLink versionLink) {
        if (!versionLink) return null
        if (!versionLink.model) {
            versionLink.model = findVersionLinkAwareByDomainTypeAndId(versionLink.modelDomainType, versionLink.modelId)
        }
        if (!versionLink.targetModel) {
            versionLink.targetModel = findVersionLinkAwareByDomainTypeAndId(versionLink.targetModelDomainType, versionLink.targetModelId)
        }
        versionLink
    }

    List<VersionLink> loadModelsIntoVersionLinks(List<VersionLink> versionLinks) {
        if (!versionLinks) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all items for {} version links', versionLinks.size())
        versionLinks.each {sl ->

            itemIdsMap.compute(sl.modelDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(sl.modelId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(sl.targetModelDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(sl.targetModelId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required items from database')
        Map<Pair<String, UUID>, Model> itemMap = [:]
        itemIdsMap.each {domain, ids ->
            VersionLinkAwareService service = findServiceForVersionLinkAwareDomainType(domain)
            List<VersionLinkAware> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries {i -> [new Pair<String, UUID>(domain, i.id), i]})
        }

        log.debug('Loading {} retrieved catalogue items into semantic links', itemMap.size())
        versionLinks.each {sl ->
            sl.model = itemMap.get(new Pair(sl.multiFacetAwareItemDomainType, sl.multiFacetAwareItemId))
            sl.targetModel = itemMap.get(new Pair(sl.targetModelDomainType, sl.targetModelId))
        }

        versionLinks
    }

    VersionLink createVersionLink(User createdBy, Model source, Model target, VersionLinkType linkType) {
        new VersionLink(createdBy: createdBy.emailAddress, linkType: linkType).with {
            setModel(source)
            setTargetModel(target)
            it
        }
    }

    @Override
    VersionLink findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        findBySourceModelIdAndId(multiFacetAwareItemId, id)
    }

    @Override
    List<VersionLink> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map paginate = [:]) {
        findAllBySourceOrTargetModelId(multiFacetAwareItemId, paginate)
    }

    @Override
    DetachedCriteria<VersionLink> getBaseDeleteCriteria() {
        VersionLink.by()
    }

    @Override
    void performDeletion(List<UUID> batch) {
        long start = System.currentTimeMillis()
        VersionLink.byAnyModelIdInList(batch).deleteAll()
        log.trace('{} removed took {}', VersionLink.simpleName, Utils.timeTaken(start))
    }

    VersionLink findBySourceModelIdAndId(UUID modelId, Serializable id) {
        VersionLink.byModelIdAndId(modelId, id).get()
    }


    VersionLink findBySourceModelAndTargetModelAndLinkType(VersionAware sourceModel, VersionAware targetModel,
                                                           VersionLinkType linkType) {
        VersionLink.bySourceModelAndTargetModelAndLinkType(sourceModel, targetModel, linkType).get()
    }

    List<VersionLink> findAllBySourceModelId(UUID modelId, Map paginate = [:]) {
        VersionLink.withFilter(VersionLink.byModelId(modelId), paginate).list(paginate)
    }

    VersionLink findBySourceModelIdAndLinkType(UUID modelId, VersionLinkType linkType) {
        VersionLink.byModelIdAndLinkType(modelId, linkType).get()
    }

    List<VersionLink> findAllByTargetModelId(Serializable modelId, Map paginate = [:]) {
        VersionLink.withFilter(VersionLink.byTargetModelId(modelId), paginate).list(paginate)
    }

    List<VersionLink> findAllBySourceOrTargetModelId(Serializable modelId, Map paginate = [:]) {
        VersionLink.withFilter(VersionLink.byAnyModelId(modelId), paginate).list(paginate)
    }

    VersionLink findLatestLinkSupersedingModelId(String modelType, UUID modelId) {
        VersionLink.by()
            .inList('linkType', VersionLinkType.NEW_MODEL_VERSION_OF, VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)
            .eq('targetModelDomainType', modelType)
            .eq('targetModelId', modelId)
            .sort('lastUpdated', 'desc')
            .get()
    }

    VersionLink findLatestLinkModelSupersedingModelId(String modelType, UUID modelId) {
        VersionLink.by().
            inList('linkType', VersionLinkType.NEW_MODEL_VERSION_OF)
            .eq('targetModelDomainType', modelType)
            .eq('targetModelId', modelId)
            .sort('lastUpdated', 'desc')
            .get()
    }

    VersionLink findLatestLinkDocumentationSupersedingModelId(String modelType, UUID modelId) {
        VersionLink.by().
            eq('linkType', VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)
            .eq('targetModelDomainType', modelType)
            .eq('targetModelId', modelId)
            .sort('lastUpdated', 'desc')
            .get()
    }

    VersionLinkAware findVersionLinkAwareByDomainTypeAndId(String domainType, UUID modelId) {
        VersionLinkAwareService service = findServiceForVersionLinkAwareDomainType(domainType)
        VersionLinkAware model = service.get(modelId)
        if (!model) throw new ApiBadRequestException('VLS04', "VersionLinkAware of type [${domainType}] id [${modelId}] cannot be found")
        model
    }

    static VersionLink findBySourceModelAndLinkType(VersionAware sourceModel, VersionLinkType linkType) {
        VersionLink.bySourceModelAndLinkType(sourceModel, linkType).get()
    }

    List<UUID> filterModelIdsWhereModelIdIsDocumentSuperseded(String modelType, List<UUID> modelIds) {
        if (!modelIds) return []
        VersionLink.by()
            .eq('linkType', VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)
            .eq('targetModelDomainType', modelType)
            .inList('targetModelId', modelIds)
            .property('targetModelId').list() as List<UUID>
    }


    List<UUID> filterModelIdsWhereModelIdIsModelSuperseded(List<UUID> modelIds) {
        if (!modelIds) return []
        findAllByTargetMultiFacetAwareItemIdInListAndIsModelSuperseded(modelIds).collect {it.targetModelId}
    }

    List<VersionLink> findAllByTargetMultiFacetAwareItemIdInListAndIsModelSuperseded(List<UUID> modelIds) {
        if (!modelIds) return []

        VersionLink.by()
            .eq('linkType', VersionLinkType.NEW_MODEL_VERSION_OF)
            .inList('targetModelId', modelIds).list()
    }

    VersionLink findBySourceModel(VersionAware source) {
        VersionLink.byModelId(source.id).get()
    }

    VersionLinkAwareService findServiceForVersionLinkAwareDomainType(String domainType) {
        VersionLinkAwareService service = versionLinkAwareServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${domainType}")
        return service
    }
}
