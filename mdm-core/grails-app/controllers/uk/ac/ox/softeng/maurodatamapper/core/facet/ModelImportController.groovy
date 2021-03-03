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

import uk.ac.ox.softeng.maurodatamapper.core.controller.FacetController
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService

import grails.gorm.transactions.Transactional

class ModelImportController extends FacetController<ModelImport> {

    static responseFormats = ['json', 'xml']

    ModelImportService modelImportService

    ModelImportController() {
        super(ModelImport)
    }

    @Override
    CatalogueItemAwareService getFacetService() {
        modelImportService
    }

    @Override
    protected ModelImport queryForResource(Serializable id) {
        ModelImport resource = super.queryForResource(id) as ModelImport
        modelImportService.loadCatalogueItemsIntoModelImport(resource)
    }

    @Override
    protected List<ModelImport> listAllReadableResources(Map params) {
        List<ModelImport> modelImports
        modelImports = modelImportService.findAllByCatalogueItemId(params.catalogueItemId, params)
        modelImportService.loadCatalogueItemsIntoModelImports(modelImports)
    }

    @Override
    protected ModelImport createResource() {
        ModelImport resource = super.createResource() as ModelImport
        resource.catalogueItem = modelImportService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    protected ModelImport saveResource(ModelImport resource) {
        modelImportService.saveResource(currentUser, resource)
    }

    @Transactional
    @Override
    protected boolean validateResource(ModelImport instance, String view) {

        boolean valid = !instance.hasErrors()  && instance.validate()
      
        if (valid) {
            instance.importedCatalogueItem = modelImportService.findCatalogueItemByDomainTypeAndId(instance.importedCatalogueItemDomainType, instance.importedCatalogueItemId)
            if (!instance.importedCatalogueItem) {
                valid = false
                instance.errors.rejectValue('importedCatalogueItemId',
                                            'modelimport.imported.catalogue.item.not.found',
                                            [instance.importedCatalogueItemId, instance.importedCatalogueItemDomainType].toArray(),
                                            'Imported catalogue item [{0}] of type [{1}] cannot be found')
            }
        }

        if (valid) {
            if (!modelImportService.catalogueItemDomainTypeImportsDomainType(params.catalogueItemDomainType, instance.importedCatalogueItemDomainType)) {
                valid = false
                instance.errors.rejectValue('importedCatalogueItemDomainType',
                                            'modelimport.domain.does.not.import.domain',
                                            [params.catalogueItemDomainType, instance.importedCatalogueItemDomainType].toArray(),
                                            'Domain type [{0}] does not import domain type [{1}]')
            }
        }

        if (valid) {
            if (!modelImportService.catalogueItemIsImportableByCatalogueItem(instance.catalogueItem, instance.importedCatalogueItem)) {
                valid = false
                instance.errors.rejectValue('importedCatalogueItemId',
                                            'modelimport.imported.catalogue.item.cannot.be.used',
                                            [instance.importedCatalogueItemId, instance.importedCatalogueItemDomainType].toArray(),
                                            'Imported catalogue item [{0}] of type [{1}] cannot be used')                
            }
        }

        if (!valid) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
       
        true
    }
}
