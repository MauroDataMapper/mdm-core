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

import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeFieldDiffData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeItemData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeObjectDiffData
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
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
class DataModelServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    DataModel complexDataModel
    DataModel simpleDataModel
    DataModelService dataModelService
    DataClassService dataClassService

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
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [
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
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [
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
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, true, editorSecurityPolicyManager, [
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
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

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
            createNewDocumentationVersion(dataModel, editor, true, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = dataModelService.
            createNewDocumentationVersion(dataModel, editor, true, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSC06 : test creating a new model version on draft model'() {
        given:
        setupData()

        when: 'creating new version on draft model is not allowed'
        DataModel dataModel = dataModelService.get(id)
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, editorSecurityPolicyManager,
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
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, editorSecurityPolicyManager,
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
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, editorSecurityPolicyManager,
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
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSave(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, editorSecurityPolicyManager,
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
        def left = dataModelService.createNewBranchModelVersion('left', dataModel, admin, false, adminSecurityPolicyManager)
        def right = dataModelService.createNewBranchModelVersion('right', dataModel, admin, false, adminSecurityPolicyManager)

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
        def expectedModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)
        def testModel = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, adminSecurityPolicyManager)
        dataModelService.finaliseModel(expectedModel, admin, null, null)
        checkAndSave(
            expectedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)

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
        dataModel.addToDataClasses(new DataClass(createdByUser: admin, label: 'deleteLeftOnly'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'deleteRightOnly'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'modifyLeftOnly'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'modifyRightOnly'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'deleteAndDelete'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'deleteAndModify'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'modifyAndDelete'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'modifyAndModifyReturningNoDifference'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'modifyAndModifyReturningDifference'))
        def existingClass = new DataClass(createdByUser: admin, label: 'existingClass')
        def deleteLeftOnlyFromExistingClass = new DataClass(createdByUser: admin, label: 'deleteLeftOnlyFromExistingClass')
        def deleteRightOnlyFromExistingClass = new DataClass(createdByUser: admin, label: 'deleteRightOnlyFromExistingClass')
        dataModel.addToDataClasses(existingClass.addToDataClasses(deleteLeftOnlyFromExistingClass)
                                       .addToDataClasses(deleteRightOnlyFromExistingClass))
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def draft = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)
        def test = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, adminSecurityPolicyManager)

        dataClassService.delete(test.dataClasses.find { it.label == 'deleteLeftOnlyFromExistingClass' } as DataClass)
        dataClassService.delete(test.childDataClasses.find { it.label == 'deleteLeftOnly' })
        dataClassService.delete(test.childDataClasses.find { it.label == 'deleteAndDelete' })
        dataClassService.delete(test.childDataClasses.find { it.label == 'deleteAndModify' })

        test.childDataClasses.find { it.label == 'modifyLeftOnly' }.description = 'Description'
        test.childDataClasses.find { it.label == 'modifyAndDelete' }.description = 'Description'
        test.childDataClasses.find { it.label == 'modifyAndModifyReturningNoDifference' }.description = 'Description'
        test.childDataClasses.find { it.label == 'modifyAndModifyReturningDifference' }.description = 'DescriptionLeft'

        test.childDataClasses.find { it.label == 'existingClass' }
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addLeftToExistingClass'))
        def leftParentDataClass = (new DataClass(createdByUser: admin, label: 'leftParentDataClass'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'leftChildDataClass'))
        test.addToDataClasses(new DataClass(createdByUser: admin, label: 'addLeftOnly'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addAndAddReturningNoDifference'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addAndAddReturningDifference', description: 'left'))
            .addToDataClasses(leftParentDataClass)
        //test.description = 'DescriptionLeft'
        checkAndSave(test)

        dataClassService.delete(draft.dataClasses.find { it.label == 'deleteRightOnlyFromExistingClass' } as DataClass)
        dataClassService.delete(draft.childDataClasses.find { it.label == 'deleteRightOnly' })
        dataClassService.delete(draft.childDataClasses.find { it.label == 'deleteAndDelete' })
        dataClassService.delete(draft.childDataClasses.find { it.label == 'modifyAndDelete' })

        draft.childDataClasses.find { it.label == 'modifyRightOnly' }.description = 'Description'
        draft.childDataClasses.find { it.label == 'deleteAndModify' }.description = 'Description'
        draft.childDataClasses.find { it.label == 'modifyAndModifyReturningNoDifference' }.description = 'Description'
        draft.childDataClasses.find { it.label == 'modifyAndModifyReturningDifference' }.description = 'DescriptionRight'

        draft.childDataClasses.find { it.label == 'existingClass' }
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addRightToExistingClass'))
        def rightParentDataClass = (new DataClass(createdByUser: admin, label: 'rightParentDataClass'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'rightChildDataClass'))
        draft.addToDataClasses(new DataClass(createdByUser: admin, label: 'addRightOnly'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addAndAddReturningNoDifference'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addAndAddReturningDifference', description: 'right'))
            .addToDataClasses(rightParentDataClass)
        draft.description = 'DescriptionRight'
        checkAndSave(draft)

        def mergeDiff = dataModelService.mergeDiff(test, draft)

        then:
        mergeDiff.class == ObjectDiff
        mergeDiff.diffs
        mergeDiff.numberOfDiffs == 11
        mergeDiff.diffs.fieldName as Set == ['branchName', 'dataClasses'] as Set
        def branchNameDiff = mergeDiff.diffs.find { it.fieldName == 'branchName' }
        branchNameDiff.left == 'main'
        branchNameDiff.right == 'test'
        !branchNameDiff.isMergeConflict
        def dataClassesDiff = mergeDiff.diffs.find { it.fieldName == 'dataClasses' }
        dataClassesDiff.created.size == 3
        dataClassesDiff.deleted.size == 2
        dataClassesDiff.modified.size == 4
        dataClassesDiff.created.value.label as Set == ['addLeftOnly', 'leftParentDataClass', 'modifyAndDelete'] as Set
        !dataClassesDiff.created.find { it.value.label == 'addLeftOnly' }.isMergeConflict
        !dataClassesDiff.created.find { it.value.label == 'addLeftOnly' }.commonAncestorValue
        !dataClassesDiff.created.find { it.value.label == 'leftParentDataClass' }.isMergeConflict
        !dataClassesDiff.created.find { it.value.label == 'leftParentDataClass' }.commonAncestorValue
        dataClassesDiff.created.find { it.value.label == 'modifyAndDelete' }.isMergeConflict
        dataClassesDiff.created.find { it.value.label == 'modifyAndDelete' }.commonAncestorValue
        dataClassesDiff.deleted.value.label as Set == ['deleteAndModify', 'deleteLeftOnly'] as Set
        dataClassesDiff.deleted.find { it.value.label == 'deleteAndModify' }.isMergeConflict
        dataClassesDiff.deleted.find { it.value.label == 'deleteAndModify' }.commonAncestorValue
        !dataClassesDiff.deleted.find { it.value.label == 'deleteLeftOnly' }.isMergeConflict
        !dataClassesDiff.deleted.find { it.value.label == 'deleteLeftOnly' }.commonAncestorValue
        dataClassesDiff.modified.left.diffIdentifier as Set == ['existingClass', 'modifyAndModifyReturningDifference', 'modifyLeftOnly',
                                                                'addAndAddReturningDifference'] as Set
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'modifyAndModifyReturningDifference' }.diffs[0].fieldName == 'description'
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'modifyAndModifyReturningDifference' }.isMergeConflict
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'modifyAndModifyReturningDifference' }.commonAncestorValue
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].fieldName == 'dataClasses'
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.isMergeConflict
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.commonAncestorValue
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].created[0].value.label == 'addLeftToExistingClass'
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].deleted[0].value.label ==
        'deleteLeftOnlyFromExistingClass'
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].created[0].isMergeConflict
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].created[0].commonAncestorValue
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].deleted[0].isMergeConflict
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'existingClass' }.diffs[0].deleted[0].commonAncestorValue
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'addAndAddReturningDifference' }.diffs[0].fieldName == 'description'
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'addAndAddReturningDifference' }.diffs[0].isMergeConflict
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'addAndAddReturningDifference' }.diffs[0].commonAncestorValue
        dataClassesDiff.modified.find { it.left.diffIdentifier == 'modifyLeftOnly' }.diffs[0].fieldName == 'description'
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'modifyLeftOnly' }.diffs[0].isMergeConflict
        !dataClassesDiff.modified.find { it.left.diffIdentifier == 'modifyLeftOnly' }.diffs[0].commonAncestorValue
    }

    void 'DMSIMI01 : test merging diff into draft model'() {
        given:
        setupData()

        when:
        DataModel dataModel = dataModelService.get(id)
        def deleteLeftOnly = new DataClass(createdByUser: admin, label: 'deleteLeftOnly')
        def modifyLeftOnly = new DataClass(createdByUser: admin, label: 'modifyLeftOnly')
        def deleteAndModify = new DataClass(createdByUser: admin, label: 'deleteAndModify')
        def modifyAndDelete = new DataClass(createdByUser: admin, label: 'modifyAndDelete')
        def modifyAndModifyReturningDifference = new DataClass(createdByUser: admin, label: 'modifyAndModifyReturningDifference')
        dataModel.addToDataClasses(deleteLeftOnly)
            .addToDataClasses(modifyLeftOnly)
            .addToDataClasses(deleteAndModify)
            .addToDataClasses(modifyAndDelete)
            .addToDataClasses(modifyAndModifyReturningDifference)
        def existingClass = new DataClass(createdByUser: admin, label: 'existingClass')
        def deleteLeftOnlyFromExistingClass = new DataClass(createdByUser: admin, label: 'deleteLeftOnlyFromExistingClass')
        dataModel.addToDataClasses(existingClass.addToDataClasses(deleteLeftOnlyFromExistingClass))
        dataModelService.finaliseModel(dataModel, admin, null, null)
        checkAndSave(dataModel)

        then:
        dataModel.branchName == 'main'

        when:
        def draft = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)
        def test = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, adminSecurityPolicyManager)

        dataClassService.delete(test.dataClasses.find { it.label == 'deleteLeftOnlyFromExistingClass' } as DataClass)
        dataClassService.delete(test.childDataClasses.find { it.label == 'deleteLeftOnly' })
        dataClassService.delete(test.childDataClasses.find { it.label == 'deleteAndModify' })

        test.childDataClasses.find { it.label == 'modifyLeftOnly' }.description = 'Description'
        test.childDataClasses.find { it.label == 'modifyAndDelete' }.description = 'Description'
        test.childDataClasses.find { it.label == 'modifyAndModifyReturningDifference' }.description = 'DescriptionLeft'

        def addLeftToExistingClass = new DataClass(createdByUser: admin, label: 'addLeftToExistingClass')
        test.childDataClasses.find { it.label == 'existingClass' }
            .addToDataClasses(addLeftToExistingClass)
        def leftParentDataClass = (new DataClass(createdByUser: admin, label: 'leftParentDataClass'))
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'leftChildDataClass'))
        def addLeftOnly = new DataClass(createdByUser: admin, label: 'addLeftOnly')
        test.addToDataClasses(addLeftOnly)
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addAndAddReturningDifference', description: 'left'))
            .addToDataClasses(leftParentDataClass)
        def modelDescriptionLeft = 'DescriptionLeft'
        test.description = modelDescriptionLeft
        checkAndSave(test)

        dataClassService.delete(draft.childDataClasses.find { it.label == 'modifyAndDelete' })

        draft.childDataClasses.find { it.label == 'deleteAndModify' }.description = 'Description'
        draft.childDataClasses.find { it.label == 'modifyAndModifyReturningDifference' }.description = 'DescriptionRight'

        draft.childDataClasses.find { it.label == 'existingClass' }
            .addToDataClasses(new DataClass(createdByUser: admin, label: 'addRightToExistingClass'))
        def addAndAddReturningDifference = new DataClass(createdByUser: admin, label: 'addAndAddReturningDifference', description: 'right')
        draft.addToDataClasses(addAndAddReturningDifference)

        draft.description = 'DescriptionRight'
        checkAndSave(draft)

        def mergeDiff = dataModelService.mergeDiff(test, draft)

        then:
        mergeDiff.class == ObjectDiff
        mergeDiff.diffs
        mergeDiff.numberOfDiffs == 12
        mergeDiff.diffs[0].fieldName == 'description'
        mergeDiff.diffs[0].left == 'DescriptionRight'
        mergeDiff.diffs[0].right == 'DescriptionLeft'
        dataClassesDiff.fieldName == 'branchName'
        dataClassesDiff.left == 'main'
        dataClassesDiff.right == 'test'
        !dataClassesDiff.isMergeConflict
        mergeDiff.diffs[2].fieldName == 'dataClasses'
        mergeDiff.diffs[2].created.size == 3
        mergeDiff.diffs[2].deleted.size == 2
        mergeDiff.diffs[2].modified.size == 4
        mergeDiff.diffs[2].created[0].value.label == 'addLeftOnly'
        !mergeDiff.diffs[2].created[0].isMergeConflict
        !mergeDiff.diffs[2].created[0].commonAncestorValue
        mergeDiff.diffs[2].created[1].value.label == 'leftParentDataClass'
        !mergeDiff.diffs[2].created[1].isMergeConflict
        !mergeDiff.diffs[2].created[1].commonAncestorValue
        mergeDiff.diffs[2].created[2].value.label == 'modifyAndDelete'
        mergeDiff.diffs[2].created[2].isMergeConflict
        mergeDiff.diffs[2].created[2].commonAncestorValue
        mergeDiff.diffs[2].deleted[0].value.label == 'deleteAndModify'
        mergeDiff.diffs[2].deleted[0].isMergeConflict
        mergeDiff.diffs[2].deleted[0].commonAncestorValue
        mergeDiff.diffs[2].deleted[1].value.label == 'deleteLeftOnly'
        !mergeDiff.diffs[2].deleted[1].isMergeConflict
        !mergeDiff.diffs[2].deleted[1].commonAncestorValue
        mergeDiff.diffs[2].modified[0].diffs[0].fieldName == 'description'
        mergeDiff.diffs[2].modified[0].isMergeConflict
        !mergeDiff.diffs[2].modified[0].commonAncestorValue
        mergeDiff.diffs[2].modified[1].diffs[0].fieldName == 'dataClasses'
        mergeDiff.diffs[2].modified[1].isMergeConflict
        mergeDiff.diffs[2].modified[1].commonAncestorValue
        mergeDiff.diffs[2].modified[1].diffs[0].created[0].value.label == 'addLeftToExistingClass'
        mergeDiff.diffs[2].modified[1].diffs[0].deleted[0].value.label == 'deleteLeftOnlyFromExistingClass'
        !mergeDiff.diffs[2].modified[1].diffs[0].created[0].isMergeConflict
        !mergeDiff.diffs[2].modified[1].diffs[0].created[0].commonAncestorValue
        !mergeDiff.diffs[2].modified[1].diffs[0].deleted[0].isMergeConflict
        !mergeDiff.diffs[2].modified[1].diffs[0].deleted[0].commonAncestorValue
        mergeDiff.diffs[2].modified[2].diffs[0].fieldName == 'description'
        mergeDiff.diffs[2].modified[2].diffs[0].isMergeConflict
        !mergeDiff.diffs[2].modified[2].diffs[0].commonAncestorValue
        mergeDiff.diffs[2].modified[3].diffs[0].fieldName == 'description'
        !mergeDiff.diffs[2].modified[3].diffs[0].isMergeConflict
        !mergeDiff.diffs[2].modified[3].diffs[0].commonAncestorValue

        when:
        def patch = new MergeObjectDiffData(
            leftId: draft.id,
            rightId: test.id,
            diffs: [
                new MergeFieldDiffData(
                    fieldName: 'description',
                    value: modelDescriptionLeft
                ),
                new MergeFieldDiffData(
                    fieldName: 'dataClasses',
                    deleted: [
                        new MergeItemData(
                            id: dataClassService.findByParentAndLabel(draft, deleteAndModify.label).id,
                            label: deleteAndModify.label
                        ),
                        new MergeItemData(
                            id: dataClassService.findByParentAndLabel(draft, deleteLeftOnly.label).id,
                            label: deleteLeftOnly.label
                        )
                    ],
                    created: [
                        new MergeItemData(
                            id: addLeftOnly.id,
                            label: addLeftOnly.label
                        ),
                        new MergeItemData(
                            id: dataClassService.findByParentAndLabel(test, modifyAndDelete.label).id,
                            label: modifyAndDelete.label
                        )
                    ],
                    modified: [
                        new MergeObjectDiffData(
                            leftId: addAndAddReturningDifference.id,
                            label: addAndAddReturningDifference.label,
                            diffs: [
                                new MergeFieldDiffData(
                                    fieldName: 'description',
                                    value: 'addedDescriptionSource'
                                )
                            ]
                        ),
                        new MergeObjectDiffData(
                            leftId: dataClassService.findByParentAndLabel(draft, existingClass.label).id,
                            label: existingClass.label,
                            diffs: [
                                new MergeFieldDiffData(
                                    fieldName: "dataClasses",

                                    deleted: [
                                        new MergeItemData(
                                            id: dataClassService.findByParentAndLabel(
                                                dataClassService.findByParentAndLabel(draft, existingClass.label),
                                                deleteLeftOnlyFromExistingClass.label).id,
                                            label: deleteLeftOnlyFromExistingClass.label
                                        )
                                    ],
                                    created: [
                                        new MergeItemData(
                                            id: addLeftToExistingClass.id,
                                            label: addLeftToExistingClass.label
                                        )
                                    ]

                                )
                            ]
                        ),
                        new MergeObjectDiffData(
                            leftId: dataClassService.findByParentAndLabel(draft, modifyAndModifyReturningDifference.label).id,
                            label: modifyAndModifyReturningDifference.label,
                            diffs: [
                                new MergeFieldDiffData(
                                    fieldName: 'description',
                                    value: 'DescriptionLeft'
                                ),
                            ]
                        ),
                        new MergeObjectDiffData(
                            leftId: dataClassService.findByParentAndLabel(draft, "modifyLeftOnly").id,
                            label: "modifyLeftOnly",
                            diffs: [
                                new MergeFieldDiffData(
                                    fieldName: 'description',
                                    value: 'Description'
                                )
                            ]

                        )
                    ]

                )
            ]
        )
        def mergedModel = dataModelService.mergeInto(test, draft, patch, adminSecurityPolicyManager)

        then:
        mergedModel.description == modelDescriptionLeft
        mergedModel.dataClasses.size() == 8
        mergedModel.dataClasses.label == ['existingClass', 'modifyAndModifyReturningDifference', 'modifyLeftOnly', 'sdmclass',
                                          'addAndAddReturningDifference', 'addLeftOnly', 'modifyAndDelete', 'addLeftToExistingClass']
        mergedModel.dataClasses.find { it.label == 'existingClass' }.dataClasses.label == ['addRightToExistingClass', 'addLeftToExistingClass']
        mergedModel.dataClasses.find { it.label == 'modifyAndModifyReturningDifference' }.description == 'DescriptionLeft'
        mergedModel.dataClasses.find { it.label == 'modifyLeftOnly' }.description == 'Description'
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
        def finalisedModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)
        def testModel = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, adminSecurityPolicyManager)
        dataModelService.finaliseModel(finalisedModel, admin, null, null)
        checkAndSave(
            finalisedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)

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
        def finalisedModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)
        def testModel = dataModelService.createNewBranchModelVersion('test', dataModel, admin, false, adminSecurityPolicyManager)
        dataModelService.finaliseModel(finalisedModel, admin, null, null)
        checkAndSave(
            finalisedModel) // must persist before createNewBranchModelVersion is called due to call to countAllByLabelAndBranchNameAndNotFinalised
        def draftModel = dataModelService.createNewBranchModelVersion('main', dataModel, admin, false, adminSecurityPolicyManager)

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

