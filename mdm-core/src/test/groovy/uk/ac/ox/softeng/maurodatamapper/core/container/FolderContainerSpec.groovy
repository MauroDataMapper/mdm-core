/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.test.unit.MdmDomainSpec

import grails.testing.gorm.DomainUnitTest
import groovy.transform.SelfType
import org.spockframework.util.InternalSpockError

@SelfType(DomainUnitTest)
abstract class FolderContainerSpec<K extends Container> extends MdmDomainSpec<K> {

    abstract Container newChildContainerClass(Map<String, Object> args)

    abstract K newContainerClass(Map<String, Object> args)

    abstract Class<K> getContainerClass()

    abstract Map<String, Object> getChildFolderArgs()

    abstract Map<String, Object> getOtherFolderArgs()

    void verifyC04Error(K other) {
        other.errors.fieldErrors.any { it.field == 'label' && it.code == 'default.not.unique.message' }
    }

    def setup() {
        mockDomain(Edit)
    }

    void 'C01 : test depth and path'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)
        item = findById()

        then:
        item.path == Path.from(domain)

    }

    void 'C02 : test parent child folders'() {
        given:
        setValidDomainValues()
        K child = newChildContainerClass(label: 'child', createdBy: admin.emailAddress)

        when:
        domain.addToChildFolders(child)

        then:
        checkAndSave(domain)

        when:
        item = findById()
        def item2 = Folder.findByLabel('child')

        then:
        item
        item2

        and:
        item.path == Path.from(domain)

        and:
        item2.path == Path.from(domain, child)

        when:
        K child2 = newChildContainerClass(label: 'child2', createdBy: admin.emailAddress)
        item2.addToChildFolders(child2)

        then:
        child2.path == Path.from(domain, child, child2)

    }

    void 'C03 : test adding edits'() {

        expect:
        domain.instanceOf(EditHistoryAware)

        when:
        setValidDomainValues()

        then:
        checkAndSave(domain)
        containerClass.count() == 1

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
        (domain as EditHistoryAware).addUpdatedEdit(editor, domain.dirtyPropertyNames)

        then:
        checkAndSave(domain)
        containerClass.count() == 1
        Edit.count() == 2

        when:
        item = findById()

        then:
        verifyDomainConstraints item
        item.edits.size() == 2
        item.edits[0].createdBy == admin.emailAddress
        item.edits[0].description == "[${containerClass.simpleName}:test] created"

        item.edits[1].createdBy == editor.emailAddress
        item.edits[1].description == "[${containerClass.simpleName}:test] changed properties [description]"
    }

    void 'C04 : test creating a new top level folder with the same name as existing'() {
        when:
        setValidDomainValues()

        then:
        checkAndSave(domain)
        domain.count() == 1

        when:
        K other = newContainerClass(createdBy: editor.emailAddress, label: domain.label)
        check(other)

        then:
        thrown(InternalSpockError)
        verifyC04Error(other)

    }

    void 'C05 : test unique label naming for direct child folders of 2 folders'() {
        given:
        setValidDomainValues()
        domain.addToChildFolders(childFolderArgs)
        K other = newChildContainerClass(otherFolderArgs)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(other)

        when: 'adding another child folder to the other folder'
        other.addToChildFolders(childFolderArgs)

        then: 'folder should be valid'
        checkAndSave(domain)
        checkAndSave(other)
    }



    @Override
    void setValidDomainOtherValues() {
        domain.label = 'test'
        domain
    }

    @Override
    void verifyDomainOtherConstraints(K subDomain) {
        assert subDomain.label == 'test'
        assert subDomain.path.size() == 1
        assert subDomain.path == Path.from(subDomain)
    }
}
