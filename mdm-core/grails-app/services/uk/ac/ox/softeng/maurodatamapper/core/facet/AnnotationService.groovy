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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.InvalidDataAccessResourceUsageException

import javax.transaction.Transactional

@Slf4j
@Transactional
class AnnotationService implements CatalogueItemAwareService<Annotation> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List <ContainerService> containerServices

    Annotation get(Serializable id) {
        Annotation.get(id)
    }

    List<Annotation> list(Map args) {
        Annotation.list(args)
    }

    Long count() {
        Annotation.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(Annotation annotation, boolean flush = false) {
        if (!annotation) return
        CatalogueItemService service = findCatalogueItemService(annotation.catalogueItemDomainType)
        if(service) {
            service.removeAnnotationFromCatalogueItem(annotation.catalogueItemId, annotation)
        }else{
            ContainerService containerService = findContainerService(annotation.catalogueItemDomainType)
            containerService.removeAnnotationFromContainer(annotation.catalogueItemId, annotation)
        }

        annotation.parentAnnotation?.removeFromChildAnnotations(annotation)
        List<Annotation> children = new ArrayList<>(annotation.childAnnotations)
        children.each {
            delete(it, false)
        }
        annotation.delete(flush: flush)
    }

    Annotation editInformation(Annotation annotation, String label, String description) {
        annotation.label = label
        annotation.description = description
        annotation.validate()
        annotation
    }

    @Override
    Annotation findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        Annotation.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<Annotation> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination = [:]) {
        Annotation.byCatalogueItemId(catalogueItemId).list(pagination)
    }

    @Override
    DetachedCriteria<Annotation> getBaseDeleteCriteria() {
        Annotation.by()
    }

    @Override
    void saveCatalogueItem(Annotation facet) {
        if (!facet) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(facet.catalogueItemDomainType)
        catalogueItemService.save(facet.catalogueItem)
    }

    @Override
    void addFacetToDomain(Annotation facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
        domain.addToAnnotations(facet)
    }

    List<Annotation> findAllWhereRootAnnotationOfCatalogueItemId(UUID catalogueItemId, Map paginate = [:]) {
        Annotation.whereRootAnnotationOfCatalogueItemId(catalogueItemId).list(paginate)
    }

    List<Annotation> findAllByParentAnnotationId(UUID parentAnnotationId, Map paginate = [:]) {
        try {
            return Annotation.byParentAnnotationId(parentAnnotationId).list(paginate)
        } catch (InvalidDataAccessResourceUsageException ignored) {
            log.warn('InvalidDataAccessResourceUsageException thrown, attempting query directly on parentAnnotation')
            return new DetachedCriteria<Annotation>(Annotation).eq('parentAnnotation', Utils.toUuid(parentAnnotationId)).list(paginate)
        }
    }

    Number countWhereRootAnnotationOfCatalogueItemId(UUID catalogueItemId) {
        Annotation.whereRootAnnotationOfCatalogueItemId(catalogueItemId).count()
    }


}