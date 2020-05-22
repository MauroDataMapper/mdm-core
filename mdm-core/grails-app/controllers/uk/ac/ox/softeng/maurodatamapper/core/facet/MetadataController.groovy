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

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class MetadataController extends EditLoggingController<Metadata> {

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
    protected Metadata queryForResource(Serializable resourceId) {
        return metadataService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<Metadata> listAllReadableResources(Map params) {
        return metadataService.findAllByCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    protected Metadata createResource() {
        Metadata resource = super.createResource() as Metadata
        resource.clearErrors()
        resource.catalogueItem = metadataService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    void serviceDeleteResource(Metadata resource) {
        metadataService.delete(resource, true)
    }

    @Override
    protected Metadata saveResource(Metadata resource) {
        resource.save flush: true, validate: false
        metadataService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected Metadata updateResource(Metadata resource) {
        resource.save flush: true, validate: false
        metadataService.addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected void deleteResource(Metadata resource) {
        serviceDeleteResource(resource)
        metadataService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected boolean validateResource(Metadata instance, String view) {
        metadataService.validate(instance)
        super.validateResource(instance, view)
    }
}
