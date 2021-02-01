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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.facet.NamespaceKeys
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeObjectDiffData
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
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

    @Autowired(required = false)
    List <ContainerService> containerServices

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
        if (service) service.removeMetadataFromCatalogueItem(metadata.catalogueItemId, metadata)
        else {
            ContainerService containerService = containerServices.find { it.handles(metadata.catalogueItemDomainType) }
            if (containerService) containerService.removeMetadataFromContainer(metadata.catalogueItemId, metadata)
            else throw new ApiBadRequestException('MS01', 'Metadata removal with no supporting service')
        }
        metadata.delete(flush: flush)
    }

    void copy(CatalogueItem target, Metadata item, UserSecurityPolicyManager userSecurityPolicyManager) {
        target.addToMetadata(item.namespace, item.key, item.value, userSecurityPolicyManager.user)
    }

    @Override
    void saveCatalogueItem(Metadata metadata) {
        if (!metadata) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(metadata.catalogueItemDomainType)
            catalogueItemService?.save(metadata.catalogueItem)
        }


    @Override
    void addFacetToDomain(Metadata facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
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

        MultiFacetAware catalogueItem = metadata.catalogueItem ?: findCatalogueItemByDomainTypeAndId(metadata.catalogueItemDomainType,
                                                                                                     metadata.catalogueItemId)

        if (catalogueItem.metadata.any {md -> md != metadata && md.namespace == metadata.namespace && md.key == metadata.key}) {
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

    List<MetadataAware> findAllCatalogueItemsByNamespace(String namespace, String domainType = null, Map pagination = [:]) {
        List<MetadataAware> returnResult = []
        catalogueItemServices.each {catalogueItemService ->
            if (!domainType || catalogueItemService.handles(domainType)) {
                returnResult.addAll(catalogueItemService.findAllByMetadataNamespace(namespace))
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

    List<Metadata> findAllByCatalogueItemIdAndNotNamespaces(UUID catalogueItemId, List<String> namespaces, Map pagination = [:]) {
        if (!namespaces || namespaces.size() == 0) {
            return Metadata.byCatalogueItemId(catalogueItemId).list(pagination)
        }
        Metadata.byCatalogueItemIdAndNotNamespaces(catalogueItemId, namespaces).list(pagination)
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

    void mergeMetadataIntoCatalogueItem(CatalogueItem targetCatalogueItem, MergeObjectDiffData mergeObjectDiffData) {

        if (!mergeObjectDiffData.hasDiffs()) return

        Metadata targetMetadata = findByCatalogueItemIdAndId(targetCatalogueItem.id, mergeObjectDiffData.leftId)
        if (!targetMetadata) {
            log.error('Attempted to merge non-existent metadata [{}] inside target catalogue item [{}]', mergeObjectDiffData.leftId,
                      targetCatalogueItem.id)
        }

        mergeObjectDiffData.getValidDiffs().each {mergeFieldDiffData ->
            if (mergeFieldDiffData.value) {
                targetMetadata.setProperty(mergeFieldDiffData.fieldName, mergeFieldDiffData.value)
            } else {
                log.error('Only field diff types can be handled inside MetadataService')
            }
        }
        targetMetadata.save(validate: false, flush: true)
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