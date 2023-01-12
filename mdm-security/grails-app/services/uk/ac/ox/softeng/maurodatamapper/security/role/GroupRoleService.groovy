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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import org.grails.plugin.cache.GrailsCacheManager
import org.springframework.cache.Cache

@Transactional
class GroupRoleService implements MdmDomainService<GroupRole> {

    public static final String GROUP_ROLES_CACHE_NAME = 'mdmSecurityGroupRoles'

    GrailsCacheManager grailsCacheManager

    // This allows use to retrieve the hierarchy of the roles with only 1 call to the database, which should happen in the bootstrapping
    void refreshCacheGroupRoles() {
        grailsCacheManager.destroyCache(GROUP_ROLES_CACHE_NAME)
        Cache cache = grailsCacheManager.getCache(GROUP_ROLES_CACHE_NAME)
        GroupRole.list().each {gr ->
            cache.put(gr.name, new VirtualGroupRole(groupRole: gr, allowedRoles: gr.extractAllowedRoles()))
        }
    }

    // If the roles havent been cached then cache them now
    Cache getGroupRolesCache() {
        if (!grailsCacheManager.cacheExists(GROUP_ROLES_CACHE_NAME)) {
            refreshCacheGroupRoles()
        }
        grailsCacheManager.getCache(GROUP_ROLES_CACHE_NAME)
    }

    VirtualGroupRole getFromCache(String name) {
        getGroupRolesCache().get(name, VirtualGroupRole)
    }

    GroupRole get(Serializable id) {
        GroupRole.get(id) ?: id instanceof String ? getFromCache(id)?.groupRole : null
    }

    @Override
    List<GroupRole> getAll(Collection<UUID> resourceIds) {
        GroupRole.getAll(resourceIds)
    }

    List<GroupRole> list(Map pagination) {
        GroupRole.list(pagination)
    }

    Long count() {
        GroupRole.count()
    }

    // Saves and deletes need to refresh the cache otherwise we'll do something wrong
    GroupRole save(GroupRole groupRole) {
        groupRole.save(flush: true, validate: false)
        refreshCacheGroupRoles()
        groupRole
    }

    @Override
    GroupRole findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        getFromCache(pathIdentifier).groupRole
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(GroupRole groupRole) {
        //refreshCacheGroupRoles()
        throw new ApiInternalException('GRS01', 'Deletion of GroupRoles is not permitted')
    }

    List<GroupRole> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        userSecurityPolicyManager.isApplicationAdministrator() ? list(pagination) : []
    }

    List<GroupRole> findAllApplicationLevelRoles(Map pagination = [:]) {
        GroupRole.byApplicationLevelRole().list(pagination)
    }

    Set<GroupRole> findAllContainerLevelRoles() {
        getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).allowedRoles
    }

    Set<GroupRole> findAllModelLevelRoles(Map pagination = [:]) {
        getFromCache(GroupRole.EDITOR_ROLE_NAME).allowedRoles
    }

    Set<GroupRole> findAllSecurableResourceLevelRoles(Class resourceClass) {
        if (Utils.parentClassIsAssignableFromChild(Container, resourceClass)) {
            return findAllContainerLevelRoles()
        }
        if (Utils.parentClassIsAssignableFromChild(Model, resourceClass)) {
            return findAllModelLevelRoles()
        }
        throw new ApiBadRequestException('GRS02', "Cannot get roles for unknown securable resource class ${resourceClass.simpleName}")
    }

    Set<GroupRole> findAllRolesForGroupRole(GroupRole groupRole, Map pagination = [:]) {
        getFromCache(groupRole.name).allowedRoles
    }
}
