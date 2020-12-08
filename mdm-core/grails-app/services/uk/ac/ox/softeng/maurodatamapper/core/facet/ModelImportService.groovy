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
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.util.Pair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.util.function.BiFunction
import javax.transaction.Transactional

@Slf4j
@Transactional
class ModelImportService implements CatalogueItemAwareService<ModelImport> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    ModelImport get(Serializable id) {
        ModelImport.get(id)
    }

    List<ModelImport> list(Map args) {
        ModelImport.list(args)
    }

    Long count() {
        ModelImport.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    //Ensure a row is inserted into the _facet table
    void addModelImportToCatalogueItem(ModelImport modelImport, CatalogueItem catalogueItem) {
        catalogueItem.addToModelImports(modelImport)
    }

    //Ensure a row is removed from the _facet table
    void removeModelImportFromCatalogueItem(ModelImport modelImport, CatalogueItem catalogueItem) {
        catalogueItem.removeFromModelImports(modelImport)
    }     

    ModelImport save(ModelImport modelImport) {
        modelImport.save(flush: true)
    }

    void delete(ModelImport modelImport, CatalogueItem) {
        if (!modelImport) return

        modelImport.delete()
    }

    void deleteAll(List<ModelImport> modelImports, boolean cleanFromOwner = true) {
        if (cleanFromOwner) {
            modelImports.each {delete(it)}
        } else {
            ModelImport.deleteAll(modelImports)
        }
    }

    ModelImport loadCatalogueItemsIntoModelImport(ModelImport modelImport) {
        if (!modelImport) return null
        if (!modelImport.catalogueItem) {
            modelImport.catalogueItem = findCatalogueItemByDomainTypeAndId(modelImport.catalogueItemDomainType, modelImport.catalogueItemId)
        }
        if (!modelImport.importedCatalogueItem) {
            modelImport.importedCatalogueItem = findCatalogueItemByDomainTypeAndId(modelImport.importedCatalogueItemDomainType,
                                                                                   modelImport.importedCatalogueItemId)
        }
        modelImport
    }

    List<ModelImport> loadCatalogueItemsIntoModelImports(List<ModelImport> modelImports) {
        if (!modelImports) return []
        Map<String, Set<UUID>> itemIdsMap = [:]

        log.debug('Collecting all catalogue items for {} model imports', modelImports.size())
        modelImports.each {mi ->

            itemIdsMap.compute(mi.catalogueItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(mi.catalogueItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(mi.importedCatalogueItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>()
                    uuids.add(mi.importedCatalogueItemId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, CatalogueItem> itemMap = [:]
        itemIdsMap.each {domain, ids ->
            CatalogueItemService service = catalogueItemServices.find {it.handles(domain)}
            if (!service) throw new ApiBadRequestException('MIS02', 'Model import loading for catalogue item with no supporting service')
            List<CatalogueItem> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries {i -> [new Pair<String, UUID>(domain, i.id), i]})
        }

        log.debug('Loading {} retrieved catalogue items into  model imports', itemMap.size())
        modelImports.each {mi ->
            mi.catalogueItem = itemMap.get(new Pair(mi.catalogueItemDomainType, mi.catalogueItemId))
            mi.importedCatalogueItem = itemMap.get(new Pair(mi.importedCatalogueItemDomainType, mi.importedCatalogueItemId))
        }

        modelImports
    }

    ModelImport createModelImport(User createdBy, CatalogueItem source, CatalogueItem imported) {
        new ModelImport(createdBy: createdBy.emailAddress).with {
            setCatalogueItem(source)
            setTargetCatalogueItem(imported)
            it
        }
    }

    @Override
    ModelImport findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        ModelImport.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<ModelImport> findAllByCatalogueItemId(UUID catalogueItemId, Map paginate = [:]) {
        ModelImport.withFilter(ModelImport.byAnyCatalogueItemId(catalogueItemId), paginate).list(paginate)
    }

    /**
     * Validate that the domain type of the imported catalogue item can be imported by the domain type
     * of the importing catalogue item.
     *
     */
    boolean catalogueItemDomainTypeImportsDomainType(String catalogueItemDomainType, String importedCatalogueItemDomainType) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemDomainType)}
        if (!service) throw new ApiBadRequestException('MIS03', 'Model import loading for catalogue item with no supporting service')
        service.importsDomainType(importedCatalogueItemDomainType)
    }

    /**
     * Validate that the importing and imported catalogue items pass domain specific checks for enabling of imports.
     *
     */
    boolean catalogueItemIsImportableByCatalogueItem(CatalogueItem catalogueItem, CatalogueItem importedCatalogueItem) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItem.class)}
        if (!service) throw new ApiBadRequestException('MIS04', 'Model import loading for catalogue item with no supporting service')
        service.isImportableByCatalogueItem(catalogueItem, importedCatalogueItem)
    }    

}
