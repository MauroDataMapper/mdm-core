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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService

import grails.gorm.transactions.Transactional

class DataClassComponentController extends EditLoggingController<DataClassComponent> {

    DataFlowService dataFlowService
    DataClassComponentService dataClassComponentService
    DataClassService dataClassService

    DataClassComponentController() {
        super(DataClassComponent)
    }

    @Transactional
    def alterDataClasses() {

        DataClassComponent instance = queryForResource(params.dataClassComponentId)

        if (!instance) return notFound(params.dataClassComponentId)

        DataClass dataClass = dataClassService.get(params.dataClassId)

        if (!dataClass) return notFound(DataClass, params.dataClassId)

        switch (request.method) {
            case 'PUT':
                switch (params.type) {
                    case 'source':
                        instance.addToSourceDataClasses(dataClass)
                        dataClassService.addDataClassesAreFromDataClass(instance.targetDataClasses, dataClass, currentUser)
                        break
                    case 'target':
                        instance.addToTargetDataClasses(dataClass)
                        dataClassService.addDataClassIsFromDataClasses(dataClass, instance.sourceDataClasses, currentUser)
                        break
                }
                break
            case 'DELETE':
                switch (params.type) {
                    case 'source':
                        instance.removeFromSourceDataClasses(dataClass)
                        dataClassService.removeDataClassesAreFromDataClass(instance.targetDataClasses, dataClass)
                        break
                    case 'target':
                        instance.removeFromTargetDataClasses(dataClass)
                        dataClassService.removeDataClassIsFromDataClasses(dataClass, instance.sourceDataClasses)
                        break
                }
                break
        }

        updateResource instance

        updateResponse instance
    }

    @Override
    protected DataClassComponent queryForResource(Serializable resourceId) {
        return dataClassComponentService.findByDataFlowIdAndId(params.dataFlowId, resourceId)
    }

    @Override
    protected List<DataClassComponent> listAllReadableResources(Map params) {
        return dataClassComponentService.findAllByDataFlowId(params.dataFlowId, params)
    }

    @Override
    void serviceDeleteResource(DataClassComponent resource) {
        dataClassComponentService.delete(resource, true)
    }

    @Override
    protected DataClassComponent createResource() {
        DataClassComponent resource = super.createResource() as DataClassComponent
        resource.dataFlow = dataFlowService.findByTargetDataModelIdAndId(params.dataModelId, params.dataFlowId)
        if (resource.targetDataClasses && resource.sourceDataClasses) {
            dataClassService.addDataClassesAreFromDataClasses(resource.targetDataClasses, resource.sourceDataClasses, currentUser)
        }

        resource
    }
}
