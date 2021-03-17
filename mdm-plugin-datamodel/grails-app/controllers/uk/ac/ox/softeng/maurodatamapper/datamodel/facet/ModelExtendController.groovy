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

class ModelExtendController extends EditLoggingController<ModelExtend> {

    static responseFormats = ['json', 'xml']

    ModelExtendService modelExtendService

    ModelExtendController() {
        super(ModelExtend)
    }

    @Override
    protected ModelExtend queryForResource(Serializable id) {
        ModelExtend resource = super.queryForResource(id) as ModelExtend
        modelExtendService.loadModelItemsIntoModelExtend(resource)
    }

    @Override
    protected List<ModelExtend> listAllReadableResources(Map params) {
        List<ModelExtend> modelExtends = modelExtendService.findAllByModelItemId(params.modelItemId, params)
        modelExtendService.loadModelItemsIntoModelExtends(modelExtends)
    }

    @Override
    protected ModelExtend createResource() {
        ModelExtend resource = super.createResource() as ModelExtend
        resource.clearErrors()
        modelExtendService.addModelExtendToModelItem(resource, params.modelItemDomainType, params.modelItemId)
        resource
    }

    @Override
    protected void serviceDeleteResource(ModelExtend resource) {
        modelExtendService.delete(resource, true)
    }

    @Override
    protected ModelExtend saveResource(ModelExtend resource) {
        resource.save flush: true, validate: false
        modelExtendService.saveModelItem(resource)
        modelExtendService.addCreatedEditToModelItem(currentUser, resource, params.modelItemDomainType, params.modelItemId)
    }

    @Override
    protected ModelExtend updateResource(ModelExtend resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        modelExtendService.
            addUpdatedEditToModelItem(currentUser, resource, params.modelItemDomainType, params.modelItemId,
                                      dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(ModelExtend resource) {
        serviceDeleteResource(resource)
        modelExtendService.addDeletedEditToModelItem(currentUser, resource, params.modelItemDomainType, params.modelItemId)
    }

    @Transactional
    @Override
    protected boolean validateResource(ModelExtend instance, String view) {
        modelExtendService.validate(instance)
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
