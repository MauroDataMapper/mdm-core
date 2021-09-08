/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Rollback
@Integration
class UserGroupServiceIntegrationSpec extends BaseIntegrationSpec implements SecurityUsers {

    UserGroupService userGroupService
    GroupRoleService groupRoleService

    @Override
    void setupDomainData() {
        log.debug('Setting up UserGroupServiceIntegrationSpec')

        folder = new Folder(label: 'catalogue', createdBy: 'integration-test@test.com')
        checkAndSave(folder)

        implementBasicSecurity('integrationTest')

        // Using service to correctly generate the domains.
        UserGroup funGroup = userGroupService.createNewGroup(reader, 'fungroup', 'A group which has fun people in it')
        checkAndSave(funGroup)

        funGroup.addToGroupMembers(reviewer)

        checkAndSave new UserGroup(createdBy: StandardEmailAddress.INTEGRATION_TEST, name: 'empty').addToGroupMembers(reviewer)

        id = readers.id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        userGroupService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<UserGroup> userGroupList = userGroupService.list(max: 2, offset: 2)

        then:
        userGroupList.size() == 2

        when:
        UserGroup readers = userGroupList[0]
        UserGroup funGroup = userGroupList[1]

        then:
        readers.name == 'readers'
        !readers.description
        funGroup.name == 'fungroup'
        funGroup.description == 'A group which has fun people in it'

        and:
        readers.groupMembers.size() == 3
        checkGroupUser readers, author
        checkGroupUser readers, reader
        checkGroupUser readers, reviewer

        and:
        funGroup.groupMembers.size() == 2
        checkGroupUser funGroup, admin, false
        checkGroupUser funGroup, editor, false
        checkGroupUser funGroup, reader
        checkGroupUser funGroup, reviewer

    }

    void "test count"() {
        given:
        setupData()

        expect:
        userGroupService.count() == 5
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        userGroupService.count() == 5

        when:
        userGroupService.delete(id)

        then:
        userGroupService.count() == 4
    }

    void "test save"() {
        given:
        setupData()

        when:
        UserGroup userGroup = userGroupService.createNewGroup(admin, 'pending', 'group for pending people', [pending])
        checkAndSave(userGroup)

        then:
        userGroup.id != null
    }

    void 'test getting user groups by application group role'() {
        given:
        setupData()

        admins.applicationGroupRole = groupRoleService.getFromCache('site_admin').groupRole
        checkAndSave(admins)

        when:
        List<UserGroup> groups = userGroupService.findAllByApplicationGroupRole(groupRoleService.getFromCache('site_admin').groupRole)

        then:
        groups.size() == 1
        groups.first().id == admins.id

        when:
        groups = userGroupService.findAllByApplicationGroupRole(groupRoleService.getFromCache(GroupRole.APPLICATION_ADMIN_ROLE_NAME).groupRole)

        then:
        groups.isEmpty()
    }

    void 'test finding user groups by non-application group role using findAllByApplicationGroupRole'() {
        given:
        setupData()

        admins.applicationGroupRole = groupRoleService.getFromCache('site_admin').groupRole
        checkAndSave(admins)

        when:
        List<UserGroup> groups = userGroupService.findAllByApplicationGroupRole(groupRoleService.getFromCache('editor').groupRole)

        then:
        groups.isEmpty()
    }

    void 'test finding user groups by securable resource and group role with no groups in that role'() {
        given:
        setupData()

        SecurableResourceGroupRole resourceGroupRole = new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.unitTest,
            userGroup: editors,
            groupRole: groupRoleService.getFromCache('editor').groupRole,
            securableResource: folder)
        checkAndSave(resourceGroupRole)

        when:
        List<UserGroup> groups = userGroupService.findAllBySecurableResourceAndGroupRoleId(Folder.simpleName,
                                                                                           folder.id,
                                                                                           groupRoleService.getFromCache('reader').groupRole.id)

        then:
        groups.isEmpty()
    }

    void 'test finding user groups by securable resource and group role'() {
        given:
        setupData()

        SecurableResourceGroupRole resourceGroupRole = new SecurableResourceGroupRole(createdBy: userEmailAddresses.unitTest,
                                                                                      userGroup: editors,
                                                                                      groupRole: groupRoleService.getFromCache('editor').groupRole,
                                                                                      securableResource: folder)
        checkAndSave(resourceGroupRole)

        when:
        List<UserGroup> groups =
            userGroupService.
                findAllBySecurableResourceAndGroupRoleId(Folder.simpleName, folder.id, groupRoleService.getFromCache('editor').groupRole.id)

        then:
        groups.size() == 1
        groups.first().id == editors.id
    }

    void checkGroupUser(UserGroup group, CatalogueUser user, boolean inGroup = true) {
        assert group.hasMember(user) == inGroup
        assert user.isInGroup(group) == inGroup
    }


}
