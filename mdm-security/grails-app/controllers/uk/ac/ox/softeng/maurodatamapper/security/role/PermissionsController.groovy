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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.springframework.beans.factory.annotation.Autowired

/**
 * This is a temporary controller until the UI can be updated to handle the new {@link SecurableResourceGroupRole}
 * @Deprecated (forRemoval = true)
 */
@Deprecated(forRemoval = true)
class PermissionsController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    static allowedMethods = [show: []]

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices

    SecurableResourceGroupRoleService securableResourceGroupRoleService
    GroupRoleService groupRoleService

    def permissions() {

        SecurableResource securableResource = findSecurableResourceByDomainTypeAndId(params.securableResourceClass, params.securableResourceId)
        if (!securableResource) return notFound(params.securableResourceClass, params.securableResourceId)

        GroupRole readerRole
        GroupRole editorRole

        if (Utils.parentClassIsAssignableFromChild(Model, params.securableResourceClass)) {
            editorRole = groupRoleService.getFromCache(GroupRole.EDITOR_ROLE_NAME).groupRole
            readerRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole
        } else if (Utils.parentClassIsAssignableFromChild(Container, params.securableResourceClass)) {
            editorRole = groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole
            readerRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole
        } else if (Utils.parentClassIsAssignableFromChild(CatalogueUser, params.securableResourceClass)) {
            editorRole = groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME).groupRole
            readerRole = groupRoleService.getFromCache(GroupRole.GROUP_ADMIN_ROLE_NAME).groupRole
        } else if (Utils.parentClassIsAssignableFromChild(UserGroup, params.securableResourceClass)) {
            editorRole = groupRoleService.getFromCache(GroupRole.GROUP_ADMIN_ROLE_NAME).groupRole
            readerRole = groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole
        } else {
            return methodNotAllowed("Cannot get permissions on securable resource ${params.securableResourceClass}")
        }

        List<SecurableResourceGroupRole> allPermissions = securableResourceGroupRoleService.findAllBySecurableResource(
            params.securableResourceDomainType,
            params.securableResourceId)

        List<UserGroup> groupsWhichCanReadResource = allPermissions.findAll {permission ->
            VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(permission.groupRole.name)
            virtualGroupRole.allowedRoles.any {role ->
                role.applicationLevelRole == readerRole.applicationLevelRole && role == readerRole
            }
        }.collect {it.userGroup}

        List<UserGroup> groupsWhichCanWriteResource = allPermissions.findAll {permission ->
            VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(permission.groupRole.name)
            virtualGroupRole.allowedRoles.any {role ->
                role.applicationLevelRole == readerRole.applicationLevelRole && role == editorRole
            }
        }.collect {it.userGroup}

        respond securableResource: securableResource, readerGroups: groupsWhichCanReadResource, writerGroups: groupsWhichCanWriteResource
    }

   protected SecurableResource findSecurableResourceByDomainTypeAndId(Class domainType, UUID securableResourceId) {
       SecurableResourceService service = securableResourceServices.find {it.handles(domainType)}
       if (!service) throw new ApiBadRequestException('PC01', "Securable resource retrieval for resource [${domainType}] with no supporting " +
                                                              "service")
       service.get(securableResourceId)
   }
}
