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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.AddsEditHistory

class GroupRoleController extends EditLoggingController<GroupRole> {
    static responseFormats = ['json', 'xml']

    GroupRoleService groupRoleService

    GroupRoleController() {
        super(GroupRole)
    }

    def listApplicationGroupRoles() {
        List<GroupRole> applicationGroupRoles = groupRoleService.findAllApplicationLevelRoles(params)
        respond applicationGroupRoles, view: 'index', model: [userSecurityPolicyManager: currentUserSecurityPolicyManager]
    }

    def listGroupRolesAvailableToSecurableResource() {
        Set<GroupRole> roles = groupRoleService.findAllSecurableResourceLevelRoles(params.securableResourceClass)
        respond roles, model: [userSecurityPolicyManager: currentUserSecurityPolicyManager]
    }

    @Override
    protected GroupRole saveResource(GroupRole resource) {
        log.trace('save resource and refresh cache')
        groupRoleService.save(resource)
        if (resource.instanceOf(AddsEditHistory) && !params.boolean('noHistory')) resource.addCreatedEdit(currentUser)
        resource
    }

    @Override
    protected void serviceDeleteResource(GroupRole resource) {
        // no-op
    }

    @Override
    protected List<GroupRole> listAllReadableResources(Map params) {
        groupRoleService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void deleteResponse(GroupRole instance) {
        methodNotAllowed('Deletion of GroupRoles is not permitted')
    }
}
