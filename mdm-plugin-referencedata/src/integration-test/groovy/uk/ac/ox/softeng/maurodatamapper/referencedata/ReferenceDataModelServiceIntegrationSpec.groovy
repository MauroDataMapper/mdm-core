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
/*package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature

@Slf4j
@Integration
@Rollback
//@Stepwise
class ReferenceDataModelServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    ReferenceDataModel complexReferenceDataModel
    ReferenceDataModel simpleReferenceDataModel
    ReferenceDataModelService referenceDataModelService

    UserSecurityPolicyManager userSecurityPolicyManager

    @Override
    void setupDomainData() {
        log.debug('Setting up ReferenceDataModelServiceSpec unit')

        complexReferenceDataModel = buildComplexReferenceDataModel()
        simpleReferenceDataModel = buildSimpleReferenceDataModel()

        ReferenceDataModel referenceDataModel1 = new ReferenceDataModel(createdByUser: reader1, label: 'test database', folder: testFolder,
                                             authority: testAuthority)
        ReferenceDataModel referenceDataModel2 = new ReferenceDataModel(createdByUser: reader2, label: 'test form', folder: testFolder,
                                             authority: testAuthority)
        ReferenceDataModel referenceDataModel3 = new ReferenceDataModel(createdByUser: editor, label: 'test standard', folder: testFolder,
                                             authority: testAuthority)

        checkAndSave(referenceDataModel1)
        checkAndSave(referenceDataModel2)
        checkAndSave(referenceDataModel3)

        ReferenceDataType dt = new ReferencePrimitiveType(createdByUser: admin, label: 'string')
        referenceDataModel1.addToReferenceDataTypes(dt)
        ReferenceDataElement dataElement = new ReferenceDataElement(label: 'sdmelement', createdByUser: editor, referenceDataType: dt)
        referenceDataModel1.addToReferenceDataElements(dataElement)

        checkAndSave(referenceDataModel1)

        id = referenceDataModel1.id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        referenceDataModelService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<ReferenceDataModel> dataModelList = referenceDataModelService.list(max: 2, offset: 2, sort: 'dateCreated')

        then:
        dataModelList.size() == 2

        when:
        def dm1 = dataModelList[0]
        def dm2 = dataModelList[1]

        then:
        dm1.label == 'test database'
        dm1.modelType == DataModelType.DATA_ASSET.label

        and:
        dm2.label == 'test form'
        dm2.modelType == DataModelType.DATA_ASSET.label

    }

    void "test count"() {
        given:
        setupData()

        expect:
        referenceDataModelService.count() == 5
    }

    void "test delete"() {
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

    void "test save"() {
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = new ReferenceDataModel(createdByUser: reader2, label: 'saving test', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                            authority: testAuthority)
        dataModel = referenceDataModelService.validate(dataModel)

        then:
        !dataModel.hasErrors()

        when:
        referenceDataModelService.save(dataModel)
        sessionFactory.currentSession.flush()

        then:
        dataModel.id != null
    }

    void 'test finding datamodel types'() {
        given:
        setupData()

        expect:
        referenceDataModelService.findAllDataAssets().size() == 2

        and:
        referenceDataModelService.findAllDataStandards().size() == 3
    }

    void 'test finalising model'() {
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = referenceDataModelService.get(id)

        then:
        !dataModel.finalised
        !dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')

        when:
        referenceDataModelService.finaliseModel(dataModel, admin, Version.from('1'), null)

        then:
        checkAndSave(dataModel)

        when:
        dataModel = referenceDataModelService.get(id)

        then:
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')
        dataModel.modelVersion == Version.from('1')
    }

    void 'DMSC01 : test creating a new documentation version on draft model'() {
        given:
        setupData()

        when: 'creating new doc version on draft model is not allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        def result = referenceDataModelService.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.not.finalised.message' }
    }

    void 'DMSC02 : test creating a new documentation version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new doc version is allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
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
        newDocVersion.dataClasses.size() == dataModel.dataClasses.size()

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
        dataModel.dataClasses.every { odc ->
            newDocVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.referenceDataElements?.size() ?: 0
                int odes = odc.referenceDataElements?.size() ?: 0
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }

    @PendingFeature(reason = 'DataModel permission copying')
    void 'DMSC03 : test creating a new documentation version on finalised model with permission copying'() {
        given:
        setupData()

        when: 'finalising model and then creating a new doc version is allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = referenceDataModelService.createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [
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
        newDocVersion.dataClasses.size() == dataModel.dataClasses.size()

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
        dataModel.dataClasses.every { odc ->
            newDocVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.referenceDataElements?.size() ?: 0
                int odes = odc.referenceDataElements?.size() ?: 0
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }


    void 'DMSC04 : test creating a new documentation version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new doc version'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, editor, null, null)
        def newDocVersion = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    @PendingFeature(reason = 'DataModel permission copying')
    void 'DMSC05 : test creating a new documentation version on finalised superseded model with permission copying'() {
        given:
        setupData()

        when: 'creating new doc version'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, editor, null, null)
        def newDocVersion = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSC06 : test creating a new model version on draft model'() {
        given:
        setupData()

        when: 'creating new version on draft model is not allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {
            it.code == 'invalid.datamodel.new.version.not.finalised.message'
        }
    }

    void 'DMSC07 : test creating a new model version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new version is allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.instanceOf(DataModel)
        checkAndSave(result)

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

        newVersion.referenceDataTypes.size() == dataModel.referenceDataTypes.size()
        newVersion.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any {
            it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF
        }

        and:
        dataModel.referenceDataTypes.every { odt ->
            newVersion.referenceDataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every { odc ->
            newVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.referenceDataElements?.size() ?: 0
                int odes = odc.referenceDataElements?.size() ?: 0
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }

    @PendingFeature(reason = 'DataModel permission copying')
    void 'DMSC08 : test creating a new model version on finalised model with permission copying'() {
        given:
        setupData()

        when: 'finalising model and then creating a new version is allowed'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.instanceOf(DataModel)
        checkAndSave(result)

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

        newVersion.referenceDataTypes.size() == dataModel.referenceDataTypes.size()
        newVersion.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any { it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF }

        and:
        dataModel.referenceDataTypes.every { odt ->
            newVersion.referenceDataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every { odc ->
            newVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.referenceDataElements?.size() ?: 0
                int odes = odc.referenceDataElements?.size() ?: 0
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }

    void 'DMSC09 : test creating a new model version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new version'
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, editor, null, null)
        def newVersion = referenceDataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = referenceDataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSICA01 : test finding common ancestor of two datamodels'() {
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def left = referenceDataModelService.createNewBranchModelVersion('left', dataModel, admin, false, userSecurityPolicyManager)
        def right = referenceDataModelService.createNewBranchModelVersion('right', dataModel, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(left)
        checkAndSave(right)
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def commonAncestor = referenceDataModelService.commonAncestor(left, right)

        then:
        commonAncestor.id == id
        commonAncestor.branchName == 'main'
        commonAncestor.modelVersion == Version.from('1')
    }

    void 'DMSILV01 : test finding latest version of a datamodel'() {*/
        /*
        dataModel (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        /*given:
        setupData()

        when:
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def expectedModel = referenceDataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def testModel = referenceDataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)
        referenceDataModelService.finaliseModel(expectedModel, admin, null, null)
        checkAndSave(
            expectedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = referenceDataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)

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
        def latestVersion = referenceDataModelService.latestVersion(testModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == 'main'
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = referenceDataModelService.latestVersion(draftModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == 'main'
        latestVersion.modelVersion == Version.from('2')
    }

    void 'DMSIMD01 : test finding merge difference between two datamodels'() {
        given:
        setupData()

        when:
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def left = referenceDataModelService.createNewBranchModelVersion('left', dataModel, admin, false, userSecurityPolicyManager)
        def right = referenceDataModelService.createNewBranchModelVersion('right', dataModel, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(left)
        checkAndSave(right)
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def mergeDiff = referenceDataModelService.mergeDiff(left, right)

        then:
        mergeDiff == [left: dataModel.diff(left), right: dataModel.diff(right)]
    }

    void 'DMSICMB01 : test getting current draft model on main branch from side branch'() {*/
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        /*given:
        setupData()

        when:
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def finalisedModel = referenceDataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def testModel = referenceDataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)
        referenceDataModelService.finaliseModel(finalisedModel, admin, null, null)
        checkAndSave(
            finalisedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = referenceDataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(testModel)
        checkAndSave(draftModel)
        testModel.modelVersion == null
        testModel.branchName == 'test'
        finalisedModel.modelVersion == Version.from('2')
        finalisedModel.branchName == 'main'
        draftModel.modelVersion == null
        draftModel.branchName == 'main'

        when:
        def currentMainBranch = referenceDataModelService.currentMainBranch(testModel)

        then:
        currentMainBranch.id == draftModel.id
        currentMainBranch.label == testModel.label
        currentMainBranch.modelVersion == null
        currentMainBranch.branchName == 'main'
    }

    void 'DMSIAB01 : test getting all draft models'() {*/
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        /*given:
        setupData()

        when:
        ReferenceDataModel dataModel = referenceDataModelService.get(id)
        referenceDataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def finalisedModel = referenceDataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def testModel = referenceDataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)
        referenceDataModelService.finaliseModel(finalisedModel, admin, null, null)
        checkAndSave(
            finalisedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = referenceDataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(testModel)
        checkAndSave(draftModel)
        testModel.modelVersion == null
        testModel.branchName == 'test'
        finalisedModel.modelVersion == Version.from('2')
        finalisedModel.branchName == 'main'
        draftModel.modelVersion == null
        draftModel.branchName == 'main'

        when:
        def availableBranches = referenceDataModelService.availableBranches(dataModel.label)

        then:
        availableBranches.size() == 2
        availableBranches.each { it.id in [draftModel.id, testModel.id] }
        availableBranches.each { it.label == dataModel.label }
    }

    void 'DMSV01 : test validation on valid model'() {
        given:
        setupData()
        ReferenceDataModel check = complexDataModel

        expect:
        !referenceDataModelService.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, type: ReferenceDataModelType.DATA_ASSET, folder: testFolder, authority: testAuthority)

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV03 : test validation on invalid primitive datatype model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: admin))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataTypes[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV04 : test validation on invalid dataclass model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataClasses(new DataClass(createdByUser: admin))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('childDataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV05 : test validation on invalid dataclass dataelement model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataElements(createdByUser: admin)
        check.addToDataClasses(parent)

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 2
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 2
        invalid.errors.getFieldError('childDataClasses[0].dataElements[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV06 : test validation on invalid reference datatype model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin, label: 'ref')
        check.addToDataClasses(dc)
        check.addToReferenceDataTypes(new ReferenceType(createdByUser: admin, label: 'ref'))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataTypes[0].referenceClass')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV07 : test validation on invalid nested reference datatype model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin)
        check.addToDataClasses(dc)
        check.addToReferenceDataTypes(new ReferenceType(createdByUser: admin, label: 'ref', referenceClass: dc))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 2
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 2
        invalid.errors.fieldErrors.any { it.field == 'dataTypes[0].referenceClass.label' }
        invalid.errors.fieldErrors.any { it.field == 'childDataClasses[0].label' }

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV08 : test validation on invalid nested dataclass model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('childDataClasses[1].dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV09 : test validation on invalid nested dataclass dataelement model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        DataClass child = new DataClass(createdByUser: admin, label: 'child')
        child.addToDataElements(createdByUser: admin, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('childDataClasses[1].dataClasses[0].dataElements[0].dataType')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV10 : test validation on invalid double nested dataclass model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdByUser: admin, label: 'grandparent')
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        grandparent.addToDataClasses(parent)
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('childDataClasses[0].dataClasses[0].dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV11 : test validation on invalid double nested dataclass dataelement model'() {
        given:
        setupData()
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdByUser: admin, label: 'grandparent')
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        grandparent.addToDataClasses(parent)
        DataClass child = new DataClass(createdByUser: admin, label: 'child')
        child.addToDataElements(createdByUser: admin, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = referenceDataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('childDataClasses[0].dataClasses[0].dataClasses[0].dataElements[0].dataType')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'test suggesting links between models'() {
        given:
        setupData()
        ReferenceDataModel dataModel = referenceDataModelService.get(id)

        when:
        List<DataElementSimilarityResult> results = referenceDataModelService.suggestLinksBetweenModels(complexDataModel, dataModel, 5)

        then:
        results.size() == 3

        when:
        DataElementSimilarityResult childRes = results.find { it.source.label == 'child' }
        DataElementSimilarityResult ele1Res = results.find { it.source.label == 'ele1' }
        DataElementSimilarityResult ele2Res = results.find { it.source.label == 'element2' }

        then:
        ele1Res
        ele1Res.size() == 1
        ele1Res.source.referenceDataType.label == 'string'
        ele1Res.first().item.id != ele1Res.source.id
        ele1Res.first().item.label == 'sdmelement'
        ele1Res.first().item.referenceDataType.label == 'string'
        ele1Res.first().similarity > 0

        and:
        childRes
        childRes.size() == 0

        then:
        ele2Res
        ele2Res.size() == 0
    }
}*/

