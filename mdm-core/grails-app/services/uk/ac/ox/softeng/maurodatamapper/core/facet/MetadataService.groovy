/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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


import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.HibernateSearchIndexingService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.facet.NamespaceKeys
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory

@Slf4j
@Transactional
class MetadataService implements MultiFacetItemAwareService<Metadata> {

    SessionFactory sessionFactory
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    HibernateSearchIndexingService hibernateSearchIndexingService

    Metadata get(Serializable id) {
        Metadata.get(id)
    }

    @Override
    List<Metadata> getAll(Collection<UUID> resourceIds) {
        Metadata.getAll(resourceIds)
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
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(metadata.multiFacetAwareItemDomainType)
        service.removeMetadataFromMultiFacetAware(metadata.multiFacetAwareItemId, metadata)
        metadata.delete(flush: flush)
    }

    @Override
    Metadata copy(Metadata facetToCopy, MultiFacetAware multiFacetAwareItemToCopyInto) {
        Metadata copy = new Metadata(namespace: facetToCopy.namespace, key: facetToCopy.key, value: facetToCopy.value, createdBy: facetToCopy.createdBy)
        multiFacetAwareItemToCopyInto.addToMetadata(copy)
        copy
    }

    @Override
    void saveMultiFacetAwareItem(Metadata metadata) {
        if (!metadata) return
        MultiFacetAwareService multiFacetAwareItemService = findServiceForMultiFacetAwareDomainType(metadata.multiFacetAwareItemDomainType)
        multiFacetAwareItemService?.save(metadata.multiFacetAwareItem)
        updateMultiFacetAwareItemIndex(metadata)
    }


    void updateMultiFacetAwareItemIndex(Metadata metadata) {
        if (!metadata.multiFacetAwareItem) {
            metadata.multiFacetAwareItem = findMultiFacetAwareItemByDomainTypeAndId(metadata.multiFacetAwareItemDomainType, metadata.multiFacetAwareItemId)
        }
        hibernateSearchIndexingService.addEntityToIndexingPlan(metadata.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(Metadata facet, String domainType, UUID domainId) {
        if (!facet) return
        MultiFacetAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId)
        facet.multiFacetAwareItem = domain
        domain.addToMetadata(facet)
    }

    def saveAll(Collection<Metadata> metadata) {
        Collection<Metadata> alreadySaved = metadata.findAll {it.ident() && it.isDirty()}
        Collection<Metadata> notSaved = metadata.findAll {!it.ident()}
        if (alreadySaved) {
            log.debug('Straight saving {} metadata', alreadySaved.size())
            Metadata.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.debug('Batch saving {} metadata', notSaved.size())
            List batch = []
            int count = 0

            notSaved.each {de ->

                batch += de
                count++
                if (count % Metadata.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }
            }
            batchSave(batch)
            batch.clear()
        }
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

        MultiFacetAware multiFacetAwareItem =
            metadata.multiFacetAwareItem ?: findMultiFacetAwareItemByDomainTypeAndId(metadata.multiFacetAwareItemDomainType,
                                                                                     metadata.multiFacetAwareItemId)

        if (multiFacetAwareItem.metadata.any {md -> md != metadata && md.namespace == metadata.namespace && md.key == metadata.key}) {
            metadata.errors.rejectValue('key', 'default.not.unique.message', ['key', Metadata.name, metadata.value].toArray(),
                                        'Property [{0}] of class [{1}] with value [{2}] must be unique')
            return false
        }
        true
    }

    @Override
    Metadata findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        Metadata.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<Metadata> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination = [:]) {
        Metadata.withFilter(Metadata.byMultiFacetAwareItemId(multiFacetAwareItemId), pagination).list(pagination)
    }

    List<Metadata> findAllByMultiFacetAwareItemIdAndNamespace(UUID multiFacetAwareItemId, String namespace, Map pagination = [:]) {
        Metadata.byMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, namespace).list(pagination)
    }

    List<MetadataAware> findAllMultiFacetAwareItemsByNamespace(String namespace, String domainType = null, Map pagination = [:]) {
        List<MetadataAware> returnResult = []
        catalogueItemServices.each {catalogueItemService ->
            if (!domainType || catalogueItemService.handles(domainType)) {
                returnResult.addAll(catalogueItemService.findAllByMetadataNamespace(namespace))
            }
        }
        containerServices.each {containerService ->
            if (!domainType || containerService.handles(domainType)) {
                returnResult.addAll(containerService.findAllByMetadataNamespace(namespace))
            }
        }
        return new PaginatedResultList<MetadataAware>(returnResult, pagination)
    }


    List<Metadata> findAllByNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        Metadata.byNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    DetachedCriteria<Metadata> getBaseDeleteCriteria() {
        Metadata.by()
    }

    List<Metadata> findAllByMultiFacetAwareItemIdAndNotNamespaces(UUID multiFacetAwareItemId, List<String> namespaces, Map pagination = [:]) {
        if (!namespaces || namespaces.size() == 0) {
            return Metadata.byMultiFacetAwareItemId(multiFacetAwareItemId, pagination).list(pagination)
        }
        Metadata.byMultiFacetAwareItemIdAndNotNamespaces(multiFacetAwareItemId, namespaces, pagination).list(pagination)
    }

    List<NamespaceKeys> findNamespaceKeysIlikeNamespace(String namespacePrefix) {
        createListOfNamespaceKeys(Metadata.findAllDistinctNamespacesKeysIlikeNamespace(namespacePrefix),
                                  mauroDataMapperServiceProviderService.findProvidersIlikeNamespace(namespacePrefix))
    }

    List<NamespaceKeys> findNamespaceKeys() {
        createListOfNamespaceKeys(Metadata.findAllDistinctNamespacesKeys(),
                                  mauroDataMapperServiceProviderService.getProviderServices())
    }

    List<NamespaceKeys> createListOfNamespaceKeys(Map<String, List<String>> databaseNamespaceKeys, Collection<MauroDataMapperService> services) {
        List<NamespaceKeys> namespaceKeys = databaseNamespaceKeys
            .collect {ns, keys ->
                createNamespaceKeys(ns, keys, services.find {it.namespace == ns})
            }

        services
            .findAll {service ->
                !(service.namespace in namespaceKeys.collect {it.namespace})
            }.each {service ->
            namespaceKeys << createNamespaceKeys(service.namespace, [], service)
        }
        namespaceKeys.toSet().sort()
    }

    NamespaceKeys createNamespaceKeys(String namespace, Collection<String> keys, MauroDataMapperService service) {
        NamespaceKeys namespaceKeys = new NamespaceKeys()
            .forNamespace(namespace)
            .withKeys(keys)
            .defaultNamespace(NamespaceKeys.KNOWN_DEFAULT_NAMESPACE_PREFIXES.any {namespace.startsWith(it)})
        if (service) {
            namespaceKeys
                .editable(service.allowsExtraMetadataKeys())
                .defaultNamespace(true)
                .withKeys(service.knownMetadataKeys)
        }
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

    @Override
    Metadata findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        String ns
        String key
        pathIdentifier.find(/^(.+)\.(.+)$/) {all, foundNs, foundKey ->
            ns = foundNs
            key = foundKey
        }

        Metadata.byMultiFacetAwareItemId(parentId)
            .eq('namespace', ns)
            .eq('key', key)
            .get()
    }
}