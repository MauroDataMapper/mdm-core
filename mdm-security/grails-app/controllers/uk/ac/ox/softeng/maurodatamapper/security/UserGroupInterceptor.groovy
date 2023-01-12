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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.TieredAccessSecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class UserGroupInterceptor extends TieredAccessSecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        UserGroup
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'userGroupId')
        Utils.toUuid(params, 'groupRoleId')
        Utils.toUuid(params, 'applicationGroupRoleId')

        mapDomainTypeToClass('securableResource')
        mapDomainTypeToClass('container')
    }

    @Override
    UUID getId() {
        params.userGroupId ?: params.id
    }

    @Override
    UUID getSecuredId() {
        getId() ?: params.securableResourceId
    }

    boolean before() {
        securableResourceChecks()
        if (isIndex()) {
            if (params.containsKey('applicationGroupRoleId')) {
                return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: forbiddenDueToNotApplicationAdministrator()
            }
            if (params.containsKey('securableResourceId')) {
                return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(params.securableResourceClass, params.securableResourceId) ?:
                       notFound(params.securableResourceClass, params.securableResourceId)
            }
        }

        if (params.containsKey('containerId')) {
            if (isDelete() || isUpdate() || isSave()) {
                return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(params.containerClass, params.containerId) &&
                       checkActionAuthorisationOnSecuredResource(UserGroup, getId())
            }

            if (isIndex() || isShow()) {
                return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(params.containerClass, params.containerId) &&
                       checkActionAuthorisationOnSecuredResource(UserGroup, getId())
            }
        }

        checkTieredAccessActionAuthorisationOnSecuredResource(UserGroup, getId())
    }

    @Override
    List<String> getEditAccessMethods() {
        ['alterMembers']
    }

    @Override
    List<String> getApplicationAdminAccessMethods() {
        ['updateApplicationGroupRole']
    }
}
