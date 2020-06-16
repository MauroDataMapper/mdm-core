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
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class SecurableResourceGroupRoleSpec extends CreatorAwareSpec<SecurableResourceGroupRole>
    implements DomainUnitTest<SecurableResourceGroupRole>, SecurityUsers {

    Folder folder
    UserGroup readers
    UserGroup writers

    def setup() {
        log.debug('Setting up SecurableResourceGroupRoleSpec')
        mockDomains(Edit, CatalogueUser, UserGroup, GroupRole, Folder, BasicModel)
        implementSecurityUsers('unitTest')
        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())
        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        readers = new UserGroup(createdBy: reader.emailAddress, name: 'readers-only')
            .addToGroupMembers(reader)
            .addToGroupMembers(reviewer)
        checkAndSave(readers)

        writers = new UserGroup(createdBy: editor.emailAddress, name: 'writers')
            .addToGroupMembers(editor)
        checkAndSave(writers)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.securableResource = folder
        domain.userGroup = readers
        domain.groupRole = GroupRole.findByName('reader')
    }

    @Override
    void verifyDomainOtherConstraints(SecurableResourceGroupRole domain) {
        assert domain.securableResourceId == folder.id
        assert domain.securableResourceDomainType == folder.domainType
        assert domain.userGroup.id == readers.id
        assert domain.groupRole.id == GroupRole.findByName('reader').id
    }

    void 'test adding securable resource with a group role to a group and saving through the group'() {
        expect:
        SecurableResourceGroupRole.count() == 0

        when:
        writers.addToSecurableResourceGroupRoles(new SecurableResourceGroupRole(groupRole: GroupRole.findByName('editor'),
                                                                                securableResourceDomainType: folder.domainType,
                                                                                securableResourceId: folder.id,
                                                                                createdBy: userEmailAddresses.unitTest))
        checkAndSave(writers)

        then:
        writers.securableResourceGroupRoles.size() == 1
        SecurableResourceGroupRole.count() == 1
        GroupRole.findByName('editor').securedResourceGroupRoles.size() == 1
        GroupRole.findByName('editor').securedResourceGroupRoles.first().securableResourceId == folder.id
    }

    void 'test changing the usergroup is not possible'() {
        given:
        setValidDomainValues()
        checkAndSave(domain)

        when:
        domain.userGroup = writers
        check(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.errors.getFieldError('userGroup').code == 'invalid.grouprole.cannot.change.usergroup.message'

    }

    void 'test changing the group role to an application level role'() {
        given:
        setValidDomainValues()
        checkAndSave(domain)

        when:
        domain.groupRole = GroupRole.findByName(GroupRole.APPLICATION_ADMIN_ROLE_NAME)
        check(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.errors.getFieldError('groupRole').code == 'invalid.grouprole.cannot.be.application.level.message'

    }
}
