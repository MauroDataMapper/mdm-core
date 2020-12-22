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


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.facet.NamespaceKeys
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import javax.transaction.Transactional

@Slf4j
@Transactional
class MetadataService implements CatalogueItemAwareService<Metadata> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    SessionFactory sessionFactory
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    Metadata get(Serializable id) {
        Metadata.get(id)
    }

    List<Metadata> list(Map args) {
        Metadata.list(args)
    }

    Long count() {
        Metadata.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    @Override
    void delete(Metadata metadata, boolean flush = false) {
        if (!metadata) return
        CatalogueItemService service = findCatalogueItemService(metadata.catalogueItemDomainType)
        service.removeMetadataFromCatalogueItem(metadata.catalogueItemId, metadata)
        metadata.delete(flush: flush)
    }

    void copy(CatalogueItem target, Metadata item, UserSecurityPolicyManager userSecurityPolicyManager) {
        target.addToMetadata(item.namespace, item.key, item.value, userSecurityPolicyManager.user)
    }

    @Override
    void saveCatalogueItem(Metadata metadata) {
        if (!metadata) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(metadata.catalogueItemDomainType)
        catalogueItemService.save(metadata.catalogueItem)
    }

    @Override
    void addFacetToDomain(Metadata facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
        domain.addToMetadata(facet)
    }

    void batchSave(Collection<Metadata> metadata) {
        log.trace('Batch saving Metadata')
        long start = System.currentTimeMillis()
        List batch = []
        int count = 0
        metadata.each {relationship ->
            batch += relationship
            count++
            if (count % Metadata.BATCH_SIZE == 0) {
                singleBatchSave(batch)
                batch.clear()
            }
        }
        // Save final batch of terms
        singleBatchSave(batch)
        batch.clear()
        log.debug('{} Metadata batch saved, took {}', metadata.size(), Utils.timeTaken(start))
    }

    boolean validate(Metadata metadata) {
        boolean valid = metadata.validate()
        if (!valid) return false

        CatalogueItem catalogueItem = metadata.catalogueItem ?: findCatalogueItemByDomainTypeAndId(metadata.catalogueItemDomainType,
                                                                                                   metadata.catalogueItemId)

        if (catalogueItem.metadata.any { md -> md != metadata && md.namespace == metadata.namespace && md.key == metadata.key }) {
            metadata.errors.rejectValue('key', 'default.not.unique.message', ['key', Metadata.name, metadata.value].toArray(),
                                        'Property [{0}] of class [{1}] with value [{2}] must be unique')
            return false
        }
        true
    }

    @Override
    Metadata findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        Metadata.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<Metadata> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination = [:]) {
        Metadata.withFilter(Metadata.byCatalogueItemId(catalogueItemId), pagination).list(pagination)
    }

    List<Metadata> findAllByCatalogueItemIdAndNamespace(UUID catalogueItemId, String namespace, Map pagination = [:]) {
        Metadata.byCatalogueItemIdAndNamespace(catalogueItemId, namespace).list(pagination)
    }

    @Override
    DetachedCriteria<Metadata> getBaseDeleteCriteria() {
        Metadata.by()
    }

    List<Metadata> findAllByCatalogueItemIdAndNotNamespaces(UUID catalogueItemId, List<String> namespaces, Map pagination = [:]) {
        if(!namespaces || namespaces.size() == 0) {
            return Metadata.byCatalogueItemId(catalogueItemId).list(pagination)
        }
        Metadata.byCatalogueItemIdAndNotNamespaces(catalogueItemId, namespaces).list(pagination)
    }

    List<NamespaceKeys> findNamespaceKeysIlikeNamespace(String namespacePrefix) {

        Set<NamespaceKeys> listOfNamespaceKeys = new HashSet<>()
        Set<MauroDataMapperService> services = mauroDataMapperServiceProviderService.findProvidersIlikeNamespace(namespacePrefix)

        Collection<String> namespaces = Metadata.findAllDistinctNamespacesIlike(namespacePrefix)

        for (MauroDataMapperService service : services) {
            listOfNamespaceKeys.add(findNamespaceKeysByServiceOrNamespace(service, service.getNamespace()))
            namespaces.remove(service.getNamespace()) // Remove service namespaces from list
        }

        // Add remaining namespaces to list as editable
        for (String namespace : namespaces) {
            listOfNamespaceKeys.add(findNamespaceKeysByServiceOrNamespace(null, namespace))
        }

        return listOfNamespaceKeys.sort()
    }

    List<NamespaceKeys> findNamespaceKeys() {
        Set<NamespaceKeys> listOfNamespaceKeys = new HashSet<>()
        Set<MauroDataMapperService> services = mauroDataMapperServiceProviderService.getProviderServices()

        Collection<String> namespaces = Metadata.findAllDistinctNamespaces()
        for (MauroDataMapperService service : services) {
            if (!(listOfNamespaceKeys.any { it.namespace == service.namespace })) {
                listOfNamespaceKeys.add(findNamespaceKeysByServiceOrNamespace(service, service.getNamespace()))
                namespaces.remove(service.getNamespace()) // Remove plugin namespaces from list
            }
        }

        // Add remaining namespaces to list as editable
        for (String namespace : namespaces) {
            listOfNamespaceKeys.add(findNamespaceKeysByServiceOrNamespace(null, namespace))
        }

        listOfNamespaceKeys.sort()
    }

    NamespaceKeys findNamespaceKeysByServiceOrNamespace(MauroDataMapperService service, String namespace) {

        NamespaceKeys namespaceKeys = new NamespaceKeys()
        namespaceKeys.editable = service ? service.allowsExtraMetadataKeys() : true
        namespaceKeys.namespace = namespace
        namespaceKeys.defaultNamespace = namespace.startsWith('ox.softeng.maurodatamapper') ||
                                         namespace.startsWith('uk.ac.ox.softeng.maurodatamapper') ||
                                         service

        if (!service) {
            namespaceKeys.keys = Metadata.findAllDistinctKeysByNamespace(namespace).sort()
            return namespaceKeys
        }

        // We need to ensure a set of keys but we want to return a sorted list
        Set<String> keys = new HashSet<>()
        keys.addAll(service.getKnownMetadataKeys())
        if (service.allowsExtraMetadataKeys()) {
            keys.addAll(Metadata.findAllDistinctKeysByNamespace(namespace))
        }

        namespaceKeys.keys = keys.sort()
        namespaceKeys
    }

    private void singleBatchSave(Collection<Metadata> metadata) {
        long start = System.currentTimeMillis()
        log.trace('Batch saving {} metadata', metadata.size())

        Metadata.saveAll(metadata)

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

}