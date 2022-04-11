/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

class VersionLinkSpec extends MdmDomainSpec<VersionLink> implements DomainUnitTest<VersionLink> {

    BasicModel db, db2
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel, Authority)
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost', createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc, authority: testAuthority)
        db2 = new BasicModel(createdBy: admin.emailAddress, label: 'test2', folder: misc, authority: testAuthority)
        checkAndSave(misc)
        checkAndSave(db)
        checkAndSave(db2)
    }

    void 'test saving valid object'() {

        when:
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.allErrors.size() == 6

        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = VersionLinkType.NEW_FORK_OF
        db.addToVersionLinks(domain)
        domain.setTargetModel(db2)

        then:
        checkAndSave(domain)

        when:
        def d1 = BasicModel.findByLabel('test')
        def d2 = BasicModel.findByLabel('test2')

        then:
        d1
        d2

        and:
        d1.versionLinks.find {it.id == domain.id}

        when:
        VersionLink tsl = VersionLink.byTargetModelId(d2.id).get()

        then:
        tsl
        tsl.id == domain.id
    }

    void 'test saving valid object through an item'() {

        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = VersionLinkType.NEW_FORK_OF
        db.addToVersionLinks(domain)
        domain.setTargetModel(db2)

        then:
        checkAndSave(db)

        when:
        def item = findById()
        def d1 = BasicModel.findByLabel('test')
        def d2 = BasicModel.findByLabel('test2')

        then:
        item
        d1
        d2

        and:
        d1.versionLinks.find {it.id == item.id}

        when:
        VersionLink tsl = VersionLink.byTargetModelId(d2.id).get()

        then:
        tsl
        tsl.id == item.id

    }

    void 'test invalid term link between the same model'() {
        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = VersionLinkType.NEW_FORK_OF
        db.addToVersionLinks(domain)
        domain.setTargetModel(db)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.hasErrors()

        when:
        def error = domain.errors.getFieldError('multiFacetAwareItemId')

        then:
        error.code == 'invalid.same.property.message'
        error.arguments.last() == 'targetModel'
    }

    @Override
    void setValidDomainOtherValues() {
        domain.linkType = VersionLinkType.NEW_FORK_OF
        db.addToVersionLinks(domain)
        domain.setTargetModel(db2)
    }

    @Override
    void verifyDomainOtherConstraints(VersionLink domain) {
        assert domain.linkType == VersionLinkType.NEW_FORK_OF
    }
}
