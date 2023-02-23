/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.security.policy

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag

@Integration
@Rollback
@Slf4j
@Tag('non-parallel')
class UserSecurityPolicyServiceIntegrationSpec extends BaseIntegrationSpec implements SecurityUsers {

    GroupRole editorRole
    Folder folder2
    UserGroup appAdmins

    UserSecurityPolicyService userSecurityPolicyService
    GroupRoleService groupRoleService

    @Override
    void setupDomainData() {
        log.debug('Setting up UserSecurityPolicyServiceIntegrationSpec')
        implementBasicSecurity('integrationTest')

        editorRole = groupRoleService.getFromCache('editor').groupRole

        UserGroup folderAdmins = new UserGroup(createdBy: userEmailAddresses.integrationTest, name: 'folderAdmins').addToGroupMembers(admin)
        checkAndSave(folderAdmins)
        appAdmins = new UserGroup(createdBy: userEmailAddresses.integrationTest, name: 'appAdmins',
                                  applicationGroupRole: groupRoleService.getFromCache(
                                      GroupRole.USER_ADMIN_ROLE_NAME)
                                      .groupRole)
            .addToGroupMembers(admin)
        checkAndSave(appAdmins)

        checkAndSave(new UserGroup(createdBy: userEmailAddresses.integrationTest, name: 'unused').addToGroupMembers(admin))

        folder = new Folder(label: 'catalogue', createdBy: userEmailAddresses.integrationTest)
        checkAndSave(folder)

        folder2 = new Folder(label: 'catalogue_2', createdBy: userEmailAddresses.integrationTest)
        checkAndSave(folder2)

        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: editors,
                                                    groupRole: groupRoleService.getFromCache(GroupRole.EDITOR_ROLE_NAME).groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: readers,
                                                    groupRole: groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: folderAdmins,
                                                    groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder2,
                                                    userGroup: editors,
                                                    groupRole: groupRoleService.getFromCache(GroupRole.AUTHOR_ROLE_NAME).groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))

        assert SecurableResourceGroupRole.count() == 4
    }

    void 'T01 : test building user security policy manager for editor'() {
        // Editor is in group editors and this has a editor role on folder and author role on folder 2
        // Therefore should have editor and below role on folder and author and below role on folder 2
        // And No application level roles
        given:
        setupData()

        when:
        UserSecurityPolicy policy = userSecurityPolicyService.buildUserSecurityPolicy(editor, editor.groups)
        policy.unlock()

        then:
        policy
        !policy.locked

        and:
        policy.user.id == editor.id
        policy.userGroups.size() == 1
        policy.applicationPermittedRoles.size() == 0
        policy.securableResourceGroupRoles.size() == 2
        policy.virtualSecurableResourceGroupRoles.values().flatten().size() == 10

        when:
        Set<VirtualSecurableResourceGroupRole> folderRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {it.domainId == folder.id}
        Set<VirtualSecurableResourceGroupRole> folder2Roles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {it.domainId == folder2.id}
        Set<VirtualSecurableResourceGroupRole> userRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {
            it.domainType == CatalogueUser.simpleName
        }

        then:
        folderRoles.size() == 4
        folderRoles.any {it.groupRole.name == 'editor'}
        folderRoles.any {it.groupRole.name == 'author'}
        folderRoles.any {it.groupRole.name == 'reviewer'}
        folderRoles.any {it.groupRole.name == 'reader'}

        and:
        folder2Roles.size() == 3
        folder2Roles.any {it.groupRole.name == 'author'}
        folder2Roles.any {it.groupRole.name == 'reviewer'}
        folder2Roles.any {it.groupRole.name == 'reader'}

        and:
        userRoles.size() == 3
        userRoles.every {it.domainId == editor.id}
        userRoles.any {it.groupRole.name == GroupRole.USER_ADMIN_ROLE_NAME}
        userRoles.any {it.groupRole.name == GroupRole.GROUP_ADMIN_ROLE_NAME}
        userRoles.any {it.groupRole.name == GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME}
    }

    void 'T02 : test building user security policy manager for admin'() {
        given:
        setupData()

        when:
        def policy = userSecurityPolicyService.buildUserSecurityPolicy(admin, admin.groups)
        policy.unlock()

        then:
        policy
        !policy.locked

        and:
        policy.user.id == admin.id
        policy.userGroups.size() == 4
        policy.applicationPermittedRoles.size() == 5
        policy.securableResourceGroupRoles.size() == 1
        policy.virtualSecurableResourceGroupRoles.values().flatten().size() == 95

        when:
        Set<VirtualSecurableResourceGroupRole> folderRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {it.domainId == folder.id}
        Set<VirtualSecurableResourceGroupRole> folder2Roles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {it.domainId == folder2.id}
        Set<VirtualSecurableResourceGroupRole> userRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {
            it.domainType == CatalogueUser.simpleName
        }
        Set<VirtualSecurableResourceGroupRole> groupRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {
            it.domainType == UserGroup.simpleName
        }

        then:
        folderRoles.size() == 5
        folderRoles.any {it.groupRole.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME}
        folderRoles.any {it.groupRole.name == 'editor'}
        folderRoles.any {it.groupRole.name == 'author'}
        folderRoles.any {it.groupRole.name == 'reviewer'}
        folderRoles.any {it.groupRole.name == 'reader'}

        and:
        folder2Roles.size() == 5
        folder2Roles.any {it.groupRole.name == 'author'}
        folder2Roles.any {it.groupRole.name == 'reviewer'}
        folder2Roles.any {it.groupRole.name == 'reader'}

        and:
        policy.applicationPermittedRoles.any {it.name == 'user_admin'}
        policy.applicationPermittedRoles.any {it.name == 'group_admin'}
        policy.applicationPermittedRoles.any {it.name == 'container_group_admin'}

        and:
        // 5 users
        userRoles.size() == 50

        and:
        // 6 groups
        groupRoles.size() == 30
    }

    void 'T03 : test building user security policy manager for admin when made siteadmin'() {
        given:
        setupData()
        appAdmins.applicationGroupRole = groupRoleService.getFromCache('site_admin').groupRole
        checkAndSave(appAdmins)

        when:
        def policy = userSecurityPolicyService.buildUserSecurityPolicy(admin, admin.groups)
        policy.unlock()

        then:
        policy
        !policy.locked

        and:
        policy.user.id == admin.id
        policy.userGroups.size() == 4
        policy.applicationPermittedRoles.size() == 5
        policy.securableResourceGroupRoles.size() == 1
        policy.virtualSecurableResourceGroupRoles.values().flatten().size() == 95

        when:
        Set<VirtualSecurableResourceGroupRole> folderRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {it.domainId == folder.id}
        Set<VirtualSecurableResourceGroupRole> folder2Roles = policy
            .virtualSecurableResourceGroupRoles.values().flatten().findAll {it.domainId == folder2.id}
        Set<VirtualSecurableResourceGroupRole> userRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {
            it.domainType == CatalogueUser.simpleName
        }
        Set<VirtualSecurableResourceGroupRole> groupRoles = policy.virtualSecurableResourceGroupRoles.values().flatten().findAll {
            it.domainType == UserGroup.simpleName
        }

        then:
        folderRoles.size() == 5
        folderRoles.any {it.groupRole.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME}
        folderRoles.any {it.groupRole.name == 'editor'}
        folderRoles.any {it.groupRole.name == 'author'}
        folderRoles.any {it.groupRole.name == 'reviewer'}
        folderRoles.any {it.groupRole.name == 'reader'}

        and:
        folder2Roles.size() == 5
        folder2Roles.any {it.groupRole.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME}
        folder2Roles.any {it.groupRole.name == 'editor'}
        folder2Roles.any {it.groupRole.name == 'author'}
        folder2Roles.any {it.groupRole.name == 'reviewer'}
        folder2Roles.any {it.groupRole.name == 'reader'}

        and:
        policy.applicationPermittedRoles.any {it.name == 'site_admin'}
        policy.applicationPermittedRoles.any {it.name == GroupRole.APPLICATION_ADMIN_ROLE_NAME}
        policy.applicationPermittedRoles.any {it.name == 'user_admin'}
        policy.applicationPermittedRoles.any {it.name == 'group_admin'}
        policy.applicationPermittedRoles.any {it.name == 'container_group_admin'}

        and:
        // 5 users
        userRoles.size() == 50

        and:
        // 6 groups
        groupRoles.size() == 30
    }
}
