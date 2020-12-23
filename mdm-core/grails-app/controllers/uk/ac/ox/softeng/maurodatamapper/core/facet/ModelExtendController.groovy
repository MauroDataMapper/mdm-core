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

class ModelExtendController extends EditLoggingController<ModelExtend> {

    static responseFormats = ['json', 'xml']

    ModelExtendService modelExtendService

    ModelExtendController() {
        super(ModelExtend)
    }

    @Override
    protected ModelExtend queryForResource(Serializable id) {
        ModelExtend resource = super.queryForResource(id) as ModelExtend
        modelExtendService.loadCatalogueItemsIntoModelExtend(resource)
    }

    @Override
    protected List<ModelExtend> listAllReadableResources(Map params) {
        List<ModelExtend> modelExtends
        modelExtends = modelExtendService.findAllByCatalogueItemId(params.catalogueItemId, params)
        modelExtendService.loadCatalogueItemsIntoModelExtends(modelExtends)
    }

    @Override
    void serviceDeleteResource(ModelExtend resource) {
        //Delete the association between ModelExtend and CatalogueItem
        CatalogueItem catalogueItem = modelExtendService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        modelExtendService.removeModelExtendFromCatalogueItem(resource, catalogueItem)

        //Delete the modelExtend
        modelExtendService.delete(resource, true)

        //Record the deletion against the CatalogueItem to which the ModelExtend belonged
        modelExtendService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ModelExtend createResource() {
        ModelExtend resource = super.createResource() as ModelExtend
        resource.catalogueItem = modelExtendService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    protected ModelExtend saveResource(ModelExtend resource) {
        modelExtendService.saveResource(currentUser, resource)
    }

    @Override
    protected void deleteResource(ModelExtend resource) {
        serviceDeleteResource(resource)
    }

    @Transactional
    @Override
    protected boolean validateResource(ModelExtend instance, String view) {

        boolean valid = !instance.hasErrors()  && instance.validate()
      
        if (valid) {
            instance.extendedCatalogueItem = modelExtendService.findCatalogueItemByDomainTypeAndId(instance.extendedCatalogueItemDomainType, instance.extendedCatalogueItemId)
            if (!instance.extendedCatalogueItem) {
                valid = false
                instance.errors.rejectValue('extendedCatalogueItemId',
                                            'modelextend.extended.catalogue.item.not.found',
                                            [instance.extendedCatalogueItemId, instance.extendedCatalogueItemDomainType].toArray(),
                                            'Extended catalogue item [{0}] of type [{1}] cannot be found')
            }
        }

        if (valid) {
            if (!modelExtendService.catalogueItemDomainTypeExtendsDomainType(params.catalogueItemDomainType, instance.extendedCatalogueItemDomainType)) {
                valid = false
                instance.errors.rejectValue('extendedCatalogueItemDomainType',
                                            'modelextend.domain.does.not.extend.domain',
                                            [params.catalogueItemDomainType, instance.extendedCatalogueItemDomainType].toArray(),
                                            'Domain type [{0}] does not extend domain type [{1}]')
            }
        }

        if (valid) {
            if (!modelExtendService.catalogueItemIsExtendableByCatalogueItem(instance.catalogueItem, instance.extendedCatalogueItem)) {
                valid = false
                instance.errors.rejectValue('extendedCatalogueItemId',
                                            'modelextend.extended.catalogue.item.cannot.be.used',
                                            [instance.extendedCatalogueItemId, instance.extendedCatalogueItemDomainType].toArray(),
                                            'Extended catalogue item [{0}] of type [{1}] cannot be used')                
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
