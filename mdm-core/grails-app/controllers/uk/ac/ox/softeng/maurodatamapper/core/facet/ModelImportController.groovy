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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import grails.gorm.transactions.Transactional

class ModelImportController extends EditLoggingController<ModelImport> {

    static responseFormats = ['json', 'xml']

    ModelImportService modelImportService

    ModelImportController() {
        super(ModelImport)
    }

    @Override
    protected ModelImport queryForResource(Serializable id) {
        ModelImport resource = super.queryForResource(id) as ModelImport
        modelImportService.loadCatalogueItemsIntoModelImport(resource)
    }

    @Override
    protected List<ModelImport> listAllReadableResources(Map params) {
        List<ModelImport> modelImports
        switch (params.type) {
            case 'source':
                modelImports = modelImportService.findAllBySourceCatalogueItemId(params.catalogueItemId, params)
                break
            case 'imported':
                modelImports = modelImportService.findAllByTargetCatalogueItemId(params.catalogueItemId, params)
                break
            default:
                modelImports = modelImportService.findAllBySourceOrTargetCatalogueItemId(params.catalogueItemId, params)
        }
        modelImportService.loadCatalogueItemsIntoModelImports(modelImports)
    }

    @Override
    void serviceDeleteResource(ModelImport resource) {
        //Delete the association between ModelImport and CatalogueItem
        CatalogueItem catalogueItem = modelImportService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        modelImportService.removeModelImportFromCatalogueItem(resource, catalogueItem)

        //Delete the modelImport
        modelImportService.delete(resource, true)

        //Record the deletion against the CatalogueItem to which the ModelImport belonged
        modelImportService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ModelImport createResource() {
        ModelImport resource = super.createResource() as ModelImport
        resource.catalogueItem = modelImportService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    protected ModelImport saveResource(ModelImport resource) {
        modelImportService.loadCatalogueItemsIntoModelImport(resource)
        resource.save flush: true, validate: false

        //Add an association between the ModelImport and CatalogueItem
        modelImportService.addModelImportToCatalogueItem(resource, resource.catalogueItem)

        //Record the creation against the CatalogueItem belongs
        modelImportService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ModelImport updateResource(ModelImport resource) {
        modelImportService.loadCatalogueItemsIntoModelImport(resource)
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        modelImportService.
            addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId, dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(ModelImport resource) {
        serviceDeleteResource(resource)
    }

    @Transactional
    @Override
    protected boolean validateResource(ModelImport instance, String view) {
        // Ensure only assignable link types are constructed via the controller
        /*if (instance.linkType && !instance.linkType.isAssignable) {
            instance.errors.rejectValue('linkType',
                                        'semanticlink.linktype.must.be.assignable.message',
                                        ['linkType', ModelImport, instance.linkType].toArray(),
                                        'Property [{0}] of class [{1}] with value [{2}] cannot be used')
        }*/
        if (instance.hasErrors() || !instance.validate()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
