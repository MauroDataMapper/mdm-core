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
package uk.ac.ox.softeng.maurodatamapper.datamodel.tree

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelTreeItem
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
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag

import java.time.OffsetDateTime

/**
 * This is to test the tree building with datamodel items in place.
 * We assume full public access, so all models are readable
 *
 * @since 30/10/2017
 */
@Slf4j
@Integration
@Rollback
@Tag('non-parallel')
class TreeItemServiceSpec extends BaseDataModelIntegrationSpec {

    TreeItemService treeItemService

    DataModel complexDataModel
    DataModel simpleDataModel

    @Override
    void preDomainDataSetup() {
        super.preDomainDataSetup()
        hibernateSearchIndexingService.purgeAllIndexes()
    }

    @Override
    void postDomainDataSetup() {
        hibernateSearchIndexingService.flushIndexes()
    }

    @SuppressWarnings('GroovyAssignabilityCheck')
    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        complexDataModel = buildComplexDataModel()
        simpleDataModel = buildSimpleDataModel()

        checkAndSave(new Folder(label: 'empty folder', createdBy: editor.emailAddress))
        Classifier testClassifier = new Classifier(label: 'integration test classifier', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        testClassifier.addToChildClassifiers(new Classifier(label: 'empty classifier', createdBy: StandardEmailAddress.INTEGRATION_TEST))
        checkAndSave(testClassifier)

        DataModel dataModel1 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dm1', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
            .addToClassifiers(testClassifier)
        DataModel dataModel2 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dm2', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
            .addToClassifiers(testClassifier)
        DataModel dataModel3 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dm3', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                             authority: testAuthority,
                                             deleted: true)
            .addToClassifiers(testClassifier)

        checkAndSave(dataModel1)
        checkAndSave(dataModel2)
        checkAndSave(dataModel3)

        DataType dt = new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'integration datatype')
        dataModel1.addToDataTypes(dt)
        DataElement dataElement = new DataElement(label: 'sdmelement', createdBy: StandardEmailAddress.INTEGRATION_TEST, dataType: dt)
        dataModel1.addToDataClasses(new DataClass(label: 'sdmclass', createdBy: StandardEmailAddress.INTEGRATION_TEST).addToDataElements(dataElement))
        dataModel1.modelVersion = Version.from('1.0.0')
        dataModel1.finalised = true
        dataModel1.dateFinalised = OffsetDateTime.now()

        checkAndSave(dataModel1)

        dataModel2.addToVersionLinks(createdBy: StandardEmailAddress.INTEGRATION_TEST, targetModel: dataModel1, linkType: VersionLinkType.NEW_MODEL_VERSION_OF)
        dataModel2.modelVersion = Version.from('2.0.0')
        dataModel2.finalised = true
        dataModel2.dateFinalised = OffsetDateTime.now()
        checkAndSave(dataModel2)

        dataModel3.addToVersionLinks(createdBy: StandardEmailAddress.INTEGRATION_TEST, targetModel: dataModel2, linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)

        checkAndSave(dataModel3)

        id = dataModel1.id
    }

    void 'F01 - test full tree building : doc, model, deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                          true, true, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}
        treeItems.any {it.label == 'empty folder'}

        when:
        def tf = treeItemService.buildDirectChildrenContainerTree(Folder, testFolder, PublicAccessSecurityPolicyManager.instance,
                                                                  true, true, true)
        then:
        tf
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
        tf.find {it.label == 'dm1'}.hasChildren()
        !tf.find {it.label == 'dm2'}.hasChildren()
        !tf.find {it.label == 'dm3'}.hasChildren()
    }

    void 'F02 - test full tree building : doc, model, deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                          true, true, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItemService.buildDirectChildrenContainerTree(Folder, testFolder, PublicAccessSecurityPolicyManager.instance,
                                                                  true, true, true)
        then:
        tf
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
        tf.any {it.label == 'dm1'}
        tf.any {it.label == 'dm2'}
        tf.any {it.label == 'dm3'}

        and:
        tf.find {it.label == 'dm1'}.hasChildren()
        !tf.find {it.label == 'dm2'}.hasChildren()
        !tf.find {it.label == 'dm3'}.hasChildren()
    }

    void 'F03 - test full tree building : doc, model, no deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                          true, true, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItemService.buildDirectChildrenContainerTree(Folder, testFolder, PublicAccessSecurityPolicyManager.instance,
                                                                  true, true, false)
        then:
        tf
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
        tf.any {it.label == 'dm1'}
        tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

        and:
        tf.find {it.label == 'dm1'}.hasChildren()
        !tf.find {it.label == 'dm2'}.hasChildren()
    }

    void 'F04 - test full tree building : doc, no model, no deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                          true, false, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItemService.buildDirectChildrenContainerTree(Folder, testFolder, PublicAccessSecurityPolicyManager.instance,
                                                                  true, false, false)
        then:
        tf
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
        !tf.any {it.label == 'dm1'}
        tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

        and:
        !tf.find {it.label == 'dm2'}.hasChildren()
    }

    void 'F05 - test full tree building : no doc, model, no deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                          false, true, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItemService.buildDirectChildrenContainerTree(Folder, testFolder, PublicAccessSecurityPolicyManager.instance,
                                                                  false, true, false)
        then:
        tf
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
        tf.any {it.label == 'dm1'}
        !tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}

        and:
        tf.find {it.label == 'dm1'}.hasChildren()
    }

    void 'F06 - test full tree building : no doc, no model, no deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                          false, false, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any {it.label == testFolder.label}

        when:
        def tf = treeItemService.buildDirectChildrenContainerTree(Folder, testFolder, PublicAccessSecurityPolicyManager.instance,
                                                                  false, false, false)
        then:
        tf
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
        !tf.any {it.label == 'dm1'}
        !tf.any {it.label == 'dm2'}
        !tf.any {it.label == 'dm3'}
    }

    void 'C01 - test full tree building : doc, model, deleted'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                          true, true, true)
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

    void 'C02 - test full tree building : doc, model, deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                          true, true, true)
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

    void 'C03 - test full tree building : doc, model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                          true, true, false)
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

    void 'C04 - test full tree building : doc, no model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                          true, false, false)
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

    void 'C05 - test full tree building : no doc, model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                          false, true, false)
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

    void 'C06 - test full tree building : no doc, no model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildRootContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                          false, false, false)
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
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null, false, false, false)

        then:
        treeItems.size() == 1

        when:
        def tf = treeItems.find {it.label == testFolder.label}
        then:
        tf
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

    void 'S02 - test searchterm "child" full tree building (DataClass label searching)'() {
        given:
        setupData()
        String searchTerm = 'child'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null, false, false, false)

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

    void 'S03 - test searchterm "string" full tree building (DataType label searching)'() {
        given:
        setupData()
        String searchTerm = 'string'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null, false, false, false)

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

    void 'S04 - test searchterm "ele" full tree building (DataElement label searching)'() {
        given:
        setupData()
        String searchTerm = 'ele'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null, false, false, false)

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

    void 'S05 - test searchterm "child" full tree building looking at DataModel domain'() {
        given:
        setupData()
        String searchTerm = 'child'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm,
                                                                            'DataModel', false, false, false)

        then:
        treeItems.size() == 0
    }

    void 'S06 - test searchterm "string" full tree building looking at DataType domain (DataType label searching)'() {
        given:
        setupData()
        String searchTerm = 'string'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm,
                                                                            'DataType', false, false, false)

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

    void 'S07 - test searchterm "ele" full tree building looking at DataElement domain (DataElement label searching)'() {
        given:
        setupData()
        String searchTerm = 'ele'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm,
                                                                            'DataElement', false, false, false)

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

    void 'R01 - test full model tree rendering'() {
        given:
        setupData()

        when:
        ModelTreeItem modelTreeItem = treeItemService.buildFullModelTree(complexDataModel, PublicAccessSecurityPolicyManager.instance)

        then:
        modelTreeItem
        modelTreeItem.children.size() == 7

        modelTreeItem.children.collect {it.label} == ['emptyclass', 'integer', 'string', 'child', 'yesnounknown', 'parent', 'content']

        when:
        ModelItemTreeItem et = modelTreeItem.children.find {it.label == 'yesnounknown'}

        then:
        et
        et.children.size() == 3
        et.children.collect {it.label} == ['Y', 'N', 'U']

        when:
        ModelItemTreeItem parent = modelTreeItem.children.find {it.label == 'parent'}

        then:
        parent
        parent.children.size() == 2
        parent.children.any {it.label == 'child' && it.domainType == 'DataClass'}
        parent.children.any {it.label == 'child' && it.domainType == 'DataElement'}

        when:
        ModelItemTreeItem content = modelTreeItem.children.find {it.label == 'content'}

        then:
        content
        content.children.size() == 2
        content.children.any {it.label == 'ele1' && it.domainType == 'DataElement'}
        content.children.any {it.label == 'element2' && it.domainType == 'DataElement'}
    }
}
