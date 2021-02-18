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
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelExtend
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import grails.util.Pair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.util.function.BiFunction
import javax.transaction.Transactional

@Slf4j
@Transactional
class ModelExtendService implements CatalogueItemAwareService<ModelExtend> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

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

    @Override
    DetachedCriteria<ModelExtend> getBaseDeleteCriteria() {
        ModelExtend.by()
    }    

    //Ensure a row is inserted into the _facet table
    void addModelExtendToCatalogueItem(ModelExtend modelExtend, CatalogueItem catalogueItem) {
        catalogueItem.addToModelExtends(modelExtend)
    }

    //Ensure a row is removed from the _facet table
    void removeModelExtendFromCatalogueItem(ModelExtend modelExtend, CatalogueItem catalogueItem) {
        catalogueItem.removeFromModelExtends(modelExtend)
    }     

    void delete(ModelExtend modelExtend) {
        if (!modelExtend) return

        modelExtend.delete()
    }

    void deleteAll(List<ModelExtend> modelExtends, boolean cleanFromOwner = true) {
        if (cleanFromOwner) {
            modelExtends.each {delete(it)}
        } else {
            ModelExtend.deleteAll(modelExtends)
        }
    }

    ModelExtend loadCatalogueItemsIntoModelExtend(ModelExtend modelExtend) {
        if (!modelExtend) return null
        if (!modelExtend.catalogueItem) {
            modelExtend.catalogueItem = findCatalogueItemByDomainTypeAndId(modelExtend.catalogueItemDomainType, modelExtend.catalogueItemId)
        }
        if (!modelExtend.extendedCatalogueItem) {
            modelExtend.extendedCatalogueItem = findCatalogueItemByDomainTypeAndId(modelExtend.extendedCatalogueItemDomainType,
                                                                                   modelExtend.extendedCatalogueItemId)
        }
        modelExtend
    }

    List<ModelExtend> loadCatalogueItemsIntoModelExtends(List<ModelExtend> modelExtends) {
        if (!modelExtends) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all catalogue items for {} model extends', modelExtends.size())
        modelExtends.each {mi ->

            itemIdsMap.compute(mi.catalogueItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(mi.catalogueItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(mi.extendedCatalogueItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(mi.extendedCatalogueItemId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, CatalogueItem> itemMap = [:]
        itemIdsMap.each {domain, ids ->
            CatalogueItemService service = catalogueItemServices.find {it.handles(domain)}
            if (!service) throw new ApiBadRequestException('MIS02', 'Model extend loading for catalogue item with no supporting service')
            List<CatalogueItem> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries {i -> [new Pair<String, UUID>(domain, i.id), i]})
        }

        log.debug('Loading {} retrieved catalogue items into  model extends', itemMap.size())
        modelExtends.each {mi ->
            mi.catalogueItem = itemMap.get(new Pair(mi.catalogueItemDomainType, mi.catalogueItemId))
            mi.extendedCatalogueItem = itemMap.get(new Pair(mi.extendedCatalogueItemDomainType, mi.extendedCatalogueItemId))
        }

        modelExtends
    }

    @Override
    ModelExtend findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        ModelExtend.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<ModelExtend> findAllByCatalogueItemId(UUID catalogueItemId, Map paginate = [:]) {
        ModelExtend.withFilter(ModelExtend.byAnyCatalogueItemId(catalogueItemId), paginate).list(paginate)
    }

    /**
     * Validate that the domain type of the extended catalogue item can be extended by the domain type
     * of the extending catalogue item.
     *
     */
    boolean catalogueItemDomainTypeExtendsDomainType(String catalogueItemDomainType, String extendedCatalogueItemDomainType) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemDomainType)}
        if (!service) throw new ApiBadRequestException('MIS03', 'Model extend loading for catalogue item with no supporting service')
        service.extendsDomainType(extendedCatalogueItemDomainType)
    }

    /**
     * Validate that the extending and extended catalogue items pass domain specific checks for enabling of extends.
     *
     */
    boolean catalogueItemIsExtendableByCatalogueItem(CatalogueItem catalogueItem, CatalogueItem extendedCatalogueItem) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItem.class)}
        if (!service) throw new ApiBadRequestException('MIS04', 'Model extend loading for catalogue item with no supporting service')
        service.isExtendableByCatalogueItem(catalogueItem, extendedCatalogueItem)
    }    

    /**
     * Save a ModelExtend resource with edit logs, and handle any consequential extends.
     *
     * @param currentUser The user doing the extend
     * @param resource The ModelExtend
     *
     */
    ModelExtend saveResource(User currentUser, ModelExtend resource) {
        loadCatalogueItemsIntoModelExtend(resource)

        resource.save(flush: true, validate: false)

        //Add an association between the ModelExtend and CatalogueItem
        addModelExtendToCatalogueItem(resource, resource.catalogueItem)

        //Record the creation against the CatalogueItem belongs
        addCreatedEditToCatalogueItem(currentUser, resource, resource.catalogueItemDomainType, resource.catalogueItemId)

        resource
    }     

}
