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

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels

import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.UNIT_TEST

@Slf4j
class ReferenceDataModelSpec extends ModelSpec<ReferenceDataModel> implements DomainUnitTest<ReferenceDataModel> {

    def setup() {
        log.debug('Setting up ReferenceDataModelSpec unit')
        mockDomains(ReferenceDataType, ReferencePrimitiveType, ReferenceType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement, ReferenceDataValue)
    }

    ReferenceDataModel buildSimpleReferenceDataModel() {
        BootstrapModels.buildAndSaveSimpleReferenceDataModel(messageSource, testFolder, testAuthority)
    }

    ReferenceDataModel buildComplexReferenceDataModel() {
        BootstrapModels.buildAndSaveComplexReferenceDataModel(messageSource, testFolder, testAuthority)
    }

    @Override
    ReferenceDataModel createValidDomain(String label) {
        new ReferenceDataModel(folder: testFolder, label: label, type: ReferenceDataModelType.DATA_STANDARD, createdBy: UNIT_TEST, authority: testAuthority)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.modelType = ReferenceDataModelType.DATA_STANDARD.label
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceDataModel domain) {
        domain.modelType == ReferenceDataModelType.DATA_STANDARD.label
    }

    /*void 'test simple ReferenceDataModel valid'() {
        when:
        ReferenceDataModel simple = buildSimpleReferenceDataModel()

        then:
        check(simple)

        and:
        checkAndSave(simple)
    }

    void 'test complex ReferenceDataModel valid'() {
        when:
        ReferenceDataModel complex = buildComplexReferenceDataModel()

        then:
        check(complex)

        and:
        checkAndSave(complex)
    }

    void 'simple diff of label'() {
        when:
        def dm1 = new ReferenceDataModel(label: 'test model 1', folder: testFolder, authority: testAuthority, createdBy: UNIT_TEST)
        def dm2 = new ReferenceDataModel(label: 'test model 2', folder: testFolder, authority: testAuthority, createdBy: UNIT_TEST)
        Diff<ReferenceDataModel> diff = dm1.diff(dm2)

        then:
        diff.getNumberOfDiffs() == 1

        when:
        dm2.label = 'test model 1'
        diff = dm1.diff(dm2)

        then:
        diff.objectsAreIdentical()
    }

    void 'test adding dataclasses to ReferenceDataModel'() {

        given:
        setValidDomainValues()
        domain.addToDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'string'))
        domain.addToDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'integer'))

        when: 'adding empty dataclass'
        domain.addToDataClasses(createdBy: UNIT_TEST, label: 'emptyclass', authority: testAuthority)

        then:
        checkAndSave(domain)
        domain.count() == 1
        DataClass.count() == 1

        when:
        DataClass empty = DataClass.findByLabel('emptyclass')

        then:
        empty.path == "/${domain.id}"
        empty.depth == 1
        !empty.parentDataClass
        empty.referenceDataModel.id == domain.id

        when: 'creating a dataclass with child dataclass'
        DataClass parent = new DataClass(createdBy: UNIT_TEST, label: 'parent')
        parent.addToDataClasses(createdBy: UNIT_TEST, label: 'child')
        domain.addToDataClasses(parent)

        then:
        checkAndSave(domain)
        domain.count() == 1
        DataClass.count() == 3

        when:
        DataClass parentS = DataClass.findByLabel('parent')
        DataClass childS = DataClass.findByLabel('child')

        then:
        parentS.path == "/${domain.id}"
        parentS.depth == 1
        !parentS.parentDataClass
        parentS.referenceDataModel.id == domain.id

        and:
        childS.path == "/${domain.id}/$parent.id"
        childS.depth == 2
        childS.parentDataClass.id == parent.id
        childS.referenceDataModel
        childS.referenceDataModel.id == domain.id

    }*/


    /*void 'test adding new datatypes to datamodel'() {

        given:
        setValidDomainValues()

        when: 'adding datatypes'
        domain.addToDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'string'))
        domain.addToDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'integer'))

        then:
        checkAndSave(domain)
        domain.count() == 1
        ReferencePrimitiveType.count() == 2

        when: 'adding invalid datatype and annotation'
        domain.addToDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, description: 'string'))
        domain.addToAnnotations(label: 'annotation', createdBy: UNIT_TEST)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }

    void 'diff in class'() {
        when:
        def dm1 = new DataModel(label: 'test model', folder: testFolder, authority: testAuthority)
        def dm2 = new DataModel(label: 'test model', folder: testFolder, authority: testAuthority)
        dm1.addToDataClasses(new DataClass(label: 'class 1'))
        Diff<DataModel> diff = dm1.diff(dm2)
        then:
        diff.getNumberOfDiffs() == 1

        when:
        dm2.addToDataClasses(new DataClass(label: 'class 2'))
        diff = dm1.diff(dm2)

        then:
        diff.getNumberOfDiffs() == 2
    }


    void 'diff in element'() {
        when:
        def dm1 = new DataModel(label: 'test model', createdBy: UNIT_TEST, folder: testFolder, authority: testAuthority)
        def dt = new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'string')
        dm1.addToDataTypes(dt)


        dm1.addToDataClasses(new DataClass(label: 'class 1', createdBy: UNIT_TEST))
        dm1.childDataClasses.each {
            c -> c.addToDataElements(new ReferenceDataElement(label: 'elem 1', referenceDataType: dt, createdBy: UNIT_TEST))
        }

        def dm2 = new DataModel(label: 'test model', createdBy: UNIT_TEST, folder: testFolder)
        dt = new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'string')
        dm2.addToDataTypes(dt)

        dm2.addToDataClasses(new DataClass(label: 'class 1', createdBy: UNIT_TEST))
        dm2.childDataClasses.each {
            c -> c.addToDataElements(new ReferenceDataElement(label: 'elem 2', referenceDataType: dt, createdBy: UNIT_TEST))
        }


        Diff<DataModel> diff = dm1.diff(dm2)
        then:
        diff.getNumberOfDiffs() == 4

        when:
        dm2.label = "test model 2"
        diff = dm1.diff(dm2)

        then:
        diff.getNumberOfDiffs() == 5
    }*/


}
