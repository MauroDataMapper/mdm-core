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
package uk.ac.ox.softeng.maurodatamapper.core.container


import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class FolderSpec extends FolderContainerSpec<Folder> implements DomainUnitTest<Folder> {

    @Override
    Folder newChildContainerClass(Map<String, Object> args) {
        new Folder(args)
    }

    @Override
    Folder newContainerClass(Map<String, Object> args) {
        new Folder(args)
    }

    @Override
    Class<Folder> getContainerClass() {
        Folder
    }

    @Override
    Map<String, Object> getChildFolderArgs() {
        [createdBy: editor.emailAddress, label: 'child']
    }

    @Override
    Map<String, Object> getOtherFolderArgs() {
        [createdBy: editor.emailAddress, label: 'other']
    }

    void 'F01 : test unique label naming for direct child folders'() {
        given:
        setValidDomainValues()
        domain.addToChildFolders(new Folder(childFolderArgs))

        expect: 'domain is currently valid'
        checkAndSave(domain)

        when: 'adding another child folder to the folder'
        domain.addToChildFolders(new Folder(childFolderArgs))
        check(domain)

        then: 'folder should not be valid'
        thrown(InternalSpockError)
        domain.errors.fieldErrors.any { it.field.contains('childFolders') && it.code.contains('unique') }
    }

    void 'C02 : test unique label naming for child folders of folder'() {
        given:
        setValidDomainValues()
        Folder child = domain.addToChildFolders(childFolderArgs)

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
        domain.errors.fieldErrors.any { it.field.contains('childFolders') && it.code.contains('unique') }
    }
}
