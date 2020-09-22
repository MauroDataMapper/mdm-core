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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Version

@Slf4j
@Integration
@Rollback
//@Stepwise
class DataModelServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    DataModel complexDataModel
    DataModel simpleDataModel
    DataModelService dataModelService

    UserSecurityPolicyManager userSecurityPolicyManager

    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        complexDataModel = buildComplexDataModel()
        simpleDataModel = buildSimpleDataModel()

        DataModel dataModel1 = new DataModel(createdByUser: reader1, label: 'test database', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        DataModel dataModel2 = new DataModel(createdByUser: reader2, label: 'test form', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        DataModel dataModel3 = new DataModel(createdByUser: editor, label: 'test standard', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                             authority: testAuthority)

        checkAndSave(dataModel1)
        checkAndSave(dataModel2)
        checkAndSave(dataModel3)

        DataType dt = new PrimitiveType(createdByUser: admin, label: 'string')
        dataModel1.addToDataTypes(dt)
        DataElement dataElement = new DataElement(label: 'sdmelement', createdByUser: editor, dataType: dt)
        dataModel1.addToDataClasses(new DataClass(label: 'sdmclass', createdByUser: editor).addToDataElements(dataElement))

        checkAndSave(dataModel1)

        id = dataModel1.id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        dataModelService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<DataModel> dataModelList = dataModelService.list(max: 2, offset: 2, sort: 'dateCreated')

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
        dataModelService.count() == 5
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        dataModelService.count() == 5
        DataModel dm = dataModelService.get(id)

        when:
        dataModelService.delete(dm)
        dataModelService.save(dm)
        sessionFactory.currentSession.flush()

        then:
        DataModel.countByDeleted(false) == 4
        DataModel.countByDeleted(true) == 1
    }

    void "test save"() {
        given:
        setupData()

        when:
        DataModel dataModel = new DataModel(createdByUser: reader2, label: 'saving test', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                            authority: testAuthority)
        dataModel = dataModelService.validate(dataModel)

        then:
        !dataModel.hasErrors()

        when:
        dataModelService.save(dataModel)
        sessionFactory.currentSession.flush()

        then:
        dataModel.id != null
    }

    void 'test finding datamodel types'() {
        given:
        setupData()

        expect:
        dataModelService.findAllDataAssets().size() == 2

        and:
        dataModelService.findAllDataStandards().size() == 3
    }

    void 'test finalising model'() {
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)

        then:
        !dataModel.finalised
        !dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')

        when:
        dataModelService.finaliseModel(dataModel, admin, Version.from('1'), null)

        then:
        checkAndSave(dataModel)

        when:
        dataModel = dataModelService.get(id)

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
        DataModel dataModel = dataModelService.get(id)
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = dataModelService.get(id)
        DataModel newDocVersion = dataModelService.get(result.id)

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

        newDocVersion.dataTypes.size() == dataModel.dataTypes.size()
        newDocVersion.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        dataModel.dataTypes.every { odt ->
            newDocVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every { odc ->
            newDocVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.dataElements?.size() ?: 0
                int odes = odc.dataElements?.size() ?: 0
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = dataModelService.get(id)
        DataModel newDocVersion = dataModelService.get(result.id)

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

        newDocVersion.dataTypes.size() == dataModel.dataTypes.size()
        newDocVersion.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        dataModel.dataTypes.every { odt ->
            newDocVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every { odc ->
            newDocVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.dataElements?.size() ?: 0
                int odes = odc.dataElements?.size() ?: 0
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, editor, null, null)
        def newDocVersion = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = dataModelService.
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, editor, null, null)
        def newDocVersion = dataModelService.
            createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = dataModelService.
            createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSC06 : test creating a new model version on draft model'() {
        given:
        setupData()

        when: 'creating new version on draft model is not allowed'
        DataModel dataModel = dataModelService.get(id)
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager,
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.instanceOf(DataModel)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = dataModelService.get(id)
        DataModel newVersion = dataModelService.get(result.id)

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

        newVersion.dataTypes.size() == dataModel.dataTypes.size()
        newVersion.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any {
            it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF
        }

        and:
        dataModel.dataTypes.every { odt ->
            newVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every { odc ->
            newVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.dataElements?.size() ?: 0
                int odes = odc.dataElements?.size() ?: 0
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.instanceOf(DataModel)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = dataModelService.get(id)
        DataModel newVersion = dataModelService.get(result.id)

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

        newVersion.dataTypes.size() == dataModel.dataTypes.size()
        newVersion.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any { it.targetModel.id == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF }

        and:
        dataModel.dataTypes.every { odt ->
            newVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every { odc ->
            newVersion.dataClasses.any {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.dataElements?.size() ?: 0
                int odes = odc.dataElements?.size() ?: 0
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
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, editor, null, null)
        def newVersion = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSICA01 : test finding common ancestor of two datamodels'() {
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def left = dataModelService.createNewBranchModelVersion('left', dataModel, admin, false, userSecurityPolicyManager)
        def right = dataModelService.createNewBranchModelVersion('right', dataModel, admin, false, userSecurityPolicyManager)

        then:
        checkAndSave(left)
        checkAndSave(right)
        left.modelVersion == null
        left.branchName == 'left'
        right.modelVersion == null
        right.branchName == 'right'

        when:
        def commonAncestor = dataModelService.commonAncestor(left, right)

        then:
        commonAncestor.id == id
        commonAncestor.branchName == 'main'
        commonAncestor.modelVersion == Version.from('1')
    }

    void 'DMSILV01 : test finding latest finalised model version of a datamodel'() {
        /*
        dataModel (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def expectedModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def testModel = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)
        dataModelService.finaliseModel(expectedModel, admin, null, null)
        checkAndSave(
            expectedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)

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
        def latestVersion = dataModelService.latestFinalisedModel(testModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == 'main'
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = dataModelService.latestFinalisedModel(draftModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == 'main'
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = dataModelService.latestModelVersion(testModel.label)

        then:
        latestVersion == Version.from('2')

        when:
        latestVersion = dataModelService.latestModelVersion(draftModel.label)

        then:
        latestVersion == Version.from('2')
    }

    void 'DMSIMD01 : test finding merge difference between two datamodels'() {
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def draft = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def test = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)

        def parent = new DataClass(createdByUser: admin, label: 'parent')
        def child = new DataClass(createdByUser: admin, label: 'child')
        draft.addToDataClasses(parent.addToDataClasses(child))

        then:
        checkAndSave(draft)
        checkAndSave(test)
        draft.modelVersion == null
        draft.branchName == 'main'
        test.modelVersion == null
        test.branchName == 'test'

        when:
        def mergeDiff = dataModelService.mergeDiff(draft, test)

        then:
        mergeDiff == test.diff(draft)
    }

    void 'DMSICMB01 : test getting current draft model on main branch from side branch'() {
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def finalisedModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def testModel = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)
        dataModelService.finaliseModel(finalisedModel, admin, null, null)
        checkAndSave(
            finalisedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)

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
        def currentMainBranch = dataModelService.currentMainBranch(testModel)

        then:
        currentMainBranch.id == draftModel.id
        currentMainBranch.label == testModel.label
        currentMainBranch.modelVersion == null
        currentMainBranch.branchName == 'main'
    }

    void 'DMSIAB01 : test getting all draft models'() {
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def finalisedModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)
        def testModel = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, userSecurityPolicyManager)
        dataModelService.finaliseModel(finalisedModel, admin, null, null)
        checkAndSave(
            finalisedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, userSecurityPolicyManager)

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
        def availableBranches = dataModelService.availableBranches(dataModel.label)

        then:
        availableBranches.size() == 2
        availableBranches.each { it.id in [draftModel.id, testModel.id] }
        availableBranches.each { it.label == dataModel.label }
    }

    void 'DMSV01 : test validation on valid model'() {
        given:
        setupData()
        DataModel check = complexDataModel

        expect:
        !dataModelService.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdByUser: reader1, type: DataModelType.DATA_ASSET, folder: testFolder, authority: testAuthority)

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataTypes(new PrimitiveType(createdByUser: admin))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataClasses(new DataClass(createdByUser: admin))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataElements(createdByUser: admin)
        check.addToDataClasses(parent)

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin, label: 'ref')
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdByUser: admin, label: 'ref'))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin)
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdByUser: admin, label: 'ref', referenceClass: dc))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        DataClass child = new DataClass(createdByUser: admin, label: 'child')
        child.addToDataElements(createdByUser: admin, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdByUser: admin, label: 'grandparent')
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        grandparent.addToDataClasses(parent)
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
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
        DataModel invalid = dataModelService.validate(check)

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
        DataModel dataModel = dataModelService.get(id)

        when:
        List<DataElementSimilarityResult> results = dataModelService.suggestLinksBetweenModels(complexDataModel, dataModel, 5)

        then:
        results.size() == 3

        when:
        DataElementSimilarityResult childRes = results.find { it.source.label == 'child' }
        DataElementSimilarityResult ele1Res = results.find { it.source.label == 'ele1' }
        DataElementSimilarityResult ele2Res = results.find { it.source.label == 'element2' }

        then:
        ele1Res
        ele1Res.size() == 1
        ele1Res.source.dataType.label == 'string'
        ele1Res.first().item.id != ele1Res.source.id
        ele1Res.first().item.label == 'sdmelement'
        ele1Res.first().item.dataType.label == 'string'
        ele1Res.first().similarity > 0

        and:
        childRes
        childRes.size() == 0

        then:
        ele2Res
        ele2Res.size() == 0
    }
}

