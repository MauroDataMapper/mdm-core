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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
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

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class DataModelServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<DataModelService> {

    UUID id
    DataModel complexDataModel
    DataModel simpleDataModel

    def setup() {
        log.debug('Setting up DataModelServiceSpec unit')
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

        DataModel dataModel1 = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test database', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        DataModel dataModel2 = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test form', type: DataModelType.DATA_ASSET, folder: testFolder,
                                             authority: testAuthority)
        DataModel dataModel3 = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test standard', type: DataModelType.DATA_STANDARD, folder: testFolder,
                                             authority: testAuthority)

        checkAndSave(dataModel1)
        checkAndSave(dataModel2)
        checkAndSave(dataModel3)

        DataType dt = new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integration datatype')
        dataModel1.addToDataTypes(dt)
        DataElement dataElement = new DataElement(label: 'sdmelement', createdBy: StandardEmailAddress.UNIT_TEST, dataType: dt)
        dataModel1.addToDataClasses(new DataClass(label: 'sdmclass', createdBy: StandardEmailAddress.UNIT_TEST).addToDataElements(dataElement))

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

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {
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

    void 'test count'() {

        expect:
        service.count() == 5
    }

    void 'test delete'() {

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

    void 'test save'() {

        when:
        DataModel dataModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'saving test', type: DataModelType.DATA_STANDARD, folder: testFolder,
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

    void 'DMSV01 : test validation on valid model'() {
        given:
        DataModel check = complexDataModel
        service.validate(check)

        expect:
        !check.hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, type: DataModelType.DATA_ASSET, folder: testFolder, authority: testAuthority)

        when:
        DataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 2
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 2
        invalid.errors.getFieldError('label')
        invalid.errors.getFieldError('path')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV03 : test validation on invalid primitive datatype model'() {
        given:
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parent')
        parent.addToDataElements(createdBy: StandardEmailAddress.UNIT_TEST)
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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ref')
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ref'))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass dc = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST)
        check.addToDataClasses(dc)
        check.addToDataTypes(new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ref', referenceClass: dc))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parent')
        parent.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST))
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'other'))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parent')
        DataClass child = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'child')
        child.addToDataElements(createdBy: StandardEmailAddress.UNIT_TEST, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(parent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'other'))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'grandparent')
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parent')
        grandparent.addToDataClasses(parent)
        parent.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST))
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'other'))

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
        DataModel check = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', type: DataModelType.DATA_ASSET, folder: testFolder,
                                        authority: testAuthority)
        DataClass grandparent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'grandparent')
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parent')
        grandparent.addToDataClasses(parent)
        DataClass child = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'child')
        child.addToDataElements(createdBy: StandardEmailAddress.UNIT_TEST, label: 'el')
        parent.addToDataClasses(child)
        check.addToDataClasses(grandparent)
        check.addToDataClasses(new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'other'))

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