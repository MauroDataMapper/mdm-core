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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class GroupRoleSpec extends MdmDomainSpec<GroupRole> implements DomainUnitTest<GroupRole>, SecurityUsers {

    def setup() {
        log.debug('Setting up ContainerGroupRole')
        mockDomains(Edit, CatalogueUser, UserGroup)
        implementSecurityUsers('unitTest')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.name = 'site_administrator'
        domain.displayName = 'Site Administrator'
    }

    @Override
    void verifyDomainOtherConstraints(GroupRole domain) {
        assert domain.name == 'site_administrator'
        assert domain.displayName == 'Site Administrator'
    }

    void 'test invalid permission name'() {
        when:
        setValidDomainValues()
        domain.name = 'Site Administrator'
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.errors.allErrors.size() == 1
        domain.errors.getFieldError('name').code == 'invalid.grouprole.name.message'
    }

    void 'test base group role model saves'() {
        when:
        GroupRole siteAdmin = GroupRole.getDefaultGroupRoleModelStructure()
        checkAndSave(siteAdmin)

        then:
        noExceptionThrown()

        and:
        GroupRole.count() == 10
    }
}
