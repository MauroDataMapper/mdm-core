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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.ArrayMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.CreationMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.DeletionMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.ObjectPatchData
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag
import org.spockframework.util.Assert
import spock.lang.PendingFeature

import java.time.OffsetDateTime
import java.util.function.Predicate

@Slf4j
@Integration
@Rollback
class DataModelServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    DataModel complexDataModel
    DataModel simpleDataModel
    DataModelService dataModelService
    DataClassService dataClassService
    MetadataService metadataService

    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        complexDataModel = buildComplexDataModel()
        simpleDataModel = buildSimpleDataModel()

        DataModel dataModel1 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test database', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        DataModel dataModel2 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test form', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        DataModel dataModel3 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test standard', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                             authority: testAuthority)
        checkAndSave(dataModel1)
        checkAndSave(dataModel2)
        checkAndSave(dataModel3)

        DataType dt = new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'string')
        dataModel1.addToDataTypes(dt)
        DataElement dataElement = new DataElement(label: 'sdmelement', createdBy: StandardEmailAddress.INTEGRATION_TEST, dataType: dt)
        dataModel1.addToDataClasses(new DataClass(label: 'sdmclass', createdBy: StandardEmailAddress.INTEGRATION_TEST).addToDataElements(dataElement))

        checkAndSave(dataModel1)

        id = dataModel1.id
    }

    void setupDataModelWithSummaryMetadata() {
        DataModel dataModel = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Data Model having Summary Metadata', type: DataModelType.DATA_ASSET,
                                            folder: testFolder,
                                            authority: testAuthority)

        checkAndSave(dataModel)

        // Add summary metadata with report to data model
        SummaryMetadata dataModelSummaryMetadata = new SummaryMetadata(label: 'Data Model Summary Metadata', createdBy: editor.emailAddress,
                                                                       summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataModelSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([A: 1, B: 2]).toString(),
            createdBy: editor.emailAddress
        )
        dataModelSummaryMetadata.addToSummaryMetadataReports(dataModelSummaryMetadataReport)
        dataModel.addToSummaryMetadata(dataModelSummaryMetadata)

        // Add summary metadata with report to data type
        DataType dataType = new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Data Type having Summary Metadata')
        dataModel.addToDataTypes(dataType)
        checkAndSave(dataModel)
        SummaryMetadata dataTypeSummaryMetadata = new SummaryMetadata(label: 'Data Type Summary Metadata', createdBy: editor.emailAddress,
                                                                      summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataTypeSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([A: 1, B: 2]).toString(),
            createdBy: editor.emailAddress
        )
        dataTypeSummaryMetadata.addToSummaryMetadataReports(dataTypeSummaryMetadataReport)
        dataType.addToSummaryMetadata(dataTypeSummaryMetadata)

        // Add summary metadata with report to data class
        DataClass dataClass = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Data Class having Summary Metadata', dataModel: dataModel)
        checkAndSave(dataClass)
        SummaryMetadata dataClassSummaryMetadata = new SummaryMetadata(label: 'Data Class Summary Metadata', createdBy: editor.emailAddress,
                                                                       summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataClassSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([A: 1, B: 2]).toString(),
            createdBy: editor.emailAddress
        )
        dataClassSummaryMetadata.addToSummaryMetadataReports(dataClassSummaryMetadataReport)
        dataClass.addToSummaryMetadata(dataClassSummaryMetadata)

        dataModel.addToDataClasses(dataClass)
        checkAndSave(dataModel)

        // Add summary metadata with report to data element
        DataElement dataElement = new DataElement(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Data Element having Summary Metadata', dataType: dataType)
        dataClass.addToDataElements(dataElement)
        checkAndSave(dataElement)

        SummaryMetadata dataElementSummaryMetadata = new SummaryMetadata(label: 'Data Element Summary Metadata', createdBy: editor.emailAddress,
                                                                         summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataElementSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([A: 1, B: 2]).toString(),
            createdBy: editor.emailAddress
        )
        dataElementSummaryMetadata.addToSummaryMetadataReports(dataElementSummaryMetadataReport)
        dataElement.addToSummaryMetadata(dataElementSummaryMetadata)

        checkAndSave(dataModel)
    }

    void setupDataModelWithMultipleDataClassesAndDataElementsAndEnumerationValues() {
        DataModel dataModel = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Data Model with Multiple Data Classes and Data Elements',
                                            type: DataModelType.DATA_ASSET, folder: testFolder,
                                            authority: testAuthority)

        DataClass dataClass
        for (int i in 1..5) {
            dataClass = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Root Data Class ' + i, dataModel: dataModel, idx: i - 1)
            dataModel.addToDataClasses(dataClass)
        }

        for (int i in 1..5) {
            DataClass childDataClass = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Child Data Class ' + i, dataModel: dataModel, idx: i - 1)
            dataClass.addToDataClasses(childDataClass)
        }

        DataType enumerationType = new EnumerationType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Enumeration Type')
        for (int i in 1..5) {
            enumerationType.addToEnumerationValues(new EnumerationValue(createdBy: StandardEmailAddress.INTEGRATION_TEST, key: 'Key ' + i, value: 'Value ' + i, idx: i - 1))
        }
        dataModel.addToDataTypes(enumerationType)

        for (int i in 1..5) {
            DataElement dataElement = new DataElement(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Data Element ' + i, dataType: enumerationType, idx: i - 1)
            dataClass.addToDataElements(dataElement)
        }

        checkAndSave(dataModel)
    }

    protected DataModel checkAndSaveNewVersion(DataModel dataModel) {
        check(dataModel)
        dataModelService.saveModelWithContent(dataModel)
    }

    protected DataModel getAndFinaliseDataModel(UUID idToFinalise = id) {
        DataModel dataModel = dataModelService.get(idToFinalise)
        dataModelService.finaliseModel(dataModel, admin, null, null, null)
        checkAndSave(dataModel)
        dataModel
    }

    protected UUID createAndSaveNewBranchModel(String branchName, DataModel base) {
        DataModel dataModel = dataModelService.createNewBranchModelVersion(branchName, base, admin, false, adminSecurityPolicyManager)
        if (dataModel.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, dataModel)
            Assert.fail('Could not create new branch version')
        }
        check(dataModel)
        dataModelService.saveModelWithContent(dataModel)
        dataModel.id
    }

    protected DataModel createSaveAndGetNewBranchModel(String branchName, DataModel base) {
        UUID id = createAndSaveNewBranchModel(branchName, base)
        dataModelService.get(id)
    }

    void 'test get'() {
        given:
        setupData()

        expect:
        dataModelService.get(id) != null
    }

    void 'test list'() {
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

    void 'test count'() {
        given:
        setupData()

        expect:
        dataModelService.count() == 5
    }

    void 'test delete'() {
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

    void 'test save'() {
        given:
        setupData()

        when:
        DataModel dataModel = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'saving test', type: DataModelType.DATA_STANDARD, folder: testFolder,
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
        dataModelService.finaliseModel(dataModel, admin, Version.from('1'), null, null)

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
        result.errors.allErrors.find {it.code == 'invalid.version.aware.new.version.not.finalised.message'}
    }

    void 'DMSC02 : test creating a new documentation version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new doc version is allowed'
        DataModel dataModel = getAndFinaliseDataModel()
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSaveNewVersion(result)

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
        newDocVersion.versionLinks.any {it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF}

        and:
        dataModel.dataTypes.every {odt ->
            newDocVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every {odc ->
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
        DataModel dataModel = getAndFinaliseDataModel()
        def result = dataModelService.createNewDocumentationVersion(dataModel, editor, true, editorSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSaveNewVersion(result)

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
        newDocVersion.versionLinks.any {it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF}

        and:
        dataModel.dataTypes.every {odt ->
            newDocVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every {odc ->
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
        DataModel dataModel = getAndFinaliseDataModel()
        def newDocVersion = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {it.code == 'invalid.version.aware.new.version.superseded.message'}
    }

    void 'DMSC05 : test creating a new fork version on draft model'() {
        given:
        setupData()

        when: 'creating new version on draft model is not allowed'
        DataModel dataModel = dataModelService.get(id)
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, editorSecurityPolicyManager,
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
        DataModel dataModel = getAndFinaliseDataModel()
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, editorSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(result)

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
            it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF
        }

        and:
        dataModel.dataTypes.every {odt ->
            newVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every {odc ->
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
    void 'DMSC07 : test creating a new fork version on finalised model with permission copying'() {
        given:
        setupData()

        when: 'finalising model and then creating a new version is allowed'
        DataModel dataModel = getAndFinaliseDataModel()
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, editorSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(result)

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
        newVersion.versionLinks.any {it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF}

        and:
        dataModel.dataTypes.every {odt ->
            newVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every {odc ->
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

    void 'DMSC08 : test creating a new fork version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new version'
        DataModel dataModel = getAndFinaliseDataModel()
        def newVersion = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = dataModelService.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, editorSecurityPolicyManager,
                                                         [copyDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {it.code == 'invalid.version.aware.new.version.superseded.message'}
    }

    void 'DMSC09 : test creating a new branch model version on draft model'() {
        given:
        setupData()

        when: 'creating new model version on draft model is not allowed'
        DataModel dataModel = dataModelService.get(id)
        def result = dataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager, [
                moveDataFlows: false,
                throwErrors  : true
            ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {it.code == 'invalid.version.aware.new.version.not.finalised.message'}
    }

    void 'DMSC10 : test creating a new branch model version on finalised model'() {
        given:
        setupData()

        when: 'finalising model and then creating a new doc version is allowed'
        DataModel dataModel = getAndFinaliseDataModel()
        def result = dataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager, [
                moveDataFlows: false,
                throwErrors  : true
            ])

        then:
        checkAndSaveNewVersion(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = dataModelService.get(id)
        DataModel newBranch = dataModelService.get(result.id)

        then: 'old model is finalised and superseded'
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')
        dataModel.modelVersion == Version.from('1')

        and: 'new branch version model is draft v2'
        newBranch.documentationVersion == Version.from('1')
        !newBranch.modelVersion
        !newBranch.finalised
        !newBranch.dateFinalised

        and: 'new branch version model matches old model'
        newBranch.label == dataModel.label
        newBranch.description == dataModel.description
        newBranch.author == dataModel.author
        newBranch.organisation == dataModel.organisation
        newBranch.modelType == dataModel.modelType

        newBranch.dataTypes.size() == dataModel.dataTypes.size()
        newBranch.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newBranch.annotations
        newBranch.edits.size() == 1

        and: 'new version of link between old and new version'
        newBranch.versionLinks.any {it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF}

        and:
        dataModel.dataTypes.every {odt ->
            newBranch.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        def missing = dataModel.dataClasses.findAll {odc ->
            !newBranch.dataClasses.find {
                int idcs = it.dataClasses?.size() ?: 0
                int odcs = odc.dataClasses?.size() ?: 0
                int ides = it.dataElements?.size() ?: 0
                int odes = odc.dataElements?.size() ?: 0
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
        !missing
    }

    @PendingFeature(reason = 'DataModel permission copying')
    void 'DMSC11 : test creating a new branch model version on finalised model with permission copying'() {
        given:
        setupData()

        when: 'finalising model and then creating a new doc version is allowed'
        DataModel dataModel = getAndFinaliseDataModel()
        def result = dataModelService.createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, true,
                                                                  editorSecurityPolicyManager, [
                                                                      moveDataFlows: false,
                                                                      throwErrors  : true
                                                                  ])

        then:
        checkAndSaveNewVersion(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = dataModelService.get(id)
        DataModel newBranch = dataModelService.get(result.id)

        then: 'old model is finalised and superseded'
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')
        dataModel.modelVersion == Version.from('1')

        and: 'new branch version model is draft v2'
        newBranch.documentationVersion == Version.from('1')
        !newBranch.modelVersion
        !newBranch.finalised
        !newBranch.dateFinalised

        and: 'new branch version model matches old model'
        newBranch.label == dataModel.label
        newBranch.description == dataModel.description
        newBranch.author == dataModel.author
        newBranch.organisation == dataModel.organisation
        newBranch.modelType == dataModel.modelType

        newBranch.dataTypes.size() == dataModel.dataTypes.size()
        newBranch.dataClasses.size() == dataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newBranch.annotations
        newBranch.edits.size() == 1

        and: 'new version of link between old and new version'
        newBranch.versionLinks.any {it.targetModelId == dataModel.id && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF}

        and:
        dataModel.dataTypes.every {odt ->
            newBranch.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        dataModel.dataClasses.every {odc ->
            newBranch.dataClasses.any {
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

    void 'DMSC12 : test creating a new branch model version on finalised superseded model'() {
        given:
        setupData()

        when: 'creating new doc version'
        DataModel dataModel = getAndFinaliseDataModel()
        def newDocVersion = dataModelService.
            createNewDocumentationVersion(dataModel, editor, false, editorSecurityPolicyManager, [moveDataFlows: false, throwErrors: true])

        then:
        checkAndSaveNewVersion(newDocVersion)

        when: 'trying to create a new branch version on the old datamodel'
        def result = dataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager,
                                        [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {it.code == 'invalid.version.aware.new.version.superseded.message'}
    }

    void 'DMSC13 : test creating a new branch model version using main branch name when it already exists'() {
        given:
        setupData()

        when:
        DataModel dataModel = getAndFinaliseDataModel()
        def mainBranch = dataModelService.createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false,
                                                                      editorSecurityPolicyManager, [
                                                                          moveDataFlows: false,
                                                                          throwErrors  : true
                                                                      ])

        then:
        checkAndSaveNewVersion(mainBranch)


        when: 'trying to create a new branch version on the old datamodel'
        def result = dataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager,
                                        [moveDataFlows: false, throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find {it.code == 'version.aware.label.branch.name.already.exists'}
    }

    void 'DMSC14 : test copying a model with ordered dataclasses, dataelements and enumerationvalues'() {
        given:
        setupData()
        setupDataModelWithMultipleDataClassesAndDataElementsAndEnumerationValues()

        when:
        DataModel dataModel = dataModelService.findByLabel('Data Model with Multiple Data Classes and Data Elements')

        then:
        dataModel.childDataClasses.size() == 5
        List<DataClass> rootDataClasses = dataModel.childDataClasses
        rootDataClasses.every {dc ->
            dc.label.startsWith('Root Data Class') && dc.label.endsWith((dc.idx + 1).toString())
        }

        when:
        DataClass parentClass = rootDataClasses.sort().last()

        then:
        parentClass.dataClasses.size() == 5
        parentClass.dataClasses.every {dc ->
            dc.label.startsWith('Child Data Class') && dc.label.endsWith((dc.idx + 1).toString())
        }

        and:
        parentClass.dataElements.size() == 5
        parentClass.dataElements.every {dc ->
            dc.label.startsWith('Data Element') && dc.label.endsWith((dc.idx + 1).toString())
        }

        when:
        DataElement dataElement = parentClass.dataElements.sort().first()
        EnumerationType enumerationType = dataElement.dataType

        then:
        enumerationType.enumerationValues.every {ev ->
            ev.key.startsWith('Key') && ev.key.endsWith((ev.idx + 1).toString()) &&
            ev.value.startsWith('Value') && ev.value.endsWith((ev.idx + 1).toString())
        }

        when:
        DataModel copiedDataModel =
            dataModelService
                .copyModel(dataModel, testFolder, editor, false, dataModel.label + ' copy', Version.from('1'), dataModel.branchName, true,
                           editorSecurityPolicyManager)

        then:
        copiedDataModel.childDataClasses.size() == 5
        List<DataClass> copiedRootDataClasses = copiedDataModel.childDataClasses
        copiedRootDataClasses.every {dc ->
            dc.label.startsWith('Root Data Class') && dc.label.endsWith((dc.idx + 1).toString())
        }

        when:
        DataClass copiedParentClass = copiedRootDataClasses.sort().last()

        then:
        copiedParentClass.dataClasses.size() == 5
        copiedParentClass.dataClasses.every {dc ->
            dc.label.startsWith('Child Data Class') && dc.label.endsWith((dc.idx + 1).toString())
        }

        and:
        copiedParentClass.dataElements.size() == 5
        copiedParentClass.dataElements.every {dc ->
            dc.label.startsWith('Data Element') && dc.label.endsWith((dc.idx + 1).toString())
        }

        when:
        DataElement copiedDataElement = copiedParentClass.dataElements.sort().first()
        EnumerationType copiedEnumerationType = copiedDataElement.dataType

        then:
        copiedEnumerationType.domainType == 'EnumerationType'
        copiedEnumerationType.enumerationValues.every {ev ->
            ev.key.startsWith('Key') && ev.key.endsWith((ev.idx + 1).toString()) &&
            ev.value.startsWith('Value') && ev.value.endsWith((ev.idx + 1).toString())
        }
    }

    void 'DMSF01 : test finding common ancestor of two datamodels'() {
        given:
        setupData()

        when:
        DataModel dataModel = getAndFinaliseDataModel()

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
        def commonAncestor = dataModelService.findCommonAncestorBetweenModels(left, right)

        then:
        commonAncestor.id == id
        commonAncestor.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        commonAncestor.modelVersion == Version.from('1')
    }

    void 'DMSF02 : test finding latest finalised model version of a datamodel'() {
        /*
        dataModel (finalised) -- expectedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        DataModel dataModel = getAndFinaliseDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        DataModel expectedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel)
        DataModel testModel = createSaveAndGetNewBranchModel('test', dataModel)

        expectedModel = getAndFinaliseDataModel(expectedModel.id)
        DataModel draftModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, expectedModel)

        then:
        testModel.modelVersion == null
        testModel.branchName == 'test'
        expectedModel.modelVersion == Version.from('2')
        expectedModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        draftModel.modelVersion == null
        draftModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def latestVersion = dataModelService.findLatestFinalisedModelByLabel(testModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = dataModelService.findLatestFinalisedModelByLabel(draftModel.label)

        then:
        latestVersion.id == expectedModel.id
        latestVersion.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        latestVersion.modelVersion == Version.from('2')

        when:
        latestVersion = dataModelService.getLatestModelVersionByLabel(testModel.label)

        then:
        latestVersion == Version.from('2')

        when:
        latestVersion = dataModelService.getLatestModelVersionByLabel(draftModel.label)

        then:
        latestVersion == Version.from('2')
    }

    void 'DMSF02a : test finding latest finalised model version works after v9.0.0'() {
        given:
        setupData()

        when:
        DataModel v1 = getAndFinaliseDataModel()
        String label = v1.label

        then:
        v1.modelVersion == Version.from('1')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('1')

        when:
        DataModel v2 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v1).id)

        then:
        v2.modelVersion == Version.from('2')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('2')

        when:
        DataModel v3 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v2).id)

        then:
        v3.modelVersion == Version.from('3')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('3')

        when:
        DataModel v4 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v3).id)

        then:
        v4.modelVersion == Version.from('4')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('4')

        when:
        DataModel v5 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v4).id)

        then:
        v5.modelVersion == Version.from('5')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('5')

        when:
        DataModel v6 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v5).id)

        then:
        v6.modelVersion == Version.from('6')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('6')

        when:
        DataModel v7 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v6).id)

        then:
        v7.modelVersion == Version.from('7')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('7')

        when:
        DataModel v8 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v7).id)

        then:
        v8.modelVersion == Version.from('8')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('8')

        when:
        DataModel v9 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v8).id)

        then:
        v9.modelVersion == Version.from('9')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('9')

        /**
         * The point of this test is to test that versions above v9 are correctly retrieved when they
         * are the latest version.
         */
        when: 'we create a v10 model'
        DataModel v10 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v9).id)

        then: 'the latest version is retrieved as v10 rather than v9'
        v10.modelVersion == Version.from('10')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('10')

        when: 'we create a v11 model'
        DataModel v11 = getAndFinaliseDataModel(createSaveAndGetNewBranchModel('main', v10).id)

        then: 'the latest version is retrieved as v11 rather than v9'
        v11.modelVersion == Version.from('11')
        dataModelService.findLatestFinalisedModelByLabel(label).modelVersion == Version.from('11')
    }

    void 'DMSF03 : test getting current draft model on main branch from side branch'() {
        /*
        dataModel (finalised) -- finalisedModel (finalised) -- draftModel (draft)
          \_ testModel (draft)
        */
        given:
        setupData()

        when:
        DataModel dataModel = getAndFinaliseDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def finalisedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel)
        def testModel = createSaveAndGetNewBranchModel('test', dataModel)
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
        def currentMainBranch = dataModelService.findCurrentMainBranchByLabel(testModel.label)

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
        DataModel dataModel = getAndFinaliseDataModel()

        then:
        dataModel.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        when:
        def finalisedModel = createSaveAndGetNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel)
        def testModel = createSaveAndGetNewBranchModel('test', dataModel)
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
        def availableBranches = dataModelService.findAllAvailableBranchesByLabel(dataModel.label)

        then:
        availableBranches.size() == 2
        availableBranches.each {it.id in [draftModel.id, testModel.id]}
        availableBranches.each {it.label == dataModel.label}
    }

    void 'DMSM01 : test finding merge difference between two datamodels'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel rightMain = dataModelService.get(mergeData.targetId)
        DataModel leftTest = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(leftTest, rightMain)

        then:
        !mergeDiff.isEmpty()
        mergeDiff.numberOfDiffs == 15

        then: 'branch name is not a diff'
        !mergeDiff.find {it.fieldName == 'branchName'}

        when: 'organisation is a non-conflicting change'
        FieldMergeDiff<String> stringFieldDiff = mergeDiff.find {it.fieldName == 'organisation'}

        then:
        stringFieldDiff.source == 'under test'
        stringFieldDiff.target == null
        stringFieldDiff.commonAncestor == null
        !stringFieldDiff.isMergeConflict()

        when: 'author is a conflicting change'
        stringFieldDiff = mergeDiff.find {it.fieldName == 'author'}

        then:
        stringFieldDiff.source == 'harry'
        stringFieldDiff.target == 'dick'
        stringFieldDiff.commonAncestor == 'john'
        stringFieldDiff.isMergeConflict()

        when: 'single array change in datatypes'
        ArrayMergeDiff<DataType> dataTypeDiff = mergeDiff.find {it.fieldName == 'dataTypes'} as ArrayMergeDiff<DataType>

        then:
        dataTypeDiff.deleted.isEmpty()
        dataTypeDiff.modified.isEmpty()
        dataTypeDiff.created.size() == 1
        dataTypeDiff.created.first().createdIdentifier == 'addSourceOnlyOnlyChangeInArray'
        !dataTypeDiff.created.first().isMergeConflict()
        !dataTypeDiff.created.first().commonAncestor
        !dataTypeDiff.created.first().target

        when: 'metadata has array diffs'
        ArrayMergeDiff<Metadata> metadataDiff = mergeDiff.find {it.fieldName == 'metadata'} as ArrayMergeDiff<Metadata>

        then:
        metadataDiff.source.size() == 1
        metadataDiff.target.size() == 2
        metadataDiff.commonAncestor.size() == 2
        metadataDiff.created.isEmpty()
        metadataDiff.deleted.size() == 1
        metadataDiff.deleted.first().deletedIdentifier == 'test.deleteSourceOnly'
        metadataDiff.modified.size() == 1
        metadataDiff.modified.first().sourceIdentifier == 'test.modifySourceOnly'
        metadataDiff.modified.first().size() == 1
        metadataDiff.modified.first().first().fieldName == 'value'
        metadataDiff.modified.first().first().source == 'altered'
        metadataDiff.modified.first().first().target == 'modifySourceOnly'
        metadataDiff.modified.first().first().commonAncestor == 'modifySourceOnly'
        !metadataDiff.modified.first().first().isMergeConflict()


        when: 'array diffs on the dataclass list'
        ArrayMergeDiff<DataClass> dataClassesDiff = mergeDiff.find {it.fieldName == 'dataClasses'} as ArrayMergeDiff<DataClass>

        then:
        dataClassesDiff.created.size() == 3
        dataClassesDiff.deleted.size() == 2
        dataClassesDiff.modified.size() == 4

        when: 'created on source side'
        CreationMergeDiff creationMergeDiff = dataClassesDiff.created.find {it.createdIdentifier == 'addSourceOnly'}

        then:
        creationMergeDiff
        creationMergeDiff.created
        !creationMergeDiff.isMergeConflict()
        !creationMergeDiff.commonAncestor

        when: 'created with nested creation on source side only'
        creationMergeDiff = dataClassesDiff.created.find {it.createdIdentifier == 'addSourceWithNestedChild'}

        then:
        creationMergeDiff
        creationMergeDiff.created //TODO more info in gson as we need to include the nested child
        !creationMergeDiff.isMergeConflict()
        !creationMergeDiff.commonAncestor

        when: 'modified on source side and deleted on target side'
        creationMergeDiff = dataClassesDiff.created.find {it.createdIdentifier == 'modifySourceAndDeleteTarget'}

        then:
        creationMergeDiff
        creationMergeDiff.created
        creationMergeDiff.isMergeConflict()
        creationMergeDiff.commonAncestor

        when: 'deleted on source side'
        DeletionMergeDiff deleteSourceOnly = dataClassesDiff.deleted.find {it.deletedIdentifier == 'deleteSourceOnly'}
        DeletionMergeDiff deleteAndModify = dataClassesDiff.deleted.find {it.deletedIdentifier == 'deleteSourceAndModifyTarget'}

        then:
        deleteSourceOnly
        deleteSourceOnly.deleted
        !deleteSourceOnly.isMergeConflict()
        deleteSourceOnly.commonAncestor

        and:
        deleteAndModify
        deleteAndModify.deleted
        deleteAndModify.isMergeConflict()
        deleteAndModify.commonAncestor
        deleteAndModify.mergeModificationDiff

        and:
        deleteAndModify.mergeModificationDiff //TODO more info


        when: 'additions on both with differences'
        MergeDiff addBothReturningDifferenceMerge = dataClassesDiff.modified.find {it.sourceIdentifier == 'addBothReturningDifference'}

        then:
        addBothReturningDifferenceMerge
        addBothReturningDifferenceMerge.size() == 1
        addBothReturningDifferenceMerge.first().diffType == 'FieldMergeDiff'
        addBothReturningDifferenceMerge.first().fieldName == 'description'
        addBothReturningDifferenceMerge.first().isMergeConflict()
        addBothReturningDifferenceMerge.first().source == 'source'
        addBothReturningDifferenceMerge.first().target == 'target'
        !addBothReturningDifferenceMerge.first().commonAncestor

        when: 'modified on source side'
        MergeDiff modifySourceOnlyMerge = dataClassesDiff.modified.find {it.sourceIdentifier == 'modifySourceOnly'}

        then:
        modifySourceOnlyMerge
        modifySourceOnlyMerge.size() == 1
        modifySourceOnlyMerge.first().diffType == 'FieldMergeDiff'
        modifySourceOnlyMerge.first().fieldName == 'description'
        !modifySourceOnlyMerge.first().isMergeConflict()
        modifySourceOnlyMerge.first().source == 'Description'
        modifySourceOnlyMerge.first().target == 'common'
        modifySourceOnlyMerge.first().commonAncestor == 'common'


        when: 'modified on both sides'
        MergeDiff modifyBothNoDifferenceMerge = dataClassesDiff.modified.find {it.sourceIdentifier == 'modifyBothReturningNoDifference'}
        MergeDiff modifyBothWithDifferenceMerge = dataClassesDiff.modified.find {it.sourceIdentifier == 'modifyBothReturningDifference'}

        then:
        !modifyBothNoDifferenceMerge

        and:
        modifyBothWithDifferenceMerge.size() == 1
        modifyBothWithDifferenceMerge.isMergeConflict()
        modifyBothWithDifferenceMerge.first().fieldName == 'description'
        modifyBothWithDifferenceMerge.first().isMergeConflict()
        modifyBothWithDifferenceMerge.first().source == 'DescriptionSource'
        modifyBothWithDifferenceMerge.first().target == 'DescriptionTarget'
        modifyBothWithDifferenceMerge.first().commonAncestor == 'common'


        when: 'nested changes made inside existing class'
        MergeDiff existingClassMerge = dataClassesDiff.modified.find {it.sourceIdentifier == 'existingClass'}

        then:
        existingClassMerge
        existingClassMerge.isMergeConflict()
        existingClassMerge.source.dataClasses.size() == 2
        existingClassMerge.target.dataClasses.size() == 2
        existingClassMerge.commonAncestor.dataClasses.size() == 2
        existingClassMerge.size() == 1
        existingClassMerge.first().fieldName == 'dataClasses'
        existingClassMerge.first().diffType == 'ArrayMergeDiff'

        when:
        ArrayMergeDiff existingClassDiff = existingClassMerge.first() as ArrayMergeDiff

        then:
        existingClassDiff.modified.isEmpty()
        existingClassDiff.created.size() == 1
        existingClassDiff.created.first().createdIdentifier == 'existingClass/addSourceToExistingClass'
        !existingClassDiff.created.first().isMergeConflict()
        !existingClassDiff.created.first().commonAncestor

        existingClassDiff.deleted.size() == 1
        existingClassDiff.deleted.size() == 1
        existingClassDiff.deleted.first().deletedIdentifier == 'existingClass/deleteSourceOnlyFromExistingClass'
        !existingClassDiff.deleted.first().isMergeConflict()
        existingClassDiff.deleted.first().commonAncestor
    }

    void 'DMSM03 : test merging new style single modification diff into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        def diff = mergeDiff.diffs.find {it.fieldName == 'author'}

        then:
        diff

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: [FieldPatchData.from(diff)])

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        mergedModel.author == 'harry'
    }

    void 'DMSM04 : test merging new style single dataclass modification diff into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        FieldMergeDiff diff = mergeDiff.flattenedDiffs.findAll {it instanceof FieldMergeDiff}
            .find {FieldMergeDiff fmd ->
                fmd.fieldName == 'description' && Path.from('dm:test database$test|dc:modifyBothReturningDifference').matches(fmd.getFullyQualifiedObjectPath())
            }

        then:
        diff

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: [FieldPatchData.from(diff)])

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        mergedModel.dataClasses.find {it.label == 'modifyBothReturningDifference'}.description == 'DescriptionSource'
    }

    void 'DMSM05 : test merging new style modification diffs into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        List<FieldMergeDiff> diffs = mergeDiff.flattenedDiffs.findAll {it instanceof FieldMergeDiff}
        diffs.removeIf([test: {FieldMergeDiff fieldMergeDiff ->
            fieldMergeDiff.fieldName == 'branchName'
        }] as Predicate)

        then:
        diffs
        diffs.size() == 6

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: diffs.collect {FieldPatchData.from(it)}
        )

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        mergedModel.author == 'harry'
        mergedModel.organisation == 'under test'
        mergedModel.dataClasses.find {it.label == 'modifyBothReturningDifference'}.description == 'DescriptionSource'
        mergedModel.dataClasses.find {it.label == 'addBothReturningDifference'}.description == 'source'
        mergedModel.dataClasses.find {it.label == 'modifySourceOnly'}.description == 'Description'
        mergedModel.metadata.find {it.namespace == 'test' && it.key == 'modifySourceOnly'}.value == 'altered'
    }

    void 'DMSM06 : test merging new style single deletion diff into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        DeletionMergeDiff diff = mergeDiff.flattenedDiffs.findAll {it instanceof DeletionMergeDiff}
            .find {DeletionMergeDiff dmd -> dmd.value.label == 'deleteSourceOnly'}

        then:
        diff

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: [FieldPatchData.from(diff)])

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        !mergedModel.dataClasses.find {it.label == 'deleteSourceOnly'}
    }

    void 'DMSM07 : test merging new style all deletion diff into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        List<DeletionMergeDiff> diffs = mergeDiff.flattenedDiffs.findAll {it instanceof DeletionMergeDiff}

        then:
        diffs
        diffs.size() == 4

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: diffs.collect {FieldPatchData.from(it)}
        )

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        !mergedModel.dataClasses.find {it.label == 'deleteSourceOnly'}
        !mergedModel.dataClasses.find {it.label == 'deleteSourceAndModifyTarget'}
        !mergedModel.dataClasses.find {it.label == 'deleteSourceOnlyFromExistingClass'}
        !mergedModel.metadata.find {it.namespace == 'test' && it.key == 'deleteSourceOnly'}
    }

    DataModel mergeObjectPatchDataIntoModel(ObjectPatchData patch, DataModel targetModel, DataModel sourceModel) {
        DataModel mergedModel = dataModelService.mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel, adminSecurityPolicyManager)
        sessionFactory.currentSession.flush()
        dataModelService.get(mergedModel.id)
    }

    void 'DMSM08 : test merging new style single creation diff into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        CreationMergeDiff diff = mergeDiff.flattenedDiffs.findAll {it instanceof CreationMergeDiff}
            .find {CreationMergeDiff cmd -> cmd.value.label == 'addSourceOnly'}

        then:
        diff

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: [FieldPatchData.from(diff)])

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        mergedModel.dataClasses.find {it.label == 'addSourceOnly'}
    }


    void 'DMSM09 : test merging new style creation diffs into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        List<CreationMergeDiff> diffs = mergeDiff.flattenedDiffs.findAll {it instanceof CreationMergeDiff}

        then:
        diffs
        diffs.size() == 5

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: diffs.collect {FieldPatchData.from(it)}
        )

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        mergedModel.dataClasses.find {it.label == 'addSourceOnly'}
        mergedModel.dataClasses.find {it.label == 'addSourceWithNestedChild'}
        mergedModel.dataClasses.find {it.label == 'modifySourceAndDeleteTarget'}
        mergedModel.dataClasses.find {it.label == 'addSourceToExistingClass'}
        mergedModel.dataClasses.find {it.label == 'existingClass'}.dataClasses.find {it.label == 'addSourceToExistingClass'}
        mergedModel.dataTypes.find {it.label == 'addSourceOnlyOnlyChangeInArray'}
    }

    void 'DMSM10 : test merging new style facet creation diff into draft model'() {
        given:
        setupData()

        when: 'generate models'
        Map<String, UUID> mergeData = BootstrapModels.buildMergeModelsForTestingOnly(id, admin, dataModelService, dataClassService, metadataService, sessionFactory,
                                                                                     messageSource)
        DataModel targetModel = dataModelService.get(mergeData.targetId)
        DataModel sourceModel = dataModelService.get(mergeData.sourceId)
        sourceModel.addToMetadata('test', 'addSourceOnly', 'addSourceOnly', StandardEmailAddress.INTEGRATION_TEST)
        sourceModel.dataClasses.find {it.label == 'existingClass'}.addToMetadata('test', 'addDCSourceOnly', 'addDCSourceOnly',
                                                                                 StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(sourceModel)

        MergeDiff mergeDiff = dataModelService.getMergeDiffForModels(sourceModel, targetModel)

        then:
        !mergeDiff.isEmpty()

        when:
        List<CreationMergeDiff> diffs = mergeDiff.flattenedDiffs
            .findAll {it instanceof CreationMergeDiff}
            .findAll {CreationMergeDiff cmd -> cmd.created instanceof Metadata}

        then:
        diffs
        diffs.size() == 2

        when: 'using a patch pulled from the actual diff'
        def patch = new ObjectPatchData(
            targetId: targetModel.id,
            sourceId: sourceModel.id,
            patches: diffs.collect {FieldPatchData.from(it)}
        )

        then:
        check(patch)

        when:
        DataModel mergedModel = mergeObjectPatchDataIntoModel(patch, targetModel, sourceModel)

        then:
        mergedModel.metadata.find {it.key == 'addSourceOnly'}
        sourceModel.dataClasses.find {it.label == 'existingClass'}.metadata.find {it.key == 'addDCSourceOnly'}
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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, type: DataModelType.DATA_ASSET, folder: testFolder, authority: testAuthority)

        when:
        DataModel invalid = dataModelService.validate(check)

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('primitiveTypes[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV04 : test validation on invalid dataclass model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV05 : test validation on invalid dataclass dataelement model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'parent')
        parent.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST)
        check.addToDataClasses(parent)

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 4
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 4
        invalid.errors.getFieldError('dataClasses[0].dataElements[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV06 : test validation on invalid reference datatype model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'ref')
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'ref'))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('referenceTypes[0].referenceClass')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV07 : test validation on invalid nested reference datatype model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST)
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'ref', referenceClass: dc))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.fieldErrors.any {it.field == 'dataClasses[0].label'}

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV08 : test validation on invalid nested dataclass model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'parent')
        parent.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST))
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV09 : test validation on invalid nested dataclass dataelement model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'parent')
        DataClass child = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'child')
        child.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].dataElements[0].dataType')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV10 : test validation on invalid double nested dataclass model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'grandparent')
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'parent')
        grandparent.addToDataClasses(parent)
        parent.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST))
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV11 : test validation on invalid double nested dataclass dataelement model'() {
        given:
        setupData()
        DataModel check = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'grandparent')
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'parent')
        grandparent.addToDataClasses(parent)
        DataClass child = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'child')
        child.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'other'))

        when:
        DataModel invalid = dataModelService.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].dataClasses[0].dataElements[0].dataType')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    @Tag('non-parallel')
    void 'test suggesting links between models'() {
        given:
        hibernateSearchIndexingService.purgeAllIndexes()
        setupData()
        hibernateSearchIndexingService.flushIndexes()
        DataModel dataModel = dataModelService.get(id)

        when:
        List<DataElementSimilarityResult> results = dataModelService.suggestLinksBetweenModels(complexDataModel, dataModel, 5)

        then:
        results.size() == 3

        when:
        DataElementSimilarityResult childRes = results.find {it.source.label == 'child'}
        DataElementSimilarityResult ele1Res = results.find {it.source.label == 'ele1'}
        DataElementSimilarityResult ele2Res = results.find {it.source.label == 'element2'}
        log.debug('{}', childRes)
        log.debug('{}', ele1Res)
        log.debug('{}', ele2Res)

        then:
        ele1Res
        ele1Res.totalSimilar() == 1
        ele1Res.source.dataType.label == 'string'
        ele1Res.first().item.id != ele1Res.source.id
        ele1Res.first().item.label == 'sdmelement'
        ele1Res.first().item.dataType.label == 'string'
        ele1Res.first().score > 0

        and:
        childRes
        childRes.totalSimilar() == 0

        then:
        ele2Res
        ele2Res.totalSimilar() == 0
    }

    void 'test summary metadata is copied to new version'() {
        given:
        setupData()
        setupDataModelWithSummaryMetadata()
        DataModel dataModel4 = dataModelService.findByLabel('Data Model having Summary Metadata')

        when: 'finalising model and then creating a new doc version is allowed'
        DataModel dataModel = getAndFinaliseDataModel(dataModel4.id)
        def result = dataModelService.
            createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME, dataModel, editor, false, editorSecurityPolicyManager, [
                moveDataFlows: false,
                throwErrors  : true
            ])

        then:
        checkAndSaveNewVersion(result)

        when: 'load new branch from DB'
        DataModel newBranch = dataModelService.get(result.id)

        then: 'there is summary metadata on the branch Data Model'
        newBranch.summaryMetadata.find {it.label == 'Data Model Summary Metadata'}
        SummaryMetadata foundDataModelSummaryMetadata = newBranch.summaryMetadata.find {it.label == 'Data Model Summary Metadata'}
        foundDataModelSummaryMetadata.summaryMetadataReports.size() == 1

        and: 'there is summary metadata on the Data Class'
        DataClass dc = newBranch.childDataClasses.find {it.label == 'Data Class having Summary Metadata'}
        SummaryMetadata foundDataClassSummaryMetadata = dc.summaryMetadata.find {it.label == 'Data Class Summary Metadata'}
        foundDataClassSummaryMetadata.summaryMetadataReports.size() == 1

        and: 'there is summary metadata on the Data Type'
        PrimitiveType dt = newBranch.dataTypes.find {it.label == 'Data Type having Summary Metadata'}
        SummaryMetadata foundDataTypeSummaryMetadata = dt.summaryMetadata.find {it.label == 'Data Type Summary Metadata'}
        foundDataTypeSummaryMetadata.summaryMetadataReports.size() == 1

        and: 'there is summary metadata on the Data Element'
        DataElement de = dc.dataElements.find {it.label == 'Data Element having Summary Metadata'}
        SummaryMetadata foundDataElementSummaryMetadata = de.summaryMetadata.find {it.label == 'Data Element Summary Metadata'}
        foundDataElementSummaryMetadata.summaryMetadataReports.size() == 1
    }
}

