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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.SearchService
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult

import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DataClassController extends CatalogueItemController<DataClass> {

    static responseFormats = ['json', 'xml']

    DataClassService dataClassService

    DataModelService dataModelService

    @Autowired
    SearchService mdmPluginDataModelSearchService

    DataClassController() {
        super(DataClass)
    }

    @Transactional
    @Override
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (dataClassService.isDataClassBeingUsedAsExtension(instance)) {
            transactionStatus.setRollbackOnly()
            instance.errors.reject('invalid.deletion.dataclass.used.as.extension',
                                   [instance.id].toArray(),
                                   'Cannot delete DataClass [{0}] as it is still used as an extension in another DataClass')
            respond instance.errors, view: 'delete' // STATUS CODE 422
            return false
        }

        deleteResource instance

        deleteResponse instance
    }

    def all() {
        params.all = true
        index()
    }

    def content(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        params.sort = params.sort ?: 'label'

        respond content: dataClassService.findAllContentOfDataClassIdInDataModelId(params.dataModelId, params.dataClassId, params)
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        if (!dataClassService.existsWhereRootDataClassOfDataModelIdAndId(params.dataModelId, params.dataClassId)) {
            return notFound(params.dataClassId)
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        params.max = params.max ?: searchParams.max ?: 10
        params.offset = params.offset ?: searchParams.offset ?: 0
        params.sort = params.sort ?: searchParams.sort ?: 'label'
        if (searchParams.order) {
            params.order = searchParams.order
        }

        PaginatedLuceneResult<ModelItem> result = mdmPluginDataModelSearchService.findAllByDataClassIdByLuceneSearch(params.dataClassId,
                                                                                                                     searchParams,
                                                                                                                     params)
        respond result
    }

    @Transactional
    def copyDataClass() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataClass original = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)

        if (!original) return notFound(params.otherDataClassId)

        DataClass copy
        try {
            copy = dataClassService.copyDataClassMatchingAllReferenceTypes(dataModel, original, currentUser, currentUserSecurityPolicyManager,
                                                                           params.dataClassId)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        if (!validateResource(copy, 'create')) return

        dataModelService.validate(dataModel)
        if (dataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        dataModelService.saveModelNewContentOnly(dataModel)

        saveResource copy

        saveResponse copy
    }

    @Transactional
    def extendDataClass() {
        if (handleReadOnly()) {
            return
        }

        DataClass instance = dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)
        if (!instance) return notFound(params.dataClassId)

        DataClass dataClassToExtend = dataClassService.findByDataModelIdAndId(params.otherDataModelId, params.otherDataClassId)
        if (!dataClassToExtend) return notFound(params.otherDataClassId)


        if (request.method == 'PUT') {
            if (!dataClassService.isExtendableDataClassInSameModelOrInFinalisedModel(dataClassToExtend, instance)) {
                instance.errors.reject('invalid.extended.dataclass.model.not.finalised',
                                       [params.otherDataClassId].toArray(),
                                       'DataClass [{0}] to be extended does not belong to a finalised DataModel')
                respond instance.errors, view: 'update' // STATUS CODE 422
                return false
            }
            log.debug('Extending DataClass {} from {}', params.dataClassId, params.otherDataClassId)
            instance.addToExtendedDataClasses(dataClassToExtend)
        } else {
            log.debug('Removing extension of DataClass {} from {}', params.dataClassId, params.otherDataClassId)
            instance.removeFromExtendedDataClasses(dataClassToExtend)
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    protected DataClass queryForResource(Serializable resourceId) {
        if (params.dataClassId) {
            return dataClassService.findByDataModelIdAndParentDataClassIdAndId(params.dataModelId, params.dataClassId, resourceId)
        }
        return dataClassService.findWhereRootDataClassOfDataModelIdAndId(params.dataModelId, resourceId)
    }

    @Override
    protected List<DataClass> listAllReadableResources(Map params) {
        params.sort = params.sort ?: ['idx': 'asc', 'label': 'asc']
        //        params.imported = params.boolean('imported', true)
        //        params.extended = params.boolean('extended', true)
        if (params.dataClassId) {
            if (!dataClassService.findByDataModelIdAndId(params.dataModelId, params.dataClassId)) {
                notFound(params.dataClassId)
                return null
            }
            //TODO change of method name in the merge
            //            return dataClassService.findAllByDataModelIdAndParentDataClassId(params.dataModelId, params.dataClassId, params, params
            //            .imported, params.extended)
            return dataClassService.findAllByDataModelIdAndParentDataClassId(params.dataModelId, params.dataClassId, params)
        }
        if (((GrailsParameterMap) params).boolean('all', false)) {
            if (params.search) {
                //                return dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(params.dataModelId, params.search,
                //                params, params.imported)
                return dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(params.dataModelId, params.search, params)
            }
            //            return dataClassService.findAllByDataModelId(params.dataModelId, params, params.imported)
            return dataClassService.findAllByDataModelId(params.dataModelId, params)
        }
        //        return dataClassService.findAllWhereRootDataClassOfDataModelId(params.dataModelId, params, params.imported)
        return dataClassService.findAllWhereRootDataClassOfDataModelId(params.dataModelId, params)
    }

    @Override
    protected void serviceDeleteResource(DataClass resource) {
        dataClassService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataClass resource) {
        dataClassService.save(DEFAULT_SAVE_ARGS, resource)
    }

    @Override
    protected DataClass createResource() {
        DataClass resource = super.createResource() as DataClass
        if (params.dataClassId) {
            dataClassService.get(params.dataClassId)?.addToDataClasses(resource)
        }
        dataModelService.get(params.dataModelId)?.addToDataClasses(resource)

        resource
    }
}
