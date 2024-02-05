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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.ModelInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class TerminologyInterceptor extends ModelInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        Terminology as Class<S>
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'terminologyId')
    }

    @Override
    UUID getId() {
        params.terminologyId ?: params.id
    }

    @Override
    boolean checkExportModelAction() {
        def json = request.getJSON()
        request.setAttribute('cached_body', json)
        params.terminologyIds = []
        if (json) {
            if (json instanceof JSONObject) {
                params.terminologyIds = json.terminologyIds.collect {Utils.toUuid(it)}
            }
            if (json instanceof JSONArray) {
                params.terminologyIds = json.collect {Utils.toUuid(it)}
            }
        }

        if (!params.terminologyIds) throw new ApiBadRequestException('TIXX', 'TerminologyIds must be supplied in the request body')

        UUID id = params.terminologyIds.find {!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), it as UUID)}
        id ? notFound(getSecuredClass(), id) : true
    }


    boolean before() {
        checkModelActionsAuthorised()
    }
}
