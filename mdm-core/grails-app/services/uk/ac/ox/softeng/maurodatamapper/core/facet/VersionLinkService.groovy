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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
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
class VersionLinkService implements CatalogueItemAwareService<VersionLink> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List<ModelService> modelServices

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

    VersionLink save(VersionLink versionLink) {
        versionLink.save(flush: true)
    }

    void delete(VersionLink versionLink) {
        if (!versionLink) return

        ModelService service = modelServices.find { it.handles(versionLink.catalogueItemDomainType) }
        if (!service) throw new ApiBadRequestException('VLS01', 'Version link removal for catalogue item with no supporting service')
        service.removeVersionLinkFromModel(versionLink.catalogueItemId, versionLink)

        versionLink.delete()
    }

    void deleteBySourceModelAndTargetModelAndLinkType(Model sourceModel, Model targetModel,
                                                      VersionLinkType linkType) {
        VersionLink sl = findBySourceModelAndTargetModelAndLinkType(sourceModel, targetModel, linkType)
        if (sl) delete(sl)
    }

    VersionLink loadModelsIntoVersionLink(VersionLink versionLink) {
        if (!versionLink) return null
        if (!versionLink.model) {
            versionLink.model = findModelByDomainTypeAndId(versionLink.catalogueItemDomainType, versionLink.catalogueItemId)
        }
        if (!versionLink.targetModel) {
            versionLink.targetModel = findModelByDomainTypeAndId(versionLink.targetModelDomainType, versionLink.targetModelId)
        }
        versionLink
    }

    List<VersionLink> loadModelsIntoVersionLinks(List<VersionLink> versionLinks) {
        if (!versionLinks) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all catalogue items for {} semantic links', versionLinks.size())
        versionLinks.each { sl ->

            itemIdsMap.compute(sl.catalogueItemDomainType, [
                apply: { String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(sl.catalogueItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(sl.targetModelDomainType, [
                apply: { String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(sl.targetModelId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, Model> itemMap = [:]
        itemIdsMap.each { domain, ids ->
            ModelService service = modelServices.find { it.handles(domain) }
            if (!service) throw new ApiBadRequestException('VLS02', 'Semantic link loading for model item with no supporting service')
            List<Model> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries { i -> [new Pair<String, UUID>(domain, i.id), i] })
        }

        log.debug('Loading {} retrieved catalogue items into semantic links', itemMap.size())
        versionLinks.each { sl ->
            sl.model = itemMap.get(new Pair(sl.catalogueItemDomainType, sl.catalogueItemId))
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
    VersionLink findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        findBySourceModelIdAndId(catalogueItemId, id)
    }

    @Override
    List<VersionLink> findAllByCatalogueItemId(UUID catalogueItemId, Map paginate = [:]) {
        findAllBySourceOrTargetModelId(catalogueItemId, paginate)
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


    VersionLink findBySourceModelAndTargetModelAndLinkType(Model sourceModel, Model targetModel,
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
        VersionLink.by().or {
            and {
                inList('linkType', VersionLinkType.SUPERSEDED_BY_DOCUMENTATION)
                    .eq('catalogueItemDomainType', modelType)
                    .eq('catalogueItemId', modelId)
            }
            and {
                inList('linkType', VersionLinkType.NEW_MODEL_VERSION_OF, VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)
                    .eq('targetModelDomainType', modelType)
                    .eq('targetModelId', modelId)
            }
        }
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
        VersionLink.by().or {
            and {
                eq('linkType', VersionLinkType.SUPERSEDED_BY_DOCUMENTATION)
                    .eq('catalogueItemDomainType', modelType)
                    .eq('catalogueItemId', modelId)
            }
            and {
                eq('linkType', VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)
                    .eq('targetModelDomainType', modelType)
                    .eq('targetModelId', modelId)
            }
        }
            .sort('lastUpdated', 'desc')
            .get()
    }

    @Deprecated(forRemoval = true)
    List<VersionLink> findAllByModelIdAndType(UUID modelId, String type, Map paginate = [:]) {
        switch (type) {
            case 'source':
                return findAllBySourceModelId(modelId, paginate)
                break
            case 'target':
                return findAllByTargetModelId(modelId, paginate)
        }
        findAllBySourceOrTargetModelId(modelId, paginate)
    }

    Model findModelByDomainTypeAndId(String domainType, UUID modelId) {
        ModelService service = modelServices.find { it.handles(domainType) }
        if (!service) throw new ApiBadRequestException('VLS03', "Retrieval of model of type [${domainType}] with no supporting service")
        Model model = service.get(modelId)
        if (!model) throw new ApiBadRequestException('VLS04', "Model of type [${domainType}] id [${modelId}] cannot be found")
        model
    }

    static VersionLink findBySourceModelAndLinkType(Model sourceModel, VersionLinkType linkType) {
        VersionLink.bySourceModelAndLinkType(sourceModel, linkType).get()
    }

    List<UUID> filterModelIdsWhereModelIdIsDocumentSuperseded(String modelType, List<UUID> modelIds) {
        if (!modelIds) return []
        List<UUID> sourceForSupersededByIds = VersionLink.by()
            .eq('linkType', VersionLinkType.SUPERSEDED_BY_DOCUMENTATION)
            .eq('catalogueItemDomainType', modelType)
            .inList('catalogueItemId', modelIds)
            .property('catalogueItemId').list() as List<UUID>

        List<UUID> targetOfNewVersionOf = VersionLink.by()
            .eq('linkType', VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)
            .eq('targetModelDomainType', modelType)
            .inList('targetModelId', modelIds)
            .property('targetModelId').list() as List<UUID>


        Utils.mergeLists(sourceForSupersededByIds, targetOfNewVersionOf)
    }


    List<UUID> filterModelIdsWhereModelIdIsModelSuperseded(String modelType, List<UUID> modelIds) {
        if (!modelIds) return []
        findAllByTargetCatalogueItemIdInListAndIsModelSuperseded(modelIds).collect { it.targetModelId }
    }

    List<VersionLink> findAllByTargetCatalogueItemIdInListAndIsModelSuperseded(List<UUID> modelIds) {
        if (!modelIds) return []

        VersionLink.by()
            .eq('linkType', VersionLinkType.NEW_MODEL_VERSION_OF)
            .inList('targetModelId', modelIds).list()
    }
}
