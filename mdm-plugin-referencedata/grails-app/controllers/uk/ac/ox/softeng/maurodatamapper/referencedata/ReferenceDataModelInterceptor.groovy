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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.ModelInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class ReferenceDataModelInterceptor extends ModelInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        ReferenceDataModel as Class<S>
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'referenceDataModelId')
    }

    @Override
    UUID getId() {
        params.referenceDataModelId ?: params.id
    }

    @Override
    List<String> getAuthenticatedAccessMethods() {
        super.getAuthenticatedAccessMethods() + ['defaultReferenceDataTypeProviders', 'types']
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
        params.referenceDataModelIds = []
        if (json) {
            if (json instanceof JSONObject) {
                params.referenceDataModelIds = json.referenceDataModelIds.collect {Utils.toUuid(it)}
            }
            if (json instanceof JSONArray) {
                params.referenceDataModelIds = json.collect {Utils.toUuid(it)}
            }
        }

        if (!params.referenceDataModelIds) throw new ApiBadRequestException('DMIXX', 'ReferenceDataModelIds must be supplied in the request body')

        UUID id = params.referenceDataModelIds.find {!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it as UUID)}
        id ? notFound(ReferenceDataModel, id) : true
    }

    boolean before() {
        securableResourceChecks()

        if (actionName in ['suggestLinks']) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, getId())) {
                return notFound(ReferenceDataModel, getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, params.otherModelId)) {
                return notFound(ReferenceDataModel, params.otherModelId)
            }
            return true
        }

        checkModelActionsAuthorised()
    }

}
