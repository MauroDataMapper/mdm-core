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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import org.spockframework.util.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * @see UserGroupController* Controller: userGroup
 *  | POST   | /api/userGroups       | Action: save   |
 *  | GET    | /api/userGroups       | Action: index  |
 *  | DELETE | /api/userGroups/${id} | Action: delete |
 *  | PUT    | /api/userGroups/${id} | Action: update |
 *  | GET    | /api/userGroups/${id} | Action: show   |
 *
 *  | GET    | /api/userGroups/${userGroupId}/catalogueUsers                    | Action: listMembers  |
 *  | DELETE | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId} | Action: alterMembers |
 *  | PUT    | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId} | Action: alterMembers |
 */
@Integration
@Slf4j
class UserGroupFunctionalSpec extends ResourceFunctionalSpec<UserGroup> implements SecurityUsers {

    @Autowired
    ApplicationContext applicationContext
    @Autowired
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService
    @Autowired
    GroupRoleService groupRoleService

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        assert CatalogueUser.count() == 2
        assert UserGroup.count() == 1
        implementSecurityUsers('functionalTest')
        assert CatalogueUser.count() == 9
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        CatalogueUser.list().findAll {it.emailAddress != UnloggedUser.instance.emailAddress}.each {it.delete(flush: true)}
        if (CatalogueUser.count() != 1) {
            Assert.fail("Resource Class ${CatalogueUser.simpleName} has not been emptied")
        }
        cleanUpResources(UserGroup)
    }

    @Transactional
    String getGroupId(String name) {
        UserGroup.findByName(name).id.toString()
    }

    @Transactional
    String getUserId(String emailAddress) {
        CatalogueUser.findByEmailAddress(emailAddress).id.toString()
    }

    @Override
    String getResourcePath() {
        'userGroups'
    }

    @Override
    Map getValidJson() {
        [
            name: 'testers'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            name: ''
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Testing peoples'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "createdBy": "unlogged_user@mdm-core.com",
  "id": "${json-unit.matches:id}",
  "name": "testers",
  "availableActions": ["delete","show", "update"]
}'''
    }

    @Override
    def setup() {
        reconfigureDefaultUserPrivileges(true)
    }

    @Override
    void cleanUpData(String id = null) {
        super.cleanUpData(id)
        reconfigureDefaultUserPrivileges(false)
    }

    @Override
    int getExpectedInitialResourceCount() {
        1
    }

    void verifyR1EmptyIndexResponse() {
        verifyResponse(OK, response)
        assert response.body().count == 1
        assert response.body().items[0].name == 'administrators'
    }

    @Transactional
    void reconfigureDefaultUserPrivileges(boolean accessGranted) {

        GroupBasedUserSecurityPolicyManager defaultUserSecurityPolicyManager = applicationContext.getBean(
            MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME)

        if (accessGranted) {
            VirtualGroupRole applicationLevelRole = groupRoleService.getFromCache(GroupRole.GROUP_ADMIN_ROLE_NAME)
            defaultUserSecurityPolicyManager.withApplicationRoles(applicationLevelRole.allowedRoles).withVirtualRoles(
                groupBasedSecurityPolicyManagerService.buildUserGroupVirtualRoles([applicationLevelRole.groupRole] as HashSet)
            )
        } else {
            defaultUserSecurityPolicyManager.withApplicationRoles([] as HashSet)
            groupBasedSecurityPolicyManagerService.buildUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
        }
        groupBasedSecurityPolicyManagerService.storeUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
    }

    void 'test getting members of a group'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when:
        String id = response.body().id
        GET("$id/catalogueUsers", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "unlogged_user@mdm-core.com",
      "firstName": "Unlogged",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "unlogged_user@mdm-core.com"
    }
  ]
}'''
    }

    void 'test adding a user to a group'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when:
        String id = response.body().id
        PUT("${id}/catalogueUsers/${getUserId(userEmailAddresses.authenticated)}", [:])

        then:
        verifyResponse OK, response

        when:
        GET("$id/catalogueUsers", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "unlogged_user@mdm-core.com",
      "firstName": "Unlogged",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "unlogged_user@mdm-core.com"
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "authenticated@test.com",
      "firstName": "authenticated",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com"
    }
  ]
}'''
    }

    void 'test removing a user from a group'() {
        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        response.status == CREATED
        response.body().id

        when:
        String id = response.body().id
        PUT("${id}/catalogueUsers/${getUserId(userEmailAddresses.authenticated)}", [:])

        then:
        verifyResponse OK, response

        when:
        DELETE("${id}/catalogueUsers/${getUserId(userEmailAddresses.authenticated)}")

        then:
        verifyResponse OK, response

        when:
        GET("$id/catalogueUsers", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "unlogged_user@mdm-core.com",
      "firstName": "Unlogged",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "unlogged_user@mdm-core.com"
    }
  ]
}'''
    }
}