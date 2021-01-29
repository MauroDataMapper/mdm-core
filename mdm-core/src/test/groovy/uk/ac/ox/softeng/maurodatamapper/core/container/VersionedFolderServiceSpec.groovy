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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.UNIT_TEST

class VersionedFolderServiceSpec extends BaseUnitSpec implements ServiceUnitTest<VersionedFolderService> {

    UUID id

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }

    def setup() {
        mockArtefact(FolderService)
        mockDomains(VersionedFolder, Folder, BasicModel, Authority)
        checkAndSave(new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST))
        checkAndSave(new VersionedFolder(label: 'catalogue', createdBy: UNIT_TEST, authority: testAuthority))
        VersionedFolder folder = new VersionedFolder(label: 'parent', createdBy: UNIT_TEST, authority: testAuthority).
            save(flush: true, failOnError: true)
        folder.addToChildFolders(new VersionedFolder(label: 'editorChild', createdBy: UNIT_TEST, authority: testAuthority))
        VersionedFolder reader1Child = new VersionedFolder(label: 'reader1Child', createdBy: UNIT_TEST, authority: testAuthority)
        folder.addToChildFolders(reader1Child)
        VersionedFolder dmFolder = new VersionedFolder(label: 'dmFolder', createdBy: UNIT_TEST, authority: testAuthority)
        folder.addToChildFolders(dmFolder)

        checkAndSave(folder)

        VersionedFolder reader2Child = new VersionedFolder(label: 'reader2Child', createdBy: UNIT_TEST, authority: testAuthority)
        reader1Child.addToChildFolders(reader2Child)

        checkAndSave(folder)
        id = folder.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<VersionedFolder> folderList = service.list(max: 2, offset: 2)

        then:
        folderList.size() == 2
    }

    void "test count"() {
        expect:
        service.count() == 6
    }

    void "test delete"() {
        expect:
        service.count() == 6

        when:
        service.delete(id)

        then:
        VersionedFolder.countByDeleted(false) == 5
        VersionedFolder.countByDeleted(true) == 1
    }

    void 'test findAllByUser'() {
        given:
        UserSecurityPolicyManager testPolicy

        when: 'using admin policy which can see all folders'
        testPolicy = Mock()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> VersionedFolder.list().collect { it.id }
        List<VersionedFolder> folderList = service.findAllByUser(testPolicy)

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
