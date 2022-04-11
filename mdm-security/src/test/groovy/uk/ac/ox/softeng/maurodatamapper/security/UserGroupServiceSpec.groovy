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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class UserGroupServiceSpec extends BaseUnitSpec implements ServiceUnitTest<UserGroupService>, DomainUnitTest<UserGroup>,
    SecurityUsers {

    UUID id

    def setup() {
        log.debug('Setting up UserGroupServiceSpec')
        mockDomains(CatalogueUser, Edit, GroupRole, SecurableResourceGroupRole, Folder)
        implementBasicSecurity('unitTest')
        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())
        // Using service to correctly generate the domains.
        UserGroup funGroup = service.createNewGroup(reader, 'fungroup', 'A group which has fun people in it')
        checkAndSave(funGroup)
        funGroup.addToGroupMembers(reviewer)
        checkAndSave new UserGroup(createdBy: StandardEmailAddress.UNIT_TEST, name: 'empty').addToGroupMembers(reviewer)
        id = readers.id
    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {
        when:
        List<UserGroup> userGroupList = service.list(max: 2, offset: 2)

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
        readers.groupMembers.size() == 1
        checkGroupUser readers, reader

        and:
        funGroup.groupMembers.size() == 2
        checkGroupUser funGroup, admin, false
        checkGroupUser funGroup, editor, false
        checkGroupUser funGroup, reader
        checkGroupUser funGroup, reviewer

    }

    void 'test count'() {
        expect:
        service.count() == 5
    }

    void 'test delete'() {
        expect:
        service.count() == 5

        when:
        service.delete(id)

        then:
        service.count() == 4
    }

    void 'test save'() {
        when:
        UserGroup userGroup = service.createNewGroup(admin, 'pending', 'group for pending people', [pending])
        checkAndSave(userGroup)

        then:
        userGroup.id != null
    }

    void 'test finding user groups by securable resource and group role with no groups in that role'() {
        given:
        Folder folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        SecurableResourceGroupRole resourceGroupRole = new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.unitTest,
            userGroup: editors,
            groupRole: GroupRole.findByName('editor'),
            securableResource: folder)
        checkAndSave(resourceGroupRole)

        when:
        List<UserGroup> groups = service.findAllBySecurableResourceAndGroupRoleId(Folder.simpleName, folder.id, GroupRole.findByName('reader').id)

        then:
        groups.isEmpty()
    }

    void 'test finding user groups by securable resource and group role'() {
        given:
        Folder folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        SecurableResourceGroupRole resourceGroupRole = new SecurableResourceGroupRole(createdBy: userEmailAddresses.unitTest,
                                                                                      userGroup: editors,
                                                                                      groupRole: GroupRole.findByName('editor'),
                                                                                      securableResource: folder)
        checkAndSave(resourceGroupRole)

        when:
        List<UserGroup> groups = service.findAllBySecurableResourceAndGroupRoleId(Folder.simpleName, folder.id, GroupRole.findByName('editor').id)

        then:
        groups.size() == 1
        groups.first().id == editors.id
    }

    void checkGroupUser(UserGroup group, CatalogueUser user, boolean inGroup = true) {
        assert group.hasMember(user) == inGroup
        assert user.isInGroup(group) == inGroup
    }
}
