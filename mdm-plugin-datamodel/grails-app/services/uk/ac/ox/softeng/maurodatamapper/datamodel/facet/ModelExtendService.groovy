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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import grails.util.Pair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.util.function.BiFunction
import javax.transaction.Transactional

@Slf4j
@Transactional
class ModelExtendService {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    ModelExtend get(Serializable id) {
        ModelExtend.get(id)
    }

    List<ModelExtend> list(Map args) {
        ModelExtend.list(args)
    }

    Long count() {
        ModelExtend.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(ModelExtend modelExtend, boolean flush = false) {
        if (!modelExtend) return
        ModelItemService service = findModelItemService(modelExtend.modelItemDomainType)
        service.removeModelExtendFromModelItem(modelExtend.modelItemId, modelExtend)
        modelExtend.delete(flush: flush)
    }

    DetachedCriteria<ModelExtend> getBaseDeleteCriteria() {
        ModelExtend.by()
    }

    ModelExtend validate(ModelExtend instance) {
        instance.validate()

        instance.extendedModelItem = findModelItemByDomainTypeAndId(instance.extendedModelItemDomainType, instance.extendedModelItemId)
        if (!instance.extendedModelItem) {
            instance.errors.rejectValue('extendedModelItemId',
                                        'modelextend.extended.catalogue.item.not.found',
                                        [instance.extendedModelItemId, instance.extendedModelItemDomainType].toArray(),
                                        'Extended catalogue item [{0}] of type [{1}] cannot be found')
        }

        if (!modelItemDomainTypeCanExtendDomainType(instance.modelItemDomainType, instance.extendedModelItemDomainType)) {
            instance.errors.rejectValue('extendedModelItemDomainType',
                                        'modelextend.domain.does.not.extend.domain',
                                        [instance.modelItemDomainType, instance.extendedModelItemDomainType].toArray(),
                                        'Domain type [{0}] does not extend domain type [{1}]')
        }

        if (!modelItemIsExtendableByModelItem(instance.modelItem, instance.extendedModelItem)) {
            instance.errors.rejectValue('extendedModelItemId',
                                        'modelextend.extended.catalogue.item.cannot.be.used',
                                        [instance.extendedModelItemId, instance.extendedModelItemDomainType].toArray(),
                                        'Extended catalogue item [{0}] of type [{1}] cannot be used')
        }
        instance
    }

    //Ensure a row is inserted into the _facet table
    void addModelExtendToModelItem(ModelExtend modelExtend, ModelItem modelItem) {
        modelItem.addToModelExtends(modelExtend)
    }

    void deleteAll(List<ModelExtend> modelExtends, boolean cleanFromOwner = true) {
        if (cleanFromOwner) {
            modelExtends.each {delete(it)}
        } else {
            ModelExtend.deleteAll(modelExtends)
        }
    }

    ModelExtend loadModelItemsIntoModelExtend(ModelExtend modelExtend) {
        if (!modelExtend) return null
        if (!modelExtend.modelItem) {
            modelExtend.modelItem = findModelItemByDomainTypeAndId(modelExtend.modelItemDomainType, modelExtend.modelItemId)
        }
        if (!modelExtend.extendedModelItem) {
            modelExtend.extendedModelItem = findModelItemByDomainTypeAndId(modelExtend.extendedModelItemDomainType,
                                                                           modelExtend.extendedModelItemId)
        }
        modelExtend
    }

    List<ModelExtend> loadModelItemsIntoModelExtends(List<ModelExtend> modelExtends) {
        if (!modelExtends) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all catalogue items for {} model extends', modelExtends.size())
        modelExtends.each {mi ->

            itemIdsMap.compute(mi.modelItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(mi.modelItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(mi.extendedModelItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(mi.extendedModelItemId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, ModelItem> itemMap = [:]
        itemIdsMap.each {domain, ids ->
            ModelItemService service = modelItemServices.find {it.handles(domain)}
            if (!service) throw new ApiBadRequestException('MIS02', 'Model extend loading for catalogue item with no supporting service')
            List<ModelItem> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries {i -> [new Pair<String, UUID>(domain, i.id), i]})
        }

        log.debug('Loading {} retrieved catalogue items into  model extends', itemMap.size())
        modelExtends.each {mi ->
            mi.modelItem = itemMap.get(new Pair(mi.modelItemDomainType, mi.modelItemId))
            mi.extendedModelItem = itemMap.get(new Pair(mi.extendedModelItemDomainType, mi.extendedModelItemId))
        }

        modelExtends
    }

    ModelExtend findByModelItemIdAndId(UUID modelItemId, Serializable id) {
        ModelExtend.byModelItemIdAndId(modelItemId, id).get()
    }

    List<ModelExtend> findAllByModelItemId(UUID modelItemId, Map paginate = [:]) {
        ModelExtend.withFilter(ModelExtend.byAnyModelItemId(modelItemId), paginate).list(paginate)
    }

    /**
     * Validate that the domain type of the extended catalogue item can be extended by the domain type
     * of the extending catalogue item.
     *
     */
    boolean modelItemDomainTypeCanExtendDomainType(String modelItemDomainType, Class extendedModelItemDomainType) {
        ModelItemService service = findModelItemService(modelItemDomainType)
        service.domainCanExtendModelItemDomain(extendedModelItemDomainType)
    }

    /**
     * Validate that the extending and extended catalogue items pass domain specific checks for enabling of extends.
     *
     */
    boolean modelItemIsExtendableByModelItem(ModelItem modelItem, ModelItem extendedModelItem) {
        ModelItemService service = modelItemServices.find {it.handles(modelItem.class)}
        if (!service) throw new ApiBadRequestException('MIS04', 'Model extend loading for catalogue item with no supporting service')
        service.isExtendableByModelItem(modelItem, extendedModelItem)
    }

    ModelExtend saveResource(ModelExtend resource, boolean flush = false) {
        resource.save(flush: flush, validate: false)
    }

    void saveModelItem(ModelExtend modelExtend) {
        if (!modelExtend) return
        ModelItemService modelItemService = findModelItemService(modelExtend.modelItemDomainType)
        modelItemService.save(modelExtend.modelItem)
    }

    void addFacetToDomain(ModelExtend facet, String domainType, UUID domainId) {
        if (!facet) return
        ModelItem domain = findModelItemByDomainTypeAndId(domainType, domainId)
        facet.modelItem = domain
        domain.addToModelExtends(facet)
    }

    ModelItemService findModelItemService(String modelItemDomainType) {
        ModelItemService service = modelItemServices.find {it.handles(modelItemDomainType)}
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${modelItemDomainType}")
        return service
    }

    ModelItem findModelItemByDomainTypeAndId(String domainType, UUID modelItemId) {
        findModelItemService(domainType).get(modelItemId)
    }


    ModelExtend addCreatedEditToModelItem(User creator, ModelExtend domain, String modelItemDomainType, UUID modelItemId) {
        EditHistoryAware modelItem =
            findModelItemByDomainTypeAndId(modelItemDomainType, modelItemId) as EditHistoryAware
        modelItem.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${modelItem.editLabel}]"
        domain
    }

    ModelExtend addUpdatedEditToModelItem(User editor, ModelExtend domain, String modelItemDomainType, UUID modelItemId,
                                          List<String> dirtyPropertyNames) {
        EditHistoryAware modelItem =
            findModelItemByDomainTypeAndId(modelItemDomainType, modelItemId) as EditHistoryAware
        modelItem.addToEditsTransactionally editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    ModelExtend addDeletedEditToModelItem(User deleter, ModelExtend domain, String modelItemDomainType, UUID modelItemId) {
        EditHistoryAware modelItem =
            findModelItemByDomainTypeAndId(modelItemDomainType, modelItemId) as EditHistoryAware
        modelItem.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${modelItem.editLabel}]"
        domain
    }
}
