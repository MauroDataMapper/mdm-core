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

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class EditServiceSpec extends BaseUnitSpec implements ServiceUnitTest<EditService> {

    UUID id

    def setup() {
        mockDomains(Classifier, Folder, Edit)
        checkAndSave(new Edit(title: EditTitle.UPDATE, createdBy: admin.emailAddress, description: 'Edit 1', resourceDomainType: 'Folder', resourceId: UUID.randomUUID()))
        checkAndSave(new Edit(title: EditTitle.UPDATE, createdBy: editor.emailAddress, description: 'Edit 2', resourceDomainType: 'Folder', resourceId: UUID.randomUUID()))
        Edit edit = new Edit(title: EditTitle.UPDATE, createdBy: pending.emailAddress, description: 'Edit 3', resourceDomainType: 'Folder', resourceId: UUID.randomUUID())
        checkAndSave(edit)
        checkAndSave(new Edit(title: EditTitle.UPDATE, createdBy: reader2.emailAddress, description: 'Edit 4', resourceDomainType: 'Classifier',
                              resourceId: UUID.randomUUID()))
        checkAndSave(new Edit(title: EditTitle.UPDATE, createdBy: reader1.emailAddress, description: 'Edit 5', resourceDomainType: 'Classifier',
                              resourceId: UUID.randomUUID()))
        id = edit.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<Edit> editList = service.list(max: 2, offset: 2)

        then:
        editList.size() == 2

        and:
        editList[0].createdBy == pending.emailAddress
        editList[0].title.toString() == 'UPDATE'
        editList[0].description == 'Edit 3'

        and:
        editList[1].createdBy == reader2.emailAddress
        editList[1].title.toString() == 'UPDATE'
        editList[1].description == 'Edit 4'
    }

    void "test count"() {
        expect:
        service.count() == 5
    }

    void "find all classifier edits"() {
        given:
        Classifier classifier = new Classifier(label: 'integration test', createdBy: admin.emailAddress)
        checkAndSave(classifier)
        classifier.addCreatedEdit(admin)
        classifier.description = 'a description'
        classifier.addUpdatedEdit(editor, classifier.dirtyPropertyNames)
        checkAndSave(classifier)

        when:
        List<Edit> edits = service.findAllByResource(Classifier.simpleName, classifier.id)

        then:
        edits.size() == 2
    }

    void "find all Folder edits"() {
        given:
        Folder folder = new Folder(label: 'integration test', createdBy: admin.emailAddress)
        checkAndSave(folder)
        folder.addCreatedEdit(admin)
        folder.description = 'a description'
        folder.addUpdatedEdit(editor, folder.dirtyPropertyNames)
        checkAndSave(folder)

        when:
        List<Edit> edits = service.findAllByResource(Folder.simpleName, folder.id)

        then:
        edits.size() == 2
    }
}
