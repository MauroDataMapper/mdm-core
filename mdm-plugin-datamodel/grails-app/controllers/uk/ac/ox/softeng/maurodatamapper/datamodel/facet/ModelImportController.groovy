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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

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
        List<ModelImport> modelImports = modelImportService.findAllByItemId(params.catalogueItemId, params)
        modelImportService.loadCatalogueItemsIntoModelImports(modelImports)
    }

    @Override
    protected ModelImport createResource() {
        ModelImport resource = super.createResource() as ModelImport
        resource.clearErrors()
        modelImportService.addModelImportToCatalogueItem(resource, params.catalogueItemDomainType, params.catalogueItemId)
        modelImportService.checkAndPerformAdditionalImports(resource)
        resource
    }

    @Override
    protected void serviceDeleteResource(ModelImport resource) {
        modelImportService.delete(resource, true)
    }


    @Override
    protected ModelImport saveResource(ModelImport resource) {
        resource.save flush: true, validate: false
        modelImportService.saveCatalogueItem(resource)
        modelImportService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ModelImport updateResource(ModelImport resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        modelImportService.
            addUpdatedEditToModelItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId,
                                      dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(ModelImport resource) {
        serviceDeleteResource(resource)
        modelImportService.addDeletedEditToModelItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Transactional
    @Override
    protected boolean validateResource(ModelImport instance, String view) {
        modelImportService.validate(instance)
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
