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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import spock.lang.Stepwise

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

/**
 * @since 30/10/2017
 */
@Stepwise
class TreeItemSpec extends BaseUnitSpec {
    BasicModel basicModel
    Folder misc

    def setup() {
        mockDomains(BasicModel, BasicModelItem, Folder, Authority)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        checkAndSave(misc)
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: misc, authority: testAuthority)
        checkAndSave(basicModel)
    }

    void 'test no children BasicModel tree'() {

        when:
        TreeItem treeItem = new ModelTreeItem(basicModel, 'folder')

        then:
        basicModel.id
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children?.isEmpty()
        !treeItem.hasChildren()
    }

    void 'test single level BasicModelItem children'() {
        given:
        basicModel.addToModelItems(label: 'dc1', createdBy: admin.emailAddress)
        basicModel.addToModelItems(label: 'dc2', createdBy: admin.emailAddress)
        basicModel.addToModelItems(label: 'dc3', createdBy: admin.emailAddress)
        checkAndSave(basicModel)

        when:
        TreeItem treeItem = new ModelTreeItem(basicModel, 'folder')

        then:
        basicModel.id
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children?.isEmpty()
        treeItem.hasChildren() // We know about the children but havent rendered them

        and:
        basicModel.modelItems.size() == 3

        when:
        treeItem.renderChildren = true

        then:
        // now we ask for rendering we have no children
        !treeItem.hasChildren()

        when:
        treeItem.recursivelyAddToChildren(basicModel.modelItems.collect {new ModelItemTreeItem(it, false)})

        then:
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children.size() == 3
        treeItem.hasChildren()

        when:
        TreeItem dc1 = treeItem.find('dc1')
        TreeItem dc2 = treeItem.find('dc2')
        TreeItem dc3 = treeItem.find('dc3')

        then:
        dc1.id
        dc1.domainType == 'BasicModelItem'
        dc1.children.isEmpty()
        !dc1.hasChildren()

        and:
        dc2.id
        dc2.domainType == 'BasicModelItem'
        dc2.children.isEmpty()
        !dc2.hasChildren()

        and:
        dc3.id
        dc3.domainType == 'BasicModelItem'
        dc3.children.isEmpty()
        !dc3.hasChildren()
    }

    void 'test double level BasicModelItem children'() {
        given:
        BasicModelItem dataClass2 = new BasicModelItem(label: 'dc2', createdBy: admin.emailAddress)
        dataClass2.addToChildModelItems(label: 'dc2dc1', createdBy: admin.emailAddress)
        dataClass2.addToChildModelItems(label: 'dc2dc2', createdBy: admin.emailAddress)
        basicModel.addToModelItems(label: 'dc1', createdBy: admin.emailAddress)
        basicModel.addToModelItems(dataClass2)
        basicModel.addToModelItems(label: 'dc3', createdBy: admin.emailAddress)
        checkAndSave(basicModel)

        when:
        TreeItem treeItem = new ModelTreeItem(basicModel, 'folder')

        then:
        basicModel.id
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children?.isEmpty()
        treeItem.hasChildren() // We know about the children but havent rendered them

        and:
        basicModel.modelItems.size() == 3

        when:
        treeItem.renderChildren = true

        then:
        // now we ask for rendering we have no children
        !treeItem.hasChildren()

        when:
        treeItem.recursivelyAddToChildren(
            basicModel.getAllModelItems().collect {new ModelItemTreeItem(it as ModelItem, (it as BasicModelItem).hasChildren())})
        //treeItem.recursivelyAddToChildren(dataClass2.childModelItems.collect {new TreeItem(it as ModelItem, (it as BasicModelItem).hasChildren())})

        then:
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children.size() == 3
        treeItem.hasChildren()

        when:
        TreeItem dc1 = treeItem.find('dc1')
        TreeItem dc2 = treeItem.find('dc2')
        TreeItem dc3 = treeItem.find('dc3')
        TreeItem dc4 = treeItem.find('dc2dc1')
        TreeItem dc5 = treeItem.find('dc2dc2')

        then:
        dc1.id
        dc1.domainType == 'BasicModelItem'
        dc1.children.isEmpty()
        !dc1.hasChildren()

        and:
        dc2.id
        dc2.domainType == 'BasicModelItem'
        dc2.children.size() == 2
        dc2.hasChildren()

        and:
        dc3.id
        dc3.domainType == 'BasicModelItem'
        dc3.children.isEmpty()
        !dc3.hasChildren()

        and:
        !dc4

        and:
        !dc5

        when:
        TreeItem dc2dc1 = dc2.find('dc2dc1')
        TreeItem dc2dc2 = dc2.find('dc2dc2')

        then:
        dc2dc1.id
        dc2dc1.domainType == 'BasicModelItem'
        dc2dc1.children.isEmpty()
        !dc2dc1.hasChildren()

        and:
        dc2dc2.id
        dc2dc2.domainType == 'BasicModelItem'
        dc2dc2.children.isEmpty()
        !dc2dc2.hasChildren()

    }

    void 'test multiple level BasicModelItem children'() {
        given:
        BasicModelItem dataClass1 = new BasicModelItem(label: 'dc1', createdBy: admin.emailAddress)
        dataClass1.addToChildModelItems(label: 'dc1dc1', createdBy: admin.emailAddress)
        dataClass1.addToChildModelItems(label: 'dc1dc2', createdBy: admin.emailAddress)
        dataClass1.addToChildModelItems(label: 'dc1dc3', createdBy: admin.emailAddress)
        dataClass1.addToChildModelItems(label: 'dc1dc4', createdBy: admin.emailAddress)

        BasicModelItem dataClass223 = new BasicModelItem(label: 'dc2dc2dc3', createdBy: admin.emailAddress)
        dataClass223.addToChildModelItems(label: 'dc2dc2dc3dc1', createdBy: admin.emailAddress)
        dataClass223.addToChildModelItems(label: 'dc2dc2dc3dc2', createdBy: admin.emailAddress)

        BasicModelItem dataClass22 = new BasicModelItem(label: 'dc2dc2', createdBy: admin.emailAddress)
        dataClass22.addToChildModelItems(label: 'dc2dc2dc1', createdBy: admin.emailAddress)
        dataClass22.addToChildModelItems(label: 'dc2dc2dc2', createdBy: admin.emailAddress)
        dataClass22.addToChildModelItems(dataClass223)

        BasicModelItem dataClass2 = new BasicModelItem(label: 'dc2', createdBy: admin.emailAddress)
        dataClass2.addToChildModelItems(label: 'dc2dc1', createdBy: admin.emailAddress)
        dataClass2.addToChildModelItems(dataClass22)

        basicModel.addToModelItems(dataClass1)
        basicModel.addToModelItems(dataClass2)
        basicModel.addToModelItems(label: 'dc3', createdBy: admin.emailAddress)
        checkAndSave(basicModel)

        when:
        TreeItem treeItem = new ModelTreeItem(basicModel, 'folder')

        then:
        basicModel.id
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children?.isEmpty()
        treeItem.hasChildren() // We know about the children but havent rendered them

        and:
        basicModel.getAllModelItems().size() == 14

        when:
        treeItem.renderChildren = true

        then:
        // now we ask for rendering we have no children
        !treeItem.hasChildren()

        when:
        treeItem.recursivelyAddToChildren(basicModel.getAllModelItems().collect {new ModelItemTreeItem(it, null)})

        then:
        treeItem.id == basicModel.id
        treeItem.label == 'dm1'
        treeItem.domainType == 'BasicModel'
        treeItem.children.size() == 3

        when:
        TreeItem dc1 = treeItem.find('dc1')
        TreeItem dc2 = treeItem.find('dc2')
        TreeItem dc3 = treeItem.find('dc3')

        then:
        dc1.id
        dc1.domainType == 'BasicModelItem'
        dc1.children.size() == 4

        and:
        dc2.id
        dc2.domainType == 'BasicModelItem'
        dc2.children.size() == 2

        and:
        dc3.id
        dc3.domainType == 'BasicModelItem'
        dc3.children.isEmpty()

        when:
        TreeItem dc2dc1 = dc2.find('dc2dc1')
        TreeItem dc2dc2 = dc2.find('dc2dc2')

        then:
        dc2dc1.id
        dc2dc1.domainType == 'BasicModelItem'
        dc2dc1.children.isEmpty()

        and:
        dc2dc2.id
        dc2dc2.domainType == 'BasicModelItem'
        dc2dc2.children.size() == 3

        when:
        TreeItem dc2dc2dc3 = dc2dc2.find('dc2dc2dc3')

        then:
        dc2dc2dc3.id
        dc2dc2dc3.domainType == 'BasicModelItem'
        dc2dc2dc3.children.size() == 2

    }
}
