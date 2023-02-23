/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.databinding.converters.ReferenceModelDataTypeValueConverter

import grails.databinding.DataBindingSource
import grails.gorm.transactions.Transactional
import grails.web.http.HttpHeaders
import grails.web.mime.MimeType

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK

class ReferenceDataTypeController extends CatalogueItemController<ReferenceDataType> {
    static responseFormats = ['json', 'xml']

    ReferenceDataTypeService referenceDataTypeService

    ReferenceDataModelService referenceDataModelService

    ReferenceDataTypeController() {
        super(ReferenceDataType)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond referenceDataTypeList: listAllResources(params), userSecurityPolicyManager: currentUserSecurityPolicyManager
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        resource ? respond(referenceDataType: resource, userSecurityPolicyManager: currentUserSecurityPolicyManager) : notFound(params.id)

    }

    @Transactional
    def copyReferenceDataType() {
        if (handleReadOnly()) {
            return
        }
        ReferenceDataModel referenceDataModel = referenceDataModelService.get(params.referenceDataModelId)
        ReferenceDataType original = referenceDataTypeService.findByReferenceDataModelIdAndId(params.otherReferenceDataModelId, params.referenceDataTypeId)

        if (!original) return notFound(params.referenceDataTypeId)

        ReferenceDataType copy
        try {
            copy = referenceDataTypeService.copyReferenceDataType(referenceDataModel, original, currentUser, currentUserSecurityPolicyManager)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        referenceDataModelService.get(params.referenceDataModelId)?.addToReferenceDataTypes(copy)

        referenceDataModelService.validate(referenceDataModel)
        if (referenceDataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond referenceDataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    @Override
    protected void saveResponse(ReferenceDataType instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond status: CREATED, view: 'show', [referenceDataType: instance, userSecurityPolicyManager: currentUserSecurityPolicyManager]
            }
        }
    }

    @Override
    protected void updateResponse(ReferenceDataType instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond status: OK, [referenceDataType: instance, userSecurityPolicyManager: currentUserSecurityPolicyManager]
            }

        }
    }

    @Override
    protected ReferenceDataType queryForResource(Serializable resourceId) {
        referenceDataTypeService.findByReferenceDataModelIdAndId(params.referenceDataModelId, resourceId)
    }

    @Override
    protected List<ReferenceDataType> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'label'

        if (params.search) {
            return referenceDataTypeService.findAllByReferenceDataModelIdAndLabelIlikeOrDescriptionIlike(params.referenceDataModelId, params.search, params)
        }
        return referenceDataTypeService.findAllByReferenceDataModelId(params.referenceDataModelId, params)
    }

    @Override
    void serviceDeleteResource(ReferenceDataType resource) {
        referenceDataTypeService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(ReferenceDataType resource) {
        referenceDataTypeService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    @Transactional
    protected ReferenceDataType createResource() {
        try {
            ReferenceModelDataTypeValueConverter converter = new ReferenceModelDataTypeValueConverter()
            def body = getObjectToBind()
            if (converter.canConvert(body)) {
                ReferenceDataType resource = converter.convert(body)
                resource.createdBy = currentUser.emailAddress
                referenceDataModelService.get(params.referenceDataModelId)?.addToReferenceDataTypes(resource)
                return resource
            }
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
        }
        null
    }

    @Override
    protected Object getObjectToBind() {
        def object = super.getObjectToBind()
        if (object instanceof DataBindingSource) {
            return object
        } else {
            if (request.mimeTypes.any {it == MimeType.JSON || it == MimeType.JSON_API || MimeType.HAL_JSON}) return request.JSON
            if (request.mimeTypes.any {it == MimeType.XML || it == MimeType.ATOM_XML || MimeType.HAL_XML}) return request.XML
            request
        }
    }

}
