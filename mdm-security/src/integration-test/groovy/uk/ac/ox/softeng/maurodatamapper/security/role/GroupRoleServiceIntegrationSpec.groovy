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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.grails.plugin.cache.GrailsCacheManager
import org.springframework.cache.Cache

@Slf4j
@Integration
@Rollback
class GroupRoleServiceIntegrationSpec extends BaseIntegrationSpec implements SecurityUsers {

    GroupRoleService groupRoleService
    GrailsCacheManager grailsCacheManager

    void setupDomainData() {
        folder = new Folder(label: 'catalogue', createdBy: 'integration-test@test.com')
        checkAndSave(folder)

        id = GroupRole.findByName('reader').id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        groupRoleService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<GroupRole> groupRoleList = groupRoleService.list(max: 2, offset: 2, sort: 'name')

        then:
        groupRoleList.size() == 2

        when:
        GroupRole a = groupRoleList[0]
        GroupRole b = groupRoleList[1]

        then:
        a.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME
        a.displayName == 'Container Administrator'

        and:
        b.name == 'container_group_admin'
        b.displayName == 'Container Group Administrator'
    }

    void "test count"() {
        given:
        setupData()

        expect:
        groupRoleService.count() == 10
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        groupRoleService.count() == 10

        when:
        groupRoleService.delete(id)

        then:
        Exception ex = thrown(ApiInternalException)
        ex.message == 'Deletion of GroupRoles is not permitted'
    }

    void 'test find all application level roles'() {
        given:
        setupData()

        when:
        List<GroupRole> roles = groupRoleService.findAllApplicationLevelRoles()

        then:
        roles.size() == 5

        and:
        roles.any {it.name == 'site_admin'}
        roles.any {it.name == GroupRole.APPLICATION_ADMIN_ROLE_NAME}
        roles.any {it.name == 'user_admin'}
        roles.any {it.name == 'group_admin'}
        roles.any {it.name == 'container_group_admin'}

    }

    void 'test find all folder level roles'() {
        given:
        setupData()

        when:
        Set<GroupRole> roles = groupRoleService.findAllContainerLevelRoles()

        then:
        roles.size() == 6

        and:
        roles.any {it.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME}
        roles.any {it.name == 'editor'}
        roles.any {it.name == 'author'}
        roles.any {it.name == 'reviewer'}
        roles.any {it.name == 'reader'}
        roles.any {it.name == 'container_group_admin'}

    }

    void 'test find all securable resource level roles'() {
        given:
        setupData()

        when:
        Set<GroupRole> roles = groupRoleService.findAllSecurableResourceLevelRoles(Folder)

        then:
        roles.size() == 6

        and:
        roles.any {it.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME}
        roles.any {it.name == 'editor'}
        roles.any {it.name == 'author'}
        roles.any {it.name == 'reviewer'}
        roles.any {it.name == 'reader'}
        roles.any {it.name == 'container_group_admin'}

    }

    void 'test find all model level roles'() {
        given:
        setupData()

        when:
        Set<GroupRole> roles = groupRoleService.findAllModelLevelRoles()

        then:
        roles.size() == 4

        and:
        roles.any {it.name == 'editor'}
        roles.any {it.name == 'author'}
        roles.any {it.name == 'reviewer'}
        roles.any {it.name == 'reader'}
    }

    void 'test find all roles for author group role'() {
        given:
        setupData()

        when:
        Set<GroupRole> roles = groupRoleService.findAllRolesForGroupRole(GroupRole.findByName('author'))

        then:
        roles.size() == 3

        and:
        roles.any {it.name == 'author'}
        roles.any {it.name == 'reviewer'}
        roles.any {it.name == 'reader'}

    }

    void 'test find all roles for reader group role'() {
        given:
        setupData()

        when:
        Set<GroupRole> roles = groupRoleService.findAllRolesForGroupRole(GroupRole.findByName('reader'))

        then:
        roles.size() == 1

        and:
        roles.any {it.name == 'reader'}
    }

    void 'test loading roles into the grails cache'() {
        given:
        setupData()

        when:
        groupRoleService.refreshCacheGroupRoles()

        then:
        grailsCacheManager.cacheExists(GroupRoleService.GROUP_ROLES_CACHE_NAME)

        when:
        Cache cache = groupRoleService.getGroupRolesCache()

        then:
        cache.get('reader', VirtualGroupRole).allowedRoles.size() == 1
        cache.get('reviewer', VirtualGroupRole).allowedRoles.size() == 2
        cache.get('author', VirtualGroupRole).allowedRoles.size() == 3
        cache.get('editor', VirtualGroupRole).allowedRoles.size() == 4
        cache.get(GroupRole.CONTAINER_ADMIN_ROLE_NAME, VirtualGroupRole).allowedRoles.size() == 6

        and:
        cache.get('container_group_admin', VirtualGroupRole).allowedRoles.size() == 1
        cache.get('group_admin', VirtualGroupRole).allowedRoles.size() == 2
        cache.get('user_admin', VirtualGroupRole).allowedRoles.size() == 3
        cache.get(GroupRole.APPLICATION_ADMIN_ROLE_NAME, VirtualGroupRole).allowedRoles.size() == 4
        cache.get('site_admin', VirtualGroupRole).allowedRoles.size() == 5
    }
}
