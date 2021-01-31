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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService

import grails.gorm.transactions.Transactional

class ReferenceDataElementController extends CatalogueItemController<ReferenceDataElement> {
    static responseFormats = ['json', 'xml']

    ReferenceDataElementService referenceDataElementService
    ReferenceDataModelService referenceDataModelService

    ReferenceDataElementController() {
        super(ReferenceDataElement)
    }

    @Transactional
    def copyReferenceDataElement() {
        if (handleReadOnly()) {
            return
        }

        ReferenceDataModel referenceDataModel = referenceDataModelService.get(params.referenceDataModelId)
        ReferenceDataElement original = referenceDataElementService.findByReferenceDataModelIdAndId(params.otherReferenceDataModelId, params.referenceDataElementId)
        ReferenceDataModel originalReferenceDataModel = referenceDataModelService.get(params.otherReferenceDataModelId)

        if (!original) return notFound(params.referenceDataElementId)
        ReferenceDataElement copy
        try {
            copy = referenceDataElementService.copyReferenceDataElement(referenceDataModel, original, currentUser, currentUserSecurityPolicyManager)
            referenceDataModel.addToReferenceDataElements(copy)
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

        ReferenceDataElement dataElement = queryForResource(params.dataElementId)
        if (!dataElement) return notFound(params.dataElementId)

        int maxResults = params.max ?: 5

        respond referenceDataElementService.findAllSimilarDataElementsInDataModel(otherReferenceDataModel, dataElement, maxResults)
    }


    @Override
    protected ReferenceDataElement queryForResource(Serializable resourceId) {
        if (params.dataTypeId) {
            return referenceDataElementService.findByDataTypeIdAndId(params.dataTypeId, resourceId)
        }

        return referenceDataElementService.findByReferenceDataModelIdAndId(params.referenceDataModelId, resourceId)
    }

    @Override
    protected List<ReferenceDataElement> listAllReadableResources(Map params) {
        if (params.referenceDataTypeId) {
            return referenceDataElementService.findAllByReferenceDataTypeId(params.referenceDataTypeId, params)
        }
        if (params.all) removePaginationParameters()

        return referenceDataElementService.findAllByReferenceDataModelId(params.referenceDataModelId, params)
    }

    @Override
    void serviceDeleteResource(ReferenceDataElement resource) {
        referenceDataElementService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(ReferenceDataElement resource) {
        referenceDataElementService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    protected ReferenceDataElement createResource() {
        ReferenceDataElement resource = super.createResource() as ReferenceDataElement
        referenceDataModelService.get(params.referenceDataModelId)?.addToReferenceDataElements(resource)
        resource
    }

    @Override
    protected ReferenceDataElement updateResource(ReferenceDataElement resource) {
        if (!resource.referenceDataType.ident()) resource.referenceDataType.save()
        super.updateResource(resource) as ReferenceDataElement
    }
}
