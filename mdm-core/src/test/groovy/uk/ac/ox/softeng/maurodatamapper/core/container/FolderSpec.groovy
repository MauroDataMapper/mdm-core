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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class FolderSpec extends CreatorAwareSpec<Folder> implements DomainUnitTest<Folder> {

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

    void 'test parent child folders'() {
        given:
        setValidDomainValues()
        Folder child = new Folder(label: 'child', createdBy: admin.emailAddress)

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
        item.depth == 0
        item.path == ''

        and:
        item2.depth == 1
        item2.path == "/${item.id}"

        when:
        Folder child2 = new Folder(label: 'child2', createdBy: admin.emailAddress)
        item2.addToChildFolders(child2)

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
        Folder.count() == 1

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
        Folder.count() == 1
        Edit.count() == 2

        when:
        item = findById()

        then:
        verifyDomainConstraints item
        item.edits.size() == 2
        item.edits[0].createdBy == admin.emailAddress
        item.edits[0].description == 'Folder:test added'

        item.edits[1].createdBy == editor.emailAddress
        item.edits[1].description == '[Folder:test] changed properties [description]'
    }

    void 'test creating a new top level folder with the same name as existing'() {
        when:
        setValidDomainValues()

        then:
        checkAndSave(domain)
        domain.count() == 1

        when:
        Folder other = new Folder(createdBy: editor.emailAddress, label: domain.label)
        check(other)

        then:
        thrown(InternalSpockError)
        other.errors.fieldErrors.any {it.field == 'label' && it.code == 'default.not.unique.message'}
    }

    void 'test unique label naming for direct child folders'() {
        given:
        setValidDomainValues()
        domain.addToChildFolders(createdBy: editor.emailAddress, label: 'child')

        expect: 'domain is currently valid'
        checkAndSave(domain)

        when: 'adding another child folder to the folder'
        domain.addToChildFolders(createdBy: editor.emailAddress, label: 'child')
        check(domain)

        then: 'folder should not be valid'
        thrown(InternalSpockError)
        domain.errors.fieldErrors.any {it.field.contains('childFolders') && it.code.contains('unique')}
    }

    void 'test unique label naming for direct child folders of 2 folders'() {
        given:
        setValidDomainValues()
        domain.addToChildFolders(createdBy: editor.emailAddress, label: 'child')
        Folder other = new Folder(createdBy: editor.emailAddress, label: 'other')

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(other)

        when: 'adding another child folder to the other folder'
        other.addToChildFolders(createdBy: editor.emailAddress, label: 'child')

        then: 'folder should be valid'
        checkAndSave(domain)
        checkAndSave(other)
    }

    void 'test unique label naming for child folders of folder'() {
        given:
        setValidDomainValues()
        Folder child = domain.addToChildFolders(createdBy: editor.emailAddress, label: 'child')

        expect: 'domain is currently valid'
        checkAndSave(domain)

        when: 'adding child folder to the child with same label as parent'
        child.addToChildFolders(label: domain.label, createdBy: admin.emailAddress)

        then: 'domain should be valid'
        checkAndSave(domain)

        when: 'adding another folder to domain'
        child.addToChildFolders(label: 'another', createdBy: admin.emailAddress)

        then: 'domain should be valid'
        checkAndSave(domain)

        when: 'adding another dataclass to domain using same label'
        child.addToChildFolders(label: 'another', createdBy: admin.emailAddress)
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.fieldErrors.any {it.field.contains('childFolders') && it.code.contains('unique')}
    }

    @Override
    void setValidDomainOtherValues() {
        domain.label = 'test'
        domain
    }

    @Override
    void verifyDomainOtherConstraints(Folder subDomain) {
        assert subDomain.label == 'test'
        assert subDomain.depth == 0
        assert subDomain.path == ''
    }
}
