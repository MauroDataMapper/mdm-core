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

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class AnnotationController extends EditLoggingController<Annotation> {

    static responseFormats = ['json', 'xml']

    AnnotationService annotationService

    AnnotationController() {
        super(Annotation)
    }

    @Override
    protected Annotation queryForResource(Serializable resourceId) {
        annotationService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<Annotation> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'desc'
        if (params.annotationId) {
            return annotationService.findAllByParentAnnotationId(params.annotationId, params)
        }

        return annotationService.findAllWhereRootAnnotationOfCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    void serviceDeleteResource(Annotation resource) {
        annotationService.delete(resource)
    }

    @Override
    protected Annotation createResource() {
        Annotation resource = super.createResource() as Annotation
        if (params.annotationId) {
            annotationService.get(params.annotationId)?.addToChildAnnotations(resource)
        }
        resource.catalogueItem = annotationService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)

        if (!resource.label && resource.parentAnnotation) {
            resource.label = "${resource.parentAnnotation.label} [${resource.parentAnnotation.childAnnotations.size()}]"
        }
        resource
    }

    @Override
    protected Annotation saveResource(Annotation resource) {
        resource.save flush: true, validate: false
        annotationService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected Annotation updateResource(Annotation resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        annotationService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(Annotation resource) {
        serviceDeleteResource(resource)
        annotationService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }
}
