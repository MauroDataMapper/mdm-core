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

class AnnotationSpec extends CreatorAwareSpec<Annotation> implements DomainUnitTest<Annotation> {

    BasicModel db
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel, Authority)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc, authority: testAuthority)
        checkAndSave(misc)
        checkAndSave(db)
    }

    void 'test depth and path'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)
        item = findById()

        then:
        item.depth == 0
        item.path == ''

    }

    void 'test parent child annotations'() {
        given:
        setValidDomainValues()
        Annotation child = new Annotation(description: 'child', createdBy: admin.emailAddress)

        when:
        domain.addToChildAnnotations(child)

        then:
        checkAndSave(domain)

        when:
        item = findById()
        Annotation item2 = Annotation.findByLabel('test [0]')

        then:
        item
        item2

        and:
        item.depth == 0
        item.path == ''

        and:
        item2.depth == 1
        item2.path == "/${item.id}"
        item2.multiFacetAwareItemId
        item2.description == 'child'

        when:
        Annotation child2 = new Annotation(description: 'child2', createdBy: admin.emailAddress)
        item2.addToChildAnnotations(child2)

        then:
        child2.depth == 2
        child2.path == "/${item.id}/${item2.id}"

    }

    void 'test no catalogue item'() {
        given:
        domain.label = 'test'
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
        domain.label = 'test'
        domain.setMultiFacetAwareItem(db)
    }

    @Override
    void verifyDomainOtherConstraints(Annotation domain) {
        assert domain.label == 'test'
        assert domain.multiFacetAwareItemId == db.id
    }
}
