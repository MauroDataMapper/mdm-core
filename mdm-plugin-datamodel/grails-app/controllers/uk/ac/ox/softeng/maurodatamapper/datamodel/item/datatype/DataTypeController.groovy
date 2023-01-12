/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService

import grails.databinding.DataBindingSource
import grails.gorm.transactions.Transactional
import grails.web.http.HttpHeaders
import grails.web.mime.MimeType

import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ORDER_ASC
import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ORDER_DESC
import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK

class DataTypeController extends CatalogueItemController<DataType> {
    static responseFormats = ['json', 'xml']

    DataTypeService dataTypeService

    DataModelService dataModelService

    DataTypeController() {
        super(DataType)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond dataTypeList: listAllResources(params), userSecurityPolicyManager: currentUserSecurityPolicyManager,
                owningDataModelId: params.dataModelId
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        resource ? respond(dataType: resource, userSecurityPolicyManager: currentUserSecurityPolicyManager) : notFound(params.id)

    }

    @Transactional
    def copyDataType(CopyInformation copyInformation) {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataType original = dataTypeService.findByDataModelIdAndId(params.otherDataModelId, params.dataTypeId)

        if (!original) return notFound(params.dataTypeId)
        DataType copy
        try {
            copy = dataTypeService.copyDataType(dataModel, original, currentUser, currentUserSecurityPolicyManager, false, copyInformation)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }
        if (copy.instanceOf(ReferenceType)) {
            throw new ApiBadRequestException('DTCXX', 'Copying of ReferenceType DataTypes is not possible')
        }
        dataModelService.get(params.dataModelId)?.addToDataTypes(copy)

        if (!validateResource(copy, 'create')) return

        dataModelService.validate(dataModel)
        if (dataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    @Override
    protected void saveResponse(DataType instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond status: CREATED, view: 'show', [dataType: instance, userSecurityPolicyManager: currentUserSecurityPolicyManager]
            }
        }
    }

    @Override
    protected void updateResponse(DataType instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond status: OK, [dataType: instance, userSecurityPolicyManager: currentUserSecurityPolicyManager]
            }

        }
    }

    @Override
    protected DataType queryForResource(Serializable resourceId) {
        dataTypeService.findByDataModelIdAndId(params.dataModelId, resourceId)
    }

    @Override
    protected List<DataType> listAllReadableResources(Map params) {
        if (params.sort instanceof String) params.sort = [(params.sort): ORDER_DESC.equalsIgnoreCase(params.order) ? ORDER_DESC : ORDER_ASC]
        params.sort = params.sort ?: ['idx': 'asc', 'label': 'asc']

        DataModel dataModel = dataModelService.get(params.dataModelId)
        if (dataModel.importedDataTypes) {
            // Cannot sort DTs with imported using idx
            (params.sort as Map).remove('idx')
        }

        if (params.search) {
            return dataTypeService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(params.dataModelId, params.search, params)
        }
        return dataTypeService.findAllByDataModelIdIncludingImported(params.dataModelId, params, params)
    }

    @Override
    void serviceDeleteResource(DataType resource) {
        dataTypeService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataType resource) {
        dataTypeService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    @Transactional
    protected DataType createResource() {
        try {
          return dataTypeService.bindDataType(objectToBind, params.dataModelId, currentUser)
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

    @Override
    @Transactional
    protected boolean validateResource(DataType instance, String view) {
        DataType validated = dataTypeService.validate(instance)
        if (validated.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond validated.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }

    @Override
    @Transactional
    protected boolean validateResourceDeletion(DataType resource, String view) {
        if (dataTypeService.isDataTypeBeingUsedAsImport(resource)) {
            transactionStatus.setRollbackOnly()
            resource.errors.reject('invalid.deletion.modelitem.used.as.import',
                                   ['DataType', resource.id].toArray(),
                                   'Cannot delete {0} [{1}] as it is still used as an import')
            respond resource.errors, view: view1 // STATUS CODE 422
            return false
        }
        true
    }
}
