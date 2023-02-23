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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.email.Email
import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKeyService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.gorm.PagedResultList
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

import java.time.LocalDate

@Slf4j
class CatalogueUserServiceSpec extends BaseUnitSpec implements ServiceUnitTest<CatalogueUserService>, DomainUnitTest<CatalogueUser>,
    SecurityUsers {

    UUID id

    def setup() {
        log.debug('Setting up CatalogueUserServiceSpec')
        mockDomains(ApiKey, Edit, Email, Folder, SecurableResourceGroupRole)
        mockArtefact(ApiKeyService)
        mockArtefact(EditService)
        mockArtefact(EmailService)
        mockArtefact(FolderService)
        mockArtefact(UserGroupService)
        mockArtefact(SecurableResourceGroupRoleService)
        implementSecurityUsers('unitTest')
        id = editor.id
    }

    void 'test get'() {
        expect:
        service.get(id) != null

        and:
        service.get('admin@maurodatamapper.com')
    }

    void 'test list'() {
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

    void 'test count'() {

        expect:
        service.count() == 8
    }

    void 'test save'() {
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
        CatalogueUser a = service.administratorRegisterNewUser(admin, 'john.smith@test2.com', 'John', 'Smith', 'Manager',
                                                               'My Organisation')
        CatalogueUser b = service.administratorRegisterNewUser(admin, 'james.smith@test2.com', 'James', 'Smith', 'Manager',
                                                               'Acme Inc')
        CatalogueUser c = service.administratorRegisterNewUser(admin, 'john.jones@test2.com', 'John', 'Jones', 'Manager',
                                                               'Acme Inc')
        CatalogueUser d = service.administratorRegisterNewUser(admin, 'james.jones@test2.com', 'James', 'Jones', 'Assistant',
                                                               'My Organisation')
        checkAndSave(a, b, c, d)

        when:
        def foundUsers = service.findAllWhereAnyMatchToTerm('Smith', [offset: 0, max: 10])

        then:
        foundUsers instanceof List
        foundUsers.size() == 2

        when:
        foundUsers = service.findAllWhereAnyMatchToTerm('@test2.com', [offset: 0, max: 10])

        then:
        foundUsers instanceof List
        foundUsers.size() == 4

        when:
        foundUsers = service.findAllWhereAnyMatchToTerm('Acme', [offset: 0, max: 10])

        then:
        foundUsers instanceof List
        foundUsers.size() == 2
    }

    void 'test simple search'() {
        given:

        when: 'and search'
        SearchParams searchParams = new SearchParams(search: 'User')

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

    void 'test exporting users to csv file'() {
        given:
        CatalogueUser catalogueUser1 = service.createNewUser(
                emailAddress: 'newtester1@email.com', firstName: 'wibble1', lastName: 'scruff1', lastLogin: null, password: 'wobble',
                organisation: 'organisation', jobTitle: 'chef', disabled: false, pending: false)
        CatalogueUser catalogueUser2 = service.createNewUser(
            emailAddress: 'newtester2@email.com', firstName: 'wibble2', lastName: 'scruff2', lastLogin: null, password: 'wobble',
            organisation: 'organisation', jobTitle: 'chef but good', disabled: true, pending: false)

        List<CatalogueUser> group = [catalogueUser1, catalogueUser2] as List<CatalogueUser>

        when:
        ByteArrayOutputStream outputStream = service.convertToCsv(group)
        String output = new String(outputStream.toByteArray())

        then:
        output

        when:
        List<String> lines = output.readLines()

        then:
        lines.size() == 3
        lines[0] == 'Email Address,First Name,Last Name,Last Login,Organisation,Job Title,Disabled,Pending'
        lines.any {it == 'newtester1@email.com,wibble1,scruff1,,organisation,chef,false,false'}
        lines.any {it == 'newtester2@email.com,wibble2,scruff2,,organisation,chef but good,true,false'}
    }

    void 'test permanent deletion and anonymisation'() {
        given:
        CatalogueUser catalogueUserAdmin = service.createNewUser(
            emailAddress: 'admin@email.com', firstName: 'admin', lastName: 'scruff1', lastLogin: null, password: 'wobble',
            organisation: 'organisation', jobTitle: 'admin', disabled: false, pending: false)
        CatalogueUser catalogueUser1 = service.createNewUser(
            emailAddress: 'newtester1@email.com', firstName: 'wibble1', lastName: 'scruff1', lastLogin: null, password: 'wobble',
            organisation: 'organisation', jobTitle: 'chef', disabled: false, pending: false)
        CatalogueUser catalogueUser2 = service.createNewUser(
            emailAddress: 'newtester2@email.com', firstName: 'wibble2', lastName: 'scruff2', lastLogin: null, password: 'wobble',
            organisation: 'organisation', jobTitle: 'chef but good', disabled: false, pending: false)

        UserGroup group = new UserGroup(name: 'readersOnly', createdBy: reader.emailAddress)
                .addToGroupMembers(catalogueUser1)
                .addToGroupMembers(catalogueUser2)
        checkAndSave(group)

        checkAndSave(new ApiKey(refreshable: true, expiryDate: LocalDate.now(), disabled: false, name: 'Key 1', createdBy: catalogueUser1.emailAddress, catalogueUser: catalogueUserAdmin))
        checkAndSave(new ApiKey(refreshable: true, expiryDate: LocalDate.now(), disabled: false, name: 'Key 2', createdBy: catalogueUser2.emailAddress, catalogueUser: catalogueUserAdmin))
        checkAndSave(new Edit(title: EditTitle.UPDATE, createdBy: catalogueUser1.emailAddress, description: 'Edit 1', resourceDomainType: 'Folder', resourceId: UUID.randomUUID()))
        checkAndSave(new Edit(title: EditTitle.UPDATE, createdBy: catalogueUser2.emailAddress, description: 'Edit 2', resourceDomainType: 'Folder', resourceId: UUID.randomUUID()))
        checkAndSave(new Email(sentToEmailAddress: catalogueUser1.emailAddress, subject: 'Test Email', body: 'Body Content', successfullySent: true))
        checkAndSave(new Email(sentToEmailAddress: catalogueUser2.emailAddress, subject: 'Test Email', body: 'Body Content', successfullySent: true))
        checkAndSave(new Folder(label: 'Folder 1', createdBy: catalogueUser1.emailAddress))
        checkAndSave(new Folder(label: 'Folder 2', createdBy: catalogueUser2.emailAddress))

        when:
        List<Folder> folders = Folder.findAll()
        List<Edit> edits = Edit.findAll()
        List<Email> emails = Email.findAll()
        List<ApiKey> apiKeys = ApiKey.findAll()

        then:
        group.groupMembers.size() == 2
        folders.size() == 2
        edits.size() == 2
        emails.size() == 2
        apiKeys.size() == 2

        and:
        group.groupMembers.any {it.id == catalogueUser1.id}
        group.groupMembers.any {it.id == catalogueUser2.id}
        folders.any {it.createdBy == catalogueUser1.createdBy}
        folders.any {it.createdBy == catalogueUser2.createdBy}
        !folders.any {it.createdBy == 'anonymous@maurodatamapper.com'}
        edits.any {it.createdBy == catalogueUser1.createdBy}
        edits.any {it.createdBy == catalogueUser2.createdBy}
        !edits.any {it.createdBy == 'anonymous@maurodatamapper.com'}
        emails.any{it.sentToEmailAddress == catalogueUser1.emailAddress}
        emails.any{it.sentToEmailAddress == catalogueUser2.emailAddress}
        apiKeys.any {it.createdBy == catalogueUser1.createdBy}
        apiKeys.any {it.createdBy == catalogueUser2.createdBy}
        !apiKeys.any {it.createdBy == 'anonymous@maurodatamapper.com'}



        when:
        service.delete(catalogueUser2, true)
        folders = Folder.findAll()
        edits = Edit.findAll()
        emails = Email.findAll()
        apiKeys = ApiKey.findAll()

        then:
        group.groupMembers.size() == 1
        folders.size() == 2
        edits.size() == 2
        emails.size() == 1
        apiKeys.size() == 2

        and:
        group.groupMembers.any {it.id == catalogueUser1.id}
        !group.groupMembers.any {it.id == catalogueUser2.id}
        folders.any {it.createdBy == catalogueUser1.createdBy}
        !folders.any {it.createdBy == catalogueUser2.createdBy}
        folders.any {it.createdBy == 'anonymous@maurodatamapper.com'}
        edits.any {it.createdBy == catalogueUser1.createdBy}
        !edits.any {it.createdBy == catalogueUser2.createdBy}
        edits.any {it.createdBy == 'anonymous@maurodatamapper.com'}
        emails.any{it.sentToEmailAddress == catalogueUser1.emailAddress}
        !emails.any{it.sentToEmailAddress == catalogueUser2.emailAddress}
        apiKeys.any {it.createdBy == catalogueUser1.createdBy}
        !apiKeys.any {it.createdBy == catalogueUser2.createdBy}
        apiKeys.any {it.createdBy == 'anonymous@maurodatamapper.com'}
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
