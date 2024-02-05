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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.ModelInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class DataModelInterceptor extends ModelInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        DataModel as Class<S>
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'dataModelId')
        Utils.toUuid(params, 'otherDataModelId')
        Utils.toUuid(params, 'otherDataTypeId')
        Utils.toUuid(params, 'otherDataClassId')
    }

    @Override
    UUID getId() {
        params.dataModelId ?: params.id
    }

    @Override
    List<String> getAuthenticatedAccessMethods() {
        super.getAuthenticatedAccessMethods() + ['defaultDataTypeProviders', 'types']
    }

    @Override
    List<String> getReadAccessMethods() {
        super.getReadAccessMethods() + ['hierarchy', 'search']
    }

    @Override
    List<String> getEditAccessMethods() {
        super.getEditAccessMethods() + ['deleteAllUnusedDataClasses', 'deleteAllUnusedDataTypes']
    }

    @Override
    boolean checkExportModelAction() {
        def json = request.getJSON()
        request.setAttribute('cached_body', json)
        params.dataModelIds = []
        if (json) {
            if (json instanceof JSONObject) {
                params.dataModelIds = json.dataModelIds.collect {Utils.toUuid(it)}
            }
            if (json instanceof JSONArray) {
                params.dataModelIds = json.collect {Utils.toUuid(it)}
            }
        }

        if (!params.dataModelIds) throw new ApiBadRequestException('DMIXX', 'DataModelIds must be supplied in the request body')

        UUID id = params.dataModelIds.find {!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it as UUID)}
        id ? notFound(DataModel, id) : true
    }

    boolean before() {
        securableResourceChecks()

        boolean canReadId = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, getId())

        if (actionName in ['intersectsMany']) {
            if (!canReadId) {
                return notFound(DataModel, getId())
            }
            return true
        }

        if (actionName in ['suggestLinks', 'intersects']) {
            if (!canReadId) {
                return notFound(DataModel, getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.otherDataModelId)) {
                return notFound(DataModel, params.otherDataModelId)
            }
            return true
        }

        if (actionName in ['importDataType', 'importDataClass']) {
            if (!currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), getId(), 'update')) {
                return forbiddenOrNotFound(canReadId, getSecuredClass(), getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), params.otherDataModelId)) {
                return notFound(getSecuredClass(), params.otherDataModelId)
            }
            return true
        }

        if (actionName in ['subset']) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), getId())) {
                return notFound(getSecuredClass(), getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), params.otherDataModelId)) {
                return notFound(getSecuredClass(), params.otherDataModelId)
            }

            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), params.otherDataModelId, actionName) ?:
                forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(getSecuredClass(), params.otherDataModelId))
        }

        checkModelActionsAuthorised()
    }

}
