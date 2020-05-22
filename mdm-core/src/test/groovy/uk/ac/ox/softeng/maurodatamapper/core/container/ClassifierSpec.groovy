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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest

class ClassifierSpec extends CreatorAwareSpec<Classifier> implements DomainUnitTest<Classifier> {

    def setup() {
        mockDomain(Edit)
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

    void 'test parent child classifiers'() {
        given:
        setValidDomainValues()
        Classifier child = new Classifier(label: 'child', createdBy: admin.emailAddress)

        when:
        domain.addToChildClassifiers(child)

        then:
        checkAndSave(domain)

        when:
        item = findById()
        def item2 = Classifier.findByLabel('child')

        then:
        item
        item2

        and:
        item.depth == 0
        item.path == ''

        and:
        item2.depth == 1
        item2.path == "/${item.id}"

        when:
        Classifier child2 = new Classifier(label: 'child2', createdBy: admin.emailAddress)
        item2.addToChildClassifiers(child2)

        then:
        child2.depth == 2
        child2.path == "/${item.id}/${item2.id}"

    }

    void 'test adding edits'() {

        expect:
        domain.instanceOf(EditHistoryAware)

        when:
        setValidDomainValues()

        then:
        checkAndSave(domain)
        Classifier.count() == 1

        when: 'adding the created edit'
        (domain as EditHistoryAware).addCreatedEdit(admin)

        then:
        Edit.count() == 1

        when:
        item = findById()

        then:
        verifyDomainConstraints item
        item.edits.size() == 1

        when:
        domain.description = 'Changing description'
        (domain as EditHistoryAware).addUpdatedEdit(editor)

        then:
        checkAndSave(domain)
        Classifier.count() == 1
        Edit.count() == 2

        when:
        item = findById()

        then:
        verifyDomainConstraints item
        item.edits.size() == 2
        item.edits[0].createdBy == admin.emailAddress
        item.edits[0].description == 'Classifier:test added'

        item.edits[1].createdBy == editor.emailAddress
        item.edits[1].description == '[Classifier:test] changed properties [description]'
    }

    @Override
    void setValidDomainOtherValues() {
        domain.label = 'test'
        domain
    }

    @Override
    void verifyDomainOtherConstraints(Classifier subDomain) {
        assert subDomain.label == 'test'
        assert subDomain.depth == 0
        assert subDomain.path == ''
    }
}
