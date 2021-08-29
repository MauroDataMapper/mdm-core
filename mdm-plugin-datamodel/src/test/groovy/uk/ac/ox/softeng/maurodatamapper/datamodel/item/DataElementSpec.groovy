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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class DataElementSpec extends ModelItemSpec<DataElement> implements DomainUnitTest<DataElement> {

    DataModel dataSet
    DataClass dataClass
    DataType dataType

    def setup() {
        log.debug('Setting up DataClassSpec unit')
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)

        dataSet = new DataModel(createdByUser: admin, label: 'dataSet', folder: testFolder, authority: testAuthority)

        checkAndSave(dataSet)
        assert DataModel.count() == 1

        dataType = new PrimitiveType(createdByUser: admin, label: 'datatype')
        dataClass = new DataClass(createdByUser: admin, label: 'dataClass')
        dataSet.addToDataClasses(dataClass)
        dataSet.addToDataTypes(dataType)
        checkAndSave(dataSet)
    }

    @Override
    void setValidDomainOtherValues() {
        dataClass.addToDataElements(domain)
        domain.dataType = dataType
        domain
    }

    @Override
    void verifyDomainOtherConstraints(DataElement subDomain) {
        assert subDomain.dataClass.id == dataClass.id
        assert subDomain.model.id == dataSet.id
        assert subDomain.dataType.id == dataType.id
    }

    @Override
    DataElement createValidDomain(String label) {
        DataElement element = new DataElement(label: label, dataModel: dataSet, dataType: dataType, createdBy: editor.emailAddress)
        dataClass.addToDataElements(element)
        element
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        'dataClass'
    }

    @Override
    void setModel(DataElement domain, Model model) {
        domain.dataClass = dataClass
        domain.dataClass.dataModel = model as DataModel
    }

    @Override
    void wipeModel() {
        domain.breadcrumbTree = null
        domain.dataClass = null
    }

    @Override
    int getExpectedBaseConstrainedErrorCount() {
        0
    }

    void 'test unique label naming'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataClass)

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
    }

    void 'DER01: test diffing DE rules with identical rules'() {
        DataElement a = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'd', representation: 'a+b'))
        DataElement b = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'd', representation: 'a+b'))

        when:
        ObjectDiff diff = a.diff(b, null)

        then:
        diff.objectsAreIdentical()
    }

    void 'DER02: test diffing DE rules with different rule names'() {
        DataElement a = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'd', representation: 'a+b'))
        DataElement b = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 3').addToRuleRepresentations(language: 'd', representation: 'a+b'))

        when:
        ObjectDiff diff = a.diff(b, null)

        then:
        diff.numberOfDiffs == 2
    }

    void 'DER03: test diffing DE rules with different languages rules'() {
        DataElement a = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'd', representation: 'a+b'))
        DataElement b = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'e', representation: 'a+b'))

        when:
        ObjectDiff diff = a.diff(b, null)

        then:
        diff.numberOfDiffs == 2
    }

    void 'DER04: test diffing DE rules with different rules representations'() {
        DataElement a = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'd', representation: 'a+b'))
        DataElement b = new DataElement(label: 'Functional Data Element', dataType: dataType).addToRules(name: 'rule 1')
            .addToRules(new Rule(name: 'rule 2').addToRuleRepresentations(language: 'd', representation: 'a+e'))

        when:
        ObjectDiff diff = a.diff(b, null)

        then:
        diff.objectsAreIdentical()
    }
}
