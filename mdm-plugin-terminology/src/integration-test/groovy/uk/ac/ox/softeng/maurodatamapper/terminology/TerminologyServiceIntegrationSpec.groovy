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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.spockframework.util.Assert

@Slf4j
@Integration
@Rollback
class TerminologyServiceIntegrationSpec extends BaseTerminologyIntegrationSpec {

    @Override
    void setupDomainData() {
        log.debug('Setting up TerminologyServiceIntegrationSpec')

        Terminology terminology1 = new Terminology(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test database', folder: testFolder,
                                                   authority: testAuthority)
        Terminology terminology2 = new Terminology(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test form', folder: testFolder,
                                                   authority: testAuthority)
        Terminology terminology3 = new Terminology(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test standard', folder: testFolder,
                                                   authority: testAuthority)

        checkAndSave(terminology1)
        checkAndSave(terminology2)
        checkAndSave(terminology3)

        id = terminology1.id
    }

    Terminology getAndFinaliseDataModel(UUID idToFinalise = id) {
        Terminology terminology = terminologyService.get(idToFinalise)
        terminologyService.finaliseModel(terminology, admin, null, null, null)
        checkAndSave(terminology)
        terminology
    }

    UUID createAndSaveNewBranchModel(String branchName, Terminology base) {
        Terminology terminology = terminologyService.createNewBranchModelVersion(branchName, base, admin, false, adminSecurityPolicyManager)
        if (terminology.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, terminology)
            Assert.fail('Could not create new branch version')
        }
        check(terminology)
        terminologyService.saveModelWithContent(terminology)
        terminology.id
    }

    Terminology createSaveAndGetNewBranchModel(String branchName, Terminology base) {
        UUID id = createAndSaveNewBranchModel(branchName, base)
        terminologyService.get(id)
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
        Terminology terminology = new Terminology(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'saving test', folder: testFolder, authority: testAuthority)
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
        terminologyService.finaliseModel(terminology, admin, Version.from('1'), null, null)

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


    void 'TSF01 : test finding common ancestor of two terminologies'() {
        given:
        setupData()

        when:
        Terminology terminology = getAndFinaliseDataModel()

        then:
        terminology.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def left = createSaveAndGetNewBranchModel('left', terminology)
        def right = createSaveAndGetNewBranchModel('right', terminology)

        then:
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def commonAncestor = terminologyService.findCommonAncestorBetweenModels(left, right)

        then:
        commonAncestor.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        commonAncestor.modelVersion == Version.from('1')
    }

    void 'TSF02 : test finding latest version of a terminology'() {
        given:
        setupData()

        when:
        Terminology terminology = getAndFinaliseDataModel()

        then:
        terminology.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        Terminology expectedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, terminology)
        Terminology testModel = createSaveAndGetNewBranchModel('test', terminology)

        expectedModel = getAndFinaliseDataModel(expectedModel.id)
        Terminology draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, expectedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        expectedModel.modelVersion == Version.from('2')
        expectedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def latestVersion = terminologyService.findLatestFinalisedModelByLabel(testModel.label)

        then:
        latestVersion.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = terminologyService.findLatestFinalisedModelByLabel(draftModel.label)

        then:
        latestVersion.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = terminologyService.getLatestModelVersionByLabel(testModel.label)

        then:
        latestVersion == Version.from('2')

        when:
        latestVersion = terminologyService.getLatestModelVersionByLabel(draftModel.label)

        then:
        latestVersion == Version.from('2')
    }

    void 'TSF03 : test getting current draft model on main branch from side branch'() {
        /*
        terminology (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        Terminology terminology = getAndFinaliseDataModel()

        then:
        terminology.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def finalisedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, terminology)
        def testModel = createSaveAndGetNewBranchModel('test', terminology)
        finalisedModel = getAndFinaliseDataModel(finalisedModel.id)
        def draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, finalisedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        finalisedModel.modelVersion == Version.from('2')
        finalisedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def currentMainBranch = terminologyService.findCurrentMainBranchByLabel(testModel.label)

        then:
        currentMainBranch.id == draftModel.id
        currentMainBranch.label == testModel.label
        currentMainBranch.modelVersion == null
        currentMainBranch.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }

    void 'TSF04 : test getting all draft models'() {
        /*
        terminology (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        Terminology terminology = getAndFinaliseDataModel()

        then:
        terminology.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def finalisedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, terminology)
        def testModel = createSaveAndGetNewBranchModel('test', terminology)
        finalisedModel = getAndFinaliseDataModel(finalisedModel.id)
        def draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, finalisedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        finalisedModel.modelVersion == Version.from('2')
        finalisedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def availableBranches = terminologyService.findAllAvailableBranchesByLabel(terminology.label)

        then:
        availableBranches.size() == 2
        availableBranches.each {it.id in [draftModel.id, testModel.id]}
        availableBranches.each {it.label == terminology.label}
    }

    void 'TSM01 : test finding merge difference between two terminologies'() {
        given:
        setupData()

        when:
        Terminology terminology = getAndFinaliseDataModel()

        then:
        terminology.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def left = createSaveAndGetNewBranchModel('left', terminology)
        def right = createSaveAndGetNewBranchModel('right', terminology)

        then:
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        MergeDiff mergeDiff = terminologyService.getMergeDiffForModels(terminologyService.get(left.id), terminologyService.get(right.id))

        then:
        mergeDiff.size() == 0
    }
}

