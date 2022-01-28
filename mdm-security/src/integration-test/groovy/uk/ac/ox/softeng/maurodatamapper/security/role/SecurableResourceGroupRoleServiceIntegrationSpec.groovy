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


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class SecurableResourceGroupRoleServiceIntegrationSpec extends BaseIntegrationSpec implements SecurityUsers {

    GroupRole editorRole
    Folder folder2

    SecurableResourceGroupRoleService securableResourceGroupRoleService
    GroupRoleService groupRoleService

    @Override
    void setupDomainData() {
        log.debug('Setting up SecurableResourceGroupRoleServiceSpec')
        implementBasicSecurity('integrationTest')

        editorRole = groupRoleService.getFromCache(GroupRole.EDITOR_ROLE_NAME).groupRole

        UserGroup folderAdmins = new UserGroup(createdBy: userEmailAddresses.integrationTest, name: 'folderAdmins').addToGroupMembers(admin)
        checkAndSave(folderAdmins)

        checkAndSave(new UserGroup(createdBy: userEmailAddresses.integrationTest, name: 'unused').addToGroupMembers(admin))

        folder = new Folder(label: 'catalogue', createdBy: userEmailAddresses.integrationTest)
        checkAndSave(folder)

        folder2 = new Folder(label: 'catalogue_2', createdBy: userEmailAddresses.integrationTest)
        checkAndSave(folder2)

        SecurableResourceGroupRole folderEditors = new SecurableResourceGroupRole(securableResource: folder,
                                                                                  userGroup: editors,
                                                                                  groupRole: groupRoleService.getFromCache('editor').groupRole,
                                                                                  createdBy: userEmailAddresses.development)
        checkAndSave(folderEditors)
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: readers,
                                                    groupRole: groupRoleService.getFromCache('reader').groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder,
                                                    userGroup: folderAdmins,
                                                    groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))
        checkAndSave(new SecurableResourceGroupRole(securableResource: folder2,
                                                    userGroup: editors,
                                                    groupRole: groupRoleService.getFromCache('editor').groupRole,
                                                    createdBy: userEmailAddresses.integrationTest))

        id = folderEditors.id

    }

    void "test get"() {
        given:
        setupData()

        expect:
        securableResourceGroupRoleService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> securableResourceGroupRoleList = securableResourceGroupRoleService.list(max: 2, offset: 1)

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
        a.groupRole.id == groupRoleService.getFromCache('reader').groupRole.id

        and:
        b.userGroup.id == UserGroup.findByName('folderAdmins').id
        b.groupRole.id == groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole.id
    }

    void "test count"() {
        given:
        setupData()

        expect:
        securableResourceGroupRoleService.count() == 4
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        securableResourceGroupRoleService.count() == 4

        when:
        securableResourceGroupRoleService.delete(id)

        then:
        securableResourceGroupRoleService.count() == 3
    }

    void 'test finding by valid resource and id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findBySecurableResourceAndId(
            Folder.simpleName, folder.id, id)

        then:
        srgr
        srgr.id == id
    }

    void 'test finding by invalid resource and id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findBySecurableResourceAndId(
            Folder.simpleName, UUID.randomUUID(), id)

        then:
        !srgr
    }

    void 'test finding by valid resource and invalid id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr =
            securableResourceGroupRoleService.findBySecurableResourceAndId(
                Folder.simpleName, folder.id, UUID.randomUUID())

        then:
        !srgr
    }

    void 'test finding by valid usergroup and id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findByUserGroupIdAndId(
            editors.id, id)

        then:
        srgr
        srgr.id == id
    }

    void 'test finding by invalid usergroup and id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findByUserGroupIdAndId(
            UUID.randomUUID(), id)

        then:
        !srgr
    }

    void 'test finding by wrong usergroup and id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findByUserGroupIdAndId(
            UserGroup.findByName('readers').id, id)

        then:
        !srgr
    }

    void 'test finding by valid usergroup and invalid id'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findByUserGroupIdAndId(
            editors.id, UUID.randomUUID())

        then:
        !srgr
    }

    void 'test finding by valid resource and valid group role and valid usergroup'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr =
            securableResourceGroupRoleService.findBySecurableResourceAndGroupRoleIdAndUserGroupId(
                Folder.simpleName, folder.id, editorRole.id,
                editors.id)

        then:
        srgr
        srgr.id == id
    }

    void 'test finding by valid resource and valid group role and invalid usergroup'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr =
            securableResourceGroupRoleService.findBySecurableResourceAndGroupRoleIdAndUserGroupId(
                Folder.simpleName, folder.id, editorRole.id, UUID.randomUUID())

        then:
        !srgr
    }

    void 'test finding by valid resource and invalid group role and valid usergroup'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr =
            securableResourceGroupRoleService.findBySecurableResourceAndGroupRoleIdAndUserGroupId(
                Folder.simpleName, folder.id, UUID.randomUUID(), editors.id)

        then:
        !srgr
    }

    void 'test finding by invalid resource and valid group role and valid usergroup'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findBySecurableResourceAndGroupRoleIdAndUserGroupId(
            Folder.simpleName, UUID.randomUUID(), editorRole.id, editors.id)

        then:
        !srgr
    }

    void 'test finding by valid resource and valid group role and wrong usergroup'() {
        given:
        setupData()

        when:
        SecurableResourceGroupRole srgr = securableResourceGroupRoleService.findBySecurableResourceAndGroupRoleIdAndUserGroupId(
            Folder.simpleName, folder.id, editorRole.id, UserGroup.findByName('readers').id)

        then:
        !srgr
    }

    void 'test finding all roles for a valid usergroup'() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> srgrs = securableResourceGroupRoleService.findAllByUserGroupId(editors.id)

        then:
        srgrs
        srgrs.size() == 2

        and:
        srgrs.every {
            it.securableResourceDomainType == Folder.simpleName
            it.userGroup.id == editors.id
            it.groupRole.id == editorRole.id
        }
        and:
        srgrs.any {it.securableResourceId == folder.id}
        srgrs.any {it.securableResourceId == folder2.id}
    }

    void 'test finding all roles for a invalid usergroup'() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> srgrs = securableResourceGroupRoleService.findAllByUserGroupId(UUID.randomUUID())

        then:
        srgrs.isEmpty()
    }

    void 'test finding all roles for a empty usergroup'() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> srgrs = securableResourceGroupRoleService.findAllByUserGroupId(UserGroup.findByName('unused').id)

        then:
        srgrs.isEmpty()
    }

    void 'test finding all roles for a resource'() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> srgrs = securableResourceGroupRoleService.findAllBySecurableResource(folder.domainType, folder.id)

        then:
        srgrs
        srgrs.size() == 3

        and:
        srgrs.every {
            it.securableResourceDomainType == Folder.simpleName
            it.securableResourceId == folder.id
        }
    }

    void 'test finding all roles for a unknown resource'() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> srgrs = securableResourceGroupRoleService.findAllBySecurableResource(folder.domainType, UUID.randomUUID())

        then:
        srgrs.isEmpty()
    }

    void 'test finding all roles for a resource and group role'() {
        given:
        setupData()

        when:
        List<SecurableResourceGroupRole> srgrs =
            securableResourceGroupRoleService.findAllBySecurableResourceAndGroupRoleId(folder.domainType, folder.id,
                                                                                       editorRole.id)

        then:
        srgrs
        srgrs.size() == 1

        and:
        srgrs.first().securableResourceDomainType == Folder.simpleName
        srgrs.first().securableResourceId == folder.id
        srgrs.first().userGroup.id == editors.id
    }

    void 'test finding securable resource'() {
        given:
        setupData()

        when:
        Folder resource = securableResourceGroupRoleService.findSecurableResource(Folder, folder.id)

        then:
        resource.id == folder.id
    }

    void 'test finding securable resource for unknown'() {
        given:
        setupData()

        when:
        Folder resource = securableResourceGroupRoleService.findSecurableResource(Folder, UUID.randomUUID())

        then:
        !resource
    }

    void 'test anonymisation'() {
        given:
        setupData()

        when: 'list securable resource group roles'
        List<SecurableResourceGroupRole> securableResourceGroupRoleList = securableResourceGroupRoleService.list(max:1000, offset: 0)

        then: 'there are 4; 3 created by integrationTest and 1 by development'
        securableResourceGroupRoleList.size() == 4
        securableResourceGroupRoleList.findAll{it.createdBy == userEmailAddresses.integrationTest}.size() == 3
        securableResourceGroupRoleList.findAll{it.createdBy == userEmailAddresses.development}.size() == 1
        securableResourceGroupRoleList.findAll{it.createdBy == 'anonymous@maurodatamapper.com'}.size() == 0

        when: 'anonymise the development user'
        securableResourceGroupRoleService.anonymise(userEmailAddresses.development)
        securableResourceGroupRoleList = securableResourceGroupRoleService.list(max:1000, offset: 0)

        then: 'the securable resource group role which was created by development is anonymised'
        securableResourceGroupRoleList.size() == 4
        securableResourceGroupRoleList.findAll{it.createdBy == userEmailAddresses.integrationTest}.size() == 3
        securableResourceGroupRoleList.findAll{it.createdBy == userEmailAddresses.development}.size() == 0
        securableResourceGroupRoleList.findAll{it.createdBy == 'anonymous@maurodatamapper.com'}.size() == 1
    }
}
