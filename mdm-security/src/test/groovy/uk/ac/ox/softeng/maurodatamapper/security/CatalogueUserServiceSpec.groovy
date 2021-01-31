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


import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.gorm.PagedResultList
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class CatalogueUserServiceSpec extends BaseUnitSpec implements ServiceUnitTest<CatalogueUserService>, DomainUnitTest<CatalogueUser>,
    SecurityUsers {

    UUID id

    def setup() {
        log.debug('Setting up CatalogueUserServiceSpec')
        implementSecurityUsers('unitTest')
        id = editor.id
    }

    void "test get"() {
        expect:
        service.get(id) != null

        and:
        service.get('admin@maurodatamapper.com')
    }

    void "test list"() {
        when:
        List<CatalogueUser> catalogueUserList = service.list(max: 2, offset: 1, sort: 'emailAddress', order: 'desc')

        then:
        catalogueUserList.size() == 2

        and:
        catalogueUserList[0].emailAddress == 'reader@test.com'
        !catalogueUserList[0].pending

        and:
        catalogueUserList[1].emailAddress == 'pending@test.com'
        catalogueUserList[1].pending
        catalogueUserList[1].createdBy == 'unit-test@test.com'
        catalogueUserList[1].firstName == 'pending'
        catalogueUserList[1].lastName == 'User'
        catalogueUserList[1].organisation == 'Oxford'
        catalogueUserList[1].jobTitle == 'tester'

        and:
        catalogueUserList[1].password != 'test password'.bytes
        catalogueUserList[1].password == SecurityUtils.getHash('test password', catalogueUserList[1].salt)
    }

    void "test count"() {

        expect:
        service.count() == 8
    }

    void "test save"() {
        when:
        CatalogueUser catalogueUser = service.createNewUser(
            emailAddress: 'newtester@email.com', firstName: 'wibble', password: 'wobble',
            lastName: 'user')
        checkAndSave(catalogueUser)

        then:
        catalogueUser.id != null

        and:
        catalogueUser.password != 'wobble'.bytes
        catalogueUser.password == SecurityUtils.getHash('wobble', catalogueUser.salt)

    }

    void 'test change user password'() {

        when: 'actor is user but password wrong'
        def user = service.changeUserPassword(reader, reader, 'wibble', 'wobble')

        then:
        user.hasErrors()
        user.errors.hasGlobalErrors()
        user.errors.getGlobalErrors().find {it.code == 'invalid.change.password.message'}

        when: 'actor is user and changing temp password'
        user.clearErrors()
        def temp = reader.tempPassword
        user = service.changeUserPassword(reader, reader, reader.tempPassword, 'wobble')

        then:
        !user.hasErrors()
        !user.tempPassword
        user.password != 'wobble'.bytes
        user.password != temp.bytes

        !basicAuthentication(reader.emailAddress, temp)
        basicAuthentication(reader.emailAddress, 'wobble')

        when: 'actor is user and changing password'
        user = service.changeUserPassword(pending, pending, 'test password', 'wobble')

        then:
        !user.hasErrors()
        !user.tempPassword
        user.password != 'test password'.bytes
        user.password != 'wobble'.bytes
        basicAuthentication(pending.emailAddress, 'wobble')
    }

    void 'test change user password using reset token'() {
        given:
        UUID token = UUID.randomUUID()
        reader.encryptAndSetPassword 'password'
        reader.resetToken = token
        reader.save(validate: false)

        when: 'using invalid token'
        def user = service.changeUserPassword(reader, UUID.randomUUID(), 'wobble')

        then:
        user.hasErrors()
        user.errors.hasGlobalErrors()
        user.errors.getGlobalErrors().find {it.code == 'invalid.change.password.message'}

        and:
        !user.tempPassword
        user.resetToken
        user.password != 'wobble'.bytes
        !basicAuthentication(reader.emailAddress, 'wobble')

        when: 'using valid reset token'
        user.clearErrors()
        user = service.changeUserPassword(reader, token, 'wobble')

        then:
        !user.hasErrors()
        !user.tempPassword
        !user.resetToken
        user.password != 'wobble'.bytes
        basicAuthentication(reader.emailAddress, 'wobble')

    }

    void 'test change user password using reset token after user has logged in'() {
        given:
        UUID token = UUID.randomUUID()
        reader.encryptAndSetPassword 'password'
        reader.resetToken = token
        reader.save(validate: false)

        when: 'logging in'
        def user = basicAuthentication(reader.emailAddress, 'password')

        then:
        !user.tempPassword
        !user.resetToken

        when: 'using valid reset token'
        user = service.changeUserPassword(reader, token, 'wobble')

        then:
        user.hasErrors()
        user.errors.hasGlobalErrors()
        user.errors.getGlobalErrors().find {it.code == 'invalid.change.password.message'}

        and:
        !user.tempPassword
        !user.resetToken
        user.password != 'wobble'.bytes
        !basicAuthentication(reader.emailAddress, 'wobble')

    }

    void 'test admin reset user password'() {
        given:
        UUID token = UUID.randomUUID()
        reader.encryptAndSetPassword 'password'

        when:
        def user = service.administratorPasswordReset(admin, reader)
        user.save(validate: false)

        then:
        user.tempPassword

        when: 'using invalid temp password'
        user = service.changeUserPassword(reader, reader, 'wibble', 'wobble')

        then:
        user.hasErrors()
        user.errors.hasGlobalErrors()
        user.errors.getGlobalErrors().find {it.code == 'invalid.change.password.message'}

        and:
        user.tempPassword
        !user.resetToken
        user.password != 'wobble'.bytes
        !basicAuthentication(reader.emailAddress, 'wobble')

        when: 'using valid temp password'
        user.clearErrors()
        user = service.changeUserPassword(reader, reader, reader.tempPassword, 'wobble')

        then:
        !user.hasErrors()
        !user.tempPassword
        !user.resetToken
        user.password != 'wobble'.bytes
        basicAuthentication(reader.emailAddress, 'wobble')
    }

    void 'test admin reset user password after user has logged in'() {
        given:
        UUID token = UUID.randomUUID()
        reader.encryptAndSetPassword 'password'

        when:
        def user = service.administratorPasswordReset(admin, reader)
        user.save(validate: false)

        then:
        user.tempPassword

        when: 'logging in'
        user = basicAuthentication(reader.emailAddress, 'password')

        then:
        !user.tempPassword
        !user.resetToken

        when: 'using valid reset token'
        user = service.changeUserPassword(reader, reader, reader.tempPassword, 'wobble')

        then:
        user.hasErrors()
        user.errors.hasGlobalErrors()
        user.errors.getGlobalErrors().find {it.code == 'invalid.change.password.message'}

        and:
        !user.tempPassword
        !user.resetToken
        user.password != 'wobble'.bytes
        !basicAuthentication(reader.emailAddress, 'wobble')

    }

    void 'test administrator registering user'() {
        given:
        CatalogueUser catalogueUser = service.createNewUser(
            emailAddress: 'newtester@email.com', firstName: 'wibble', password: 'wobble',
            lastName: 'user')

        when:
        def user = service.administratorRegisterNewUser(admin, catalogueUser)
        checkAndSave(user)

        then:
        !user.hasErrors()
        user.tempPassword
        !user.pending
        user.id

        when:
        def found = service.get(catalogueUser.emailAddress)

        then:
        found
        found.tempPassword
        found.id == user.id
    }

    void 'test administrator confirming registration'() {

        when: 'admin confirming a pending user'
        def user = service.approveUserRegistration(admin, pending)
        checkAndSave(user)

        then:
        !user.hasErrors()
        user.id
        !service.get(pending.emailAddress).pending
    }

    void 'second simple search'() {
        given:
        CatalogueUser a = service.administratorRegisterNewUser(admin, "john.smith@test2.com", "John", "Smith", "Manager",
                                                               "My Organisation")
        CatalogueUser b = service.administratorRegisterNewUser(admin, "james.smith@test2.com", "James", "Smith", "Manager",
                                                               "Acme Inc")
        CatalogueUser c = service.administratorRegisterNewUser(admin, "john.jones@test2.com", "John", "Jones", "Manager",
                                                               "Acme Inc")
        CatalogueUser d = service.administratorRegisterNewUser(admin, "james.jones@test2.com", "James", "Jones", "Assistant",
                                                               "My Organisation")
        checkAndSave(a, b, c, d)

        when:
        def foundUsers = service.findAllWhereAnyMatchToTerm("Smith", [offset: 0, max: 10])

        then:
        foundUsers instanceof List
        foundUsers.size() == 2

        when:
        foundUsers = service.findAllWhereAnyMatchToTerm("@test2.com", [offset: 0, max: 10])

        then:
        foundUsers instanceof List
        foundUsers.size() == 4

        when:
        foundUsers = service.findAllWhereAnyMatchToTerm("Acme", [offset: 0, max: 10])

        then:
        foundUsers instanceof List
        foundUsers.size() == 2
    }

    void 'test simple search'() {
        given:

        when: 'and search'
        SearchParams searchParams = new SearchParams(search: "User")

        then:
        check searchParams

        when: 'performing or search'
        PagedResultList<CatalogueUser> results = service.findAllWhereAnyMatchToTerm(searchParams.searchTerm, [max: 10])

        then:
        results.size() == 8
        results.find {it.emailAddress == 'pending@test.com'}
        results.every {it.lastName == 'User'}

        when: 'performing or search with limit'
        results = service.findAllWhereAnyMatchToTerm(searchParams.searchTerm, [max: 2])

        then:
        results.getTotalCount() == 8
        results.size() == 2
        results.every {it.lastName == 'User'}

    }

    void 'test getting users by group id'() {
        given:
        UserGroup group = new UserGroup(name: 'readersOnly', createdBy: reader.emailAddress)
            .addToGroupMembers(reader)
            .addToGroupMembers(reviewer)
        checkAndSave(group)
        when:
        List<CatalogueUser> users = service.findAllByUserGroupId(group.id)

        then:
        users.size() == 2

        and:
        users.any {it.id == reader.id}
        users.any {it.id == reviewer.id}
    }


    void 'test getting users by group id and not disabled'() {
        given:
        UserGroup group = new UserGroup(name: 'readersOnly', createdBy: reader.emailAddress)
            .addToGroupMembers(reader)
            .addToGroupMembers(reviewer)
        checkAndSave(group)
        reader.disabled = true
        checkAndSave(reader)

        when:
        List<CatalogueUser> users = service.findAllByUserGroupIdAndNotDisabled(group.id)

        then:
        users.size() == 1

        and:
        users.any {it.id == reviewer.id}
    }

    private CatalogueUser basicAuthentication(String emailAddress, String password) {

        log.debug("Authenticating user ${emailAddress} using basic database authentication")
        if (!emailAddress) return null
        CatalogueUser user = service.findByEmailAddress(emailAddress)
        if (!user || user.isDisabled()) return null

        if (service.validateTempPassword(user, password?.trim()) ||
            service.validateUserPassword(user, password?.trim())) {
            service.setUserLastLoggedIn(user)
            return user
        }
        null
    }
}
