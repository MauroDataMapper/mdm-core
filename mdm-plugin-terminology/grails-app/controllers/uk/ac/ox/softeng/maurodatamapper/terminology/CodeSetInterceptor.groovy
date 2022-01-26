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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.ModelInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class CodeSetInterceptor extends ModelInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        CodeSet as Class<S>
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'codeSetId')
        Utils.toUuid(params, 'terminologyId')
        Utils.toUuid(params, 'termId')
    }

    @Override
    UUID getId() {
        params.codeSetId ?: params.id
    }

    @Override
    boolean checkExportModelAction() {
        def json = request.getJSON()
        params.codeSetIds = []
        if (json) {
            if (json instanceof JSONObject) {
                params.codeSetIds = json.codeSetIds.collect {Utils.toUuid(it)}
            }
            if (json instanceof JSONArray) {
                params.codeSetIds = json.collect {Utils.toUuid(it)}
            }
        }

        if (!params.codeSetIds) throw new ApiBadRequestException('TIXX', 'CodeSetIds must be supplied in the request body')

        UUID id = params.codeSetIds.find {!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(getSecuredClass(), it as UUID)}
        id ? notFound(getSecuredClass(), id) : true
    }

    boolean before() {
        securableResourceChecks()

        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(CodeSet, getId())

        if (actionName in ['alterTerms']) {
            if (!currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), getId(), 'update')) {
                return forbiddenOrNotFound(canRead, getSecuredClass(), getId())
            }
            return true
        }

        if (isIndex() && params.containsKey('terminologyId')) {
            // checks user's accessibility to resources based on terminologyId
            boolean canReadTerminology = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, params.get('terminologyId'))
            return canReadTerminology ?: notFound(Term, params.termId)
        }

        checkModelActionsAuthorised()
    }
}
