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

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.springframework.dao.InvalidDataAccessResourceUsageException

import javax.transaction.Transactional

@Slf4j
@Transactional
class AnnotationService implements MultiFacetItemAwareService<Annotation> {

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
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(annotation.multiFacetAwareItemDomainType)
        service.removeAnnotationFromMultiFacetAware(annotation.multiFacetAwareItemId, annotation)

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
    Annotation findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        Annotation.byyMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<Annotation> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination = [:]) {
        Annotation.byMultiFacetAwareItemId(multiFacetAwareItemId).list(pagination)
    }

    @Override
    DetachedCriteria<Annotation> getBaseDeleteCriteria() {
        Annotation.by()
    }

    @Override
    void saveMultiFacetAwareItem(Annotation facet) {
        if (!facet) return
        MultiFacetAwareService multiFacetAwareItemService = findServiceForMultiFacetAwareDomainType(facet.multiFacetAwareItemDomainType)
        multiFacetAwareItemService.save(facet.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(Annotation facet, String domainType, UUID domainId) {
        if (!facet) return
        MultiFacetAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId)
        domain.addToAnnotations(facet)
        facet.multiFacetAwareItem = domain
        log.debug('stop')
    }

    List<Annotation> findAllWhereRootAnnotationOfMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map paginate = [:]) {
        Annotation.whereRootAnnotationOfMultiFacetAwareItemId(multiFacetAwareItemId).list(paginate)
    }

    List<Annotation> findAllByParentAnnotationId(UUID parentAnnotationId, Map paginate = [:]) {
        try {
            return Annotation.byParentAnnotationId(parentAnnotationId).list(paginate)
        } catch (InvalidDataAccessResourceUsageException ignored) {
            log.warn('InvalidDataAccessResourceUsageException thrown, attempting query directly on parentAnnotation')
            return new DetachedCriteria<Annotation>(Annotation).eq('parentAnnotation', Utils.toUuid(parentAnnotationId)).list(paginate)
        }
    }

    Number countWhereRootAnnotationOfMultiFacetAwareItemId(UUID multiFacetAwareItemId) {
        Annotation.whereRootAnnotationOfMultiFacetAwareItemId(multiFacetAwareItemId).count()
    }
}