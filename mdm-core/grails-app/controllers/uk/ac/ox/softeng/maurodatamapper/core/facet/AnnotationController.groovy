/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.controller.FacetController
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService

class AnnotationController extends FacetController<Annotation> {

    static responseFormats = ['json', 'xml']

    AnnotationService annotationService

    AnnotationController() {
        super(Annotation)
    }

    @Override
    MultiFacetItemAwareService getFacetService() {
        annotationService
    }

    @Override
    protected Annotation queryForResource(Serializable resourceId) {
        Annotation annotation = super.queryForResource(resourceId) as Annotation
        annotationService.populateAnnotationUser(annotation)
        annotation
    }

    @Override
    protected List<Annotation> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'desc'
        List<Annotation> annotations
        if (params.annotationId) {
            annotations = annotationService.findAllByParentAnnotationId(params.annotationId, params)
        } else {
            annotations = annotationService.findAllWhereRootAnnotationOfMultiFacetAwareItemId(params.multiFacetAwareItemId, params)
        }
        annotations.collect {annotationService.populateAnnotationUser(it)}
    }

    @Override
    protected Annotation createResource() {
        Annotation resource = super.createResource() as Annotation
        if (params.annotationId) {
            annotationService.findByMultiFacetAwareItemIdAndId(params.multiFacetAwareItemId, params.annotationId)?.addToChildAnnotations(resource)
        }
        if (!resource.label && resource.parentAnnotation) {
            resource.label = "${resource.parentAnnotation.label} [${resource.parentAnnotation.childAnnotations.size()}]"
        }
        resource
    }

    @Override
    protected void updateResponse(Annotation instance) {
        annotationService.populateAnnotationUser(instance)
        super.updateResponse(instance)
    }

    @Override
    protected void saveResponse(Annotation instance) {
        annotationService.populateAnnotationUser(instance)
        super.saveResponse(instance)
    }
}
