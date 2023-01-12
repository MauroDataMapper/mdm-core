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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroupService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class SecurableResourceGroupRoleController extends EditLoggingController<SecurableResourceGroupRole> {
    static responseFormats = ['json', 'xml']

    SecurableResourceGroupRoleService securableResourceGroupRoleService
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    UserGroupService userGroupService
    GroupRoleService groupRoleService

    SecurableResourceGroupRoleController() {
        super(SecurableResourceGroupRole)
    }

    @Override
    protected SecurableResourceGroupRole queryForResource(Serializable id) {
        if (params.containsKey('groupRoleId') && params.containsKey('userGroupId')) {
            return securableResourceGroupRoleService.findBySecurableResourceAndGroupRoleIdAndUserGroupId(params.securableResourceDomainType,
                                                                                                         params.securableResourceId,
                                                                                                         params.groupRoleId,
                                                                                                         params.userGroupId)
        }

        if (params.containsKey('userGroupId')) {
            return securableResourceGroupRoleService.findByUserGroupIdAndId(params.userGroupId, Utils.toUuid(id))
        }

        securableResourceGroupRoleService.findBySecurableResourceAndId(params.securableResourceDomainType,
                                                                       params.securableResourceId,
                                                                       Utils.toUuid(id))
    }

    @Override
    protected SecurableResourceGroupRole createResource() {
        SecurableResourceGroupRole securableResourceGroupRole = super.createResource() as SecurableResourceGroupRole

        if (params.containsKey('userGroupId')) {
            securableResourceGroupRole.userGroup = userGroupService.get(params.userGroupId)
        }
        if (params.containsKey('groupRoleId')) {
            securableResourceGroupRole.groupRole = groupRoleService.get(params.groupRoleId)
        }
        if (params.containsKey('securableResourceId')) {
            // If the endpoint is POST /userGroups/{userGroupId}/securableResourceGroupRoles
            // with a body containing the securableResourceDomainType, securableResourceId and groupRole,
            // then at this point params.securableResourceClass will be set to 'UserGroup'. So, silence the
            // exception that would otherwise be raised, and drop into the block following this one.
            SecurableResource r = securableResourceGroupRoleService.findSecurableResource(
                params.securableResourceClass as Class, params.securableResourceId, true)
            if (r) {
                securableResourceGroupRole.securableResource = r
            }
        }

        // If the securable resource hasn't been loaded then try and load from what's saved in the domain
        if (!securableResourceGroupRole.securableResource &&
            securableResourceGroupRole.securableResourceId &&
            securableResourceGroupRole.securableResourceDomainType) {
            securableResourceGroupRole.securableResource = securableResourceGroupRoleService
                .findSecurableResource(securableResourceGroupRole.securableResourceDomainType, securableResourceGroupRole.securableResourceId)
        }
        securableResourceGroupRole
    }

    @Override
    protected void serviceDeleteResource(SecurableResourceGroupRole resource) {
        securableResourceGroupRoleService.delete(resource)
    }

    @Override
    protected SecurableResourceGroupRole saveResource(SecurableResourceGroupRole resource) {
        SecurableResourceGroupRole resourceGroupRole = super.saveResource(resource)
        // Update the policies now that we've added a new securable resource group role
        currentUserSecurityPolicyManager = groupBasedSecurityPolicyManagerService
            .refreshAllUserSecurityPolicyManagersBySecurableResourceGroupRole(resourceGroupRole, currentUser)
        resourceGroupRole
    }

    @Override
    protected List<SecurableResourceGroupRole> listAllReadableResources(Map params) {
        if (params.containsKey('userGroupId')) {
            return securableResourceGroupRoleService.findAllByUserGroupId(params.userGroupId, params)
        }
        if (params.containsKey('groupRoleId')) {
            return securableResourceGroupRoleService.findAllBySecurableResourceAndGroupRoleId(params.securableResourceDomainType,
                                                                                              params.securableResourceId,
                                                                                              params.groupRoleId,
                                                                                              params)
        }
        securableResourceGroupRoleService.findAllBySecurableResource(params.securableResourceDomainType, params.securableResourceId, params)
    }
}
