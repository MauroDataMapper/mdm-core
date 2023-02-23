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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class MetadataSpec extends MdmDomainSpec<Metadata> implements DomainUnitTest<Metadata> {

    BasicModel db
    Folder misc

    def setup() {
        mockDomains(Folder, Authority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost', createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc, authority: testAuthority)
        mockDomains(Folder, BasicModel)
        checkAndSave(misc)
        checkAndSave(db)
    }

    void 'test no catalogue item'() {
        given:
        domain.namespace = 'http://test.com/valid'
        domain.key = 'test_key'
        domain.value = 'a value'
        domain.createdBy = admin.emailAddress

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.getFieldError('multiFacetAwareItemId')
        domain.errors.getFieldError('multiFacetAwareItemDomainType')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.namespace = 'http://test.com/valid'
        domain.key = 'test_key'
        domain.value = 'a value'
        domain.multiFacetAwareItem = db
    }

    @Override
    void verifyDomainOtherConstraints(Metadata domain) {
        assert domain.namespace == 'http://test.com/valid'
        assert domain.key == 'test_key'
        assert domain.value == 'a value'
        assert domain.multiFacetAwareItem.id
        assert domain.multiFacetAwareItemDomainType == BasicModel.simpleName
        assert domain.multiFacetAwareItem.id == db.id
        assert domain.createdBy == admin.emailAddress
    }
}
