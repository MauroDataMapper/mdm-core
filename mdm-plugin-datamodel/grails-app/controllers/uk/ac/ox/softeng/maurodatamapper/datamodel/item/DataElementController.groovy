/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService

import grails.gorm.transactions.Transactional

class DataElementController extends CatalogueItemController<DataElement> {
    static responseFormats = ['json', 'xml']

    DataClassService dataClassService
    DataElementService dataElementService
    DataModelService dataModelService
    DataTypeService dataTypeService

    DataElementController() {
        super(DataElement)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, [
            model: [
                owningDataModelId        : params.dataModelId,
                owningDataClassId        : params.dataClassId,
                userSecurityPolicyManager: currentUserSecurityPolicyManager,
            ],
            view : 'index'
        ]
    }

    @Transactional
    def copyDataElement(CopyInformation copyInformation) {
        if (handleReadOnly()) {
            return
        }

        DataModel destinationDataModel = dataModelService.get(params.dataModelId)
        DataClass destinationDataClass = dataClassService.get(params.dataClassId)
        DataElement originalDataElement = dataElementService.findByDataClassIdAndId(params.otherDataClassId, params.dataElementId)
        DataModel sourceDataModel = dataModelService.get(params.otherDataModelId)

        if (!originalDataElement) return notFound(params.dataElementId)
        DataElement copy
        try {
            copy = dataElementService.copyDataElement(destinationDataModel, originalDataElement, currentUser, currentUserSecurityPolicyManager, false, copyInformation)
            destinationDataClass.addToDataElements(copy)
            dataClassService.matchUpAndAddMissingReferenceTypeClasses(destinationDataModel, sourceDataModel, currentUser,
                                                                      currentUserSecurityPolicyManager)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        if (!validateResource(copy, 'create')) return

        dataModelService.validate(destinationDataModel)
        if (destinationDataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond destinationDataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    def suggestLinks() {
        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataModel otherDataModel = dataModelService.get(params.otherDataModelId)

        if (!dataModel) return notFound(params.dataModelId)
        if (!otherDataModel) return notFound(params.otherDataModelId)

        DataElement dataElement = queryForResource(params.dataElementId)
        if (!dataElement) return notFound(params.dataElementId)

        int maxResults = params.max ?: 5

        respond dataElementService.findAllSimilarDataElementsInDataModel(otherDataModel, dataElement, maxResults)
    }


    @Override
    protected DataElement queryForResource(Serializable resourceId) {
        if (params.dataTypeId) {
            if (!dataTypeService.findByDataModelIdAndId(params.dataModelId, params.dataTypeId)) return null
            return dataElementService.findByDataTypeIdAndId(params.dataTypeId, resourceId)
        }
        if (!dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)) return null
        return dataElementService.findByDataClassIdAndId(params.dataClassId, resourceId)
    }

    @Override
    protected List<DataElement> listAllReadableResources(Map params) {

        if (params.dataTypeId) {
            params.sort = params.sort ?: ['idx': 'asc', 'label': 'asc']
            if (!dataTypeService.findByDataModelIdAndId(params.dataModelId, params.dataTypeId)) {
                notFound(params.dataTypeId)
                return null
            }
            return dataElementService.findAllByDataTypeId(params.dataTypeId, params)
        }
        if (params.all) removePaginationParameters()

        if (!params.dataClassId) {
            params.sort = params.sort ?: ['dataClass.idx': 'asc', 'idx': 'asc']
            return dataElementService.findAllByDataModelId(params.dataModelId, params)
        }

        params.sort = params.sort ?: ['idx': 'asc', 'label': 'asc']
        if (!dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)) {
            notFound(params.dataClassId)
            return null
        }
        // Cannot sort DEs including imported using idx combined with any other field
        if (params.sort instanceof Map && (params.sort as Map).size() > 1) (params.sort as Map).remove('idx')
        return dataElementService.findAllByDataClassIdIncludingImported(params.dataClassId, params, params)
    }

    @Override
    void serviceDeleteResource(DataElement resource) {
        dataElementService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataElement resource) {
        dataElementService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    protected DataElement createResource() {
        DataElement resource = super.createResource() as DataElement
        DataType boundDataType = dataTypeService.checkBoundDataType(params.dataModelId, resource.dataType)
        if (boundDataType) boundDataType.addToDataElements(resource)
        else resource.dataType = null
        // Protect against mismatch DM and DC (DC not inside DM
        dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)?.addToDataElements(resource)
        resource
    }

    @Override
    protected DataElement updateResource(DataElement resource) {
        if (!resource.dataType.ident()) {
            DataType boundDataType = dataTypeService.checkBoundDataType(params.dataModelId, resource.dataType)
            if (boundDataType) boundDataType.addToDataElements(resource)
            else resource.dataType = null
            if (resource.dataType && !resource.dataType.ident()) resource.dataType.save()
        }
        super.updateResource(resource) as DataElement
    }

    @Override
    @Transactional
    protected boolean validateResource(DataElement instance, String view) {
        DataElement validated = dataElementService.validate(instance)
        if (validated.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond validated.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
