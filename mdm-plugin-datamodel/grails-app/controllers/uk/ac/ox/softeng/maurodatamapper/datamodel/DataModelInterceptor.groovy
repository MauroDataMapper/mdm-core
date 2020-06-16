/*
 * Copyright 2020 University of Oxford
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
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.TieredAccessSecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class DataModelInterceptor extends TieredAccessSecurableResourceInterceptor {

    DataModelService dataModelService

    DataModelInterceptor() {
        match(controller: 'dataModel')
    }

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        DataModel as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'dataModelId')
        Utils.toUuid(params, 'folderId')
        Utils.toUuid(params, 'otherDataModelId')
    }

    @Override
    UUID getId() {
        params.dataModelId ?: params.id
    }

    @Override
    List<String> getPublicAccessMethods() {
        ['exporterProviders']
    }

    @Override
    List<String> getAuthenticatedAccessMethods() {
        ['importDataModel', 'importDataModels', 'importerProviders', 'defaultDataTypeProviders', 'types']
    }

    @Override
    List<String> getReadAccessMethods() {
        ['exportDataModel', 'newModelVersion', 'hierarchy', 'search']
    }

    @Override
    List<String> getEditAccessMethods() {
        ['finalise', 'deleteAllUnusedDataClasses', 'deleteAllUnusedDataTypes']
    }

    @Override
    List<String> getApplicationAdminAccessMethods() {
        ['deleteAll', 'documentSuperseded', 'modelSuperseded', 'deleted']
    }

    boolean before() {

        securableResourceChecks()


        if (params.containsKey('folderId')) {
            boolean canReadFolder = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, params.folderId)

            // We control addition of DataModels into containers by using container permissions
            if (isSave()) {
                return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(Folder, params.folderId) ?:
                       forbiddenOrNotFound(canReadFolder, Folder, params.folderId)
            }
            if (actionName == 'changeFolder') {
                boolean canReadModel = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, getId())

                if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(DataModel, getId())) {
                    return forbiddenOrNotFound(canReadModel, DataModel, getId())
                }

                if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, params.folderId)) {
                    return forbiddenOrNotFound(canReadFolder, Folder, params.folderId)
                }
                return true
            }
            if (isIndex() && params.folderId) {
                return canReadFolder ?: notFound(Folder, params.folderId)
            }
        }

        if (actionName in ['diff', 'suggestLinks']) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, getId())) {
                return notFound(DataModel, getId())
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.otherDataModelId)) {
                return notFound(DataModel, params.otherDataModelId)
            }
            return true
        }

        if (actionName == 'newDocumentationVersion') {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, getId())) {
                return notFound(DataModel, getId())
            }
            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(DataModel, getId(), 'newDocumentationVersion') ?:
                   forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(DataModel, getId()))
        }


        if (actionName == 'exportDataModels') {
            def json = request.getJSON()
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
            return !id ?: notFound(DataModel, id)
        }

        checkTieredAccessActionAuthorisationOnSecuredResource(DataModel, getId())
    }

}
