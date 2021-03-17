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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
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
class ModelImportService {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

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

    void delete(ModelImport modelImport, boolean flush = false) {
        if (!modelImport) return
        ModelItemService service = findCatalogueItemService(modelImport.catalogueItemDomainType)
        service.removeModelImportFromCatalogueItem(modelImport.catalogueItemId, modelImport)
        modelImport.delete(flush: flush)
    }

    ModelImport validate(ModelImport instance) {

        instance.validate()

        instance.importedModelItem = findModelItemByDomainTypeAndId(instance.importedModelItemDomainType,
                                                                    instance.importedModelItemId)
        if (!instance.importedModelItem) {
            instance.errors.rejectValue('importedModelItemId',
                                        'modelimport.imported.catalogue.item.not.found',
                                        [instance.importedModelItemId, instance.importedModelItemDomainType].toArray(),
                                        'Imported catalogue item [{0}] of type [{1}] cannot be found')
        }

        if (!catalogueItemDomainTypeImportsDomainType(instance.catalogueItemDomainType, instance.importedModelItem.class)) {
            instance.errors.rejectValue('importedModelItemDomainType',
                                        'modelimport.domain.does.not.import.domain',
                                        [instance.catalogueItemDomainType, instance.importedModelItemDomainType].toArray(),
                                        'Domain type [{0}] does not import domain type [{1}]')
        }

        if (!modelItemIsImportableByCatalogueItem(instance.importedModelItem, instance.catalogueItem)) {
            instance.errors.rejectValue('importedModelItemId',
                                        'modelimport.imported.catalogue.item.cannot.be.used',
                                        [instance.importedModelItemId, instance.importedModelItemDomainType].toArray(),
                                        'Imported catalogue item [{0}] of type [{1}] cannot be used')
        }
        instance
    }

    DetachedCriteria<ModelImport> getBaseDeleteCriteria() {
        ModelImport.by()
    }

    ModelImport save(ModelImport resource, boolean flush = false) {
        resource.save(flush: flush, validate: false)
    }

    void checkAndPerformAdditionalImports(ModelImport resource) {
        //Does the import of this resource require any other things to be imported?
        ModelItemService service = findModelItemService(resource.importedModelItemDomainType)
        service.performAdditionalModelImports(resource)
    }

    void addModelImportToCatalogueItem(ModelImport modelImport, String domainType, UUID domainId) {
        if (!modelImport) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        modelImport.catalogueItem = domain
        domain.addToModelImports(modelImport)
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
            modelImport.catalogueItem = findCatalogueItemByDomainTypeAndId(modelImport.catalogueItemDomainType,
                                                                           modelImport.catalogueItemId)
        }
        if (!modelImport.importedModelItem) {
            modelImport.importedModelItem = findModelItemByDomainTypeAndId(modelImport.importedModelItemDomainType,
                                                                           modelImport.importedModelItemId)
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
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(mi.catalogueItemId)
                    uuids
                }
            ] as BiFunction)

            itemIdsMap.compute(mi.importedModelItemDomainType, [
                apply: {String s, Set<UUID> uuids ->
                    uuids = uuids ?: new HashSet<>() as Set<UUID>
                    uuids.add(mi.importedModelItemId)
                    uuids
                }
            ] as BiFunction)
        }

        log.debug('Loading required catalogue items from database')
        Map<Pair<String, UUID>, CatalogueItem> itemMap = [:]
        itemIdsMap.each {domain, ids ->
            CatalogueItemService service = findCatalogueItemService(domain)
            List<ModelItem> items = service.getAll(ids)
            itemMap.putAll(items.collectEntries {i -> [new Pair<String, UUID>(domain, i.id), i]})
        }

        log.debug('Loading {} retrieved catalogue items into  model imports', itemMap.size())
        modelImports.each {mi ->
            mi.catalogueItem = itemMap[new Pair(mi.catalogueItemDomainType, mi.catalogueItemId)]
            mi.importedModelItem = itemMap[new Pair(mi.importedModelItemDomainType, mi.importedModelItemId)] as ModelItem
        }

        modelImports
    }

    ModelImport findByModelItemIdAndId(UUID modelItemId, Serializable id) {
        ModelImport.byCatalogueItemIdAndId(modelItemId, id).get()
    }

    List<ModelImport> findAllByItemId(UUID catalogueItemId, Map paginate = [:]) {
        ModelImport.withFilter(ModelImport.byAnyItemId(catalogueItemId), paginate).list(paginate)
    }

    boolean hasCatalogueItemImportedCatalogueItem(CatalogueItem catalogueItem, CatalogueItem importedCatalogueItem) {
        ModelImport.byCatalogueItemIdAndImportedCatalogueItemId(catalogueItem.id, importedCatalogueItem.id).count()
    }

    /**
     * Validate that the domain type of the imported catalogue item can be imported by the domain type
     * of the importing catalogue item.
     *
     */
    boolean catalogueItemDomainTypeImportsDomainType(String catalogueItemDomainType, Class importedModelItemClass) {
        CatalogueItemService service = findCatalogueItemService(catalogueItemDomainType)
        service.domainCanImportModelItemDomain(importedModelItemClass)
    }

    /**
     * Validate that the importing and imported catalogue items pass domain specific checks for enabling of imports.
     *
     */
    boolean modelItemIsImportableByCatalogueItem(ModelItem importedModelItem, CatalogueItem catalogueItem) {
        CatalogueItemService service = findCatalogueItemService(catalogueItem.domainType)
        service.domainCanImportModelItem(catalogueItem, importedModelItem)
    }

    void saveCatalogueItem(ModelImport modelImport) {
        if (!modelImport) return
        CatalogueItemService service = findCatalogueItemService(modelImport.catalogueItemDomainType)
        service.save(modelImport.catalogueItem)
    }

    ModelImport addCreatedEditToCatalogueItem(User creator, ModelImport domain, String catalogueItemDomainType, UUID catalogueItemId) {
        EditHistoryAware catalogueItem =
            findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId) as EditHistoryAware
        catalogueItem.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    ModelImport addUpdatedEditToModelItem(User editor, ModelImport domain, String catalogueItemDomainType, UUID catalogueItemId,
                                          List<String> dirtyPropertyNames) {
        EditHistoryAware catalogueItem =
            findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId) as EditHistoryAware
        catalogueItem.addToEditsTransactionally editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    ModelImport addDeletedEditToModelItem(User deleter, ModelImport domain, String catalogueItemDomainType, UUID catalogueItemId) {
        EditHistoryAware catalogueItem =
            findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId) as EditHistoryAware
        catalogueItem.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }

    CatalogueItemService findCatalogueItemService(String catalogueItemDomainType) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemDomainType)}
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${catalogueItemDomainType}")
        return service
    }

    ModelItemService findModelItemService(String modelItemDomainType) {
        ModelItemService service = modelItemServices.find {it.handles(modelItemDomainType)}
        if (!service) throw new ApiBadRequestException('FS01', "No supporting service for ${modelItemDomainType}")
        return service
    }

    CatalogueItem findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        findCatalogueItemService(domainType).get(catalogueItemId)
    }

    ModelItem findModelItemByDomainTypeAndId(String domainType, UUID modelItemId) {
        findModelItemService(domainType).get(modelItemId)
    }
}
