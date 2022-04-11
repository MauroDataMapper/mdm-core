/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.dataflow.test.BaseDataFlowIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.dataflow.test.TestDataFlowSecuredUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 06/06/2018
 */
@Slf4j
@Integration
@Rollback
class DataFlowServiceSpec extends BaseDataFlowIntegrationSpec {

    DataFlowService dataFlowService

    TestDataFlowSecuredUserSecurityPolicyManager editorUserSecurityPolicyManager
    TestDataFlowSecuredUserSecurityPolicyManager readerUserSecurityPolicyManager
    TestDataFlowSecuredUserSecurityPolicyManager reader2UserSecurityPolicyManager
    TestDataFlowSecuredUserSecurityPolicyManager adminUserSecurityPolicyManager

    @Override
    void setupDomainData() {
        DataModel a = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'a', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        checkAndSave(a)
        DataModel b = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'b', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        checkAndSave(b)
        DataModel c = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'c', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        checkAndSave(c)

        DataFlow ab = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: a, target: b, label: 'a to b')
        DataFlow bc = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: b, target: c, label: 'b to c')

        checkAndSave(ab, bc)

        DataModel d = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'd', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        DataModel e = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'e', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        DataModel f = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'f', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        DataModel g = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'g', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        DataModel h = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'h', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        DataModel i = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'i', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)
        DataModel j = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'j', folder: folder, type: DataModelType.DATA_ASSET,
                                    authority: testAuthority)

        checkAndSave(d, e, f, g, h, i, j)

        DataFlow de = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: d, target: e, label: 'd to e')
        DataFlow ef = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: e, target: f, label: 'e to f')
        DataFlow fg = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: f, target: g, label: 'f to g')
        DataFlow gh = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: g, target: h, label: 'g to h')
        DataFlow ig = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: i, target: g, label: 'i to g')
        DataFlow ij = new DataFlow(createdBy: StandardEmailAddress.INTEGRATION_TEST, source: i, target: j, label: 'i to j')

        checkAndSave(de, ef, fg, gh, ig, ij)

        editorUserSecurityPolicyManager = new TestDataFlowSecuredUserSecurityPolicyManager(
            getEditor(), [], [], [], [a.id, b.id, c.id, d.id, e.id, f.id, g.id, h.id, i.id, j.id]
        )
        readerUserSecurityPolicyManager = new TestDataFlowSecuredUserSecurityPolicyManager(
            getReader1(), [], [c.id, f.id, i.id], [a.id, b.id, d.id, e.id, g.id, h.id, j.id], []
        )
        adminUserSecurityPolicyManager = new TestDataFlowSecuredUserSecurityPolicyManager(
            getAdmin(), [], [], [], [a.id, b.id, c.id, d.id, e.id, f.id, g.id, h.id, i.id, j.id]
        )
        reader2UserSecurityPolicyManager = new TestDataFlowSecuredUserSecurityPolicyManager(
            getReader1(), [], [a.id, b.id, c.id, d.id, e.id, f.id, g.id, h.id, i.id, j.id], [], []
        )

        id = dataFlow.id
    }

    void 'test get'() {
        given:
        setupData()

        expect:
        dataFlowService.get(id) != null
    }

    void 'test list'() {
        given:
        setupData()

        when:
        List<DataFlow> dataFlows = dataFlowService.list(max: 2, offset: 2)

        then:
        dataFlows.size() == 2

        and:
        dataFlows[0].label == 'b to c'

        and:
        dataFlows[1].label == 'd to e'

    }

    void 'test count'() {
        given:
        setupData()

        expect:
        dataFlowService.count() == 9
    }

    void 'test delete'() {
        given:
        setupData()

        expect:
        dataFlowService.count() == 9
        DataFlow dm = dataFlowService.get(id)

        when:
        dataFlowService.delete(dm)
        sessionFactory.currentSession.flush()

        then:
        dataFlowService.count() == 8
    }

    void 'Test readableByUser list'() {
        given:
        setupData()

        when:
        def readable = dataFlowService.findAllReadableByUser(editorUserSecurityPolicyManager)

        then:
        readable.size() == 8

        when:
        readable = dataFlowService.findAllReadableByUser(adminUserSecurityPolicyManager)

        then:
        readable.size() == 8

        when:
        readable = dataFlowService.findAllReadableByUser(readerUserSecurityPolicyManager)

        then:
        readable.size() == 3

        when:
        readable = dataFlowService.findAllReadableByUser(reader2UserSecurityPolicyManager)

        then:
        readable.size() == 0
    }

    void 'DFSC01 : Test building no chain from DM b with DF ab,bc'() {

        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('b').id)

        then:
        chain.size() == 2

        and:
        chain.find {it.label == 'a to b'}
        chain.find {it.label == 'b to c'}
    }

    void 'DFSC02 : Test building target line chain from DM a with DF ab,bc'() {

        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('a').id)

        then:
        chain.size() == 2

        and:
        chain.find {it.label == 'a to b'}
        chain.find {it.label == 'b to c'}
    }

    void 'DFSC03 : Test building source line chain from DM c with DF ab,bc'() {

        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('c').id)

        then:
        chain.size() == 2

        and:
        chain.find {it.label == 'a to b'}
        chain.find {it.label == 'b to c'}
    }

    void 'DFSC04 : Test building chain for d as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('d').id)

        then:
        chain.size() == 4

        and:
        chain.find {it.label == 'd to e'}
        chain.find {it.label == 'e to f'}
        chain.find {it.label == 'f to g'}
        chain.find {it.label == 'g to h'}
    }

    void 'DFSC05 : Test building chain for e as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('e').id)

        then:
        chain.size() == 4

        and:
        chain.find {it.label == 'd to e'}
        chain.find {it.label == 'e to f'}
        chain.find {it.label == 'f to g'}
        chain.find {it.label == 'g to h'}
    }

    void 'DFSC06 : Test building chain for f as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('f').id)

        then:
        chain.size() == 4

        and:
        chain.find {it.label == 'd to e'}
        chain.find {it.label == 'e to f'}
        chain.find {it.label == 'f to g'}
        chain.find {it.label == 'g to h'}
    }

    void 'DFSC07 : Test building chain for g as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('g').id)

        then:
        chain.size() == 5

        and:
        chain.find {it.label == 'd to e'}
        chain.find {it.label == 'e to f'}
        chain.find {it.label == 'f to g'}
        chain.find {it.label == 'g to h'}
        chain.find {it.label == 'i to g'}
    }

    void 'DFSC08 : Test building chain for h as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('h').id)

        then:
        chain.size() == 5

        and:
        chain.find {it.label == 'd to e'}
        chain.find {it.label == 'e to f'}
        chain.find {it.label == 'f to g'}
        chain.find {it.label == 'g to h'}
        chain.find {it.label == 'i to g'}
    }

    void 'DFSC09 : Test building chain for i as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('i').id)

        then:
        chain.size() == 3

        and:
        chain.find {it.label == 'g to h'}
        chain.find {it.label == 'i to g'}
        chain.find {it.label == 'i to j'}
    }

    void 'DFSC10 : Test building chain for j as editor'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(editorUserSecurityPolicyManager, DataModel.findByLabel('j').id)

        then:
        chain.size() == 1

        and:
        chain.find {it.label == 'i to j'}
    }

    void 'DFSC11 : Test building chain for d as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('d').id)

        then:
        chain.size() == 1

        and:
        chain.find {it.label == 'd to e'}
    }

    void 'DFSC12 : Test building chain for e as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('e').id)

        then:
        chain.size() == 1

        and:
        chain.find {it.label == 'd to e'}
    }

    void 'DFSC13 : Test building chain for f as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('f').id)

        then:
        chain.size() == 0
    }

    void 'DFSC14 : Test building chain for g as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('g').id)

        then:
        chain.size() == 1

        and:
        chain.find {it.label == 'g to h'}
    }

    void 'DFSC15 : Test building chain for h as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('h').id)

        then:
        chain.size() == 1

        and:
        chain.find {it.label == 'g to h'}
    }

    void 'DFSC16 : Test building chain for i as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('i').id)

        then:
        chain.size() == 0
    }

    void 'DFSC17 : Test building chain for j as reader1'() {
        given:
        setupData()

        when:
        def chain = dataFlowService.findAllReadableChainedByDataModel(readerUserSecurityPolicyManager, DataModel.findByLabel('j').id)

        then:
        chain.size() == 0
    }
}
