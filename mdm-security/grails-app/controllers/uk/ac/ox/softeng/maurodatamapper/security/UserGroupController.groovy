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

import static org.springframework.http.HttpStatus.FORBIDDEN

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

        int preEditCount = instance.groupMembers.size()

        CatalogueUser catalogueUser = catalogueUserService.get(params.catalogueUserId)

        if (!catalogueUser) return notFound(CatalogueUser, params.catalogueUserId)

        if (request.method == 'PUT' || params.method == 'PUT') instance.addToGroupMembers(catalogueUser)
        else instance.removeFromGroupMembers(catalogueUser)

        int postEditCount = instance.groupMembers.size()

        updateResource instance

        /*
         * The edit is an insert or delete on a joining entity.
         * If and only if the number of group members has changed, so record both IDs
         * against an edit record for both the UserGroup and the CatalogueUser.
         * This avoids logging an edit when an already deleted user is DELETEd again,
         * or an existing member is PUT.
         */
        if (postEditCount != preEditCount) {
            List<String> editedPropertyNames = [request.method ?: params.method, "userGroupID: ${params.userGroupId}, catalogueUserId: ${params.catalogueUserId}"]
            instance.addUpdatedEdit(getCurrentUser(), editedPropertyNames)
            catalogueUser.addUpdatedEdit(getCurrentUser(), editedPropertyNames)
        }

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

    @Transactional
    @Override
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (instance.undeleteable) {
            undeleteableResponse()
        } else {
            deleteResource instance

            deleteResponse instance
        }
    }

    @Override
    void serviceDeleteResource(UserGroup resource) {
        currentUserSecurityPolicyManager = groupBasedSecurityPolicyManagerService.removeUserGroupFromUserSecurityPolicyManagers(
            currentUserSecurityPolicyManager.getUser(),
            resource
        )
        userGroupService.delete(resource)
    }

    private void undeleteableResponse() {
        request.withFormat {
            '*' {
                render status: FORBIDDEN
            }
        }
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
