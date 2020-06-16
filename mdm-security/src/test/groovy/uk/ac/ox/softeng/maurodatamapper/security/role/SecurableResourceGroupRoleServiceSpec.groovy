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


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class SecurableResourceGroupRoleServiceSpec extends BaseUnitSpec implements ServiceUnitTest<SecurableResourceGroupRoleService>,
    DomainUnitTest<GroupRole>,
    SecurityUsers {

    UUID id
    Folder folder
    GroupRole editorRole
    UserGroup editors

    def setup() {
        log.debug('Setting up SecurableResourceGroupRoleServiceSpec')
        mockDomains(CatalogueUser, Edit, UserGroup, GroupRole, Folder)
        implementSecurityUsers('unitTest')
        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())

        editorRole = GroupRole.findByName('editor')

        editors = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'editors').addToGroupMembers(editor)
        checkAndSave(editors)
        UserGroup readers = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'readers').addToGroupMembers(reader).
            addToGroupMembers(reviewer)
        checkAndSave(readers)
        UserGroup folderAdmins = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'folderAdmins').addToGroupMembers(admin)
        checkAndSave(folderAdmins)

        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)

        SecurableResourceGroupRole folderEditors = new SecurableResourceGroupRole(securableResource: folder,
                                                                                  userGroup: editors,
                                                                                  groupRole: GroupRole.findByName('editor'),
                                                                                  createdBy: userEmailAddresses.unitTest)
        checkAndSave(folderEditors)
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: readers,
                                                    groupRole: GroupRole.findByName('reader'),
                                                    createdBy: userEmailAddresses.unitTest))
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: folderAdmins,
                                                    groupRole: GroupRole.findByName(GroupRole.CONTAINER_ADMIN_ROLE_NAME),
                                                    createdBy: userEmailAddresses.unitTest))

        id = folderEditors.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<SecurableResourceGroupRole> securableResourceGroupRoleList = service.list(max: 2, offset: 1)

        then:
        securableResourceGroupRoleList.size() == 2

        and:
        securableResourceGroupRoleList.every {
            it.securableResourceId == folder.id
            it.securableResourceDomainType == Folder.simpleName
        }

        when:
        SecurableResourceGroupRole a = securableResourceGroupRoleList[0]
        SecurableResourceGroupRole b = securableResourceGroupRoleList[1]

        then:
        a.userGroup.id == UserGroup.findByName('readers').id
        a.groupRole.id == GroupRole.findByName('reader').id

        and:
        b.userGroup.id == UserGroup.findByName('folderAdmins').id
        b.groupRole.id == GroupRole.findByName(GroupRole.CONTAINER_ADMIN_ROLE_NAME).id
    }

    void "test count"() {
        expect:
        service.count() == 3
    }

    void "test delete"() {
        expect:
        service.count() == 3

        when:
        service.delete(id)

        then:
        service.count() == 2
    }
}
