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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService

import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature

@Slf4j
class ReferenceDataModelServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceDataModelService> {

    UUID id
    ReferenceDataModel complexReferenceDataModel
    ReferenceDataModel simpleReferenceDataModel

    def setup() {
        log.debug('Setting up ReferenceDataModelServiceSpec unit')
        mockArtefact(DataClassService)
        mockArtefact(ReferenceDataElementService)
        mockArtefact(ReferenceDataTypeService)
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType,
                    ReferenceType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        complexReferenceDataModel = buildComplexReferenceDataModel()
        simpleReferenceDataModel = buildSimpleReferenceDataModel()

        ReferenceDataModel referenceDataModel1 = new ReferenceDataModel(createdByUser: reader1, label: 'test database', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        ReferenceDataModel referenceDataModel2 = new ReferenceDataModel(createdByUser: reader2, label: 'test form', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        ReferenceDataModel referenceDataModel3 = new ReferenceDataModel(createdByUser: editor, label: 'test standard', type: ReferenceDataModelType.DATA_STANDARD, folder: testFolder,
                                             authority: testAuthority)

        checkAndSave(referenceDataModel1)
        checkAndSave(referenceDataModel2)
        checkAndSave(referenceDataModel3)

        ReferenceDataType dt = new ReferencePrimitiveType(createdByUser: admin, label: 'integration datatype')
        referenceDataModel1.addToDataTypes(dt)
        ReferenceDataElement dataElement = new ReferenceDataElement(label: 'sdmelement', createdByUser: editor, referenceDataType: dt)

        checkAndSave(referenceDataModel1)

        verifyBreadcrumbTrees()
        id = referenceDataModel1.id
    }

    ReferenceDataModel buildSimpleReferenceDataModel(Authority authority) {
        BootstrapModels.buildAndSaveSimpleReferenceDataModel(messageSource, testFolder, testAuthority)
    }

    ReferenceDataModel buildComplexReferenceDataModel() {
        BootstrapModels.buildAndSaveComplexReferenceDataModel(messageSource, testFolder, testAuthority)
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<ReferenceDataModel> referenceDataModelList = service.list(max: 2, offset: 2, sort: 'dateCreated')

        then:
        referenceDataModelList.size() == 2

        when:
        def dm1 = referenceDataModelList[0]
        def dm2 = referenceDataModelList[1]

        then:
        dm1.label == 'test database'
        dm1.modelType == ReferenceDataModelType.DATA_ASSET.label

        and:
        dm2.label == 'test form'
        dm2.modelType == ReferenceDataModelType.DATA_ASSET.label

    }

    void "test count"() {

        expect:
        service.count() == 5
    }

    void "test delete"() {

        expect:
        service.count() == 5
        ReferenceDataModel dm = service.get(id)

        when:
        service.delete(dm)
        service.save(dm)

        then:
        ReferenceDataModel.countByDeleted(false) == 4
        ReferenceDataModel.countByDeleted(true) == 1
    }

    void "test save"() {

        when:
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(createdByUser: reader2, label: 'saving test', type: ReferenceDataModelType.DATA_STANDARD, folder: testFolder,
                                            authority: testAuthority)
        service.save(referenceDataModel)

        then:
        referenceDataModel.id != null

        when:
        ReferenceDataModel saved = service.get(referenceDataModel.id)

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
        ReferenceDataModel referenceDataModel = service.get(id)

        then:
        !referenceDataModel.finalised
        !referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')

        when:
        service.finaliseModel(referenceDataModel, admin, null, null)

        then:
        checkAndSave(referenceDataModel)

        when:
        referenceDataModel = service.get(id)

        then:
        referenceDataModel.finalised
        referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')
    }

    void 'DMSC01 : test creating a new documentation version on draft model'() {

        when: 'creating new doc version on draft model is not allowed'
        ReferenceDataModel referenceDataModel = service.get(id)
        def result = service.createNewDocumentationVersion(referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.not.finalised.message' }
    }

    void 'DMSC02 : test creating a new documentation version on finalised model'() {
        when: 'finalising model and then creating a new doc version is allowed'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, admin, null, null)
        checkAndSave(referenceDataModel)
        def result = service.createNewDocumentationVersion(referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        referenceDataModel = service.get(id)
        ReferenceDataModel newDocVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        referenceDataModel.finalised
        referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')

        and: 'new doc version model is draft v2'
        newDocVersion.documentationVersion == Version.from('2')
        !newDocVersion.finalised
        !newDocVersion.dateFinalised

        and: 'new doc version model matches old model'
        newDocVersion.label == referenceDataModel.label
        newDocVersion.description == referenceDataModel.description
        newDocVersion.author == referenceDataModel.author
        newDocVersion.organisation == referenceDataModel.organisation
        newDocVersion.modelType == referenceDataModel.modelType

        newDocVersion.dataTypes.size() == referenceDataModel.dataTypes.size()
        newDocVersion.dataClasses.size() == referenceDataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == referenceDataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        referenceDataModel.dataTypes.every { odt ->
            newDocVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        referenceDataModel.dataClasses.every { odc ->
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

    @PendingFeature(reason = 'ReferenceDataModel permission copying')
    void 'DMSC03 : test creating a new documentation version on finalised model with permission copying'() {
        when: 'finalising model and then creating a new doc version is allowed'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, admin, null, null)
        checkAndSave(referenceDataModel)
        def result = service.createNewDocumentationVersion(referenceDataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        referenceDataModel = service.get(id)
        ReferenceDataModel newDocVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        referenceDataModel.finalised
        referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')

        and: 'new doc version model is draft v2'
        newDocVersion.documentationVersion == Version.from('2')
        !newDocVersion.finalised
        !newDocVersion.dateFinalised

        and: 'new doc version model matches old model'
        newDocVersion.label == referenceDataModel.label
        newDocVersion.description == referenceDataModel.description
        newDocVersion.author == referenceDataModel.author
        newDocVersion.organisation == referenceDataModel.organisation
        newDocVersion.modelType == referenceDataModel.modelType

        newDocVersion.dataTypes.size() == referenceDataModel.dataTypes.size()
        newDocVersion.dataClasses.size() == referenceDataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == referenceDataModel.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        referenceDataModel.dataTypes.every { odt ->
            newDocVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        referenceDataModel.dataClasses.every { odc ->
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

        when: 'creating new doc version'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, editor, null, null)
        def newDocVersion = service.createNewDocumentationVersion(referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = service.createNewDocumentationVersion(referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    @PendingFeature(reason = 'ReferenceDataModel permission copying')
    void 'DMSC05 : test creating a new documentation version on finalised superseded model with permission copying'() {

        when: 'creating new doc version'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, editor, null, null)
        def newDocVersion = service.createNewDocumentationVersion(referenceDataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old datamodel'
        def result = service.createNewDocumentationVersion(referenceDataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSC06 : test creating a new model version on draft model'() {


        when: 'creating new version on draft model is not allowed'
        ReferenceDataModel referenceDataModel = service.get(id)
        def result = service.createNewForkModel("${referenceDataModel.label}-1", referenceDataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.not.finalised.message' }
    }

    void 'DMSC07 : test creating a new model version on finalised model'() {

        when: 'finalising model and then creating a new version is allowed'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, admin, null, null)
        checkAndSave(referenceDataModel)
        def result = service.createNewForkModel("${referenceDataModel.label}-1", referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.instanceOf(ReferenceDataModel)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        referenceDataModel = service.get(id)
        ReferenceDataModel newVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        referenceDataModel.finalised
        referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')

        and: 'new  version model is draft v2'
        newVersion.documentationVersion == Version.from('1')
        !newVersion.finalised
        !newVersion.dateFinalised

        and: 'new  version model matches old model'
        newVersion.label != referenceDataModel.label
        newVersion.description == referenceDataModel.description
        newVersion.author == referenceDataModel.author
        newVersion.organisation == referenceDataModel.organisation
        newVersion.modelType == referenceDataModel.modelType

        newVersion.dataTypes.size() == referenceDataModel.dataTypes.size()
        newVersion.dataClasses.size() == referenceDataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any { it.targetModel.id == referenceDataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF }

        and:
        referenceDataModel.dataTypes.every { odt ->
            newVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        referenceDataModel.dataClasses.every { odc ->
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

    @PendingFeature(reason = 'ReferenceDataModel permission copying')
    void 'DMSC08 : test creating a new model version on finalised model with permission copying'() {

        when: 'finalising model and then creating a new version is allowed'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, admin, null, null)
        checkAndSave(referenceDataModel)
        def result = service.createNewForkModel("${referenceDataModel.label}-1", referenceDataModel, editor, true, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.instanceOf(ReferenceDataModel)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        referenceDataModel = service.get(id)
        ReferenceDataModel newVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        referenceDataModel.finalised
        referenceDataModel.dateFinalised
        referenceDataModel.documentationVersion == Version.from('1')

        and: 'new  version model is draft v2'
        newVersion.documentationVersion == Version.from('1')
        !newVersion.finalised
        !newVersion.dateFinalised

        and: 'new  version model matches old model'
        newVersion.label != referenceDataModel.label
        newVersion.description == referenceDataModel.description
        newVersion.author == referenceDataModel.author
        newVersion.organisation == referenceDataModel.organisation
        newVersion.modelType == referenceDataModel.modelType

        newVersion.dataTypes.size() == referenceDataModel.dataTypes.size()
        newVersion.dataClasses.size() == referenceDataModel.dataClasses.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any { it.targetModel.id == referenceDataModel.id && it.linkType == VersionLinkType.NEW_FORK_OF }

        and:
        referenceDataModel.dataTypes.every { odt ->
            newVersion.dataTypes.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        referenceDataModel.dataClasses.every { odc ->
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

        when: 'creating new version'
        ReferenceDataModel referenceDataModel = service.get(id)
        service.finaliseModel(referenceDataModel, editor, null, null)
        def newVersion = service.createNewDocumentationVersion(referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        checkAndSave(newVersion)

        when: 'trying to create a new version on the old datamodel'
        def result = service.createNewForkModel("${referenceDataModel.label}-1", referenceDataModel, editor, false, userSecurityPolicyManager, [
            moveDataFlows: false,
            throwErrors  : true
        ])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.datamodel.new.version.superseded.message' }
    }

    void 'DMSV01 : test validation on valid model'() {
        given:
        ReferenceDataModel check = complexReferenceDataModel

        expect:
        !service.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, type: ReferenceDataModelType.DATA_ASSET, folder: testFolder, authority: testAuthority)

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin))

        when:
        ReferenceDataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('dataTypes[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    /*void 'DMSV04 : test validation on invalid dataclass model'() {
        given:
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataClasses(new DataClass(createdByUser: admin))

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataElements(createdByUser: admin)
        check.addToDataClasses(parent)

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin, label: 'ref')
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdByUser: admin, label: 'ref'))

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdByUser: admin)
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdByUser: admin, label: 'ref', referenceClass: dc))

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        DataClass child = new DataClass(createdByUser: admin, label: 'child')
        child.addToDataElements(createdByUser: admin, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdByUser: admin, label: 'grandparent')
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        grandparent.addToDataClasses(parent)
        parent.addToDataClasses(new DataClass(createdByUser: admin))
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdByUser: admin, label: 'other'))

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdByUser: reader1, label: 'test invalid', type: ReferenceDataModelType.DATA_ASSET, folder: testFolder,
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
        ReferenceDataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('childDataClasses[0].dataClasses[0].dataClasses[0].dataElements[0].dataType')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }*/
}