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
package uk.ac.ox.softeng.maurodatamapper.datamodel


import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature

@Slf4j
class DataModelServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<DataModelService> {

    UUID id
    DataModel complexDataModel
    DataModel simpleDataModel

    def setup() {
        log.debug('Setting up DataModelServiceSpec unit')
        mockArtefact(BreadcrumbTreeService)
        mockArtefact(DataClassService)
        mockArtefact(DataElementService)
        mockArtefact(DataTypeService)
        mockArtefact(SummaryMetadataService)
        mockDomains(DataModel, DataClass, DataType, PrimitiveType,
                    ReferenceType, EnumerationType, EnumerationValue, DataElement)

        service.breadcrumbTreeService = Stub(BreadcrumbTreeService) {
            finalise(_) >> {
                BreadcrumbTree bt ->
                    bt.finalised = true
                    bt.buildTree()

            }
        }


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

        DataType dt = new PrimitiveType(createdByUser: admin, label: 'integration datatype')
        dataModel1.addToDataTypes(dt)
        DataElement dataElement = new DataElement(label: 'sdmelement', createdByUser: editor, dataType: dt)
        dataModel1.addToDataClasses(new DataClass(label: 'sdmclass', createdByUser: editor).addToDataElements(dataElement))

        checkAndSave(dataModel1)

        verifyBreadcrumbTrees()
        id = dataModel1.id
    }

    DataModel buildSimpleDataModel() {
        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, testFolder, testAuthority)
    }

    DataModel buildComplexDataModel() {
        BootstrapModels.buildAndSaveComplexDataModel(messageSource, testFolder, testAuthority)
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<DataModel> dataModelList = service.list(max: 2, offset: 2, sort: 'dateCreated')

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

        expect:
        service.count() == 5
    }

    void "test delete"() {

        expect:
        service.count() == 5
        DataModel dm = service.get(id)

        when:
        service.delete(dm)
        service.save(dm)

        then:
        DataModel.countByDeleted(false) == 4
        DataModel.countByDeleted(true) == 1
    }

    void "test save"() {

        when:
        DataModel dataModel = new DataModel(createdByUser: reader2, label: 'saving test', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                            authority: testAuthority)
        service.save(dataModel)

        then:
        dataModel.id != null

        when:
        DataModel saved = service.get(dataModel.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
    }

    void 'test finding datamodel types'() {
        expect:
        service.findAllDataAssets().size() == 2

        and:
        service.findAllDataStandards().size() == 3
    }

    void 'test finalising model'() {

        when:
        DataModel dataModel = service.get(id)

        then:
        !dataModel.finalised
        !dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')

        when:
        service.finaliseModel(dataModel, admin, null, null, null)

        then:
        checkAndSave(dataModel)

        when:
        dataModel = service.get(id)

        then:
        dataModel.finalised
        dataModel.dateFinalised
        dataModel.documentationVersion == Version.from('1')
    }

    void 'DMSC01 : test creating a new documentation version on draft model'() {

        when: 'creating new doc version on draft model is not allowed'
        DataModel dataModel = service.get(id)
        def result = service.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.not.finalised.message' }
    }

    void 'DMSC02 : test creating a new documentation version on finalised model'() {
        when: 'finalising model and then creating a new doc version is allowed'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, admin, null, null, null)
        checkAndSave(dataModel)
        def result = service.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = service.get(id)
        DataModel newDocVersion = service.get(result.id)

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
        when: 'finalising model and then creating a new doc version is allowed'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, admin, null, null, null)
        checkAndSave(dataModel)
        def result = service.createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = service.get(id)
        DataModel newDocVersion = service.get(result.id)

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

        when: 'creating new doc version'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, editor, null, null, null)
        def newDocVersion = service.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = service.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.superseded.message' }
    }

    @PendingFeature(reason = 'DataModel permission copying')
    void 'DMSC05 : test creating a new documentation version on finalised superseded model with permission copying'() {

        when: 'creating new doc version'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, editor, null, null, null)
        def newDocVersion = service.createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = service.createNewDocumentationVersion(dataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.superseded.message' }
    }

    void 'DMSC06 : test creating a new model version on draft model'() {


        when: 'creating new version on draft model is not allowed'
        DataModel dataModel = service.get(id)
        def result = service.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.not.finalised.message' }
    }

    void 'DMSC07 : test creating a new model version on finalised model'() {

        when: 'finalising model and then creating a new version is allowed'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, admin, null, null, null)
        checkAndSave(dataModel)
        def result = service.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.instanceOf(DataModel)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = service.get(id)
        DataModel newVersion = service.get(result.id)

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

    @PendingFeature(reason = 'DataModel permission copying')
    void 'DMSC08 : test creating a new model version on finalised model with permission copying'() {

        when: 'finalising model and then creating a new version is allowed'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, admin, null, null, null)
        checkAndSave(dataModel)
        def result = service.createNewForkModel("${dataModel.label}-1", dataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.instanceOf(DataModel)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        dataModel = service.get(id)
        DataModel newVersion = service.get(result.id)

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

        when: 'creating new version'
        DataModel dataModel = service.get(id)
        service.finaliseModel(dataModel, editor, null, null, null)
        def newVersion = service.createNewDocumentationVersion(dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = service.createNewForkModel("${dataModel.label}-1", dataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.version.aware.new.version.superseded.message' }
    }

    void 'DMSV01 : test validation on valid model'() {
        given:
        DataModel check = complexDataModel

        expect:
        !service.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        DataModel check = new DataModel(createdByUser: reader1, type: DataModelType.DATA_ASSET, folder: testFolder, authority: testAuthority)

        when:
        DataModel invalid = service.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataTypes(new PrimitiveType(createdByUser: admin))

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('primitiveTypes[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV04 : test validation on invalid dataclass model'() {
        given:
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataClasses(new DataClass(createdByUser: admin))

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV05 : test validation on invalid dataclass dataelement model'() {
        given:
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataElements(createdByUser: admin)
        check.addToDataClasses(parent)

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 2
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 2
        invalid.errors.getFieldError('dataClasses[0].dataElements[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV06 : test validation on invalid reference datatype model'() {
        given:
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin, label: 'ref')
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdByUser: admin, label: 'ref'))

        when:
        DataModel invalid = service.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin)
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdByUser: admin, label: 'ref', referenceClass: dc))

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.fieldErrors.any { it.field == 'dataClasses[0].label' }

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV08 : test validation on invalid nested dataclass model'() {
        given:
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV09 : test validation on invalid nested dataclass dataelement model'() {
        given:
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        DataClass child = new DataClass(createdByUser: admin, label: 'child')
        child.addToDataElements(createdByUser: admin, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        DataModel invalid = service.validate(check)

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
        DataModel check = new DataModel(createdByUser: reader1, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdByUser: admin, label: 'grandparent')
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        grandparent.addToDataClasses(parent)
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].dataClasses[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV11 : test validation on invalid double nested dataclass dataelement model'() {
        given:
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
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataClasses[0].dataClasses[0].dataClasses[0].dataElements[0].dataType')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }
}