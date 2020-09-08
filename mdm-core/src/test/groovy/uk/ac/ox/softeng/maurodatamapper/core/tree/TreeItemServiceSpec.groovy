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
package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.testing.services.ServiceUnitTest

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class TreeItemServiceSpec extends BaseUnitSpec implements ServiceUnitTest<TreeItemService> {

    FolderService folderService
    ClassifierService classifierService
    Authority testAuthority

    def setup() {
        mockDomains(Folder, BasicModel, BasicModelItem, Authority)
        testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
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

        mockDomains(BasicModel, BasicModelItem)

        folderService = Spy(FolderService)
        classifierService = Spy(ClassifierService)
        service.containerServices = [folderService, classifierService]
    }

    void 'test building container folder only tree'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Folder.list().collect {it.id}

        when:
        List<ContainerTreeItem> tree = service.buildContainerOnlyTree(Folder, testPolicy, false)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        tree[0].isEmptyContainerTree()
        !tree[0].hasChildren()
        !tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[1].size() == 3
        tree[1].every {it.renderChildren}
        tree[1].every {!it.deleted}
        tree[1].every {it.containerType == 'Folder'}
        tree[1].every {it.domainType == 'Folder'}
        tree[1].every {it.path == "/${tree[1].id}"}
        tree[1].every {it.rootId == tree[1].id}
        tree[1].every {it.parentId == tree[1].id}
        tree[1].every {it.depth == 1}

        and:
        tree[1][0].label == 'dmFolder'
        tree[1][0].id == Folder.findByLabel('dmFolder').id
        tree[1][0].isEmptyContainerTree()
        !tree[1][0].hasChildren()
        !tree[1][0].childrenExist

        and:
        tree[1][1].label == 'editorChild'
        tree[1][1].id == Folder.findByLabel('editorChild').id
        tree[1][1].isEmptyContainerTree()
        !tree[1][1].hasChildren()
        !tree[1][1].childrenExist

        and:
        tree[1][2].label == 'reader1Child'
        tree[1][2].id == Folder.findByLabel('reader1Child').id
        tree[1][2].isEmptyContainerTree()
        tree[1][2].hasChildren()
        tree[1][2].childrenExist

        and:
        tree[1][2].size() == 1
        tree[1][2].every {it.renderChildren}
        tree[1][2].every {!it.deleted}
        tree[1][2].every {it.containerType == 'Folder'}
        tree[1][2].every {it.domainType == 'Folder'}
        tree[1][2].every {it.path == "/${tree[1].id}/${tree[1][2].id}"}
        tree[1][2].every {it.rootId == tree[1].id}
        tree[1][2].every {it.parentId == tree[1][2].id}
        tree[1][2].every {it.depth == 2}

        and:
        tree[1][2][0].label == 'reader2Child'
        tree[1][2][0].id == Folder.findByLabel('reader2Child').id
        tree[1][2][0].isEmptyContainerTree()
        !tree[1][2][0].hasChildren()
        !tree[1][2][0].childrenExist
    }

    void 'test building container folder tree with no models existing'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Folder.list().collect {it.id}

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, false)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        tree[0].isEmptyContainerTree()
        !tree[0].hasChildren()
        !tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[1].size() == 3
        tree[1].every {it.renderChildren}
        tree[1].every {!it.deleted}
        tree[1].every {it.containerType == 'Folder'}
        tree[1].every {it.domainType == 'Folder'}
        tree[1].every {it.path == "/${tree[1].id}"}
        tree[1].every {it.rootId == tree[1].id}
        tree[1].every {it.parentId == tree[1].id}
        tree[1].every {it.depth == 1}

        and:
        tree[1][0].label == 'dmFolder'
        tree[1][0].id == Folder.findByLabel('dmFolder').id
        tree[1][0].isEmptyContainerTree()
        !tree[1][0].hasChildren()
        !tree[1][0].childrenExist

        and:
        tree[1][1].label == 'editorChild'
        tree[1][1].id == Folder.findByLabel('editorChild').id
        tree[1][1].isEmptyContainerTree()
        !tree[1][1].hasChildren()
        !tree[1][1].childrenExist

        and:
        tree[1][2].label == 'reader1Child'
        tree[1][2].id == Folder.findByLabel('reader1Child').id
        tree[1][2].isEmptyContainerTree()
        tree[1][2].hasChildren()
        tree[1][2].childrenExist

        and:
        tree[1][2].size() == 1
        tree[1][2].every {it.renderChildren}
        tree[1][2].every {!it.deleted}
        tree[1][2].every {it.containerType == 'Folder'}
        tree[1][2].every {it.domainType == 'Folder'}
        tree[1][2].every {it.path == "/${tree[1].id}/${tree[1][2].id}"}
        tree[1][2].every {it.rootId == tree[1].id}
        tree[1][2].every {it.parentId == tree[1][2].id}
        tree[1][2].every {it.depth == 2}

        and:
        tree[1][2][0].label == 'reader2Child'
        tree[1][2][0].id == Folder.findByLabel('reader2Child').id
        tree[1][2][0].isEmptyContainerTree()
        !tree[1][2][0].hasChildren()
        !tree[1][2][0].childrenExist
    }

    void 'test building container folder tree with no models existing remove empty folders'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Folder.list().collect {it.id}

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.isEmpty()

    }

    void 'test building container folder only tree remove empty folders'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Folder.list().collect {it.id}

        when:
        List<ContainerTreeItem> tree = service.buildContainerOnlyTree(Folder, testPolicy, true)

        then:
        tree.isEmpty()
    }

    void 'test building container tree with 1 empty model and empty folders removed'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
        }
        service.modelServices = [basicModelService]

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.size() == 1

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        !tree[0][0].hasChildren()
        !tree[0][0].childrenExist
        !tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')


    }

    void 'test building container tree with 2 empty models at the top level and empty folders removed'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('parent'),
                                                authority: testAuthority)
        checkAndSave(basicModel)
        checkAndSave(basicModel2)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel, basicModel2]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
        }
        service.modelServices = [basicModelService]

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        !tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        !tree[0][0].hasChildren()
        !tree[0][0].childrenExist
        !tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[1].size() == 1

        and:
        tree[1][0].label == 'dm2'
        tree[1][0].id == basicModel2.id
        tree[1][0].domainType == 'BasicModel'
        !tree[1][0].hasChildren()
        !tree[1][0].childrenExist
        !tree[1][0].renderChildren
        ((ModelTreeItem) tree[1][0]).containerId == Folder.findByLabel('parent').id
        !((ModelTreeItem) tree[1][0]).deleted
        !((ModelTreeItem) tree[1][0]).finalised
        !((ModelTreeItem) tree[1][0]).superseded
        ((ModelTreeItem) tree[1][0]).documentationVersion == Version.from('1.0.0')


    }

    void 'test building container tree with 2 empty models with different superseding and deleted variations'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'), deleted: true,
                                               finalised: true, modelVersion: Version.from('1.0.0'),
                                               authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('parent'),
                                                documentationVersion: Version.from('2.1.0'),
                                                authority: testAuthority)
        basicModel2.addToVersionLinks(new VersionLink(linkType: VersionLinkType.SUPERSEDED_BY_FORK, targetModel: basicModel))
        checkAndSave(basicModel)
        checkAndSave(basicModel2)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel, basicModel2]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
            findAllSupersededModelIds(_) >> [basicModel2.id]
        }
        service.modelServices = [basicModelService]

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        !tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        !tree[0][0].hasChildren()
        !tree[0][0].childrenExist
        !tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        ((ModelTreeItem) tree[0][0]).deleted
        ((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[1].size() == 1

        and:
        tree[1][0].label == 'dm2'
        tree[1][0].id == basicModel2.id
        tree[1][0].domainType == 'BasicModel'
        !tree[1][0].hasChildren()
        !tree[1][0].childrenExist
        !tree[1][0].renderChildren
        ((ModelTreeItem) tree[1][0]).containerId == Folder.findByLabel('parent').id
        !((ModelTreeItem) tree[1][0]).deleted
        !((ModelTreeItem) tree[1][0]).finalised
        ((ModelTreeItem) tree[1][0]).superseded
        ((ModelTreeItem) tree[1][0]).documentationVersion == Version.from('2.1.0')
    }

    void 'test building container tree with 1 empty model and 1 non-empty at the top level and empty folders removed'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('parent'),
                                                authority: testAuthority)
        checkAndSave(basicModel)
        checkAndSave(basicModel2)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel, basicModel2]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> [basicModel.id]
        }
        service.modelServices = [basicModelService]

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        !tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        tree[0][0].childrenExist
        tree[0][0].hasChildren()
        !tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[1].size() == 1

        and:
        tree[1][0].label == 'dm2'
        tree[1][0].id == basicModel2.id
        tree[1][0].domainType == 'BasicModel'
        !tree[1][0].hasChildren()
        !tree[1][0].childrenExist
        !tree[1][0].renderChildren
        ((ModelTreeItem) tree[1][0]).containerId == Folder.findByLabel('parent').id
        !((ModelTreeItem) tree[1][0]).deleted
        !((ModelTreeItem) tree[1][0]).finalised
        !((ModelTreeItem) tree[1][0]).superseded
        ((ModelTreeItem) tree[1][0]).documentationVersion == Version.from('1.0.0')


    }

    void 'test building container tree with 2 empty models at different levels and empty folders removed'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('dmFolder'),
                                                authority: testAuthority)
        checkAndSave(basicModel)
        checkAndSave(basicModel2)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel, basicModel2]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
        }
        service.modelServices = [basicModelService]

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        !tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        !tree[0][0].hasChildren()
        !tree[0][0].childrenExist
        !tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[1].size() == 1

        and:
        tree[1][0].label == 'dmFolder'
        tree[1][0].id == Folder.findByLabel('dmFolder').id
        !tree[1][0].isEmptyContainerTree()
        tree[1][0].hasChildren()
        tree[1][0].childrenExist

        and:
        tree[1][0].size() == 1

        and:
        tree[1][0][0].label == 'dm2'
        tree[1][0][0].id == basicModel2.id
        tree[1][0][0].domainType == 'BasicModel'
        !tree[1][0][0].hasChildren()
        !tree[1][0][0].childrenExist
        !tree[1][0][0].renderChildren
        ((ModelTreeItem) tree[1][0][0]).containerId == Folder.findByLabel('dmFolder').id
        !((ModelTreeItem) tree[1][0][0]).deleted
        !((ModelTreeItem) tree[1][0][0]).finalised
        !((ModelTreeItem) tree[1][0][0]).superseded
        ((ModelTreeItem) tree[1][0][0]).documentationVersion == Version.from('1.0.0')


    }

    void 'test building container tree with 2 empty models at other different levels and empty folders removed'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('reader2Child'),
                                                authority: testAuthority)
        checkAndSave(basicModel)
        checkAndSave(basicModel2)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel, basicModel2]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
        }
        service.modelServices = [basicModelService]

        when:
        List<ContainerTreeItem> tree = service.buildContainerTree(Folder, testPolicy, true, true, true, true)

        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        !tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        !tree[0][0].hasChildren()
        !tree[0][0].childrenExist
        !tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[1].size() == 1

        and:
        tree[1][0].label == 'reader1Child'
        tree[1][0].id == Folder.findByLabel('reader1Child').id
        !tree[1][0].isEmptyContainerTree()
        tree[1][0].hasChildren()
        tree[1][0].childrenExist

        and:
        tree[1][0].size() == 1

        and:
        tree[1][0][0].label == 'reader2Child'
        tree[1][0][0].id == Folder.findByLabel('reader2Child').id
        !tree[1][0][0].isEmptyContainerTree()
        tree[1][0][0].hasChildren()
        tree[1][0][0].childrenExist

        and:
        tree[1][0][0].size() == 1

        and:
        tree[1][0][0][0].label == 'dm2'
        tree[1][0][0][0].id == basicModel2.id
        tree[1][0][0][0].domainType == 'BasicModel'
        !tree[1][0][0][0].hasChildren()
        !tree[1][0][0][0].childrenExist
        !tree[1][0][0][0].renderChildren
        ((ModelTreeItem) tree[1][0][0][0]).containerId == Folder.findByLabel('reader2Child').id
        !((ModelTreeItem) tree[1][0][0][0]).deleted
        !((ModelTreeItem) tree[1][0][0][0]).finalised
        !((ModelTreeItem) tree[1][0][0][0]).superseded
        ((ModelTreeItem) tree[1][0][0][0]).documentationVersion == Version.from('1.0.0')


    }

    void 'test findTreeCapableCatalogueItem'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Folder.list().collect {it.id}
        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
            get(basicModel.id) >> basicModel
            get(_) >> null
            handles(BasicModel) >> true
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService]

        when:
        def item = service.findTreeCapableCatalogueItem(BasicModel, null)

        then:
        !item

        when:
        item = service.findTreeCapableCatalogueItem(BasicModel, UUID.randomUUID())

        then:
        !item

        when:
        item = service.findTreeCapableCatalogueItem(BasicModel, basicModel.id)

        then:
        item
        item.id == basicModel.id
    }

    void 'test building catalogue item tree for model with no content'() {
        // this call should never actually be made but its worth testing
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)
        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> []
            handles(_) >> {Class clazz -> clazz == BasicModel}
            hasTreeTypeModelItems(basicModel) >> false
            findAllTreeTypeModelItemsIn(basicModel) >> []
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService]

        when:
        List<TreeItem> tree = service.buildCatalogueItemTree(basicModel)

        then:
        tree.isEmpty()
    }

    void 'test building catalogue item tree for model single level of content'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)

        BasicModelItem item1 = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item1)
        BasicModelItem item2 = new BasicModelItem(label: 'bmi2', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item2)

        basicModel.addToModelItems(item1)
        basicModel.addToModelItems(item2)
        checkAndSave(basicModel)

        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> [basicModel]
            handles(_) >> {Class clazz -> clazz == BasicModel}
            hasTreeTypeModelItems(basicModel) >> true
            findAllTreeTypeModelItemsIn(basicModel) >> [item1, item2]
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService]

        when:
        List<ModelItemTreeItem> tree = service.buildCatalogueItemTree(basicModel)

        then:
        tree.size() == 2
        tree.every {it.domainType == 'BasicModelItem'}
        tree.every {it.path == "/${basicModel.id}"}
        tree.every {it.rootId == basicModel.id}
        tree.every {it.parentId == basicModel.id}
        tree.every {it.depth == 1}
        tree.every {it.order == 0}
        tree.every {!it.renderChildren}

        and:
        tree[0].label == 'bmi1'
        tree[0].id == item1.id
        !tree[0].hasChildren()
        !tree[0].childrenExist

        and:
        tree[1].label == 'bmi2'
        tree[1].id == item2.id
        !tree[1].hasChildren()
        !tree[1].childrenExist
    }

    void 'test building catalogue item tree for model 2 levels of content'() {
        // we should record that there is a child but not get the child in the tree as this requires an additional call
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)

        BasicModelItem item1 = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item1)
        BasicModelItem item2 = new BasicModelItem(label: 'bmi2', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item2)
        BasicModelItem item3 = new BasicModelItem(label: 'bmi3', createdBy: admin.emailAddress, model: basicModel, parent: item2,
                                                  description: 'basic model item child')
        checkAndSave(item3)

        item2.addToChildModelItems(item3)
        checkAndSave(item2)
        basicModel.addToModelItems(item1)
        basicModel.addToModelItems(item2)
        checkAndSave(basicModel)

        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> [basicModel]
            handles(_) >> {Class clazz -> clazz == BasicModel}
            hasTreeTypeModelItems(basicModel) >> true
            findAllTreeTypeModelItemsIn(basicModel) >> [item1, item2]
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService]

        when:
        List<ModelItemTreeItem> tree = service.buildCatalogueItemTree(basicModel)

        then:
        tree.size() == 2
        tree.every {it.domainType == 'BasicModelItem'}
        tree.every {it.path == "/${basicModel.id}"}
        tree.every {it.rootId == basicModel.id}
        tree.every {it.parentId == basicModel.id}
        tree.every {it.depth == 1}
        tree.every {it.order == 0}
        tree.every {!it.renderChildren}

        and:
        tree[0].label == 'bmi1'
        tree[0].id == item1.id
        !tree[0].hasChildren()
        !tree[0].childrenExist

        and:
        tree[1].label == 'bmi2'
        tree[1].id == item2.id
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[1].size() == 0
    }

    void 'test building catalogue item tree for model item no content'() {
        // should not really be called but its worth testing
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)

        BasicModelItem item1 = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item1)
        BasicModelItem item2 = new BasicModelItem(label: 'bmi2', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item2)
        BasicModelItem item3 = new BasicModelItem(label: 'bmi3', createdBy: admin.emailAddress, model: basicModel, parent: item2,
                                                  description: 'basic model item child')
        checkAndSave(item3)

        item2.addToChildModelItems(item3)
        checkAndSave(item2)
        basicModel.addToModelItems(item1)
        basicModel.addToModelItems(item2)
        checkAndSave(basicModel)

        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> [basicModel]
            handles(_) >> {Class clazz -> clazz == BasicModel}
            hasTreeTypeModelItems(basicModel) >> true
            findAllTreeTypeModelItemsIn(basicModel) >> [item1, item2]
        }
        ModelItemService basicModelItemService = Stub() {
            getModelClass() >> BasicModelItem
            handles(_) >> {Class clazz -> clazz == BasicModelItem}
            hasTreeTypeModelItems(item1) >> false
            hasTreeTypeModelItems(item2) >> true
            hasTreeTypeModelItems(item3) >> false
            findAllTreeTypeModelItemsIn(item1) >> []
            findAllTreeTypeModelItemsIn(item2) >> [item3]
            findAllTreeTypeModelItemsIn(item3) >> []
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService, basicModelItemService]

        when:
        List<ModelItemTreeItem> tree = service.buildCatalogueItemTree(item1)

        then:
        tree.isEmpty()

        when:
        tree = service.buildCatalogueItemTree(item3)

        then:
        tree.isEmpty()
    }

    void 'test building catalogue item tree for model item with content'() {
        given:
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                               authority: testAuthority)
        checkAndSave(basicModel)

        BasicModelItem item1 = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item1)
        BasicModelItem item2 = new BasicModelItem(label: 'bmi2', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item2)
        BasicModelItem item3 = new BasicModelItem(label: 'bmi3', createdBy: admin.emailAddress, model: basicModel, parent: item2,
                                                  description: 'basic model item child')
        checkAndSave(item3)

        item2.addToChildModelItems(item3)
        checkAndSave(item2)
        basicModel.addToModelItems(item1)
        basicModel.addToModelItems(item2)
        checkAndSave(basicModel)

        ModelService basicModelService = Stub() {
            findAllReadableModels(_, true, true, true) >> [basicModel]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> [basicModel]
            handles(_) >> {Class clazz -> clazz == BasicModel}
            hasTreeTypeModelItems(basicModel) >> true
            findAllTreeTypeModelItemsIn(basicModel) >> [item1, item2]
        }
        ModelItemService basicModelItemService = Stub() {
            getModelClass() >> BasicModelItem
            handles(_) >> {Class clazz -> clazz == BasicModelItem}
            hasTreeTypeModelItems(item1) >> false
            hasTreeTypeModelItems(item2) >> true
            hasTreeTypeModelItems(item3) >> false
            findAllTreeTypeModelItemsIn(item1) >> []
            findAllTreeTypeModelItemsIn(item2) >> [item3]
            findAllTreeTypeModelItemsIn(item3) >> []
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService, basicModelItemService]

        when:
        List<ModelItemTreeItem> tree = service.buildCatalogueItemTree(item2)

        then:
        tree.size() == 1

        and:
        tree[0].label == 'bmi3'
        tree[0].id == item3.id
        tree[0].domainType == 'BasicModelItem'
        !tree[0].hasChildren()
        !tree[0].childrenExist
        !tree[0].renderChildren
        tree[0].path == "/${basicModel.id}/${item2.id}"
        tree[0].rootId == basicModel.id
        tree[0].parentId == item2.id
        tree[0].depth == 2
        ((ModelItemTreeItem) tree[0]).order == 0
    }

    void 'test building search tree 1 for search term [2]'() {
        given:
        UserSecurityPolicyManager testPolicy = configureSearchData()

        when:
        List<ContainerTreeItem> tree = service.buildContainerSearchTree(Folder, testPolicy, '2', null)

        /**
         * C
         *  dm1
         *      bmi2*
         * P
         *  r1C
         *      r2C*
         *  dm2*
         */
        then:
        tree.size() == 2

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        tree[0][0].childrenExist
        tree[0][0].hasChildren()
        tree[0][0].renderChildren
        !tree[0][0].path
        !tree[0][0].rootId
        !tree[0][0].parentId
        tree[0][0].depth == 0
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[0][0].size() == 1

        and:
        tree[0][0][0].label == 'bmi2'
        tree[0][0][0].id == item2.id
        !tree[0][0][0].hasChildren()
        !tree[0][0][0].childrenExist
        tree[0][0][0].domainType == 'BasicModelItem'
        tree[0][0][0].path == "/${basicModel.id}"
        tree[0][0][0].rootId == basicModel.id
        tree[0][0][0].parentId == basicModel.id
        tree[0][0][0].depth == 1
        tree[0][0][0].renderChildren
        ((ModelItemTreeItem) tree[0][0][0]).order == 0

        and:
        tree[1].label == 'parent'
        tree[1].id == Folder.findByLabel('parent').id
        !tree[1].isEmptyContainerTree()
        tree[1].hasChildren()
        tree[1].childrenExist

        and:
        tree[1].size() == 2
        tree[1].every {it.renderChildren}
        tree[1].every {!it.deleted}


        and:
        tree[1][0].label == 'reader1Child'
        tree[1][0].id == Folder.findByLabel('reader1Child').id
        ((ContainerTreeItem) tree[1][0]).isEmptyContainerTree()
        ((ContainerTreeItem) tree[1][0]).containerType == 'Folder'
        tree[1][0].domainType == 'Folder'
        tree[1][0].hasChildren()
        tree[1][0].childrenExist
        tree[1][0].depth == 1
        tree[1][0].path == "/${tree[1].id}"
        tree[1][0].rootId == tree[1].id
        tree[1][0].parentId == tree[1].id

        and:
        tree[1][0].size() == 1

        and:
        tree[1][0][0].label == 'reader2Child'
        tree[1][0][0].id == Folder.findByLabel('reader2Child').id
        ((ContainerTreeItem) tree[1][0][0]).isEmptyContainerTree()
        !tree[1][0][0].hasChildren()
        !tree[1][0][0].childrenExist
        tree[1][0][0].renderChildren
        !((ContainerTreeItem) tree[1][0][0]).deleted
        ((ContainerTreeItem) tree[1][0][0]).containerType == 'Folder'
        tree[1][0][0].domainType == 'Folder'
        tree[1][0][0].path == "/${tree[1].id}/${tree[1][0].id}"
        tree[1][0][0].rootId == tree[1].id
        tree[1][0][0].parentId == tree[1][0].id
        tree[1][0][0].depth == 2

        and:
        tree[1][1].label == 'dm2'
        tree[1][1].id == basicModel2.id
        tree[1][1].domainType == 'BasicModel'
        !tree[1][1].hasChildren()
        !tree[1][1].childrenExist
        tree[1][1].renderChildren
        ((ModelTreeItem) tree[1][1]).containerId == Folder.findByLabel('parent').id
        !((ModelTreeItem) tree[1][1]).deleted
        !((ModelTreeItem) tree[1][1]).finalised
        ((ModelTreeItem) tree[1][1]).superseded
        ((ModelTreeItem) tree[1][1]).documentationVersion == Version.from('2.1.0')
    }


    void 'test building search tree 2 for search term [2] domain type folder'() {
        given:
        UserSecurityPolicyManager testPolicy = configureSearchData()

        when:
        List<ContainerTreeItem> tree = service.buildContainerSearchTree(Folder, testPolicy, '2', 'Folder')

        /**
         * P
         *  r1C
         *      r2C*
         */
        then:
        tree.size() == 1

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'parent'
        tree[0].id == Folder.findByLabel('parent').id
        tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[0].size() == 1
        tree[0].every {it.renderChildren}
        tree[0].every {!it.deleted}


        and:
        tree[0][0].label == 'reader1Child'
        tree[0][0].id == Folder.findByLabel('reader1Child').id
        ((ContainerTreeItem) tree[0][0]).isEmptyContainerTree()
        ((ContainerTreeItem) tree[0][0]).containerType == 'Folder'
        tree[0][0].domainType == 'Folder'
        tree[0][0].hasChildren()
        tree[0][0].childrenExist
        tree[0][0].depth == 1
        tree[0][0].path == "/${tree[0].id}"
        tree[0][0].rootId == tree[0].id
        tree[0][0].parentId == tree[0].id

        and:
        tree[0][0].size() == 1

        and:
        tree[0][0][0].label == 'reader2Child'
        tree[0][0][0].id == Folder.findByLabel('reader2Child').id
        ((ContainerTreeItem) tree[0][0][0]).isEmptyContainerTree()
        !tree[0][0][0].hasChildren()
        !tree[0][0][0].childrenExist
        tree[0][0][0].renderChildren
        !((ContainerTreeItem) tree[0][0][0]).deleted
        ((ContainerTreeItem) tree[0][0][0]).containerType == 'Folder'
        tree[0][0][0].domainType == 'Folder'
        tree[0][0][0].path == "/${tree[0].id}/${tree[0][0].id}"
        tree[0][0][0].rootId == tree[0].id
        tree[0][0][0].parentId == tree[0][0].id
        tree[0][0][0].depth == 2

    }

    void 'test building search tree 3 for search term [2] domain type basic model'() {
        given:
        UserSecurityPolicyManager testPolicy = configureSearchData()

        when:
        List<ContainerTreeItem> tree = service.buildContainerSearchTree(Folder, testPolicy, '2', 'BasicModel')

        /**
         * P
         *  dm2*
         */
        then:
        tree.size() == 1

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'parent'
        tree[0].id == Folder.findByLabel('parent').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[0].size() == 1
        tree[0].every {it.renderChildren}
        tree[0].every {!it.deleted}

        and:
        tree[0][0].label == 'dm2'
        tree[0][0].id == basicModel2.id
        tree[0][0].domainType == 'BasicModel'
        !tree[0][0].hasChildren()
        !tree[0][0].childrenExist
        tree[0][0].renderChildren
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('parent').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        ((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('2.1.0')
    }

    void 'test building search tree 4 for search term [2] domain type basic model item'() {
        given:
        UserSecurityPolicyManager testPolicy = configureSearchData()

        when:
        List<ContainerTreeItem> tree = service.buildContainerSearchTree(Folder, testPolicy, '2', 'BasicModelItem')

        /**
         * C
         *  dm1
         *      bmi2*
         */
        then:
        tree.size() == 1

        and:
        tree.every {it.renderChildren}
        tree.every {!it.deleted}
        tree.every {it.containerType == 'Folder'}
        tree.every {it.domainType == 'Folder'}
        tree.every {!it.path}
        tree.every {!it.rootId}
        tree.every {!it.parentId}
        tree.every {it.depth == 0}

        and:
        tree[0].label == 'catalogue'
        tree[0].id == Folder.findByLabel('catalogue').id
        !tree[0].isEmptyContainerTree()
        tree[0].hasChildren()
        tree[0].childrenExist

        and:
        tree[0].size() == 1

        and:
        tree[0][0].label == 'dm1'
        tree[0][0].id == basicModel.id
        tree[0][0].domainType == 'BasicModel'
        tree[0][0].childrenExist
        tree[0][0].hasChildren()
        tree[0][0].renderChildren
        !tree[0][0].path
        !tree[0][0].rootId
        !tree[0][0].parentId
        tree[0][0].depth == 0
        ((ModelTreeItem) tree[0][0]).containerId == Folder.findByLabel('catalogue').id
        !((ModelTreeItem) tree[0][0]).deleted
        !((ModelTreeItem) tree[0][0]).finalised
        !((ModelTreeItem) tree[0][0]).superseded
        ((ModelTreeItem) tree[0][0]).documentationVersion == Version.from('1.0.0')

        and:
        tree[0][0].size() == 1

        and:
        tree[0][0][0].label == 'bmi2'
        tree[0][0][0].id == item2.id
        !tree[0][0][0].hasChildren()
        !tree[0][0][0].childrenExist
        tree[0][0][0].domainType == 'BasicModelItem'
        tree[0][0][0].path == "/${basicModel.id}"
        tree[0][0][0].rootId == basicModel.id
        tree[0][0][0].parentId == basicModel.id
        tree[0][0][0].depth == 1
        tree[0][0][0].renderChildren
        ((ModelItemTreeItem) tree[0][0][0]).order == 0
    }

    BasicModel basicModel
    BasicModel basicModel2
    BasicModelItem item1
    BasicModelItem item2
    BasicModelItem item3

    UserSecurityPolicyManager configureSearchData() {
        UserSecurityPolicyManager testPolicy

        testPolicy = Stub()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Folder) >> Folder.list().collect {it.id}

        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('parent'),
                                     documentationVersion: Version.from('2.1.0'),
                                     authority: testAuthority)
        basicModel2.addToVersionLinks(new VersionLink(linkType: VersionLinkType.SUPERSEDED_BY_FORK, targetModel: basicModel))
        checkAndSave(basicModel)
        checkAndSave(basicModel2)

        item1 = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item1)
        item2 = new BasicModelItem(label: 'bmi2', createdBy: admin.emailAddress, model: basicModel, description: 'basic model item')
        checkAndSave(item2)
        item3 = new BasicModelItem(label: 'bmi3', createdBy: admin.emailAddress, model: basicModel, parent: item2,
                                   description: 'basic model item child')
        checkAndSave(item3)

        item2.addToChildModelItems(item3)
        checkAndSave(item2)
        basicModel.addToModelItems(item1)
        basicModel.addToModelItems(item2)
        checkAndSave(basicModel)

        ModelService basicModelService = Stub() {
            getCatalogueItemClass() >> BasicModel
            findAllReadableModels(_, true, true, true) >> [basicModel, basicModel2]
            getModelClass() >> BasicModel
            findAllModelIdsWithTreeChildren(_) >> [basicModel]
            findAllSupersededModelIds(_) >> [basicModel2.id]
            handles(_) >> {Class clazz -> clazz == BasicModel}
            hasTreeTypeModelItems(basicModel) >> true
            findAllTreeTypeModelItemsIn(basicModel) >> [item1, item2]
            findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(testPolicy, '2', null) >> [basicModel2]
            findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(testPolicy, '2', 'Folder') >> []
            findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(testPolicy, '2', 'BasicModel') >> [basicModel2]
            getAll([basicModel.id]) >> [basicModel]
            shouldPerformSearchForTreeTypeCatalogueItems(null) >> true
            shouldPerformSearchForTreeTypeCatalogueItems('BasicModel') >> true
            shouldPerformSearchForTreeTypeCatalogueItems('BasicModelItem') >> false
        }
        ModelItemService basicModelItemService = Stub() {
            getCatalogueItemClass() >> BasicModelItem
            getModelItemClass() >> BasicModelItem
            handles(_) >> {Class clazz -> clazz == BasicModelItem}
            hasTreeTypeModelItems(item1) >> false
            hasTreeTypeModelItems(item2) >> true
            hasTreeTypeModelItems(item3) >> false
            findAllTreeTypeModelItemsIn(item1) >> []
            findAllTreeTypeModelItemsIn(item2) >> [item3]
            findAllTreeTypeModelItemsIn(item3) >> []
            shouldPerformSearchForTreeTypeCatalogueItems(null) >> true
            shouldPerformSearchForTreeTypeCatalogueItems('BasicModel') >> false
            shouldPerformSearchForTreeTypeCatalogueItems('BasicModelItem') >> true
            findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(testPolicy, '2', null) >> [item2]
            findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(testPolicy, '2', 'BasicModelItem') >> [item2]
        }
        service.modelServices = [basicModelService]
        service.catalogueItemServices = [basicModelService, basicModelItemService]

        folderService.findAllReadableContainersBySearchTerm(testPolicy, '2') >> [Folder.findByLabel('reader2Child')]
        testPolicy
    }


}
