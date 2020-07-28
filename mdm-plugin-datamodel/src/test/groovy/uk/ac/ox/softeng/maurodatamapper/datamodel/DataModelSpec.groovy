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
package uk.ac.ox.softeng.maurodatamapper.datamodel


import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.DataBootstrap
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class DataModelSpec extends ModelSpec<DataModel> implements DomainUnitTest<DataModel>, DataBootstrap {

    def setup() {
        log.debug('Setting up DataModelSpec unit')
        mockDomains(DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)
    }

    @Override
    DataModel createValidDomain(String label) {
        new DataModel(folder: testFolder, label: label, type: DataModelType.DATA_STANDARD, createdByUser: editor)
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

    void 'test updating a datamodel label'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        domain.label = 'a new better label'
        checkAndSave(domain)

        then:
        noExceptionThrown()
    }

    void 'test creating a new model with the same name as existing'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        DataModel other = new DataModel(createdBy: editor.emailAddress, label: domain.label, folder: testFolder)
        checkAndSave(other)

        then:
        thrown(InternalSpockError)
        other.errors.getFieldError('label').code == 'default.not.unique.message'
    }

    void 'simple diff of label'() {
        when:
        def dm1 = new DataModel(label: 'test model 1', folder: testFolder)
        def dm2 = new DataModel(label: 'test model 2', folder: testFolder)
        Diff<DataModel> diff = dm1.diff(dm2)

        then:
        diff.getNumberOfDiffs() == 1

        when:
        dm2.label = 'test model 1'
        diff = dm1.diff(dm2)

        then:
        diff.objectsAreIdentical()
    }

    void 'test adding dataclasses to datamodel'() {

        given:
        setValidDomainValues()
        domain.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        domain.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'integer'))

        when: 'adding empty dataclass'
        domain.addToDataClasses(createdByUser: admin, label: 'emptyclass')

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
        empty.dataModel.id == domain.id

        when: 'creating a dataclass with child dataclass'
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
        parent.addToDataClasses(createdByUser: admin, label: 'child')
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
        parentS.dataModel.id == domain.id

        and:
        childS.path == "/${domain.id}/$parent.id"
        childS.depth == 2
        childS.parentDataClass.id == parent.id
        childS.dataModel
        childS.dataModel.id == domain.id

        when: 'creating dataclass with dataelements'
        DataClass content = new DataClass(createdByUser: editor, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdByUser: editor, label: 'ele1', dataType: domain.findDataTypeByLabel('string'))
        content.addToDataElements(createdByUser: reader1, label: 'element2', dataType: domain.findDataTypeByLabel('integer'))
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
        domain.addToDataClasses(createdByUser: admin, description: 'emptyclass')
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }

    void 'test invalid dataclass child'() {
        given:
        setValidDomainValues()

        when: 'creating a dataclass with invalid dataclass'
        DataClass parent = new DataClass(createdByUser: admin, label: 'parent')
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
        DataClass content = new DataClass(createdByUser: editor, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdByUser: editor, label: 'ele1', dataType: domain.findDataTypeByLabel('string'))
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
        domain.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        domain.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'integer'))

        then:
        checkAndSave(domain)
        domain.count() == 1
        PrimitiveType.count() == 2

        when: 'adding invalid datatype and annotation'
        domain.addToDataTypes(new PrimitiveType(createdByUser: admin, description: 'string'))
        domain.addToAnnotations(label: 'annotation', createdBy: editor.emailAddress)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }

    void 'diff in class'() {
        when:
        def dm1 = new DataModel(label: 'test model', folder: testFolder)
        def dm2 = new DataModel(label: 'test model', folder: testFolder)
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
        def dm1 = new DataModel(label: 'test model', createdByUser: admin, folder: testFolder)
        def dt = new PrimitiveType(createdBy: admin.emailAddress, label: 'string')
        dm1.addToDataTypes(dt)


        dm1.addToDataClasses(new DataClass(label: 'class 1', createdByUser: admin))
        dm1.childDataClasses.each {
            c -> c.addToDataElements(new DataElement(label: 'elem 1', dataType: dt, createdByUser: admin))
        }

        def dm2 = new DataModel(label: 'test model', createdBy: admin.emailAddress, folder: testFolder)
        dt = new PrimitiveType(createdBy: admin.emailAddress, label: 'string')
        dm2.addToDataTypes(dt)

        dm2.addToDataClasses(new DataClass(label: 'class 1', createdByUser: admin))
        dm2.childDataClasses.each {
            c -> c.addToDataElements(new DataElement(label: 'elem 2', dataType: dt, createdByUser: admin))
        }


        Diff<DataModel> diff = dm1.diff(dm2)
        then:
        diff.getNumberOfDiffs() == 4

        when:
        dm2.label = "test model 2"
        diff = dm1.diff(dm2)

        then:
        diff.getNumberOfDiffs() == 5
    }

}
