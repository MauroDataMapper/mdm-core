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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class GroupRoleServiceSpec extends BaseUnitSpec implements ServiceUnitTest<GroupRoleService>, DomainUnitTest<GroupRole>,
    SecurityUsers {

    UUID id

    def setup() {
        log.debug('Setting up UserGroupServiceSpec')
        mockDomains(CatalogueUser, Edit, UserGroup)
        implementSecurityUsers('unitTest')
        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())

        id = GroupRole.findByName('reader').id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<GroupRole> groupRoleList = service.list(max: 2, offset: 2, sort: 'name')

        then:
        groupRoleList.size() == 2

        when:
        GroupRole a = groupRoleList[0]
        GroupRole b = groupRoleList[1]

        then:
        a.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME
        a.displayName == 'Container Administrator'

        and:
        b.name == 'container_group_admin'
        b.displayName == 'Container Group Administrator'
    }

    void "test count"() {
        expect:
        service.count() == 10
    }

    void "test delete"() {
        expect:
        service.count() == 10

        when:
        service.delete(id)

        then:
        Exception ex = thrown(ApiInternalException)
        ex.message == 'Deletion of GroupRoles is not permitted'
    }

    void 'test find all application level roles'() {

        when:
        List<GroupRole> roles = service.findAllApplicationLevelRoles()

        then:
        roles.size() == 5

        and:
        roles.any {it.name == 'site_admin'}
        roles.any {it.name == GroupRole.APPLICATION_ADMIN_ROLE_NAME}
        roles.any {it.name == 'user_admin'}
        roles.any {it.name == 'group_admin'}
        roles.any {it.name == 'container_group_admin'}

    }
}
