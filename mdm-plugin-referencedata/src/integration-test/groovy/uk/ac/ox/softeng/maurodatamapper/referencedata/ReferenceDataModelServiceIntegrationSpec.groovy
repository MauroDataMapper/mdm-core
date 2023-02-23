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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.ReferenceDataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag
import org.spockframework.util.Assert

@Slf4j
@Integration
@Rollback
class ReferenceDataModelServiceIntegrationSpec extends BaseReferenceDataModelIntegrationSpec {

    ReferenceDataModel referenceDataModel
    ReferenceDataModel secondReferenceDataModel
    ReferenceDataModelService referenceDataModelService

    UserSecurityPolicyManager userSecurityPolicyManager

    @Override
    void setupDomainData() {
        log.debug('Setting up ReferenceDataModelServiceSpec unit')

        referenceDataModel = buildExampleReferenceDataModel()
        secondReferenceDataModel = buildSecondExampleReferenceDataModel()

        ReferenceDataModel referenceDataModel1 = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test database', folder: testFolder,
                                                                        authority: testAuthority)
        ReferenceDataModel referenceDataModel2 = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test form', folder: testFolder,
                                                                        authority: testAuthority)
        ReferenceDataModel referenceDataModel3 = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test standard', folder: testFolder,
                                                                        authority: testAuthority)

        checkAndSave(referenceDataModel1)
        checkAndSave(referenceDataModel2)
        checkAndSave(referenceDataModel3)

        ReferenceDataType referenceDataType = new ReferencePrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'string')
        referenceDataModel1.addToReferenceDataTypes(referenceDataType)
        ReferenceDataElement referenceDataElement = new ReferenceDataElement(label: 'sdmelement', createdBy: StandardEmailAddress.INTEGRATION_TEST,
                                                                             referenceDataType: referenceDataType)
        referenceDataModel1.addToReferenceDataElements(referenceDataElement)

        checkAndSave(referenceDataModel1)

        id = referenceDataModel1.id
    }

    protected ReferenceDataModel checkAndSaveNewVersion(ReferenceDataModel referenceDataModel) {
        check(referenceDataModel)
        referenceDataModelService.saveModelWithContent(referenceDataModel)
    }

    protected ReferenceDataModel getAndFinaliseReferenceDataModel(UUID idToFinalise = id) {
        ReferenceDataModel referenceDataModel = referenceDataModelService.get(idToFinalise)
        referenceDataModelService.finaliseModel(referenceDataModel, admin, null, null, null)
        checkAndSave(referenceDataModel)
        referenceDataModel
    }

    protected UUID createAndSaveNewBranchModel(String branchName, ReferenceDataModel base) {
        ReferenceDataModel referenceDataModel =
            referenceDataModelService.createNewBranchModelVersion(branchName, base, admin, false, adminSecurityPolicyManager)
        if (referenceDataModel.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, referenceDataModel)
            Assert.fail('Could not create new branch version')
        }
        check(referenceDataModel)
        referenceDataModelService.saveModelWithContent(referenceDataModel)
        referenceDataModel.id
    }

    protected ReferenceDataModel createSaveAndGetNewBranchModel(String branchName, ReferenceDataModel base) {
        UUID id = createAndSaveNewBranchModel(branchName, base)
        referenceDataModelService.get(id)
    }

    void 'test get'() {
        given:
        setupData()

        expect:
        referenceDataModelService.get(id) != null
    }

    void 'test list'() {
        given:
        setupData()

        when:
        List<ReferenceDataModel> referenceDataModelList = referenceDataModelService.list(max: 2, offset: 2, sort: 'dateCreated')

        then:
        referenceDataModelList.size() == 2

        when:
        def dm1 = referenceDataModelList[0]
        def dm2 = referenceDataModelList[1]

        then:
        dm1.label == 'test database'
        dm1.modelType == 'ReferenceDataModel'

        and:
        dm2.label == 'test form'
        dm1.modelType == 'ReferenceDataModel'

    }

    void 'test count'() {
        given:
        setupData()

        expect:
        referenceDataModelService.count() == 5
    }

    void 'test delete'() {
        given:
        setupData()

        expect:
        referenceDataModelService.count() == 5
        ReferenceDataModel dm = referenceDataModelService.get(id)

        when:
        referenceDataModelService.delete(dm)
        referenceDataModelService.save(dm)
        sessionFactory.currentSession.flush()

        then:
        ReferenceDataModel.countByDeleted(false) == 4
        ReferenceDataModel.countByDeleted(true) == 1
    }

    void 'test save'() {
        given:
        setupData()

        when:
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'saving test', folder: testFolder,
                                                                       authority: testAuthority)
        referenceDataModel = referenceDataModelService.validate(referenceDataModel)

        then:
        !referenceDataModel.hasErrors()

        when:
        referenceDataModelService.save(referenceDataModel)
        sessionFactory.currentSession.flush()

        then:
        referenceDataModel.id != null
    }

    void 'test finalising model'() {
        given:
        setupData()

        when:
        ReferenceDataModel referenceDataModel = referenceDataModelService.get(id)

        then:
        !referenceDataModel.finalised
        !referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')

        when:
        referenceDataModelService.finaliseModel(referenceDataModel, admin, Version.from('1'), null, null)

        then:
        checkAndSave(referenceDataModel)

        when:
        referenceDataModel = referenceDataModelService.get(id)

        then:
        referenceDataModel.finalised
        referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')
        referenceDataModel.modelVersion == Version.from('1')
    }

    void 'DMSC01 : test creating a new documentation version on draft model'() {
        given:
        setupData()

        when: 'creating new doc version on draft model is not allowed'
        ReferenceDataModel referenceDataModel = referenceDataModelService.get(id)
        def result = referenceDataModelService.createNewDocumentationVersion(referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.not.finalised.message' }
    }

    void 'DMSC02 : test creating a new documentation version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new doc version is allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null, null)
        checkAndSave(dataModel)
        def result = referenceDataModelService.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = referenceDataModelService.get(id)
        ReferenceDataModel newDocVersion = referenceDataModelService.get(result.id)

        then: 'old model is finalised and superseded'
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')

        and: 'new doc version model is draft v2'
        newDocVersion.documentationVersion == Version.from('2')
        !newDocVersion.finalised
        !newDocVersion.dateFinalised

        and: 'new doc version model matches old model'
        newDocVersion.label == dataModel.label
        newDocVersion.description == dataModel.description
        newDocVersion.author == dataModel.author
        newDocVersion.organisation == dataModel.organisation
        newDocVersion.modelType == dataModel.modelType

        newDocVersion.referenceDataTypes.size() == dataModel.referenceDataTypes.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        dataModel.referenceDataTypes.every { odt ->
            newDocVersion.referenceDataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.referenceDataElements.every { ode ->
            newDocVersion.referenceDataElements.any {
                it.label == ode.label &&
                it.id != ode.id &&
                it.domainType == ode.domainType
            }
        }

    }

    void 'DMSC04 : test creating a new documentation version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new doc version'
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()
        def newDocVersion = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.superseded.message' }
    }

    void 'DMSC05 : test creating a new fork version on draft model'() {
        given:
        setupData()

        when: 'creating new version on draft model is not allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, editorSecurityPolicyManager,
                                                                  [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {
            it.code == 'invalid.version.aware.new.version.not.finalised.message'
        }
    }

    void 'DMSC06 : test creating a new fork version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new version is allowed'
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, editorSecurityPolicyManager,
                                                                  [copyDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = referenceDataModelService.get(id)
        ReferenceDataModel newVersion = referenceDataModelService.get(result.id)

        then: 'old model is finalised and superseded'
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')

        and: 'new  version model is draft v2'
        newVersion.documentationVersion == Version.from('1')
        !newVersion.finalised
        !newVersion.dateFinalised

        and: 'new  version model matches old model'
        newVersion.label != dataModel.label
        newVersion.description == dataModel.description
        newVersion.author == dataModel.author
        newVersion.organisation == dataModel.organisation
        newVersion.modelType == dataModel.modelType

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any {
            it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF
        }
    }


    void 'DMSC08 : test creating a new fork version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new version'
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()
        def newVersion = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, editorSecurityPolicyManager,
                                                                  [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.superseded.message' }
    }

    void 'DMSC09 : test creating a new branch model version on draft model'() {
        given:
        setupData()

        when: 'creating new version on draft model is not allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager,
                                                                  [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {
            it.code == 'invalid.version.aware.new.version.not.finalised.message'
        }
    }

    void 'DMSC10 : test creating a new branch model version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new version is allowed'
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()
        def result = referenceDataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager, [
                moveDataFlows: false,
                throwErrors  : true
            ])

        then:
        checkAndSaveNewVersion(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = referenceDataModelService.get(id)
        ReferenceDataModel newVersion = referenceDataModelService.get(result.id)

        then: 'old model is finalised and superseded'
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')
        dataModel.modelVersion == Version.from('1')

        and: 'new  version model is draft v2'
        newVersion.documentationVersion == Version.from('1')
        !newVersion.modelVersion
        !newVersion.finalised
        !newVersion.dateFinalised

        and: 'new  version model matches old model'
        newVersion.label == dataModel.label
        newVersion.description == dataModel.description
        newVersion.author == dataModel.author
        newVersion.organisation == dataModel.organisation
        newVersion.modelType == dataModel.modelType

        newVersion.referenceDataTypes.size() == dataModel.referenceDataTypes.size()
        newVersion.referenceDataElements.size() == dataModel.referenceDataElements.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1

        and: 'link between old and new version'
        newVersion.versionLinks.any {
            it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF
        }

        and:
        dataModel.referenceDataTypes.every { odt ->
            newVersion.referenceDataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.referenceDataElements.every { ode ->
            newVersion.referenceDataElements.any {
                it.label == ode.label &&
                it.id != ode.id &&
                it.domainType == ode.domainType
            }
        }
    }

    void 'DMSC12 : test creating a new branch model version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new version'
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()
        def newVersion = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager,
                                                                  [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.superseded.message' }
    }

    void 'DMSC13 : test creating a new branch model version using main branch name when it already exists'() {
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()
        def mainBranch = referenceDataModelService.createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false,
                                                                               editorSecurityPolicyManager, [
                                                                                   moveDataFlows: false,
                                                                                   throwErrors  : true
                                                                               ])

        then:
        checkAndSaveNewVersion(mainBranch)


        when: 'trying to create a new branch version on the old datamodel'
        def result = referenceDataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager,
                                        [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'version.aware.label.branch.name.already.exists' }
    }

    void 'DMSF01 : test finding common ancestor of two datamodels'() {
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def left = createSaveAndGetNewBranchModel('left', dataModel)
        def right = createSaveAndGetNewBranchModel('right', dataModel)

        then:
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def commonAncestor = referenceDataModelService.findCommonAncestorBetweenModels(left, right)

        then:
        commonAncestor.id == id
        commonAncestor.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        commonAncestor.modelVersion == Version.from('1')
    }

    void 'DMSF02 : test finding latest finalised model version of a datamodel'() {
        //
        // dataModel (finalised) -- expectedModel (finalised) -- draftModel (draft)
        //   \_ testModel (draft)
        //
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        ReferenceDataModel expectedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel)
        ReferenceDataModel testModel = createSaveAndGetNewBranchModel('test', dataModel)

        expectedModel = getAndFinaliseReferenceDataModel(expectedModel.id)
        ReferenceDataModel draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, expectedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        expectedModel.modelVersion == Version.from('2')
        expectedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def latestVersion = referenceDataModelService.findLatestFinalisedModelByLabel(testModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = referenceDataModelService.findLatestFinalisedModelByLabel(draftModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = referenceDataModelService.getLatestModelVersionByLabel(testModel.label)

        then:
        latestVersion == Version.from('2')

        when:
        latestVersion = referenceDataModelService.getLatestModelVersionByLabel(draftModel.label)

        then:
        latestVersion == Version.from('2')
    }

    void 'DMSF03 : test getting current draft model on main branch from side branch'() {
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def finalisedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel)
        def testModel = createSaveAndGetNewBranchModel('test', dataModel)
        finalisedModel = getAndFinaliseReferenceDataModel(finalisedModel.id)
        def draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, finalisedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        finalisedModel.modelVersion == Version.from('2')
        finalisedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def currentMainBranch = referenceDataModelService.findCurrentMainBranchByLabel(testModel.label)

        then:
        currentMainBranch.id == draftModel.id
        currentMainBranch.label == testModel.label
        currentMainBranch.modelVersion == null
        currentMainBranch.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }

    void 'DMSF04 : test getting all draft models'() {
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = getAndFinaliseReferenceDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def finalisedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel)
        def testModel = createSaveAndGetNewBranchModel('test', dataModel)
        finalisedModel = getAndFinaliseReferenceDataModel(finalisedModel.id)
        def draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, finalisedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        finalisedModel.modelVersion == Version.from('2')
        finalisedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def availableBranches = referenceDataModelService.findAllAvailableBranchesByLabel(dataModel.label)

        then:
        availableBranches.size() == 2
        availableBranches.every {it.id in [draftModel.id, testModel.id]}
        availableBranches.every {it.label == dataModel.label}
    }

    void 'DMSV01 : test validation on valid model'() {
        given:
        setupData()
        ReferenceDataModel check = referenceDataModel

        expect:
        !referenceDataModelService.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, folder: testFolder, authority: testAuthority)

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('label')
        invalid.errors.getFieldError('path')
        invalid.errors.getFieldError('breadcrumbTree.path')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV03 : test validation on invalid primitive datatype model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', folder: testFolder,
                                                          authority: testAuthority)
        check.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('referenceDataTypes[0].label')
        invalid.errors.getFieldError('referenceDataTypes[0].path')
        invalid.errors.getFieldError('referenceDataTypes[0].breadcrumbTree.path')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    @Tag('non-parallel')
    void 'test suggesting links between models'() {
        given:
        hibernateSearchIndexingService.purgeAllIndexes()
        setupData()
        hibernateSearchIndexingService.flushIndexes()
        ReferenceDataModel dataModel = referenceDataModelService.get(id)

        when:
        List<ReferenceDataElementSimilarityResult> results = referenceDataModelService.suggestLinksBetweenModels(referenceDataModel, dataModel, 5)

        then:
        results.size() == 2

        when:
        ReferenceDataElementSimilarityResult ele1Res = results.find { it.source.label == 'Organisation name' }
        ReferenceDataElementSimilarityResult ele2Res = results.find { it.source.label == 'Organisation code' }

        then:
        ele1Res
        ele1Res.totalSimilar() == 1
        ele1Res.source.referenceDataType.label == 'string'
        ele1Res.first().item.id != ele1Res.source.id
        ele1Res.first().item.label == 'sdmelement'
        ele1Res.first().item.referenceDataType.label == 'string'
        ele1Res.first().score > 0


        then:
        ele2Res
        ele2Res.totalSimilar() == 1
        ele2Res.first().item.id != ele2Res.source.id
        ele2Res.first().item.label == 'sdmelement'
        ele2Res.first().item.referenceDataType.label == 'string'
        ele2Res.first().score > 0
    }
}

