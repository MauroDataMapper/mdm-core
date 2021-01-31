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

import uk.ac.ox.softeng.maurodatamapper.core.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.rest.transport.ChangePassword
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Unroll

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_CONFIRM_REGISTRATION_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_CONFIRM_REGISTRATION_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_REGISTER_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_REGISTER_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_FORGOTTEN_PASSWORD_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_FORGOTTEN_PASSWORD_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_PASSWORD_RESET_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_PASSWORD_RESET_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_SELF_REGISTER_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_SELF_REGISTER_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.SITE_URL

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class CatalogueUserControllerSpec extends ResourceControllerSpec<CatalogueUser> implements ControllerUnitTest<CatalogueUserController>,
    DomainUnitTest<CatalogueUser>, SecurityUsers {

    //static CatalogueFile defaultProfilePic = new CatalogueFile(CatalogueUser.NO_PROFILE_IMAGE_FILE_NAME, 'somecontent'.bytes, 'image/jpeg', admin)

    static UUID resetToken = UUID.randomUUID()

    UserGroup group

    def setup() {
        log.debug('Setting up CatalogueUserControllerSpec')
        implementSecurityUsers('unitTest')
        assert CatalogueUser.count() == 8

        domain.id = reader.id
        domain.emailAddress = reader.emailAddress
        domain.firstName = reader.firstName
        domain.lastName = reader.lastName
        domain.tempPassword = reader.tempPassword

        mockDomain(Edit)
        mockDomain(UserGroup)
        group = new UserGroup(name: 'testgroup', createdByUser: admin).addToGroupMembers(admin)
        checkAndSave(group)


        controller.catalogueUserService = Stub(CatalogueUserService) {
            get(_) >> { UUID id -> CatalogueUser.get(id) }
            findAllByUser(_, _) >> CatalogueUser.list()
            delete(_) >> { CatalogueUser catalogueUser -> catalogueUser.disabled = true }
            findAllByUserAndUserGroupId(_, _, _) >> { UserSecurityPolicyManager userSecurityPolicyManager, UUID userGroupId, Map pagination ->
                if (userSecurityPolicyManager.isApplicationAdministrator()) {
                    return CatalogueUser.byUserGroupId(userGroupId).list(pagination)
                }
                CatalogueUser.byUserGroupIdAndNotDisabled(userGroupId).list(pagination)
            }
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        controller.groupBasedSecurityPolicyManagerService = Stub(GroupBasedSecurityPolicyManagerService) {
            refreshAllUserSecurityPolicyManagersBySecurableResource(_, _) >> {sr ->}
        }

        boolean isR2_3 = specificationContext.currentIteration.name.contains('R2.3')

        controller.emailService = Mock(EmailService) {
            (isR2_3 ? 1 : 0) * sendEmailToUser(_, EMAIL_SELF_REGISTER_SUBJECT, EMAIL_SELF_REGISTER_BODY, _)
        }

        controller.apiPropertyService = Mock(ApiPropertyService) {
            (isR2_3 ? 1 : 0) * findByApiPropertyEnum(SITE_URL) >> null
        }

        givenParameters()
    }


    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 8,
  "items": [
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "Admin",
      "lastName": "User",
      "emailAddress": "admin@maurodatamapper.com",
      "createdBy": "unit-test@test.com",
      "pending": false,
      "jobTitle": "God",
      "organisation": "Oxford BRC Informatics",
      "groups": [
        {
          "name": "testgroup",
          "id": "${json-unit.matches:id}"
        }
      ],
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "editor",
      "lastName": "User",
      "emailAddress": "editor@test.com",
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "pending",
      "lastName": "User",
      "emailAddress": "pending@test.com",
      "createdBy": "unit-test@test.com",
      "pending": true,
      "jobTitle": "tester",
      "organisation": "Oxford",
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "reader",
      "lastName": "User",
      "emailAddress": "reader@test.com",
      "needsToResetPassword": true,
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "authenticated",
      "lastName": "User",
      "emailAddress": "authenticated@test.com",
      "needsToResetPassword": true,
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "author",
      "lastName": "User",
      "emailAddress": "author@test.com",
      "needsToResetPassword": true,
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "reviewer",
      "lastName": "User",
      "emailAddress": "reviewer@test.com",
      "needsToResetPassword": true,
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    },
    {
      "availableActions": [ "delete", "show", "update"],
      "firstName": "containerAdmin",
      "lastName": "User",
      "emailAddress": "container_admin@test.com",
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '{"total": 4,"errors": [' +
        '{"message": "Property [createdBy] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"},' +
        '{"message": "Property [firstName] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"},' +
        '{"message": "Property [emailAddress] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"},' +
        '{"message": "Property [lastName] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"}' +
        ']}'
    }

    @Override
    String getExpectedInvalidSavedJson() {
        // Self registration
        '{"total":2, "errors": [' +
        '{"message": "Property [emailAddress] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"},' +
        '{"message": "Property [createdBy] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"}' +
        ']}'
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
    "firstName": "valid",
    "lastName": "user",
    "emailAddress": "unit-test@test.com",
    "availableActions": ["delete","show","update"],
    "disabled": false,
    "id": "${json-unit.matches:id}",
    "pending": true,
    "createdBy":"unit-test@test.com"
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
    "firstName": "reader",
    "lastName": "User",
    "emailAddress": "reader@test.com",
    "availableActions": ["delete","show","update"],
    "disabled": false,
    "id": "${json-unit.matches:id}",
    "pending": false,
    "needsToResetPassword": true,
    "createdBy": "unit-test@test.com"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '{"total":1,"errors":[{"message":"Property [firstName] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] ' +
        'cannot be null"}]}'
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
    "firstName": "reader",
    "lastName": "User",
    "emailAddress": "reader@test.com",
    "availableActions": ["delete","show","update"],
    "disabled": false,
    "id": "${json-unit.matches:id}",
    "pending": false,
    "organisation": "unit test",
    "jobTitle": "tester",
    "needsToResetPassword": true,
    "createdBy": "unit-test@test.com"
}'''
    }

    @Override
    CatalogueUser invalidUpdate(CatalogueUser instance) {
        instance.with {
            firstName = ''
            it
        }
    }

    @Override
    CatalogueUser validUpdate(CatalogueUser instance) {
        instance.with {
            organisation = 'unit test'
            jobTitle = 'tester'
            it
        }
    }

    @Override
    CatalogueUser getInvalidUnsavedInstance() {
        new CatalogueUser(firstName: 'invalid',
                          lastName: 'user')
    }

    @Override
    CatalogueUser getValidUnsavedInstance() {
        new CatalogueUser(firstName: 'valid',
                          lastName: 'user',
                          emailAddress: userEmailAddresses.unitTest)
    }

    void verifyR53DeleteActionWithAnInstanceResponse() {
        verifyJsonResponse OK, '''{
    "firstName": "reader",
    "lastName": "User",
    "emailAddress": "reader@test.com",
    "availableActions": ["delete","show","update"],
    "disabled": true,
    "id": "${json-unit.matches:id}",
    "pending": false,
    "needsToResetPassword": true,
    "createdBy": "unit-test@test.com"
}'''
    }

    @Override
    String getTemplate() {
        """
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser

model {
    CatalogueUser catalogueUser
}

json {

    emailAddress catalogueUser.emailAddress
    firstName catalogueUser.firstName
    lastName catalogueUser.lastName

// Hack to pass the password during save/self registration
    if( catalogueUser.emailAddress == '${userEmailAddresses.unitTest}') password 'test password'

    if (catalogueUser.organisation) organisation catalogueUser.organisation
    if (catalogueUser.jobTitle) jobTitle catalogueUser.jobTitle
    if (catalogueUser.userPreferences) userPreferences catalogueUser.userPreferences
    if (catalogueUser.groups) groups catalogueUser.groups.collect{it.id}
    
}
"""
    }

    void 'Test list members of null group'() {
        when:
        params.userGroupId = null
        controller.index()

        then:
        verifyJsonResponse OK, '{"count": 0,"items": []}'
    }

    void 'Test list members of unknown group'() {
        when:
        params.userGroupId = UUID.randomUUID()
        controller.index()

        then:
        verifyJsonResponse OK, '{"count": 0,"items": []}'
    }

    void 'Test list members of known group'() {
        given:
        UserGroup userGroup = new UserGroup(name: 'readers',
                                            description: 'readers only',
                                            createdBy: reader.emailAddress)
            .addToGroupMembers(reader).addToGroupMembers(reviewer)
        checkAndSave(userGroup)
        when:
        params.userGroupId = userGroup.id
        controller.index()

        then:
        verifyJsonResponse OK, '''{"count":2, "items": [
    {
        "firstName": "reader",
        "lastName": "User",
        "emailAddress": "reader@test.com",
        "disabled": false,
        "id": "${json-unit.matches:id}",
        "pending": false,
        "needsToResetPassword": true,
        "createdBy": "unit-test@test.com"
    },
    {
        "firstName": "reviewer",
        "lastName": "User",
        "emailAddress": "reviewer@test.com",
        "disabled": false,
        "id": "${json-unit.matches:id}",
        "pending": false,
        "needsToResetPassword": true,
        "createdBy": "unit-test@test.com"
    }
]}'''
    }

    void 'Test list members of known group when user is disabled'() {
        given:
        UserGroup userGroup = new UserGroup(name: 'readers',
                                            description: 'readers only',
                                            createdBy: reader.emailAddress)
            .addToGroupMembers(reader).addToGroupMembers(reviewer)
        checkAndSave(userGroup)
        params.currentUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(admin, UUID.randomUUID(), UUID.randomUUID(),
                                                                                         UUID.randomUUID(), UUID.randomUUID())
        reader.disabled = true
        checkAndSave(reader)

        when:
        params.currentUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(admin, UUID.randomUUID(), UUID.randomUUID(),
                                                                                         UUID.randomUUID(), UUID.randomUUID())
        params.userGroupId = userGroup.id
        controller.index()

        then:
        verifyJsonResponse OK, '''{"count":2, "items": [
    {
        "firstName": "reader",
        "lastName": "User",
        "emailAddress": "reader@test.com",
        "disabled": true,
        "id": "${json-unit.matches:id}",
        "pending": false,
        "needsToResetPassword": true,
        "createdBy": "unit-test@test.com"
    },
    {
        "firstName": "reviewer",
        "lastName": "User",
        "emailAddress": "reviewer@test.com",
        "disabled": false,
        "id": "${json-unit.matches:id}",
        "pending": false,
        "needsToResetPassword": true,
        "createdBy": "unit-test@test.com"
    }
]}'''

        when:
        response.reset()
        controller.modelAndView = null // reset model and view from last request
        params.currentUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(editor, UUID.randomUUID(), UUID.randomUUID(),
                                                                                         UUID.randomUUID(), UUID.randomUUID())
        params.userGroupId = userGroup.id
        controller.index()

        then:
        verifyJsonResponse OK, '''{"count":1, "items": [
    {
        "firstName": "reviewer",
        "lastName": "User",
        "emailAddress": "reviewer@test.com",
        "disabled": false,
        "id": "${json-unit.matches:id}",
        "pending": false,
        "needsToResetPassword": true,
        "createdBy": "unit-test@test.com"
    }
]}'''
    }


    private CatalogueUser getExistingUnsavedInstance() {
        new CatalogueUser(firstName: 'existingEmail',
                          lastName: 'user',
                          emailAddress: userEmailAddresses.reader)
    }

    private ChangePassword getInvalidChangePassword() {
        new ChangePassword(oldPassword: 'incorrect', newPassword: 'another')
    }

    private ChangePassword getValidChangePassword() {
        new ChangePassword(oldPassword: 'readerpwd', newPassword: 'another')
    }

    void "test save/self registration with an existing email instance"() {
        given:
        controller.emailService = Mock(EmailService) {
            0 * sendEmailToUser(*_)
        }

        when:
        request.method = 'POST'
        setRequestJson reader, getTemplate()
        controller.save()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY,
                           '{"total":1, "errors": [{"message": "Property [emailAddress] of class ' +
                           '[class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] with value [reader@test.com] must be unique"}]}'
    }

    void 'test search with invalid instance'() {
        given:
        request.method = 'POST'
        SearchParams search = new SearchParams(limit: 10, offset: 10, searchTerm: '')
        controller.catalogueUserService = Mock(CatalogueUserService) {
            0 * findAllWhereAnyMatchToTerm(_, _)
        }

        when:
        controller.search(search)

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, '{"total": 1,' +
                                                 '"errors": [{"message": "Property [searchTerm] of class [class ' +
                                                 'uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams] cannot be blank"}]}'
    }

    void 'test search with valid instance'() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            1 * findAllWhereAnyMatchToTerm(_, _) >> [reader]
        }

        when:
        request.method = 'POST'
        controller.search(new SearchParams(searchTerm: 'reader'))

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "firstName": "reader",
      "lastName": "User",
      "emailAddress": "reader@test.com",
      "needsToResetPassword": true,
      "createdBy": "unit-test@test.com",
      "pending": false,
      "disabled": false,
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    void 'test search with valid instance but no results'() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            1 * findAllWhereAnyMatchToTerm(_, _) >> []
        }

        when:
        request.method = 'POST'
        controller.search(new SearchParams(searchTerm: 'editor'))

        then:
        verifyJsonResponse OK, '{"count":0, "items": []}'
    }


    void "test pending returns the correct response"() {

        controller.catalogueUserService = Mock(CatalogueUserService) {
            findAllPendingUsers(_) >> {
                CatalogueUser.byPending().list()
            }
        }

        when:
        controller.pending()

        then: "The response is correct"
        verifyJsonResponse OK, '''{
      "count": 1,
      "items": [
        {
      "firstName": "pending",
      "lastName": "User",
      "emailAddress": "pending@test.com",
      "createdBy": "unit-test@test.com",
      "availableActions": ["delete","show","update"],
      "pending": true,
      "jobTitle": "tester",
      "organisation": "Oxford",
      "disabled": false,
      "id": "${json-unit.matches:id}"
    }
      ]
    }'''
    }

    void "test pending count returns the correct response"() {

        controller.catalogueUserService = Mock(CatalogueUserService) {
            countPendingUsers(_) >> {
                CatalogueUser.byPending().count()
            }
        }

        when:
        controller.pendingCount()

        then: "The response is correct"
        verifyJsonResponse OK, '{"count": 1}'
    }

    void 'test user exists'() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            1 * emailAddressExists(userEmailAddresses.reader) >> true
            1 * emailAddressExists('3456789') >> false
        }

        when:
        params.emailAddress = reader.emailAddress
        controller.userExists()

        then:
        verifyJsonResponse OK, '{"userExists": true}'

        when:
        response.reset()
        params.emailAddress = '3456789'

        controller.userExists()

        then:
        verifyJsonResponse OK, '{"userExists": false}'
    }

    @Unroll
    void "test adminRegister for #userName"() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            administratorRegisterNewUser(_, _) >> {a, u ->
                u.createdBy = u.createdBy ?: a.emailAddress
                u.tempPassword = SecurityUtils.generateRandomPassword()
                u.password = null
                u.validate()
                u
            }
            findOrCreateUserFromInterface(_) >> {User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        controller.emailService = Mock(EmailService) {
            (expectedStatus == CREATED ? 1 : 0) * sendEmailToUser(_, EMAIL_ADMIN_REGISTER_SUBJECT, EMAIL_ADMIN_REGISTER_BODY, _)
        }
        controller.apiPropertyService = Mock(ApiPropertyService) {
            (expectedStatus == CREATED ? 1 : 0) * findByApiPropertyEnum(SITE_URL) >> null
        }
        request.method = 'POST'

        when:
        setRequestJson(user)
        controller.adminRegister()

        then:
        verifyJsonResponse expectedStatus, expectedJson

        where:
        user                         || expectedStatus       | expectedJson
        // Null
        null                         || UNPROCESSABLE_ENTITY | '{"total": 3,"errors": [' +
        '{"message": "Property [firstName] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"},' +
        '{"message": "Property [emailAddress] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"},' +
        '{"message": "Property [lastName] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"}' +
        ']}'
        // Valid
        validUnsavedInstance         || CREATED              | '''{
  "firstName": "valid",
  "lastName": "user",
  "emailAddress": "unit-test@test.com",
  "needsToResetPassword": true,
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "pending": true,
  "disabled": false,
  "id": "${json-unit.matches:id}"
}'''
        // Invalid
        invalidUnsavedInstance       || UNPROCESSABLE_ENTITY | '{"total":1, "errors": [' +
        '{"message": ' +
        '"Property [emailAddress] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] cannot be null"}]}'
        // Already exists
        getExistingUnsavedInstance() || UNPROCESSABLE_ENTITY | '{"total":1, "errors": [' +
        '{"message": "Property [emailAddress] of class [class uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser] ' +
        'with value [reader@test.com] must be unique"}]}'


        userName = user?.emailAddress ?: user?.firstName ?: 'No User'
    }

    void 'test get userPreferences'() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            1 * get(_) >> reader
        }
        reader.userPreferences = '{"countPerTable":"5","counts":[5,10,20,50],"expandMoreDescription":"true"}'
        reader.save(flush: true)
        params.catalogueUserId = reader.id

        when:
        controller.userPreferences()

        then:
        verifyJsonResponse OK, '{"countPerTable":"5","counts":[5,10,20,50],"expandMoreDescription":"true"}'
    }


    void 'test update userPreferences'() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            2 * get(_) >> reader
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        params.catalogueUserId = reader.id

        when:
        request.setJson('{"countPerTable":"5","counts":[5,10,20,50],"expandMoreDescription":"true"}')
        controller.updateUserPreferences()

        then:
        verifyJsonResponse OK, '''{
  "firstName": "reader",
  "lastName": "User",
  "emailAddress": "reader@test.com",
  "needsToResetPassword": true,
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "pending": false,
  "disabled": false,
  "id": "${json-unit.matches:id}"
}'''

        when:
        response.reset()
        controller.userPreferences()

        then:
        verifyJsonResponse OK, '{"countPerTable":"5","counts":[5,10,20,50],"expandMoreDescription":"true"}'
    }


    @Unroll
    void "test changePassword for #userName using #pwd password"() {
        given:
        def user = CatalogueUser.findByEmailAddress(userEmail)
        controller.catalogueUserService = Mock(CatalogueUserService) {
            changeUserPassword(_, _, _, _) >> {a, u, o, n ->

                if (u.getId() != editor.id || o != 'readerpwd') {
                    u.errors.reject('invalid.change.password.message', [u.emailAddress].toArray(),
                                    'Cannot change password for user [{0}] as old password is not valid')
                } else u.encryptAndSetPassword(n)
                u
            }

            get(_) >> {id -> CatalogueUser.get(id)}
            findOrCreateUserFromInterface(_) >> {User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        request.method = 'POST'

        when:
        params.catalogueUserId = user?.id
        request.json = change ? "{\"newPassword\": \"${change.newPassword}\",\"oldPassword\":\"${change.oldPassword}\"}".toString() : ''
        controller.changePassword()

        then:
        verifyJsonResponse expectedStatus, expectedJson

        where:
        userEmail                 | change                || expectedStatus       | expectedJson
        // No user but valid password
        null                      | validChangePassword   || NOT_FOUND            | getNotFoundNullJson()
        // Null password change
        userEmailAddresses.editor | null                  || UNPROCESSABLE_ENTITY | '{"total": 2,' +
        '"errors": [{"message": "Property [newPassword] of class [class uk.ac.ox.softeng.maurodatamapper.security.rest.transport.' +
        'ChangePassword] cannot be null"},{"message": "Cannot change password for user as one of reset token or old password must be provided"}]}'
        // Incorrect old
        userEmailAddresses.editor | invalidChangePassword || UNPROCESSABLE_ENTITY | '{"total":1, "errors": [{"message":' +
        '"Cannot change password for user [editor@test.com] as old password is not valid"}]}'
        // Correct password
        userEmailAddresses.editor | validChangePassword   || OK                   | ''' {
        "firstName": "editor",
        "lastName": "User",
        "emailAddress": "editor@test.com",
        "availableActions": ["delete","show","update"],
        "disabled": false,
        "id": "${json-unit.matches:id}",
        "pending": false,
        "createdBy": "unit-test@test.com"
    }'''

        userName = userEmail ?: 'No user'
        pwd = change?.oldPassword ?: 'null'
    }

    @Unroll
    void "test approveRegistration for #userName"() {
        given:
        def user = CatalogueUser.findByEmailAddress(userEmail)
        controller.catalogueUserService = Mock(CatalogueUserService) {
            approveUserRegistration(_, _) >> {a, u ->
                if (!u.pending) {
                    u.errors.reject('invalid.already.registered.message', [u.emailAddress].toArray(),
                                    'Cannot approve or reject user registration for [{0}] as user is already registered')
                    return u
                }

                u.pending = false
                u
            }
            get(_) >> {id -> CatalogueUser.get(id)}
            findOrCreateUserFromInterface(_) >> {User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        controller.emailService = Mock(EmailService) {
            (expectedStatus == OK ? 1 : 0) * sendEmailToUser(_, EMAIL_ADMIN_CONFIRM_REGISTRATION_SUBJECT,
                                                             EMAIL_ADMIN_CONFIRM_REGISTRATION_BODY, _)
        }
        controller.apiPropertyService = Mock(ApiPropertyService) {
            (expectedStatus == OK ? 1 : 0) * findByApiPropertyEnum(SITE_URL) >> null
        }
        request.method = 'PUT'

        when:
        params.catalogueUserId = user ? user.id : userEmail ? UUID.randomUUID() : null
        controller.approveRegistration()

        then:
        verifyJsonResponse expectedStatus, expectedJson

        where:
        userEmail                   || expectedStatus       | expectedJson
        // Null user
        null                        || NOT_FOUND            | getNotFoundNullJson()
        // Valid
        userEmailAddresses.pending  || OK                   | '''{
  "firstName": "pending",
  "lastName": "User",
  "emailAddress": "pending@test.com",
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "pending": false,
  "jobTitle": "tester",
  "organisation": "Oxford",
  "disabled": false,
  "id": "${json-unit.matches:id}"
}'''
        // Invalid
        userEmailAddresses.reader   || UNPROCESSABLE_ENTITY | '{"total":1, "errors": [{"message"' +
        ': "Cannot approve or reject user registration for [reader@test.com] as user ' +
        'is already registered"}]}'
        // Unregistered
        userEmailAddresses.unitTest || NOT_FOUND            | getNotFoundIdJson()

        userName = userEmail ?: 'No User'
    }

    @Unroll
    void "test rejectRegistration for #userName when logged in as #actorName"() {
        given:
        def user = CatalogueUser.findByEmailAddress(userEmail)
        controller.catalogueUserService = Mock(CatalogueUserService) {
            rejectUserRegistration(_, _) >> {a, u ->
                if (!u.pending) {
                    u.errors.reject('invalid.already.registered.message', [u.emailAddress].toArray(),
                                    'Cannot approve or reject user registration for [{0}] as user is already registered')
                    return u
                }
                u.disabled = true
                u
            }
            get(_) >> {id -> CatalogueUser.get(id)}
            findOrCreateUserFromInterface(_) >> {User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        controller.emailService = Mock(EmailService) {
            0 * sendEmailToUser(*_)
        }
        request.method = 'PUT'

        when:
        params.catalogueUserId = user ? user.id : userEmail ? UUID.randomUUID() : null
        controller.rejectRegistration()

        then:
        verifyJsonResponse expectedStatus, expectedJson

        where:
        userEmail                   || expectedStatus       | expectedJson
        // Null
        null                        || NOT_FOUND            | getNotFoundNullJson()
        // Valid
        userEmailAddresses.pending  || OK                   | '''{
  "firstName": "pending",
  "lastName": "User",
  "emailAddress": "pending@test.com",
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "pending": true,
  "jobTitle": "tester",
  "organisation": "Oxford",
  "disabled": true,
  "id": "${json-unit.matches:id}"
}'''
        // Invalid
        userEmailAddresses.reader   || UNPROCESSABLE_ENTITY | '{"total":1, "errors": [{"message"' +
        ': "Cannot approve or reject user registration for [reader@test.com] as user is already registered"}]}'
        // Unregistered
        userEmailAddresses.unitTest || NOT_FOUND            | getNotFoundIdJson()

        userName = userEmail ?: 'No User'
    }

    void 'test reset password with a null token after token requested'() {
        given:
        controller.apiPropertyService = Mock(ApiPropertyService) {
            1 * findByApiPropertyEnum(SITE_URL) >> null
        }
        controller.emailService = Mock(EmailService) {
            1 * sendEmailToUser(_, EMAIL_FORGOTTEN_PASSWORD_SUBJECT,
                                EMAIL_FORGOTTEN_PASSWORD_BODY, _, _)
        }
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> { id -> CatalogueUser.get(id) }
            findByEmailAddress(_) >> { String em -> CatalogueUser.findByEmailAddress(em) }
            1 * generatePasswordResetLink(_, _) >> { u, b ->
                u.resetToken = UUID.randomUUID()
                u.save(validate: false)
                "${b}/resetPasswordWebPage?token=${u.resetToken.toString()}"
            }
            0 * changeUserPassword(_, _, _)
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }

        when: 'requesting a reset link'
        params.emailAddress = reader.emailAddress
        controller.sendPasswordResetLink()

        then:
        verifyResponse OK

        and: 'reset token is set'
        reader.resetToken

        when: 'user then tries to use reset token'
        response.reset()
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.catalogueUserId = reader.id.toString()
        request.method = 'POST'

        request.json = '{"newPassword": "another", "resetToken":""}'
        controller.resetPassword()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, '''{
  "total": 1,
  "errors": [
    {
      "message": "Cannot change password for user as one of reset token or old password must be provided"
    }
  ]
}'''
    }

    void 'test reset password with a null token without token being requested'() {
        given:
        controller.emailService = Mock(EmailService) {
            0 * sendEmailToUser(_, EMAIL_FORGOTTEN_PASSWORD_SUBJECT,
                                EMAIL_FORGOTTEN_PASSWORD_BODY, _, _)
        }
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> { id -> CatalogueUser.get(id) }
            findByEmailAddress(_) >> { String em -> CatalogueUser.findByEmailAddress(em) }
            0 * generatePasswordResetLink(_, _)
            0 * changeUserPassword(_, _, _)
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }

        when: 'user then tries to use reset token'
        request.method = 'POST'
        params.catalogueUserId = reader.id.toString()
        params.emailAddress = reader.emailAddress
        request.json = '{"newPassword": "another", "resetToken":""}'
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        controller.resetPassword()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, '{' +
                                                 '"total": 1,"errors": [{' +
                                                 '"message": "Cannot change password for user as one of reset token or old password must be ' +
                                                 'provided"}]}'

    }

    void 'test reset password with the user after reset link sent'() {
        given:
        controller.apiPropertyService = Mock(ApiPropertyService) {
            1 * findByApiPropertyEnum(SITE_URL) >> null
        }
        controller.emailService = Mock(EmailService) {
            1 * sendEmailToUser(_, EMAIL_FORGOTTEN_PASSWORD_SUBJECT,
                                EMAIL_FORGOTTEN_PASSWORD_BODY, _, _)
        }
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> {id -> CatalogueUser.get(id)}
            findByEmailAddress(_) >> {String em -> CatalogueUser.findByEmailAddress(em)}
            1 * generatePasswordResetLink(_, _) >> {u, b ->
                u.resetToken = UUID.randomUUID()
                u.skipValidation(true)
                "${b}/resetPasswordWebPage?token=${u.resetToken.toString()}"
            }

            1 * changeUserPassword(_, _, _) >> { u, t, n ->
                if (u.resetToken && u.resetToken == t) {
                    u.encryptAndSetPassword(n)
                    u.validate()
                } else {
                    u.errors.reject('invalid.change.password.message', [u.emailAddress].toArray(),
                                    'Cannot change password for user [{0}] as old password is not valid')
                }
                u
            }
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }

        when: 'requesting a reset link'
        params.emailAddress = reader.emailAddress
        controller.sendPasswordResetLink()

        then:
        verifyResponse OK

        and: 'reset token is set'
        reader.resetToken

        when: 'user then tries to use reset token'
        UUID token = reader.resetToken
        response.reset()
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.catalogueUserId = reader.id.toString()
        request.method = 'POST'
        request.json = "{\"newPassword\": \"another\", \"resetToken\":\"${token}\"}".toString()
        controller.resetPassword()

        then:
        verifyResponse OK

        and:
        !reader.resetToken

    }

    void 'test reset password with the user logging in after reset link sent'() {
        given:
        controller.apiPropertyService = Mock(ApiPropertyService) {
            1 * findByApiPropertyEnum(SITE_URL) >> null
        }
        controller.emailService = Mock(EmailService) {
            1 * sendEmailToUser(_, EMAIL_FORGOTTEN_PASSWORD_SUBJECT,
                                EMAIL_FORGOTTEN_PASSWORD_BODY, _, _)
        }
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> {id -> CatalogueUser.get(id)}
            findByEmailAddress(_) >> {String em -> CatalogueUser.findByEmailAddress(em)}
            1 * generatePasswordResetLink(_, _) >> {u, b ->
                u.resetToken = UUID.randomUUID()
                u.skipValidation(true)
                "${b}/resetPasswordWebPage?token=${u.resetToken.toString()}"
            }
            1 * setUserLastLoggedIn(_) >> {CatalogueUser user ->
                user.lastLogin = OffsetDateTime.now()
                user.resetToken = null
                user.save(validate: false)
            }
            1 * changeUserPassword(_, _, _) >> { u, t, n ->
                if (u.resetToken && u.resetToken == t) {
                    u.encryptAndSetPassword(n)
                    u.validate()
                } else {
                    u.errors.reject('invalid.change.password.message', [u.emailAddress].toArray(),
                                    'Cannot change password for user [{0}] as old password is not valid')
                }
                u
            }
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }

        when: 'requesting a reset link'
        params.emailAddress = reader.emailAddress
        controller.sendPasswordResetLink()

        then: 'reset token is set'
        reader.resetToken

        when: 'user then logs in using known password'
        UUID token = reader.resetToken
        response.reset()
        params.catalogueUserId = reader.id.toString()
        controller.catalogueUserService.setUserLastLoggedIn(reader)

        then: 'token is removed'
        !reader.resetToken

        when: 'user then tries to use reset token'
        response.reset()
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.catalogueUserId = reader.id.toString()
        request.method = 'POST'
        request.json = "{\"newPassword\": \"another\", \"resetToken\":\"${token}\"}".toString()
        controller.resetPassword()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, '{"total":1, "errors": [{"message":' +
                                                 '"Cannot change password for user [reader@test.com] as old password is not valid"}]}'

    }

    void 'test admin password reset for non-existent user'() {

        given:
        controller.emailService = Mock(EmailService) {
            0 * sendEmailToUser(_, EMAIL_PASSWORD_RESET_SUBJECT, EMAIL_PASSWORD_RESET_BODY, _)
        }
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> { id -> CatalogueUser.get(id) }
            0 * administratorPasswordReset(_, _)
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }

        when:
        params.catalogueUserId = UUID.randomUUID()
        controller.adminPasswordReset()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void 'test admin password reset for user'() {

        given:
        controller.apiPropertyService = Mock(ApiPropertyService) {
            1 * findByApiPropertyEnum(SITE_URL) >> null
        }
        controller.emailService = Mock(EmailService) {
            1 * sendEmailToUser(_, EMAIL_PASSWORD_RESET_SUBJECT, EMAIL_PASSWORD_RESET_BODY, _)
        }
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> { id -> CatalogueUser.get(id) }
            1 * administratorPasswordReset(_, _) >> { actor, user ->
                user.password = null
                user.tempPassword = SecurityUtils.generateRandomPassword()
                user
            }
            findOrCreateUserFromInterface(_) >> { User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }

        when:
        params.catalogueUserId = editor.id
        controller.adminPasswordReset()

        then:
        verifyJsonResponse OK, '''{
  "firstName": "editor",
  "lastName": "User",
  "emailAddress": "editor@test.com",
  "needsToResetPassword": true,
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "pending": false,
  "disabled": false,
  "id": "${json-unit.matches:id}"
}'''
        and:
        editor.tempPassword
        !editor.password
    }


    void 'test passing groups to adminRegister'() {
        given:
        controller.catalogueUserService = Mock(CatalogueUserService) {
            get(_) >> {id -> CatalogueUser.get(id)}
            administratorRegisterNewUser(_, _) >> {a, u ->
                u.createdBy = u.createdBy ?: a.emailAddress
                u.tempPassword = SecurityUtils.generateRandomPassword()
                u.password = null
                u.validate()
                u
            }
            findOrCreateUserFromInterface(_) >> {User u ->
                if (u instanceof CatalogueUser) return u
                CatalogueUser catalogueUser = CatalogueUser.get(u.id) ?: CatalogueUser.findByEmailAddress(u.emailAddress)
                catalogueUser ?: CatalogueUser.fromInterface(u)
            }
        }
        controller.emailService = Mock(EmailService) {
            1 * sendEmailToUser(_, EMAIL_ADMIN_REGISTER_SUBJECT, EMAIL_ADMIN_REGISTER_BODY, _)
        }
        controller.apiPropertyService = Mock(ApiPropertyService) {
            1 * findByApiPropertyEnum(SITE_URL) >> null
        }
        request.method = 'POST'

        when:
        CatalogueUser user = getValidUnsavedInstance()
        user.addToGroups(group)
        requestJson user
        controller.adminRegister()

        then:
        verifyJsonResponse CREATED, '''{
  "firstName": "valid",
  "lastName": "user",
  "emailAddress": "unit-test@test.com",
  "needsToResetPassword": true,
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "pending": true,
  "disabled": false,
  "id": "${json-unit.matches:id}",
  "groups": [{
               "id": "${json-unit.matches:id}",
               "name": "testgroup"
  }]
}'''
        when:
        CatalogueUser testUser = CatalogueUser.findByEmailAddress(user.emailAddress)
        UserGroup testGroup = UserGroup.findByName(group.name)

        then:
        testUser
        testUser.groups.size() == 1
        testUser.groups[0].name == group.name

        and:
        testGroup
        testGroup.groupMembers.size() == 2
        testGroup.groupMembers.any {it.emailAddress == user.emailAddress}
    }

    /*
        void verifyFileResponse(HttpStatus expectedStatus, CatalogueFile file) {
            verifyResponse(expectedStatus)
            if (expectedStatus == OK) {
                assert response.header(HttpHeaders.CONTENT_DISPOSITION) == "$DISPOSITION_HEADER_PREFIX\"${file.fileName}\""
                assert response.contentAsByteArray == file.fileContents
                assert response.contentType == file.fileType + ';charset=utf-8'
            }
        }
    */
}