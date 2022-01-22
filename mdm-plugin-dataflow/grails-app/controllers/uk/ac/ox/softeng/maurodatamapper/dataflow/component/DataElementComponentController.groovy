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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
class DataElementComponentController extends EditLoggingController<DataElementComponent> {

    DataElementComponentService dataElementComponentService
    DataClassComponentService dataClassComponentService
    DataElementService dataElementService

    DataElementComponentController() {
        super(DataElementComponent)
    }

    @Transactional
    def alterDataElements() {

        DataElementComponent instance = queryForResource(params.dataElementComponentId)

        if (!instance) return notFound(params.dataElementComponentId)

        DataElement dataElement = dataElementService.get(params.dataElementId)

        if (!dataElement) return notFound(DataElement, params.dataElementId)

        switch (request.method) {
            case 'PUT':
                switch (params.type) {
                    case 'source':
                        instance.addToSourceDataElements(dataElement)
                        dataElementService.addDataElementsAreFromDataElement(instance.targetDataElements, dataElement, currentUser)
                        break
                    case 'target':
                        instance.addToTargetDataElements(dataElement)
                        dataElementService.addDataElementIsFromDataElements(dataElement, instance.sourceDataElements, currentUser)
                        break
                }
                break
            case 'DELETE':
                switch (params.type) {
                    case 'source':
                        instance.removeFromSourceDataElements(dataElement)
                        dataElementService.removeDataElementsAreFromDataElement(instance.targetDataElements, dataElement)
                        break
                    case 'target':
                        instance.removeFromTargetDataElements(dataElement)
                        dataElementService.removeDataElementIsFromDataElements(dataElement, instance.sourceDataElements)
                        break
                }
                break
        }

        updateResource instance

        updateResponse instance
    }

    @Override
    protected DataElementComponent queryForResource(Serializable resourceId) {
        if (!params.dataClassComponentId) {
            log.warn('Old URL being used, accessing DataElementComponent via DataFlow not DataClassComponent')
            return dataElementComponentService.findByDataFlowIdAndId(params.dataFlowId, resourceId)
        }
        return dataElementComponentService.findByDataClassComponentIdAndId(params.dataClassComponentId, resourceId)
    }

    @Override
    protected List<DataElementComponent> listAllReadableResources(Map params) {
        if (params.dataClassId) {
            log.warn('Old URL being used, accessing DataElementComponent via DataClass not DataClassComponent')
            return dataElementComponentService.findAllByDataFlowIdAndDataClassId(params.dataFlowId, params.dataClassId, params)
        }
        if (!params.dataClassComponentId) {
            log.warn('Old URL being used, accessing DataElementComponent via DataFlow not DataClassComponent')
            return dataElementComponentService.findAllByDataFlowId(params.dataFlowId, params)
        }
        return dataElementComponentService.findAllByDataClassComponentId(params.dataClassComponentId, params)
    }

    @Override
    void serviceDeleteResource(DataElementComponent resource) {
        dataElementComponentService.delete(resource, true)
    }

    @Override
    protected DataElementComponent createResource() {
        DataElementComponent resource = super.createResource() as DataElementComponent
        resource.dataClassComponent = dataClassComponentService.get(params.dataClassComponentId)

        if (!resource.dataClassComponent) {
            resource.dataClassComponent = dataClassComponentService.findOrCreateDataClassComponentForDataElementComponent(resource, currentUser)
        }

        if (resource.targetDataElements && resource.sourceDataElements) {
            dataElementService.addDataElementsAreFromDataElements(resource.targetDataElements, resource.sourceDataElements, currentUser)
        }

        resource
    }
}
