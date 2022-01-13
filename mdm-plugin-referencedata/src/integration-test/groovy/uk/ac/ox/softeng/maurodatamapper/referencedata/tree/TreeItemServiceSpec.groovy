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
package uk.ac.ox.softeng.maurodatamapper.referencedata.tree

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec
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
class TreeItemServiceSpec extends BaseReferenceDataModelIntegrationSpec {

    TreeItemService treeItemService

    ReferenceDataModel simpleDataModel
    ReferenceDataModel complexDataModel

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
        log.debug('Setting up TreeItemServiceSpec unit')

        simpleDataModel = buildExampleReferenceDataModel()
        complexDataModel = buildSecondExampleReferenceDataModel()

        checkAndSave(new Folder(label: 'empty folder', createdBy: editor.emailAddress))
        Classifier testClassifier = new Classifier(label: 'integration test classifier', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        testClassifier.addToChildClassifiers(new Classifier(label: 'empty classifier', createdBy: StandardEmailAddress.INTEGRATION_TEST))
        checkAndSave(testClassifier)

        ReferenceDataModel dataModel1 = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dm1', folder: testFolder, authority: testAuthority)
            .addToClassifiers(testClassifier)
        ReferenceDataModel dataModel2 = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dm2', folder: testFolder, authority: testAuthority)
            .addToClassifiers(testClassifier)
        ReferenceDataModel dataModel3 = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dm3', folder: testFolder, authority: testAuthority,
                                                               deleted: true)
            .addToClassifiers(testClassifier)

        checkAndSave(dataModel1)
        checkAndSave(dataModel2)
        checkAndSave(dataModel3)

        ReferenceDataType dt = new ReferencePrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'integration datatype')
        dataModel1.addToReferenceDataTypes(dt)
        ReferenceDataElement dataElement = new ReferenceDataElement(label: 'sdmelement', createdBy: StandardEmailAddress.INTEGRATION_TEST, referenceDataType: dt)
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

    void 'F01 - test full tree building : doc, model, deleted, empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == testFolder.label }
        treeItems.any { it.label == 'empty folder' }

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 5

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        !tree2.hasChildren()

        and:
        tree1
        !tree1.hasChildren()

        and:
        !tf.find { it.label == 'dm1' }.hasChildren()
        !tf.find { it.label == 'dm2' }.hasChildren()
        !tf.find { it.label == 'dm3' }.hasChildren()
    }

    void 'F02 - test full tree building : doc, model, deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any { it.label == testFolder.label }
        !treeItems.any { it.label == 'empty folder' }

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 5

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        !tree2.hasChildren()

        and:
        tree1
        !tree1.hasChildren()

        and:
        tf.any { it.label == 'dm1' }
        tf.any { it.label == 'dm2' }
        tf.any { it.label == 'dm3' }

        and:
        !tf.find { it.label == 'dm1' }.hasChildren()
        !tf.find { it.label == 'dm2' }.hasChildren()
        !tf.find { it.label == 'dm3' }.hasChildren()
    }

    void 'F03 - test full tree building : doc, model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any { it.label == testFolder.label }
        !treeItems.any { it.label == 'empty folder' }

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 4

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        !tree2.hasChildren()

        and:
        tree1
        !tree1.hasChildren()

        and:
        tf.any { it.label == 'dm1' }
        tf.any { it.label == 'dm2' }
        !tf.any { it.label == 'dm3' }

        and:
        !tf.find { it.label == 'dm1' }.hasChildren()
        !tf.find { it.label == 'dm2' }.hasChildren()
    }

    void 'F04 - test full tree building : doc, no model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      true, false,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any { it.label == testFolder.label }
        !treeItems.any { it.label == 'empty folder' }

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 3

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        !tree2.hasChildren()

        and:
        tree1
        !tree1.hasChildren()

        and:
        !tf.any { it.label == 'dm1' }
        tf.any { it.label == 'dm2' }
        !tf.any { it.label == 'dm3' }

        and:
        !tf.find { it.label == 'dm2' }.hasChildren()
    }

    void 'F05 - test full tree building : no doc, model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      false, true,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any { it.label == testFolder.label }
        !treeItems.any { it.label == 'empty folder' }

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 3

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        !tree2.hasChildren()

        and:
        tree1
        !tree1.hasChildren()

        and:
        tf.any { it.label == 'dm1' }
        !tf.any { it.label == 'dm2' }
        !tf.any { it.label == 'dm3' }

        and:
        !tf.find { it.label == 'dm1' }.hasChildren()
    }

    void 'F06 - test full tree building : no doc, no model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Folder, PublicAccessSecurityPolicyManager.instance,
                                                                      false, false,
                                                                      false, true)
        then:
        treeItems.size() == 1

        and:
        treeItems.any { it.label == testFolder.label }
        !treeItems.any { it.label == 'empty folder' }

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 2

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        !tree2.hasChildren()

        and:
        tree1
        !tree1.hasChildren()

        and:
        !tf.any { it.label == 'dm1' }
        !tf.any { it.label == 'dm2' }
        !tf.any { it.label == 'dm3' }

    }

    void 'C01 - test full tree building : doc, model, deleted, empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, false)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == 'integration test classifier' }
        treeItems.any { it.label == 'test classifier simple' }
        !treeItems.any { it.label == 'empty classifier' }

        and:
        treeItems.find { it.label == 'integration test classifier' }.hasChildren()
        !treeItems.find { it.label == 'test classifier simple' }.hasChildren()
    }

    void 'C02 - test full tree building : doc, model, deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      true, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == 'integration test classifier' }
        treeItems.any { it.label == 'test classifier simple' }
        !treeItems.any { it.label == 'empty classifier' }

        and:
        treeItems.find { it.label == 'integration test classifier' }.hasChildren()
        !treeItems.find { it.label == 'test classifier simple' }.hasChildren()
    }

    void 'C03 - test full tree building : doc, model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, true,
                                                                      false, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == 'integration test classifier' }
        treeItems.any { it.label == 'test classifier simple' }
        !treeItems.any { it.label == 'empty classifier' }

        and:
        treeItems.find { it.label == 'integration test classifier' }.hasChildren()
        !treeItems.find { it.label == 'test classifier simple' }.hasChildren()
    }

    void 'C04 - test full tree building : doc, no model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      true, false,
                                                                      false, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == 'integration test classifier' }
        treeItems.any { it.label == 'test classifier simple' }
        !treeItems.any { it.label == 'empty classifier' }

        and:
        treeItems.find { it.label == 'integration test classifier' }.hasChildren()
        !treeItems.find { it.label == 'test classifier simple' }.hasChildren()
    }

    void 'C05 - test full tree building : no doc, model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      false, true,
                                                                      false, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == 'integration test classifier' }
        treeItems.any { it.label == 'test classifier simple' }
        !treeItems.any { it.label == 'empty classifier' }

        and:
        treeItems.find { it.label == 'integration test classifier' }.hasChildren()
        !treeItems.find { it.label == 'test classifier simple' }.hasChildren()
    }

    void 'C06 - test full tree building : no doc, no model, no deleted, no empty containers'() {
        given:
        setupData()

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerTree(Classifier, PublicAccessSecurityPolicyManager.instance,
                                                                      false, false,
                                                                      false, true)
        then:
        treeItems.size() == 2

        and:
        treeItems.any { it.label == 'integration test classifier' }
        treeItems.any { it.label == 'test classifier simple' }
        !treeItems.any { it.label == 'empty classifier' }

        and:
        treeItems.find { it.label == 'integration test classifier' }.hasChildren()
        !treeItems.find { it.label == 'test classifier simple' }.hasChildren()

    }

    void 'S01 - test searchterm "Reference" full tree building'() {
        given:
        setupData()
        String searchTerm = 'Reference'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then:
        treeItems.size() == 1

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 2

        when:
        def tree1 = tf.find { it.label == 'Simple Reference Data Model' }
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        tree2.children.isEmpty()

        and:
        tree1
        tree1.children.isEmpty()
    }


    void 'S02 - test searchterm "Second" full tree building'() {
        given:
        setupData()
        String searchTerm = 'Second'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then:
        treeItems.size() == 1

        when:
        def tf = treeItems.find { it.label == testFolder.label }

        then:
        tf
        tf.hasChildren()
        tf.size() == 1
        !tf.find { it.label == 'Simple Reference Data Model' }
        tf.find { it.label == 'Second Simple Reference Data Model' }

        when:
        def tree2 = tf.find { it.label == 'Second Simple Reference Data Model' }

        then:
        tree2
        tree2.children.isEmpty()
    }

    void 'S03 - test searchterm "integration datatype" full tree building on ReferenceDataType'() {
        given:
        setupData()
        String searchTerm = 'integration datatype'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, null)

        then: 'no domain provided should not search datatypes'
        treeItems.size() == 0
    }

    void 'S06 - test searchterm "integration datatype" full tree building on ReferenceDataType looking at ReferenceDataType domain'() {
        given:
        setupData()
        String searchTerm = 'integration datatype'

        when:
        List<TreeItem> treeItems = treeItemService.buildContainerSearchTree(Folder, PublicAccessSecurityPolicyManager.instance, searchTerm, 'ReferenceDataType')

        then: 'search returns 1 tree item'
        treeItems.size() == 1

        when:
        def tf = treeItems.find { it.label == testFolder.label }
        then:
        tf
        tf.hasChildren()
        tf.size() == 1

        when:
        TreeItem tree1 = tf.find { it.label == 'dm1' }

        then:
        tree1
    }

}
