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
package uk.ac.ox.softeng.maurodatamapper.security


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService

import grails.gorm.transactions.Transactional

class UserGroupController extends EditLoggingController<UserGroup> {

    static allowedMethods = [alterMembers: ["DELETE", "PUT"]]

    static responseFormats = ['json', 'xml']

    UserGroupService userGroupService
    CatalogueUserService catalogueUserService
    GroupRoleService groupRoleService
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    UserGroupController() {
        super(UserGroup)
    }

    @Transactional
    def alterMembers() {

        UserGroup instance = queryForResource(params.userGroupId)

        if (!instance) return notFound(params.userGroupId)

        CatalogueUser catalogueUser = catalogueUserService.get(params.catalogueUserId)

        if (!catalogueUser) return notFound(CatalogueUser, params.catalogueUserId)

        if (request.method == 'PUT' || params.method == 'PUT') instance.addToGroupMembers(catalogueUser)
        else instance.removeFromGroupMembers(catalogueUser)

        updateResource instance

        groupBasedSecurityPolicyManagerService.refreshUserSecurityPolicyManager(catalogueUser)

        updateResponse instance
    }

    @Transactional
    def updateApplicationGroupRole() {
        UserGroup instance = queryForResource(params.userGroupId)

        if (!instance) return notFound(params.userGroupId)

        GroupRole groupRole = groupRoleService.get(params.applicationGroupRoleId)

        if (!groupRole) return notFound(GroupRole, params.applicationGroupRoleId)

        if (!groupRole.isApplicationLevelRole()) {
            throw new ApiBadRequestException('UGCXX', 'Cannot set UserGroup Application role to a non-application level role')
        }

        if (request.method == 'PUT' || params.method == 'PUT') instance.applicationGroupRole = groupRole
        else instance.applicationGroupRole = null

        currentUserSecurityPolicyManager = groupBasedSecurityPolicyManagerService.refreshAllUserSecurityPolicyManagersByUserGroup(
            currentUserSecurityPolicyManager.getUser(),
            instance)

        updateResource instance

        updateResponse instance
    }

    @Override
    protected List<UserGroup> listAllReadableResources(Map params) {
        if (params.containsKey('applicationGroupRoleId')) {
            return userGroupService.findAllByApplicationGroupRoleId(params.applicationGroupRoleId, params)
        }
        if (params.containsKey('securableResourceId') && params.containsKey('groupRoleId')) {
            return userGroupService.findAllBySecurableResourceAndGroupRoleId(params.securableResourceDomainType,
                                                                             params.securableResourceId,
                                                                             params.groupRoleId,
                                                                             params)
        }
        userGroupService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    void serviceDeleteResource(UserGroup resource) {
        currentUserSecurityPolicyManager = groupBasedSecurityPolicyManagerService.removeUserGroupFromUserSecurityPolicyManagers(
            currentUserSecurityPolicyManager.getUser(),
            resource
        )
        userGroupService.delete(resource)
    }

    @Override
    protected UserGroup createResource() {
        UserGroup resource = super.createResource() as UserGroup
        resource.addToGroupMembers(getCurrentUser())
        resource
    }

    @Override
    protected UserGroup saveResource(UserGroup resource) {
        UserGroup userGroup = super.saveResource(resource)
        currentUserSecurityPolicyManager = groupBasedSecurityPolicyManagerService.addUserGroupToUserSecurityPolicyManagers(
            currentUserSecurityPolicyManager.getUser(),
            userGroup
        )
        userGroup
    }

    @Override
    CatalogueUser getCurrentUser() {
        groupBasedSecurityPolicyManagerService.ensureUserSecurityPolicyManagerHasCatalogueUser(currentUserSecurityPolicyManager)
        currentUserSecurityPolicyManager.getUser() as CatalogueUser
    }
}
