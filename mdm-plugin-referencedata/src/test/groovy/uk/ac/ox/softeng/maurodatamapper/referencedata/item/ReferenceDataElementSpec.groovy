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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class ReferenceDataElementSpec extends ModelItemSpec<ReferenceDataElement> implements DomainUnitTest<ReferenceDataElement> {

    ReferenceDataModel dataSet
    ReferenceDataType referenceDataType

    def setup() {
        log.debug('Setting up DataClassSpec unit')
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceDataType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        dataSet = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dataSet', folder: testFolder, authority: testAuthority)

        checkAndSave(dataSet)
        assert ReferenceDataModel.count() == 1

        referenceDataType = new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'datatype')
        
        dataSet.addToReferenceDataTypes(referenceDataType)
        checkAndSave(dataSet)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.referenceDataType = referenceDataType
        domain
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceDataElement subDomain) {
        assert subDomain.model.id == dataSet.id
        assert subDomain.referenceDataType.id == referenceDataType.id
    }

    @Override
    ReferenceDataElement createValidDomain(String label) {
        ReferenceDataElement element = new ReferenceDataElement(label: label, referenceDataModel: dataSet, referenceDataType: referenceDataType, createdBy: editor.emailAddress)
        element
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        'referenceDataModel'
    }

    @Override
    void setModel(ReferenceDataElement domain, Model model) {
        domain.referenceDataModel = model as ReferenceDataModel
    }

    @Override
    void wipeModel() {
        domain.referenceDataModel = null
        domain.breadcrumbTree = null
    }

    void 'test unique label naming'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)

        when: 'adding reference data element with same label to reference data model'
        dataSet.addToReferenceDataElements(label: domain.label, createdBy: StandardEmailAddress.UNIT_TEST, referenceDataType: referenceDataType)
        checkAndSave(dataSet)

        then: 'dataSet should not be valid'
        thrown(InternalSpockError)
        dataSet.errors.fieldErrors.any { it.field.contains('label') && it.code.contains('unique') }
    }

    void 'test unique label naming across reference reference data models'() {
        given:
        setValidDomainValues()
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(label: 'another model', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder,
                                                                       authority: testAuthority)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)
        checkAndSave(referenceDataModel)

        when: 'adding reference data element with same label as existing to different model'
        referenceDataModel.addToReferenceDataElements(createValidDomain(domain.label))

        then:
        checkAndSave(dataSet)
        checkAndSave(referenceDataModel)

        when: 'adding multiple reference data elements with same label'
        referenceDataModel.addToReferenceDataElements(createValidDomain('a'))
        referenceDataModel.addToReferenceDataElements(createValidDomain('b'))
        referenceDataModel.addToReferenceDataElements(createValidDomain('a'))

        then: 'dataset is still valid'
        checkAndSave(dataSet)

        when: 'datamodel should be invalid'
        checkAndSave(referenceDataModel)

        then:
        thrown(InternalSpockError)
        referenceDataModel.errors.allErrors.size() == 2
        referenceDataModel.errors.fieldErrors.any {it.field.contains('referenceDataElements[1].label') && it.code.contains('unique')}
        referenceDataModel.errors.fieldErrors.any {it.field.contains('referenceDataElements[3].label') && it.code.contains('unique')}
    }
}
