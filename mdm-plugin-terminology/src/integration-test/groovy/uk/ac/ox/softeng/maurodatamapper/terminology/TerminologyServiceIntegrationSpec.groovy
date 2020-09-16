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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
//@Stepwise
class TerminologyServiceIntegrationSpec extends BaseTerminologyIntegrationSpec {

    UserSecurityPolicyManager userSecurityPolicyManager

    @Override
    void setupDomainData() {
        log.debug('Setting up TerminologyServiceSpec unit')

        Terminology terminology1 = new Terminology(createdByUser: reader1, label: 'test database', folder: testFolder,
                                                   authority: testAuthority)
        Terminology terminology2 = new Terminology(createdByUser: reader2, label: 'test form', folder: testFolder,
                                                   authority: testAuthority)
        Terminology terminology3 = new Terminology(createdByUser: editor, label: 'test standard', folder: testFolder,
                                                   authority: testAuthority)

        checkAndSave(terminology1)
        checkAndSave(terminology2)
        checkAndSave(terminology3)

        id = terminology1.id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        terminologyService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<Terminology> terminologyList = terminologyService.list(max: 2, offset: 2, sort: 'dateCreated')

        then:
        terminologyList.size() == 2

        when:
        def dm1 = terminologyList[0]
        def dm2 = terminologyList[1]

        then:
        dm1.label == 'test database'
        dm1.modelType == 'Terminology'

        and:
        dm2.label == 'test form'
        dm2.modelType == 'Terminology'

    }

    void "test count"() {
        given:
        setupData()

        expect:
        terminologyService.count() == 5
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        terminologyService.count() == 5
        Terminology dm = terminologyService.get(id)

        when:
        terminologyService.delete(dm)
        terminologyService.save(dm)
        sessionFactory.currentSession.flush()

        then:
        Terminology.countByDeleted(false) == 4
        Terminology.countByDeleted(true) == 1
    }

    void "test save"() {
        given:
        setupData()

        when:
        Terminology terminology = new Terminology(createdByUser: reader2, label: 'saving test', folder: testFolder, authority: testAuthority)
        terminology = terminologyService.validate(terminology)

        then:
        !terminology.hasErrors()

        when:
        terminologyService.save(terminology)
        sessionFactory.currentSession.flush()

        then:
        terminology.id != null
    }


    void 'test finalising model'() {
        given:
        setupData()

        when:
        Terminology terminology = terminologyService.get(id)

        then:
        !terminology.finalised
        !terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')

        when:
        terminologyService.finaliseModel(terminology, admin, Version.from('1'), null)

        then:
        checkAndSave(terminology)

        when:
        terminology = terminologyService.get(id)

        then:
        terminology.finalised
        terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')
        terminology.modelVersion == Version.from('1')
    }


    void 'TSICA01 : test finding common ancestor of two terminologies'() {
        given:
        setupData()

        when:
        Terminology terminology = terminologyService.get(id)
        terminologyService.finaliseModel(terminology, admin, null, null)
        checkAndSave(terminology)

        then:
        terminology.branchName == 'main'

        when:
        def left = terminologyService.createNewBranchModelVersion('left', terminology, admin, false, userSecurityPolicyManager)
        def right = terminologyService.createNewBranchModelVersion('right', terminology, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(left)
        checkAndSave(right)
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def commonAncestor = terminologyService.commonAncestor(left, right)

        then:
        commonAncestor.branchName == 'main'
        commonAncestor.modelVersion == Version.from('1')
    }

    void 'TSILV01 : test finding latest version of a terminology'() {
        given:
        setupData()

        when:
        Terminology terminology = terminologyService.get(id)
        terminologyService.finaliseModel(terminology, admin, null, null)
        checkAndSave(terminology)

        then:
        terminology.branchName == 'main'

        when:
        def expectedModel = terminologyService.createNewBranchModelVersion('main', terminology, admin, false, userSecurityPolicyManager)
        def testModel = terminologyService.createNewBranchModelVersion('test', terminology, admin, false, userSecurityPolicyManager)
        terminologyService.finaliseModel(expectedModel, admin, null, null)
        checkAndSave(
            expectedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = terminologyService.createNewBranchModelVersion('main', terminology, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(testModel)
        checkAndSave(draftModel)
        testModel.modelVersion == null
        testModel.branchName == 'test'
        expectedModel.modelVersion == Version.from('2')
        expectedModel.branchName == 'main'
        draftModel.modelVersion == null
        draftModel.branchName == 'main'

        when:
        def latestVersion = terminologyService.latestVersion(testModel.label)

        then:
        latestVersion.branchName == 'main'
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = terminologyService.latestVersion(draftModel.label)

        then:
        latestVersion.branchName == 'main'
        latestVersion.modelVersion == Version.from('2')
    }

    void 'TSIMD01 : test finding merge difference between two terminologies'() {
        given:
        setupData()

        when:
        Terminology terminology = terminologyService.get(id)
        terminologyService.finaliseModel(terminology, admin, null, null)
        checkAndSave(terminology)

        then:
        terminology.branchName == 'main'

        when:
        def left = terminologyService.createNewBranchModelVersion('left', terminology, admin, false, userSecurityPolicyManager)
        def right = terminologyService.createNewBranchModelVersion('right', terminology, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(left)
        checkAndSave(right)
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def mergeDiff = terminologyService.mergeDiff(left, right)

        then:
        mergeDiff == [twoWayDiff: left.diff(right), threeWayDiff: [left: terminology.diff(left), right: terminology.diff(right)]]
    }

    void 'TSICMB01 : test getting current draft model on main branch from side branch'() {
        /*
        terminology (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        Terminology terminology = terminologyService.get(id)
        terminologyService.finaliseModel(terminology, admin, null, null)
        checkAndSave(terminology)

        then:
        terminology.branchName == 'main'

        when:
        def expectedModel = terminologyService.createNewBranchModelVersion('main', terminology, admin, false, userSecurityPolicyManager)
        def testModel = terminologyService.createNewBranchModelVersion('test', terminology, admin, false, userSecurityPolicyManager)
        terminologyService.finaliseModel(expectedModel, admin, null, null)
        checkAndSave(
            expectedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = terminologyService.createNewBranchModelVersion('main', terminology, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(testModel)
        checkAndSave(draftModel)
        testModel.modelVersion == null
        testModel.branchName == 'test'
        expectedModel.modelVersion == Version.from('2')
        expectedModel.branchName == 'main'
        draftModel.modelVersion == null
        draftModel.branchName == 'main'

        when:
        def currentMainBranch = terminologyService.currentMainBranch(testModel)

        then:
        currentMainBranch.id == draftModel.id
        currentMainBranch.label == testModel.label
        currentMainBranch.modelVersion == null
        currentMainBranch.branchName == 'main'
    }

    void 'TSIAB01 : test getting all draft models'() {
        /*
        terminology (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        Terminology terminology = terminologyService.get(id)
        terminologyService.finaliseModel(terminology, admin, null, null)
        checkAndSave(terminology)

        then:
        terminology.branchName == 'main'

        when:
        def expectedModel = terminologyService.createNewBranchModelVersion('main', terminology, admin, false, userSecurityPolicyManager)
        def testModel = terminologyService.createNewBranchModelVersion('test', terminology, admin, false, userSecurityPolicyManager)
        terminologyService.finaliseModel(expectedModel, admin, null, null)
        checkAndSave(
            expectedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = terminologyService.createNewBranchModelVersion('main', terminology, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(testModel)
        checkAndSave(draftModel)
        testModel.modelVersion == null
        testModel.branchName == 'test'
        expectedModel.modelVersion == Version.from('2')
        expectedModel.branchName == 'main'
        draftModel.modelVersion == null
        draftModel.branchName == 'main'

        when:
        def availableBranches = terminologyService.availableBranches(terminology.label)

        then:
        availableBranches.size() == 2
        availableBranches.each { it.id in [draftModel.id, testModel.id] }
        availableBranches.each { it.label == terminology.label }
    }
}

