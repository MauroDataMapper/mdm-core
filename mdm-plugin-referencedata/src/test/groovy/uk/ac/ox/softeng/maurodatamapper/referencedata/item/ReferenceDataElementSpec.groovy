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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item


import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class ReferenceDataElementSpec extends ModelItemSpec<ReferenceDataElement> implements DomainUnitTest<ReferenceDataElement> {

    ReferenceDataModel dataSet
    ReferenceDataType dataType

    def setup() {
        log.debug('Setting up DataClassSpec unit')
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceDataType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        dataSet = new ReferenceDataModel(createdByUser: admin, label: 'dataSet', folder: testFolder, authority: testAuthority)

        checkAndSave(dataSet)
        assert ReferenceDataModel.count() == 1

        dataType = new ReferencePrimitiveType(createdByUser: admin, label: 'datatype')
        
        dataSet.addToDataTypes(dataType)
        checkAndSave(dataSet)
    }

    @Override
    void setValidDomainOtherValues() {
        dataClass.addToDataElements(domain)
        domain.referenceDataType = dataType
        domain
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceDataElement subDomain) {
        assert subDomain.model.id == dataSet.id
        assert subDomain.referenceDataType.id == dataType.id
    }

    @Override
    ReferenceDataElement createValidDomain(String label) {
        ReferenceDataElement element = new ReferenceDataElement(label: label, referenceReferenceDataModel: dataSet, referenceDataType: dataType, createdBy: editor.emailAddress)
        element
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        null
    }

    @Override
    void setModel(ReferenceDataElement domain, Model model) {
        domain.dataClass.referenceReferenceDataModel = model as ReferenceDataModel
    }

    @Override
    void wipeModel() {
        domain.breadcrumbTree = null
    }

    /*void 'test unique label naming'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)

        when: 'adding data element with same label to dataclass'
        dataClass.addToDataElements(label: domain.label, createdByUser: admin, dataType: dataType)
        checkAndSave(dataClass)

        then: 'dataclass should not be valid'
        thrown(InternalSpockError)
        dataClass.errors.fieldErrors.any { it.field.contains('label') && it.code.contains('unique') }
    }

    void 'test unique label naming across dataclasses'() {
        given:
        setValidDomainValues()
        DataClass other = new DataClass(label: 'other', createdByUser: editor)
        dataSet.addToDataClasses(other)

        when: 'adding data element with same label to new dataclass'
        other.addToDataElements(label: domain.label, createdByUser: admin, dataType: dataType)

        then: 'should be valid'
        checkAndSave(dataClass)
        checkAndSave(other)
        checkAndSave(dataSet)

        when: 'adding a child dataclass'
        DataClass child = new DataClass(label: 'child', createdByUser: editor)
        dataClass.addToDataClasses(child)
        child.addToDataElements(label: domain.label, createdByUser: admin, dataType: dataType)

        then: 'should be valid'
        checkAndSave(dataSet)
        checkAndSave(child)
        checkAndSave(dataClass)
        checkAndSave(other)
    }*/
}
