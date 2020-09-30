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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel

import grails.gorm.transactions.Transactional

class DataElementController extends CatalogueItemController<DataElement> {
    static responseFormats = ['json', 'xml']

    DataElementService dataElementService
    ReferenceDataModelService referenceDataModelService

    DataElementController() {
        super(DataElement)
    }

    @Transactional
    def copyDataElement() {
        if (handleReadOnly()) {
            return
        }

        ReferenceDataModel referenceDataModel = referenceDataModelService.get(params.dataModelId)
        DataElement original = dataElementService.findByReferenceDataModelIdAndId(params.otherReferenceDataModelId, params.dataElementId)
        ReferenceDataModel originalReferenceDataModel = referenceDataModelService.get(params.otherReferenceDataModelId)

        if (!original) return notFound(params.dataElementId)
        DataElement copy
        try {
            copy = dataElementService.copyDataElement(referenceDataModel, original, currentUser, currentUserSecurityPolicyManager)
            referenceDataModel.addToDataElements(copy)
            referenceDataModelService.matchUpAndAddMissingReferenceTypeClasses(referenceDataModel, referenceDataModelService.get(params.otherReferenceDataModelId), currentUser,
                                                                      currentUserSecurityPolicyManager)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        referenceDataModelService.validate(referenceDataModel)
        if (referenceDataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond referenceDataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    def suggestLinks() {
        ReferenceDataModel referenceDataModel = referenceDataModelService.get(params.referenceDataModelId)
        ReferenceDataModel otherReferenceDataModel = referenceDataModelService.get(params.otherReferenceDataModelId)

        if (!referenceDataModel) return notFound(params.referenceDataModelId)
        if (!otherReferenceDataModel) return notFound(params.otherReferenceDataModelId)

        DataElement dataElement = queryForResource(params.dataElementId)
        if (!dataElement) return notFound(params.dataElementId)

        int maxResults = params.max ?: 5

        respond dataElementService.findAllSimilarDataElementsInDataModel(otherReferenceDataModel, dataElement, maxResults)
    }


    @Override
    protected DataElement queryForResource(Serializable resourceId) {
        if (params.dataTypeId) {
            return dataElementService.findByDataTypeIdAndId(params.dataTypeId, resourceId)
        }

        return dataElementService.findByReferenceDataModelIdAndId(params.referenceDataModelId, resourceId)
    }

    @Override
    protected List<DataElement> listAllReadableResources(Map params) {
        if (params.dataTypeId) {
            return dataElementService.findAllByDataTypeId(params.dataTypeId, params)
        }
        if (params.all) removePaginationParameters()

        return dataElementService.findAllByReferenceDataModelId(params.referenceDataModelId, params)
    }

    @Override
    void serviceDeleteResource(DataElement resource) {
        dataElementService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataElement resource) {
        dataElementService.save(flush: true, resource)
    }

    @Override
    protected DataElement createResource() {
        DataElement resource = super.createResource() as DataElement
        referenceDataModelService.get(params.referenceDataModelId)?.addToDataElements(resource)
        resource
    }

    @Override
    protected DataElement updateResource(DataElement resource) {
        if (!resource.dataType.ident()) resource.dataType.save()
        super.updateResource(resource) as DataElement
    }
}
