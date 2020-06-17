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
package uk.ac.ox.softeng.maurodatamapper.datamodel.tree


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * This is to test the tree building with datamodel items in place.
 * We assume full public access, so all models are readable
 *
 * @since 30/10/2017
 */
@Slf4j
@Integration
@Rollback
class TreeItemServiceSpec extends BaseDataModelIntegrationSpec {

    TreeItemService treeItemService

    DataModel complexDataModel
    DataModel simpleDataModel

    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        complexDataModel = buildComplexDataModel()
        simpleDataModel = buildSimpleDataModel()

        checkAndSave(new Folder(label: 'empty folder', createdBy: editor.emailAddress))

        Classifier testClassifier = new Classifier(label: 'integration test classifier', createdByUser: admin)
        testClassifier.addToChildClassifiers(new Classifier(label: 'empty classifier', createdByUser: admin))
        checkAndSave(testClassifier)

        DataModel dataModel1 = new DataModel(createdByUser: reader1, label: 'tdm', type: DataModelType.DATA_ASSET, folder: testFolder)
            .addToClassifiers(testClassifier)
        DataModel dataModel2 = new DataModel(createdByUser: reader2, label: 'dm2', type: DataModelType.DATA_ASSET, folder: testFolder)
            .addToClassifiers(testClassifier)
        DataModel dataModel3 = new DataModel(createdByUser: editor, label: 'dm3', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                             deleted: true)
            .addToClassifiers(testClassifier)

        checkAndSave(dataModel1)
        checkAndSave(dataModel2)
        checkAndSave(dataModel3)

        DataType dt = new PrimitiveType(createdByUser: admin, label: 'integration datatype')
        dataModel1.addToDataTypes(dt)
        DataElement dataElement = new DataElement(label: 'sdmelement', createdByUser: editor, dataType: dt)
        dataModel1.addToDataClasses(new DataClass(label: 'sdmclass', createdByUser: editor).addToDataElements(dataElement))

        checkAndSave(dataModel1)

        dataModel1.addToVersionLinks(createdByUser: admin, targetModel: dataModel2, linkType: VersionLinkType.SUPERSEDED_BY_MODEL)
        dataModel2.addToVersionLinks(createdByUser: admin, targetModel: dataModel3, linkType: VersionLinkType.SUPERSEDED_BY_DOCUMENTATION)

        checkAndSave(dataModel1)
        checkAndSave(dataModel2)

        id = dataModel1.id
    }

    void 'F01 - test full tree building'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}
        treeItems.any {it.label == 'empty folder'}

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 5

        when:
        def tree1 = tf.find {it.label == 'Complex Test DataModel'}
        def tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.hasChildren()

        and:
        tree1
        tree1.hasChildren()

        and:
        tf.find {it.label == 'tdm'}.hasChildren()
        !tf.find {it.label == 'dm2'}.hasChildren()
        !tf.find {it.label == 'dm3'}.hasChildren()
    }

    void 'F02 - test full tree building remove empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 5

        when:
        def tree1 = tf.find {it.label == 'Complex Test DataModel'}
        def tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.hasChildren()

        and:
        tree1
        tree1.hasChildren()

        and:
        tf.any {it.label == 'tdm'}
        tf.any {it.label == 'dm2'}
        tf.any {it.label == 'dm3'}

        and:
        tf.find {it.label == 'tdm'}.hasChildren()
        !tf.find {it.label == 'dm2'}.hasChildren()
        !tf.find {it.label == 'dm3'}.hasChildren()
    }

    void 'F03 - test full tree building remove empty containers, deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 4

        when:
        def tree1 = tf.find {it.label == 'Complex Test DataModel'}
        def tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.hasChildren()

        and:
        tree1
        tree1.hasChildren()

        and:
        tf.any {it.label == 'tdm'}
        tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

        and:
        tf.find {it.label == 'tdm'}.hasChildren()
        !tf.find {it.label == 'dm2'}.hasChildren()
    }

    void 'F03 - test full tree building remove empty containers, deleted, model superseded'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, false,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 3

        when:
        def tree1 = tf.find {it.label == 'Complex Test DataModel'}
        def tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.hasChildren()

        and:
        tree1
        tree1.hasChildren()

        and:
        !tf.any {it.label == 'tdm'}
        tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

        and:
        !tf.find {it.label == 'dm2'}.hasChildren()
    }

    void 'F04 - test full tree building remove empty containers, deleted, document superseded'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      false, true,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 3

        when:
        def tree1 = tf.find {it.label == 'Complex Test DataModel'}
        def tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.hasChildren()

        and:
        tree1
        tree1.hasChildren()

        and:
        tf.any {it.label == 'tdm'}
        !tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

        and:
        tf.find {it.label == 'tdm'}.hasChildren()
    }

    void 'F05 - test full tree building remove all'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      false, false,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 2

        when:
        def tree1 = tf.find {it.label == 'Complex Test DataModel'}
        def tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.hasChildren()

        and:
        tree1
        tree1.hasChildren()

        and:
        !tf.any {it.label == 'tdm'}
        !tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

    }

    void 'C01 - test full tree building'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, false)
        then:
        treeItems.size() == 4

        and:
        treeItems.any {it.label == 'test classifier'}
        treeItems.any {it.label == 'integration test classifier'}
        treeItems.any {it.label == 'test classifier2'}
        treeItems.any {it.label == 'test classifier simple'}
        !treeItems.any {it.label == 'empty classifier'}

        and:
        !treeItems.find {it.label == 'test classifier'}.hasChildren()
        treeItems.find {it.label == 'integration test classifier'}.hasChildren()
        !treeItems.find {it.label == 'test classifier2'}.hasChildren()
        !treeItems.find {it.label == 'test classifier simple'}.hasChildren()
    }

    void 'C02 - test full tree building remove empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, true)
        then:
        treeItems.size() == 4

        and:
        treeItems.any {it.label == 'test classifier'}
        treeItems.any {it.label == 'integration test classifier'}
        treeItems.any {it.label == 'test classifier2'}
        treeItems.any {it.label == 'test classifier simple'}
        !treeItems.any {it.label == 'empty classifier'}

        and:
        !treeItems.find {it.label == 'test classifier'}.hasChildren()
        treeItems.find {it.label == 'integration test classifier'}.hasChildren()
        !treeItems.find {it.label == 'test classifier2'}.hasChildren()
        !treeItems.find {it.label == 'test classifier simple'}.hasChildren()
    }

    void 'C03 - test full tree building remove empty containers, deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      false, true)
        then:
        treeItems.size() == 4

        and:
        treeItems.any {it.label == 'test classifier'}
        treeItems.any {it.label == 'integration test classifier'}
        treeItems.any {it.label == 'test classifier2'}
        treeItems.any {it.label == 'test classifier simple'}
        !treeItems.any {it.label == 'empty classifier'}

        and:
        !treeItems.find {it.label == 'test classifier'}.hasChildren()
        treeItems.find {it.label == 'integration test classifier'}.hasChildren()
        !treeItems.find {it.label == 'test classifier2'}.hasChildren()
        !treeItems.find {it.label == 'test classifier simple'}.hasChildren()
    }

    void 'C03 - test full tree building remove empty containers, deleted, model superseded'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, false,
                                                                      false, true)
        then:
        treeItems.size() == 4

        and:
        treeItems.any {it.label == 'test classifier'}
        treeItems.any {it.label == 'integration test classifier'}
        treeItems.any {it.label == 'test classifier2'}
        treeItems.any {it.label == 'test classifier simple'}
        !treeItems.any {it.label == 'empty classifier'}

        and:
        !treeItems.find {it.label == 'test classifier'}.hasChildren()
        treeItems.find {it.label == 'integration test classifier'}.hasChildren()
        !treeItems.find {it.label == 'test classifier2'}.hasChildren()
        !treeItems.find {it.label == 'test classifier simple'}.hasChildren()
    }

    void 'C04 - test full tree building remove empty containers, deleted, document superseded'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      false, true,
                                                                      false, true)
        then:
        treeItems.size() == 4

        and:
        treeItems.any {it.label == 'test classifier'}
        treeItems.any {it.label == 'integration test classifier'}
        treeItems.any {it.label == 'test classifier2'}
        treeItems.any {it.label == 'test classifier simple'}
        !treeItems.any {it.label == 'empty classifier'}

        and:
        !treeItems.find {it.label == 'test classifier'}.hasChildren()
        treeItems.find {it.label == 'integration test classifier'}.hasChildren()
        !treeItems.find {it.label == 'test classifier2'}.hasChildren()
        !treeItems.find {it.label == 'test classifier simple'}.hasChildren()
    }

    void 'C05 - test full tree building remove all'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      false, false,
                                                                      false, true)
        then:
        treeItems.size() == 4

        and:
        treeItems.any {it.label == 'test classifier'}
        treeItems.any {it.label == 'integration test classifier'}
        treeItems.any {it.label == 'test classifier2'}
        treeItems.any {it.label == 'test classifier simple'}
        !treeItems.any {it.label == 'empty classifier'}

        and:
        !treeItems.find {it.label == 'test classifier'}.hasChildren()
        treeItems.find {it.label == 'integration test classifier'}.hasChildren()
        !treeItems.find {it.label == 'test classifier2'}.hasChildren()
        !treeItems.find {it.label == 'test classifier simple'}.hasChildren()

    }

    void 'S01 - test searchterm "test" full tree building (label searching)'() {
        given:
        setupData()
        String searchTerm = 'test'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then:
        treeItems.size() == 1

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 2

        when:
        TreeItem tree1 = tf.find {it.label == 'Complex Test DataModel'}
        TreeItem tree2 = tf.find {it.label == 'Simple Test DataModel'}

        then:
        tree2
        tree2.children.isEmpty()

        and:
        tree1
        tree1.children.isEmpty()
    }

    void 'S02 - test searchterm "mdk1" full tree building (metadata searching)'() {
        given:
        setupData()
        String searchTerm = 'mdk1'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then: 'Tree search no longer searches metadata but labels only'
        treeItems.size() == 0

        /*
        when:
        TreeItem tree1 = treeItems.find {it.label == 'test'}
        TreeItem tree2 = treeItems.find {it.label == 'test simple'}

        then:
        tree2
        tree2.children.isEmpty()

        and:
        tree1
        tree1.children.size() == 1
        tree1.find('content')
        */
    }

    void 'S03 - test searchterm "desc" full tree building (description searching)'() {
        given:
        setupData()
        String searchTerm = 'desc'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then: 'Tree search no longer searches metadata but labels only'
        treeItems.size() == 0

        /*
        when:
        TreeItem tree1 = treeItems.find {it.label == 'test'}

        then:
        tree1
        tree1.children.size() == 1
        */

    }

    void 'S04 - test searchterm "child" full tree building (DataClass label searching)'() {
        given:
        setupData()
        String searchTerm = 'child'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then:
        treeItems.size() == 1

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 1

        when:
        TreeItem tree1 = tf.find {it.label == 'Complex Test DataModel'}

        then:
        tree1
        tree1.children.size() == 1

        when:
        TreeItem tree3 = tree1.find 'parent'

        then:
        tree3
        tree3.children.size() == 1

        when:
        TreeItem tree6 = tree3.find('child')

        then:
        tree6
        tree6.children.isEmpty()
    }

    void 'S05 - test searchterm "string" full tree building (DataType label searching)'() {
        given:
        setupData()
        String searchTerm = 'string'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then: 'no domain provided should not search datatypes'
        treeItems.size() == 0

        /*
        when:
        TreeItem tree1 = treeItems.find {it.label == 'test'}

        then:
        tree1
        tree1.children.isEmpty()
        */
    }

    void 'S06 - test searchterm "ele" full tree building (DataElement label searching)'() {
        given:
        setupData()
        String searchTerm = 'ele'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then: 'no domain provided should not search data elements'
        treeItems.size() == 0

        /*
        when:
        TreeItem tree1 = treeItems.find {it.label == 'test'}

        then:
        tree1
        tree1.children.size() == 1

        when:
        TreeItem tree5 = tree1.find 'content'

        then:
        tree5
        tree5.children.isEmpty()
        */
    }

    void 'S07 - test searchterm "child" full tree building looking at DataModel domain'() {
        given:
        setupData()
        String searchTerm = 'child'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm,
                                                                            'DataModel')

        then:
        treeItems.size() == 0
    }

    void 'S08 - test searchterm "string" full tree building looking at DataType domain (DataType label searching)'() {
        given:
        setupData()
        String searchTerm = 'string'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm,
                                                                            'DataType')

        then: 'no domain provided should not search datatypes'
        treeItems.size() == 1

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 1

        when:
        TreeItem tree1 = tf.find {it.label == 'Complex Test DataModel'}

        then:
        tree1

    }

    void 'S09 - test searchterm "ele" full tree building looking at DataElement domain (DataElement label searching)'() {
        given:
        setupData()
        String searchTerm = 'ele'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm,
                                                                            'DataElement')

        then: 'no domain provided should not search data elements'
        treeItems.size() == 1

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
        tf.hasChildren()
        tf.size() == 1

        when:
        TreeItem tree1 = tf.find {it.label == 'Complex Test DataModel'}

        then:
        tree1
    }
}