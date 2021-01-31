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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class MetadataSpec extends CreatorAwareSpec<Metadata> implements DomainUnitTest<Metadata> {

    BasicModel db
    Folder misc

    def setup() {
        mockDomains(Folder, Authority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc, authority: testAuthority)
        mockDomains(Folder, BasicModel)
        checkAndSave(misc)
        checkAndSave(db)
    }

    void 'test no catalogue item'() {
        given:
        domain.namespace = "http://test.com/valid"
        domain.key = 'test_key'
        domain.value = 'a value'
        domain.createdBy = admin.emailAddress

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.getFieldError('catalogueItemId')
        domain.errors.getFieldError('catalogueItemDomainType')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.namespace = "http://test.com/valid"
        domain.key = 'test_key'
        domain.value = 'a value'
        domain.catalogueItem = db
    }

    @Override
    void verifyDomainOtherConstraints(Metadata domain) {
        assert domain.namespace == "http://test.com/valid"
        assert domain.key == 'test_key'
        assert domain.value == 'a value'
        assert domain.catalogueItem.id
        assert domain.catalogueItemDomainType == BasicModel.simpleName
        assert domain.catalogueItem.id == db.id
        assert domain.createdBy == admin.emailAddress
    }
}
