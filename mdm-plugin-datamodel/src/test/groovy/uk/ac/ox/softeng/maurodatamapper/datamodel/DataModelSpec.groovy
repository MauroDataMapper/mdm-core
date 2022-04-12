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

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.UNIT_TEST

@Slf4j
class DataModelSpec extends ModelSpec<DataModel> implements DomainUnitTest<DataModel> {

    def setup() {
        log.debug('Setting up DataModelSpec unit')
        mockDomains(DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)
    }

    DataModel buildSimpleDataModel() {
        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, testFolder, testAuthority)
    }

    DataModel buildComplexDataModel() {
        BootstrapModels.buildAndSaveComplexDataModel(messageSource, testFolder, testAuthority)
    }

    @Override
    DataModel createValidDomain(String label) {
        new DataModel(folder: testFolder, label: label, type: DataModelType.DATA_STANDARD, createdBy: UNIT_TEST, authority: testAuthority)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.modelType = DataModelType.DATA_STANDARD.label
    }

    @Override
    void verifyDomainOtherConstraints(DataModel domain) {
        domain.modelType == DataModelType.DATA_STANDARD.label
    }

    void 'test simple datamodel valid'() {
        when:
        DataModel simple = buildSimpleDataModel()

        then:
        check(simple)

        and:
        checkAndSave(simple)
    }

    void 'test complex datamodel valid'() {
        when:
        DataModel complex = buildComplexDataModel()

        then:
        check(complex)

        and:
        checkAndSave(complex)
    }

    void 'simple diff of label'() {
        when:
        def dm1 = new DataModel(label: 'test model 1', folder: testFolder, authority: testAuthority, createdBy: UNIT_TEST)
        def dm2 = new DataModel(label: 'test model 2', folder: testFolder, authority: testAuthority, createdBy: UNIT_TEST)
        Diff<DataModel> diff = dm1.diff(dm2, null)

        then:
        diff.getNumberOfDiffs() == 1

        when:
        dm2.label = 'test model 1'
        diff = dm1.diff(dm2, null)

        then:
        diff.objectsAreIdentical()
    }

    void 'test adding dataclasses to datamodel'() {

        given:
        setValidDomainValues()
        domain.addToDataTypes(new PrimitiveType(createdBy: UNIT_TEST, label: 'string'))
        domain.addToDataTypes(new PrimitiveType(createdBy: UNIT_TEST, label: 'integer'))

        when: 'adding empty dataclass'
        domain.addToDataClasses(createdBy: UNIT_TEST, label: 'emptyclass', authority: testAuthority)

        then:
        checkAndSave(domain)
        domain.count() == 1
        DataClass.count() == 1

        when:
        DataClass empty = DataClass.findByLabel('emptyclass')

        then:
        empty.path == Path.from('dm', 'test$main').resolve('dc', 'emptyclass')
        !empty.parentDataClass
        empty.dataModel.id == domain.id

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
        parentS.path == Path.from(domain).resolve('dc', 'parent')
        !parentS.parentDataClass
        parentS.dataModel.id == domain.id

        and:
        childS.path == Path.from(domain).resolve('dc', 'parent').resolve('dc', 'child')
        childS.parentDataClass.id == parent.id
        childS.dataModel
        childS.dataModel.id == domain.id

        when: 'creating dataclass with dataelements'
        DataClass content = new DataClass(createdBy: UNIT_TEST, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdBy: UNIT_TEST, label: 'ele1', dataType: domain.findDataTypeByLabel('string'))
        content.addToDataElements(createdBy: UNIT_TEST, label: 'element2', dataType: domain.findDataTypeByLabel('integer'))
        domain.addToDataClasses(content)

        then:
        checkAndSave(domain)
        domain.count() == 1
        DataClass.count() == 4
        DataElement.count() == 2
        PrimitiveType.count() == 2

        when:
        DataClass contentS = DataClass.findByLabel('content')

        then:
        contentS.description == 'A dataclass with elements'
        contentS.dataElements.size() == 2
    }

    void 'test invalid dataclass'() {
        given:
        setValidDomainValues()

        when: 'adding invalid dataclass'
        domain.addToDataClasses(createdBy: UNIT_TEST, description: 'emptyclass')
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }

    void 'test invalid dataclass child'() {
        given:
        setValidDomainValues()

        when: 'creating a dataclass with invalid dataclass'
        DataClass parent = new DataClass(createdBy: UNIT_TEST, label: 'parent')
        parent.addToDataClasses(label: 'child')
        domain.addToDataClasses(parent)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }

    void 'test invalid dataclass element'() {
        given:
        setValidDomainValues()

        when: 'creating dataclass with invalid dataelement'
        DataClass content = new DataClass(createdBy: UNIT_TEST, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdBy: UNIT_TEST, label: 'ele1', dataType: domain.findDataTypeByLabel('string'))
        domain.addToDataClasses(content)
        !checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }


    void 'test adding new datatypes to datamodel'() {

        given:
        setValidDomainValues()

        when: 'adding datatypes'
        domain.addToDataTypes(new PrimitiveType(createdBy: UNIT_TEST, label: 'string'))
        domain.addToDataTypes(new PrimitiveType(createdBy: UNIT_TEST, label: 'integer'))

        then:
        checkAndSave(domain)
        domain.count() == 1
        PrimitiveType.count() == 2

        when: 'adding invalid datatype and annotation'
        domain.addToDataTypes(new PrimitiveType(createdBy: UNIT_TEST, description: 'string'))
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
        Diff<DataModel> diff = dm1.diff(dm2, null)
        then:
        diff.getNumberOfDiffs() == 1

        when:
        dm2.addToDataClasses(new DataClass(label: 'class 2'))
        diff = dm1.diff(dm2, null)

        then:
        diff.getNumberOfDiffs() == 2
    }


    void 'diff in element'() {
        when:
        def dm1 = new DataModel(label: 'test model', createdBy: UNIT_TEST, folder: testFolder, authority: testAuthority)
        def dt = new PrimitiveType(createdBy: UNIT_TEST, label: 'string')
        dm1.addToDataTypes(dt)


        dm1.addToDataClasses(new DataClass(label: 'class 1', createdBy: UNIT_TEST))
        dm1.childDataClasses.each {
            c -> c.addToDataElements(new DataElement(label: 'elem 1', dataType: dt, createdBy: UNIT_TEST))
        }

        def dm2 = new DataModel(label: 'test model', createdBy: UNIT_TEST, folder: testFolder)
        dt = new PrimitiveType(createdBy: UNIT_TEST, label: 'string')
        dm2.addToDataTypes(dt)

        dm2.addToDataClasses(new DataClass(label: 'class 1', createdBy: UNIT_TEST))
        dm2.childDataClasses.each {
            c -> c.addToDataElements(new DataElement(label: 'elem 2', dataType: dt, createdBy: UNIT_TEST))
        }


        Diff<DataModel> diff = dm1.diff(dm2, null)
        then:
        diff.getNumberOfDiffs() == 2

        when:
        dm2.label = 'test model 2'
        diff = dm1.diff(dm2, null)

        then:
        diff.getNumberOfDiffs() == 3
    }


}
