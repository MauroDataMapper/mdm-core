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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.UNIT_TEST

@Slf4j
@Integration
@Rollback
class VersionedFolderServiceSpec extends BaseIntegrationSpec {

    private static String VF_HISTORY_LABEL = 'VF History'

    UUID id
    List<VersionedFolder> versionedContainers
    VersionedFolderService versionedFolderService

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }

    void setupDomainData() {
        checkAndSave(new VersionedFolder(label: 'catalogue', createdBy: UNIT_TEST, authority: testAuthority))
        VersionedFolder folder = new VersionedFolder(label: 'parent', createdBy: UNIT_TEST, authority: testAuthority).
            save(flush: true, failOnError: true)
        folder.addToChildFolders(new Folder(label: 'editorChild', createdBy: UNIT_TEST, authority: testAuthority))
        Folder reader1Child = new Folder(label: 'reader1Child', createdBy: UNIT_TEST, authority: testAuthority)
        folder.addToChildFolders(reader1Child)
        Folder dmFolder = new Folder(label: 'dmFolder', createdBy: UNIT_TEST, authority: testAuthority)
        folder.addToChildFolders(dmFolder)

        checkAndSave(folder)

        Folder reader2Child = new Folder(label: 'reader2Child', createdBy: UNIT_TEST, authority: testAuthority)
        reader1Child.addToChildFolders(reader2Child)

        (1..6).each {
            new VersionedFolder(label: "VF $it", createdBy: UNIT_TEST, authority: testAuthority).
                save(flush: true, failOnError: true)
        }

        checkAndSave(folder)
        id = folder.id

        // Setup VersionedFolder model version history
        def vf1Version1 = new VersionedFolder(label: VF_HISTORY_LABEL, createdBy: UNIT_TEST, authority: testAuthority, modelVersion: Version.from('1'), finalised: true)
        checkAndSave(vf1Version1)
        def vf1Version2 = new VersionedFolder(label: VF_HISTORY_LABEL, createdBy: UNIT_TEST, authority: testAuthority, modelVersion: Version.from('2'), finalised: true)
        checkAndSave(vf1Version2)
        def vf1Version3Draft = new VersionedFolder(label: VF_HISTORY_LABEL, createdBy: UNIT_TEST, authority: testAuthority, finalised: false, branchName: 'main',
                                                   deleted: true)
        checkAndSave(vf1Version3Draft)

        vf1Version2.addToVersionLinks(linkType: VersionLinkType.NEW_MODEL_VERSION_OF, createdBy: UNIT_TEST, targetModel: vf1Version1)
        checkAndSave(vf1Version1)

        vf1Version3Draft.addToVersionLinks(linkType: VersionLinkType.NEW_MODEL_VERSION_OF, createdBy: UNIT_TEST, targetModel: vf1Version2)
        checkAndSave(vf1Version2)

        versionedContainers = [vf1Version1, vf1Version2, vf1Version3Draft]
    }

    void 'test get'() {
        given:
        setupDomainData()

        expect:
        versionedFolderService.get(id) != null
    }

    void 'test list'() {
        given:
        setupDomainData()

        when:
        List<VersionedFolder> folderList = versionedFolderService.list(max: 2, offset: 2)

        then:
        folderList.size() == 2
    }

    void 'test count'() {
        given:
        setupDomainData()

        expect:
        versionedFolderService.count() == 11
    }

    void 'test delete'() {
        given:
        setupDomainData()

        expect:
        versionedFolderService.count() == 11

        when:
        versionedFolderService.delete(id)
        sessionFactory.currentSession.flush()

        then:
        VersionedFolder.countByDeleted(false) == 9
        VersionedFolder.countByDeleted(true) == 2
    }

    void 'test findAllByUser'() {
        given:
        setupDomainData()
        UserSecurityPolicyManager testPolicy

        when: 'using admin policy which can see all folders'
        testPolicy = Mock()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> VersionedFolder.list().collect {it.id}
        List<VersionedFolder> folderList = versionedFolderService.findAllByUser(testPolicy)

        then:
        folderList.size() == 11

        when: 'using policy that can only read the id folder'
        testPolicy = Mock()
        testPolicy.listReadableSecuredResourceIds(_) >> [id]
        folderList = versionedFolderService.findAllByUser(testPolicy)

        then:
        folderList.size() == 1

        when: 'using policy that provides an unknown id'
        testPolicy = Mock()
        testPolicy.listReadableSecuredResourceIds(_) >> [UUID.randomUUID()]
        folderList = versionedFolderService.findAllByUser(testPolicy)

        then:
        folderList.size() == 0

        when: 'using no access policy'
        folderList = versionedFolderService.findAllByUser(NoAccessSecurityPolicyManager.instance)

        then:
        folderList.size() == 0

        when: 'using public access policy'
        folderList = versionedFolderService.findAllByUser(PublicAccessSecurityPolicyManager.instance)

        then:
        folderList.size() == 11
    }

    void 'test version family tree on id'() {
        given:
        setupDomainData()
        Folder f = Folder.get(id)

        expect:
        !versionedFolderService.hasVersionedFolderParent(f)
        versionedFolderService.isVersionedFolderFamily(f)
    }

    void 'test version family tree on editorChild'() {
        given:
        setupDomainData()
        Folder f = Folder.findByLabel('editorChild')

        expect:
        versionedFolderService.isVersionedFolderFamily(f)
        versionedFolderService.hasVersionedFolderParent(f)
    }

    void 'test version family tree on custom'() {
        given:
        setupDomainData()
        Folder p = Folder.findByLabel('editorChild')
        Folder f = new Folder(label: 'grandchild', createdBy: UNIT_TEST)
        p.addToChildFolders(f).save(flush: true, failOnError: true)

        expect:
        versionedFolderService.isVersionedFolderFamily(f)
        versionedFolderService.hasVersionedFolderParent(f)
    }

    void 'test moving top level folder to VF'() {
        given:
        setupDomainData()
        Folder f = new Folder(label: 'topfolder', createdBy: UNIT_TEST).save(flush: true, failOnError: true)
        Folder moveTo = Folder.get(id)

        expect:
        !versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(f, moveTo)
    }

    void 'test moving top level folder to F inside VF'() {
        given:
        setupDomainData()
        Folder f = new Folder(label: 'topfolder', createdBy: UNIT_TEST).save(flush: true, failOnError: true)
        Folder moveTo = Folder.findByLabel('editorChild')

        expect:
        !versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(f, moveTo)
    }

    void 'test moving folder out of VF'() {
        given:
        setupDomainData()
        Folder moveTo = new Folder(label: 'topfolder', createdBy: UNIT_TEST).save(flush: true, failOnError: true)
        Folder f = Folder.findByLabel('editorChild')

        expect:
        !versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(f, moveTo)
    }

    void 'test moving top level VF to VF'() {
        given:
        setupDomainData()
        Folder f = Folder.findByLabel('VF 1')
        Folder moveTo = Folder.get(id)

        expect:
        versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(f, moveTo)
    }

    void 'test moving top level VF to F inside VF'() {
        given:
        setupDomainData()
        Folder f = Folder.findByLabel('VF 2')
        Folder moveTo = Folder.findByLabel('editorChild')

        expect:
        versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(f, moveTo)
    }

    void 'test moving a F with a VF into a VF'() {
        given:
        setupDomainData()
        Folder moving = new Folder(label: 'topfolder', createdBy: UNIT_TEST).save(flush: true, failOnError: true)
        moving.addToChildFolders(Folder.findByLabel('VF 2')).save(flush: true, failOnError: true)
        Folder moveTo = Folder.findByLabel('VF 1')

        expect:
        versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(moving, moveTo)
    }

    void 'test moving a F with a VF into a F inside a VF'() {
        given:
        setupDomainData()
        Folder moving = new Folder(label: 'topfolder', createdBy: UNIT_TEST).save(flush: true, failOnError: true)
        moving.addToChildFolders(Folder.findByLabel('VF 2')).save(flush: true, failOnError: true)
        Folder moveTo = Folder.findByLabel('editorChild')

        expect:
        versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(moving, moveTo)
    }

    void 'test moving a F with a VF into a F'() {
        given:
        setupDomainData()
        Folder moving = new Folder(label: 'topfolder', createdBy: UNIT_TEST).save(flush: true, failOnError: true)
        moving.addToChildFolders(Folder.findByLabel('VF 2')).save(flush: true, failOnError: true)
        Folder moveTo = new Folder(label: 'topfolder 2', createdBy: UNIT_TEST).save(flush: true, failOnError: true)

        expect:
        !versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(moving, moveTo)
    }

    void 'test excluding model superseded and deleted containers'() {
        given:
        setupDomainData()

        when:
        List<VersionedFolder> filtered = versionedFolderService.filterAllReadableContainers(versionedContainers,
                                                                                            false,
                                                                                            false,
                                                                                            false)

        then:
        filtered.size() == 1
        !filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 1}
        filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 2}
        !filtered.any {it.label == VF_HISTORY_LABEL && !it.finalised && it.deleted}
    }

    void 'test including model superseded containers'() {
        given:
        setupDomainData()

        when:
        List<VersionedFolder> filtered = versionedFolderService.filterAllReadableContainers(versionedContainers,
                                                                                            false,
                                                                                            true,
                                                                                            false)

        then:
        filtered.size() == 2
        filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 1}
        filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 2}
        !filtered.any {it.label == VF_HISTORY_LABEL && !it.finalised && it.deleted}
    }

    void 'test include deleted containers'() {
        given:
        setupDomainData()

        when:
        List<VersionedFolder> filtered = versionedFolderService.filterAllReadableContainers(versionedContainers,
                                                                                            false,
                                                                                            false,
                                                                                            true)

        then:
        filtered.size() == 2
        !filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 1}
        filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 2}
        filtered.any {it.label == VF_HISTORY_LABEL && !it.finalised && it.deleted}
    }

    void 'test include model superseded and deleted containers'() {
        given:
        setupDomainData()

        when:
        List<VersionedFolder> filtered = versionedFolderService.filterAllReadableContainers(versionedContainers,
                                                                                            false,
                                                                                            true,
                                                                                            true)

        then:
        filtered.size() == 3
        filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 1}
        filtered.any {it.label == VF_HISTORY_LABEL && it.modelVersion?.major == 2}
        filtered.any {it.label == VF_HISTORY_LABEL && !it.finalised && it.deleted}
    }
}
