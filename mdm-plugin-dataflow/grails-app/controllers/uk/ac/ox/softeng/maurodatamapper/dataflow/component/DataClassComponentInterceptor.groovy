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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.controller.DataModelSecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

@Slf4j
class DataClassComponentInterceptor extends DataModelSecuredInterceptor {

    DataFlowService dataFlowService

    @Override
    Class getModelItemClass() {
        DataClassComponent
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'dataFlowId')
        Utils.toUuid(params, 'dataClassComponentId')
        Utils.toUuid(params, 'dataClassId')
    }

    @Override
    void checkParentModelItemId() throws ApiBadRequestException {
        if (!dataFlowService.existsByTargetDataModelIdAndId(params.dataModelId, params.dataFlowId)) {
            if (!dataFlowService.existsBySourceDataModelIdAndId(params.dataModelId, params.dataFlowId)) {
                throw new ApiBadRequestException('DCCI01', 'Provided dataFlowId is not inside provided dataModelId')
            } else {
                log.warn('Access has been acheived through the source DataModel but it should be done through the target DataModel')
            }
        }
    }

    boolean before() {
        performChecks()

        boolean canReadDataModel = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.dataModelId)

        if (actionName in ['alterDataClasses']) {
            return currentUserSecurityPolicyManager.userCanEditResourceId(DataClassComponent, params.dataClassComponentId, DataModel,
                                                                          params.dataModelId) ?:
                   forbiddenOrNotFound(canReadDataModel, DataClassComponent, params.dataClassComponentId)
        }

        checkStandardActions()
    }
}
