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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.controller.DataModelSecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class DataFlowInterceptor extends DataModelSecuredInterceptor {

    @Override
    Class getModelItemClass() {
        DataFlow
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'dataFlowId')
    }

    boolean before() {

        if (actionName in ['exporterProviders']) {
            return true
        }
            
        performChecks()

        if (actionName in ['importerProviders']) {
            return currentUserSecurityPolicyManager.isAuthenticated() ?: forbiddenDueToNotAuthenticated()
        }

        boolean canReadDataModel = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.dataModelId)

        if (actionName in ['exportDataFlow', 'exportDataFlows']) {
            return currentUserSecurityPolicyManager.userCanReadResourceId(DataFlow, params.id, DataModel, params.dataModelId) ?:
                   forbiddenOrNotFound(canReadDataModel, DataFlow, params.id)
        }

        if (actionName in ['importDataFlow', 'importDataFlows']) {
            return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(DataModel, params.dataModelId) ?:
                   forbiddenOrNotFound(canReadDataModel, DataModel, params.dataModelId)
        }

        if (actionName in ['updateDiagramLayout']) {
            return currentUserSecurityPolicyManager.userCanEditResourceId(DataFlow, params.dataFlowId, DataModel, params.dataModelId) ?:
                   forbiddenOrNotFound(canReadDataModel, DataFlow, params.dataFlowId)
        }

        checkStandardActions()
    }
}
