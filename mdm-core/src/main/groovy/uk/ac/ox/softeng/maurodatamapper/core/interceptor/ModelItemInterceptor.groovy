/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.interceptor

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 02/03/2022
 */
abstract class ModelItemInterceptor implements MdmInterceptor {

    abstract Class getModelItemClass()

    abstract Class getModelClass()

    abstract String getModelIdParameterField()

    abstract String getOtherModelIdParameterField()

    void checkIds() {
        Utils.toUuid(params, getModelIdParameterField())
        Utils.toUuid(params, getOtherModelIdParameterField())
        Utils.toUuid(params, 'id')
    }

    void checkModelId() throws ApiBadRequestException {
        if (!params[modelIdParameterField]) throw new ApiBadRequestException('MII01', 'No Model Id provided against secured resource')
    }

    /**
     * Check that when an is MI accessed through another MI the parent MI is contained inside the Model
     * Should throw ApiBadRequestException
     */
    void checkParentModelItemId() throws ApiBadRequestException {
    }

    void performChecks() {
        checkIds()
        checkModelId()
        checkParentModelItemId()
    }

    boolean checkStandardActions() {
        checkActionAuthorisationOnUnsecuredResource(getModelItemClass(), params.id, getModelClass(), params[modelIdParameterField])
    }

    boolean canReadModel() {
        currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getModelClass(), params[modelIdParameterField]) ?:
        notFound(getModelClass(), params[modelIdParameterField].toString()
        )
    }

    boolean canEditModelAndReadOtherModel() {
        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getModelClass(), params[modelIdParameterField])
        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(getModelClass(), params[modelIdParameterField])) {
            return forbiddenOrNotFound(canRead, getModelClass(), params[modelIdParameterField])
        }
        if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getModelClass(), params[otherModelIdParameterField])) {
            return notFound(getModelClass(), params[otherModelIdParameterField])
        }
        true
    }
}
