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
package uk.ac.ox.softeng.maurodatamapper.datamodel.traits.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 20/03/2020
 */
abstract class DataModelSecuredInterceptor implements MdmInterceptor {

    abstract Class getModelItemClass()

    void checkIds() {
        Utils.toUuid(params, 'dataModelId')
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'otherDataModelId')
    }

    void checkDataModelId() {
        if (!params.dataModelId) throw new ApiBadRequestException('DMSI01', 'No DataModel Id provided against secured resource')
    }

    void performChecks() {
        checkIds()
        checkDataModelId()
    }

    boolean checkStandardActions() {
        checkActionAuthorisationOnUnsecuredResource(getModelItemClass(), params.id, DataModel, params.dataModelId)
    }

    boolean canReadDataModel() {
        currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.dataModelId) ?: notFound(DataModel,
                                                                                                                 params.dataModelId.toString()
        )
    }

    boolean canCopyFromDataModelToOtherDataModel() {
        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.dataModelId)
        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(DataModel, params.dataModelId)) {
            return forbiddenOrNotFound(canRead, DataModel, params.dataModelId)
        }
        if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.otherDataModelId)) {
            return notFound(DataModel, params.otherDataModelId)
        }
        true
    }
}
