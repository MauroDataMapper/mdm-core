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

class MetadataController extends FacetController<Metadata> {

    static responseFormats = ['json', 'xml']

    MetadataService metadataService

    MetadataController() {
        super(Metadata)
    }

    def namespaces() {
        if (params.id) respond metadataService.findNamespaceKeysIlikeNamespace(params.id)
        else respond metadataService.findNamespaceKeys()
    }

    @Override
    MultiFacetItemAwareService getFacetService() {
        metadataService
    }

    @Override
    protected boolean validateResource(Metadata instance, String view) {
        metadataService.validate(instance)
        super.validateResource(instance, view)
    }

    @Override
    protected Metadata updateResource(Metadata resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        metadataService.updateMultiFacetAwareItemIndex(resource)
        resource.save flush: true, validate: false
        getFacetService().addUpdatedEditToMultiFacetAwareItem(currentUser, resource,
                                                              params[getOwnerDomainTypeField()],
                                                              params[getOwnerIdField()],
                                                              dirtyPropertyNames)
    }
}
