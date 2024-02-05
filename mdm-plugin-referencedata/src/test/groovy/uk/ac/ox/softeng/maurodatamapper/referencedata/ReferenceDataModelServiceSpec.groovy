/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.diff.MergeDiffService
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceDataModelServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceDataModelService> {

    UUID id
    ReferenceDataModel simpleReferenceDataModel

    def setup() {
        log.debug('Setting up ReferenceDataModelServiceSpec unit')
        mockArtefact(MergeDiffService)
        mockArtefact(BreadcrumbTreeService)
        mockArtefact(ReferenceDataElementService)
        mockArtefact(ReferenceDataTypeService)
        mockArtefact(ReferenceSummaryMetadataService)
        mockArtefact(PathService)
        mockArtefact(VersionedFolderService)
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType,
                    ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        service.breadcrumbTreeService = Stub(BreadcrumbTreeService) {
            finalise(_) >> {
                BreadcrumbTree bt ->
                    bt.finalised = true
                    bt.buildTree()

            }
        }

        simpleReferenceDataModel = buildExampleReferenceDataModel()

        ReferenceDataModel referenceDataModel1 = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test database', folder: testFolder,
                                                                        authority: testAuthority)
        ReferenceDataModel referenceDataModel2 = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test form', folder: testFolder,
                                                                        authority: testAuthority)
        ReferenceDataModel referenceDataModel3 = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test standard', folder: testFolder,
                                                                        authority: testAuthority)

        checkAndSave(referenceDataModel1)
        checkAndSave(referenceDataModel2)
        checkAndSave(referenceDataModel3)

        ReferenceDataType dt = new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integration datatype')
        referenceDataModel1.addToReferenceDataTypes(dt)
        ReferenceDataElement dataElement = new ReferenceDataElement(label: 'sdmelement', createdBy: StandardEmailAddress.UNIT_TEST, referenceDataType: dt)

        checkAndSave(referenceDataModel1)

        verifyBreadcrumbTrees()
        id = referenceDataModel1.id
    }

    ReferenceDataModel buildExampleReferenceDataModel(Authority authority) {
        BootstrapModels.buildAndSaveExampleReferenceDataModel(messageSource, testFolder, testAuthority)
    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {
        when:
        List<ReferenceDataModel> referenceDataModelList = service.list(max: 2, offset: 2, sort: 'dateCreated')

        then:
        referenceDataModelList.size() == 2

        when:
        def dm1 = referenceDataModelList[0]
        def dm2 = referenceDataModelList[1]

        then:
        dm1.label == 'test form'

        and:
        dm2.label == 'test standard'

    }

    void 'test count'() {

        expect:
        service.count() == 4
    }

    void 'test delete'() {

        expect:
        service.count() == 4
        ReferenceDataModel dm = service.get(id)

        when:
        service.delete(dm)
        service.save(dm)

        then:
        ReferenceDataModel.countByDeleted(false) == 3
        ReferenceDataModel.countByDeleted(true) == 1
    }

    void 'test save'() {

        when:
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'saving test', folder: testFolder,
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

    void 'DMSV01 : test validation on valid model'() {
        given:
        ReferenceDataModel check = simpleReferenceDataModel

        expect:
        !service.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        ReferenceDataModel check = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder, authority: testAuthority)

        when:
        ReferenceDataModel invalid = service.validate(check)

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
        ReferenceDataModel check = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', folder: testFolder, authority: testAuthority)
        check.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST))

        when:
        ReferenceDataModel invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('referenceDataTypes[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

       void 'DMSV06 : test validation on invalid reference datatype model'() {
           given:
           ReferenceDataModel check = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'test invalid', folder: testFolder, authority: testAuthority)
           check.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST))

           when:
           ReferenceDataModel invalid = service.validate(check)

           then:
           invalid.hasErrors()
           invalid.errors.errorCount == 1
           invalid.errors.globalErrorCount == 0
           invalid.errors.fieldErrorCount == 1
           invalid.errors.getFieldError('referenceDataTypes[0].label')

           cleanup:
           GormUtils.outputDomainErrors(messageSource, invalid)
       }
}