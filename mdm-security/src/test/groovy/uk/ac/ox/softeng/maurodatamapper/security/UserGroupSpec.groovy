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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class UserGroupSpec extends MdmDomainSpec<UserGroup> implements DomainUnitTest<UserGroup>, SecurityUsers {

    def setup() {
        log.debug('Setting up UserGroupSpec')
        mockDomains(Edit, CatalogueUser)
        implementSecurityUsers('unitTest')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.name = "test"
        domain.addToGroupMembers(admin)
    }

    @Override
    void verifyDomainOtherConstraints(UserGroup domain) {
        assert domain.name == 'test'
        assert domain.groupMembers.size() == 1
        assert domain.groupMembers.first().id == admin.id
    }

    void "test properly created usergroup"() {

        when:
        setValidDomainValues()

        then:
        checkAndSave(domain)
        UserGroup.count() == 1

        when:
        domain.addToGroupMembers(admin)

        then:
        checkAndSave(domain)
        UserGroup.count() == 1

        when:
        def group = UserGroup.findByName('test')
        def user = CatalogueUser.findByEmailAddress('admin@maurodatamapper.com')

        then:
        group.groupMembers.size() == 1

        and:
        user.groups.size() == 1

    }

    void 'test various usergroup setups'() {
        given:
        UserGroup admins = new UserGroup(createdBy: StandardEmailAddress.UNIT_TEST, name: 'administrators').addToGroupMembers(admin)
        UserGroup editors = new UserGroup(createdBy: StandardEmailAddress.UNIT_TEST, name: 'editors').addToGroupMembers(admin)
        UserGroup readers = new UserGroup(createdBy: StandardEmailAddress.UNIT_TEST, name: 'readers').addToGroupMembers(admin)
        UserGroup funGroup = new UserGroup(createdBy: StandardEmailAddress.UNIT_TEST, name: 'fungroup').addToGroupMembers(reader)

        editors.addToGroupMembers(editor)
        readers.addToGroupMembers(editor)
        readers.addToGroupMembers(reader)
        readers.addToGroupMembers(reviewer)
        funGroup.addToGroupMembers(reviewer)

        expect:
        checkAndSave admins
        checkAndSave editors
        checkAndSave readers
        checkAndSave funGroup
        checkAndSave new UserGroup(createdBy: StandardEmailAddress.UNIT_TEST, name: 'plain').addToGroupMembers(reader)

        when:
        item = UserGroup.findByName('administrators')

        then:
        item.groupMembers.size() == 1
        checkGroupUser item, admin
        checkGroupUser item, editor, false
        checkGroupUser item, reader, false
        checkGroupUser item, reviewer, false

        when:
        item = UserGroup.findByName('editors')

        then:
        item.groupMembers.size() == 2
        checkGroupUser item, admin
        checkGroupUser item, editor
        checkGroupUser item, reader, false
        checkGroupUser item, reviewer, false

        when:
        item = UserGroup.findByName('readers')

        then:
        item.groupMembers.size() == 4
        checkGroupUser item, admin
        checkGroupUser item, editor
        checkGroupUser item, reader
        checkGroupUser item, reviewer

        when:
        item = UserGroup.findByName('fungroup')

        then:
        item.groupMembers.size() == 2
        checkGroupUser item, admin, false
        checkGroupUser item, editor, false
        checkGroupUser item, reader
        checkGroupUser item, reviewer

        when:
        item = UserGroup.findByName('plain')

        then:
        item.groupMembers.size() == 1
        checkGroupUser item, admin, false
        checkGroupUser item, editor, false
        checkGroupUser item, reader
        checkGroupUser item, reviewer, false

        when:
        reviewer.addToGroups(item)
        checkAndSave reviewer
        item = UserGroup.findByName('plain')

        then:
        item.groupMembers.size() == 2
        checkGroupUser item, admin, false
        checkGroupUser item, editor, false
        checkGroupUser item, reader
        checkGroupUser item, reviewer


    }

    void checkGroupUser(UserGroup group, CatalogueUser user, boolean inGroup = true) {
        assert group.hasMember(user) == inGroup
        assert user.isInGroup(group) == inGroup
    }


}
