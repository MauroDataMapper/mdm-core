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
package uk.ac.ox.softeng.maurodatamapper.security


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

@Slf4j
class UserGroupControllerSpec extends ResourceControllerSpec<UserGroup> implements ControllerUnitTest<UserGroupController>,
    DomainUnitTest<UserGroup>, SecurityUsers {

    def setup() {
        log.debug('Setting up usergroup controller unit')
        mockDomains(CatalogueUser, Edit, GroupRole)
        implementSecurityUsers('unitTest')
        domain.name = 'readers'
        domain.description = 'readers only'
        domain.createdBy = reader.emailAddress
        domain.addToGroupMembers(reader).addToGroupMembers(reviewer)
        checkAndSave(domain)

        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())

        controller.userGroupService = Stub(UserGroupService) {
            get(_) >> {UUID id -> UserGroup.get(id)}
            findAllByUser(_, _) >> UserGroup.list()
            delete(_) >> {UserGroup group ->
                if (!group) return
                List<CatalogueUser> members = []
                members += group.groupMembers
                members.each {
                    it.removeFromGroups(group)
                }
                group.delete(flush: true)
            }
            findAllByApplicationGroupRoleId(_, _) >> {UUID rid, Map p ->
                if (rid == GroupRole.findByName('site_admin').id) return [UserGroup.findByName('admins')]
                []
            }
        }
        controller.catalogueUserService = Stub(CatalogueUserService) {
            get(_) >> {UUID id -> CatalogueUser.get(id)}
            findOrCreateUserFromInterface(_) >> {User user ->
                if (user instanceof CatalogueUser) return user
                CatalogueUser catalogueUser = CatalogueUser.get(user.id) ?: CatalogueUser.findByEmailAddress(user.emailAddress)
                if (catalogueUser) return catalogueUser
                new CatalogueUser().with {
                    emailAddress = user.emailAddress
                    firstName = user.firstName
                    lastName = user.lastName
                    tempPassword = user.tempPassword
                    createdBy = user.emailAddress
                    save()
                }
            }
        }

        controller.groupRoleService = Stub(GroupRoleService) {
            get(_) >> {UUID id -> GroupRole.get(id)}
        }

        controller.groupBasedSecurityPolicyManagerService = Stub(GroupBasedSecurityPolicyManagerService) {
            refreshUserSecurityPolicyManager(_) >> null
            addUserGroupToUserSecurityPolicyManagers(_, _) >> PublicAccessSecurityPolicyManager.instance
            removeUserGroupFromUserSecurityPolicyManagers(_, _) >> PublicAccessSecurityPolicyManager.instance
            refreshAllUserSecurityPolicyManagersByUserGroup(_, _) >> PublicAccessSecurityPolicyManager.instance
            ensureUserSecurityPolicyManagerHasCatalogueUser(_) >> {UserSecurityPolicyManager userSecurityPolicyManager ->
                userSecurityPolicyManager.user = controller.catalogueUserService.findOrCreateUserFromInterface(userSecurityPolicyManager.user)
            }
        }

        givenParameters()
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "createdBy": "reader@test.com",
      "description": "readers only",
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "availableActions": ["delete","show","update"]
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '{"total":1, "errors": [{"message": "Property [name] of class ' +
        '[class uk.ac.ox.softeng.maurodatamapper.security.UserGroup] cannot be null"}]}'
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '{"total":1, "errors": [{"message": "Property [name] of class ' +
        '[class uk.ac.ox.softeng.maurodatamapper.security.UserGroup] cannot be null"}]}'
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
        "name": "valid",
        "description": "a description",
        "id":"${json-unit.ignore}",
        "createdBy":"unlogged_user@mdm-core.com",
        "availableActions": ["delete","show","update"]
        }'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
      "createdBy": "reader@test.com",
      "description": "readers only",
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "availableActions": ["delete","show","update"]
    }'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '{"total":1, "errors": [{"message": "Property [name] of class ' +
        '[class uk.ac.ox.softeng.maurodatamapper.security.UserGroup] cannot be null"}]}'
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
      "createdBy": "reader@test.com",
      "description": "Updated description",
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "availableActions": ["delete","show","update"]
    }'''
    }

    @Override
    UserGroup invalidUpdate(UserGroup instance) {
        instance.name = ''
        instance
    }

    @Override
    UserGroup validUpdate(UserGroup instance) {
        instance.description = 'Updated description'
        instance
    }

    @Override
    UserGroup getInvalidUnsavedInstance() {
        new UserGroup(description: 'my test group')
    }

    @Override
    UserGroup getValidUnsavedInstance() {
        new UserGroup(name: 'valid', description: 'a description')
    }

    void 'RU01 - Test remove null user from null group'() {
        when:
        params.userGroupId = null
        params.catalogueUserId = null
        request.method = 'DELETE'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void 'RU01 - Test remove null user from unknown group'() {
        when:
        params.userGroupId = UUID.randomUUID()
        params.catalogueUserId = null
        request.method = 'DELETE'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void 'RU02 - Test remove null user from known group'() {
        when:
        params.userGroupId = domain.id
        params.catalogueUserId = null
        request.method = 'DELETE'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson(CatalogueUser)
    }

    void 'RU03 - Test remove known user from null group'() {
        when:
        params.userGroupId = null
        params.catalogueUserId = reader.id
        request.method = 'DELETE'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void 'RU04 - Test remove random user from known group'() {
        when:
        params.userGroupId = domain.id
        params.catalogueUserId = UUID.randomUUID()
        request.method = 'DELETE'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson(CatalogueUser)
    }

    void 'RU05 - Test remove known user from known group'() {
        when:
        params.userGroupId = domain.id
        params.catalogueUserId = reader.id
        request.method = 'DELETE'
        controller.alterMembers()

        then:
        verifyJsonResponse OK, getExpectedShowJson()

        and:
        UserGroup.get(domain.id).groupMembers.size() == 1
        UserGroup.get(domain.id).groupMembers.any {it.id != reader.id}

        and:
        CatalogueUser.get(reader.id).groups.isEmpty()
    }

    void 'AU01 - Test add null user to null group'() {
        when:
        params.userGroupId = null
        params.catalogueUserId = null
        request.method = 'PUT'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void 'AU02 - Test add null user to unknown group'() {
        when:
        params.userGroupId = UUID.randomUUID()
        params.catalogueUserId = null
        request.method = 'PUT'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void 'AU03 - Test add null user to known group'() {
        when:
        params.userGroupId = domain.id
        params.catalogueUserId = null
        request.method = 'PUT'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson(CatalogueUser)
    }

    void 'AU03 - Test add known user to null group'() {
        when:
        params.userGroupId = null
        params.catalogueUserId = editor.id
        request.method = 'PUT'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void 'AU04 - Test add random user to known group'() {
        when:
        params.userGroupId = domain.id
        params.catalogueUserId = UUID.randomUUID()
        request.method = 'PUT'
        controller.alterMembers()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson(CatalogueUser)
    }

    void 'AU05 - Test add known user to known group'() {
        when:
        params.userGroupId = domain.id
        params.catalogueUserId = editor.id
        request.method = 'PUT'
        controller.alterMembers()

        then:
        verifyJsonResponse OK, getExpectedShowJson()

        and:
        UserGroup.get(domain.id).groupMembers.size() == 3
        UserGroup.get(domain.id).groupMembers.any {it.id == editor.id}

        and:
        CatalogueUser.get(editor.id).groups.size() == 1
        CatalogueUser.get(editor.id).groups.any {it.id == domain.id}
    }

    void 'GR01 - Test getting groups of empty application role'() {

        when: 'no groups in the role'
        params.applicationGroupRoleId = GroupRole.findByName('user_admin').id
        controller.index()

        then:
        verifyJsonResponse OK, '{"count": 0,"items": []}'
    }

    void 'GR02 - Test getting groups of non application role'() {

        when: 'no groups in the role'
        params.applicationGroupRoleId = GroupRole.findByName('editor').id
        controller.index()

        then:
        verifyJsonResponse OK, '{"count": 0,"items": []}'
    }

    void 'GR03 - Test getting groups of non-empty application role'() {
        given:
        GroupRole siteAdmin = GroupRole.findByName('site_admin')
        UserGroup adminGroup = new UserGroup(
            createdBy: userEmailAddresses.unitTest,
            name: 'admins',
            applicationGroupRole: siteAdmin
        ).addToGroupMembers(admin)
        checkAndSave(adminGroup)

        when: 'no groups in the role'
        params.applicationGroupRoleId = siteAdmin.id
        controller.index()

        then:
        verifyJsonResponse OK, '''{"count": 1,"items": [{
      "createdBy": "unit-test@test.com",
      "id": "${json-unit.matches:id}",
      "name": "admins",
      "availableActions": ["delete","show","update"]
    }]}'''
    }

    void 'SR01 - Set null group to application role'() {
        given:
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('site_admin').id
        params.userGroupId = null

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void 'SR02 - Set unknown group to application role'() {
        given:
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('site_admin').id
        params.userGroupId = UUID.randomUUID()

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void 'SR03 - Set group to null application role'() {
        given:
        request.method = 'PUT'
        params.applicationGroupRoleId = null
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson(GroupRole)
    }

    void 'SR04 - Set group to unknown application role'() {
        given:
        request.method = 'PUT'
        params.applicationGroupRoleId = UUID.randomUUID()
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson(GroupRole)
    }

    void 'SR05 - Set group to non-application role'() {
        given:
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('editor').id
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        Exception ex = thrown(ApiBadRequestException)
        ex.message == 'Cannot set UserGroup Application role to a non-application level role'
    }

    void 'SR06 - Set group to application role'() {
        given:
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('site_admin').id
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse OK, '''{
      "createdBy": "reader@test.com",
      "description": "readers only",
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "availableActions": ["delete","show","update"]
    }'''

        and:
        UserGroup.get(domain.id).applicationGroupRole.id == GroupRole.findByName('site_admin').id
    }

    void 'RR01 - Set null group to application role'() {
        given:
        domain.applicationGroupRole = GroupRole.findByName('site_admin')
        checkAndSave(domain)
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('site_admin').id
        params.userGroupId = null

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void 'RR02 - Set unknown group to application role'() {
        given:
        domain.applicationGroupRole = GroupRole.findByName('site_admin')
        checkAndSave(domain)
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('site_admin').id
        params.userGroupId = UUID.randomUUID()

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void 'RR03 - Set group to null application role'() {
        given:
        domain.applicationGroupRole = GroupRole.findByName('site_admin')
        checkAndSave(domain)
        request.method = 'PUT'
        params.applicationGroupRoleId = null
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson(GroupRole)
    }

    void 'RR04 - Set group to unknown application role'() {
        given:
        domain.applicationGroupRole = GroupRole.findByName('site_admin')
        checkAndSave(domain)
        request.method = 'PUT'
        params.applicationGroupRoleId = UUID.randomUUID()
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson(GroupRole)
    }

    void 'RR05 - Set group to non-application role'() {
        given:
        domain.applicationGroupRole = GroupRole.findByName('site_admin')
        checkAndSave(domain)
        request.method = 'PUT'
        params.applicationGroupRoleId = GroupRole.findByName('editor').id
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        Exception ex = thrown(ApiBadRequestException)
        ex.message == 'Cannot set UserGroup Application role to a non-application level role'
    }

    void 'RR06 - Set group to application role'() {
        given:
        domain.applicationGroupRole = GroupRole.findByName('site_admin')
        checkAndSave(domain)
        request.method = 'DELETE'
        params.applicationGroupRoleId = GroupRole.findByName('site_admin').id
        params.userGroupId = domain.id

        when:
        controller.updateApplicationGroupRole()

        then:
        verifyJsonResponse OK, '''{
      "createdBy": "reader@test.com",
      "description": "readers only",
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "availableActions": ["delete","show","update"]
    }'''

        and:
        !UserGroup.get(domain.id).applicationGroupRole
    }
}