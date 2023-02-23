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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class FolderServiceSpec extends BaseUnitSpec implements ServiceUnitTest<FolderService> {

    UUID id

    def setup() {
        mockDomains(Folder, BasicModel)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        Folder folder = new Folder(label: 'parent', createdBy: editor.emailAddress).save(flush: true, failOnError: true)
        folder.addToChildFolders(new Folder(label: 'editorChild', createdBy: editor.emailAddress))
        Folder reader1Child = new Folder(label: 'reader1Child', createdBy: reader1.emailAddress)
        folder.addToChildFolders(reader1Child)
        Folder dmFolder = new Folder(label: 'dmFolder', createdBy: editor.emailAddress)
        folder.addToChildFolders(dmFolder)

        checkAndSave(folder)

        Folder reader2Child = new Folder(label: 'reader2Child', createdBy: reader2.emailAddress)
        reader1Child.addToChildFolders(reader2Child)

        checkAndSave(folder)
        id = folder.id
    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {
        when:
        List<Folder> folderList = service.list(max: 2, offset: 2)

        then:
        folderList.size() == 2
    }

    void 'test count'() {
        expect:
        service.count() == 6
    }

    void 'test delete'() {
        expect:
        service.count() == 6
        Folder folder = service.get(id)

        when:
        service.delete(folder)
        service.save(folder)

        then:
        Folder.countByDeleted(false) == 5
        Folder.countByDeleted(true) == 1
    }

    void 'test findAllByUser'() {
        given:
        UserSecurityPolicyManager testPolicy

        when: 'using admin policy which can see all folders'
        testPolicy = Mock()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Folder.list().collect {it.id}
        List<Folder> folderList = service.findAllByUser(testPolicy)

        then:
        folderList.size() == 6

        when: 'using policy that can only read the id folder'
        testPolicy = Mock()
        testPolicy.listReadableSecuredResourceIds(_) >> [id]
        folderList = service.findAllByUser(testPolicy)

        then:
        folderList.size() == 1

        when: 'using policy that provides an unknown id'
        testPolicy = Mock()
        testPolicy.listReadableSecuredResourceIds(_) >> [UUID.randomUUID()]
        folderList = service.findAllByUser(testPolicy)

        then:
        folderList.size() == 0

        when: 'using no access policy'
        folderList = service.findAllByUser(NoAccessSecurityPolicyManager.instance)

        then:
        folderList.size() == 0

        when: 'using public access policy'
        folderList = service.findAllByUser(PublicAccessSecurityPolicyManager.instance)

        then:
        folderList.size() == 6
    }
}
