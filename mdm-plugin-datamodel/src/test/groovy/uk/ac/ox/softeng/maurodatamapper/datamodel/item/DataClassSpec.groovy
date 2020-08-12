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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
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
class DataClassSpec extends ModelItemSpec<DataClass> implements DomainUnitTest<DataClass> {

    DataModel dataSet

    def setup() {
        log.debug('Setting up DataClassSpec unit')
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)

        dataSet = new DataModel(createdByUser: admin, label: 'dataSet', folder: testFolder, authority: testAuthority)
        dataSet.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        dataSet.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'integer'))

        checkAndSave(dataSet)
        assert DataModel.count() == 1
        assert DataType.count() == 2
    }

    @Override
    void wipeModel() {
        domain.dataModel = null
        domain.breadcrumbTree = null
    }

    @Override
    void setModel(DataClass domain, Model model) {
        domain.dataModel = model as DataModel
    }

    @Override
    void setValidDomainOtherValues() {
        dataSet.addToDataClasses(domain)
        domain
    }

    @Override
    void verifyDomainOtherConstraints(DataClass domain) {
        assert domain.model.id == dataSet.id
        assert !domain.parentDataClass
    }

    @Override
    DataClass createValidDomain(String label) {
        DataClass other = new DataClass(label: label, createdBy: editor.emailAddress)
        dataSet.addToDataClasses(other)
        other
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        'dataModel'
    }

    void 'test adding child dataclasses'() {
        given:
        setValidDomainValues()

        when:
        domain.addToDataClasses(createdByUser: editor, label: 'child')
        domain.addToDataClasses(createdByUser: editor, label: 'child2')

        then:
        checkAndSave(domain)
        DataClass.count() == 3

        when:
        item = findById()
        DataClass child = DataClass.findByLabel('child')

        then:
        item.dataClasses.size() == 2
        child.model.id == dataSet.id
        child.parentDataClass.id == item.id

        when:
        DataClass child3 = createValidDomain('child3')
        checkAndSave(child3)
        DataClass item2 = DataClass.findByLabel('child3')

        then:
        DataClass.count() == 4
        item2.depth == 1
        item2.path == "/$dataSet.id"
        item2.model.id == dataSet.id
        !item2.parentDataClass

        when:
        domain.addToDataClasses(item2)
        checkAndSave(domain)
        DataClass item3 = DataClass.findByLabel('child3')

        then:
        DataClass.count() == 4
        item2.id == item3.id
        item3.depth == 2
        item3.path == "/$dataSet.id/${domain.id}"
        item3.model.id == dataSet.id
        item3.parentDataClass.id == item.id
    }

    void 'test adding dataelements'() {
        given:
        setValidDomainValues()

        when:
        domain.addToDataElements(createdByUser: reader1, label: 'element1', dataType: dataSet.findDataTypeByLabel('string'))
        domain.addToDataElements(createdByUser: reader1, label: 'element2', dataType: dataSet.findDataTypeByLabel('integer'))

        then:
        checkAndSave(domain)
        DataClass.count() == 1
        DataElement.count() == 2

        when:
        item = findById()
        DataElement element = DataElement.findByLabel('element1')

        then:
        item.dataElements.size() == 2

        and:
        element.depth == 2
        element.path == "/${dataSet.id}/${domain.id}"
        element.model.id == dataSet.id
        element.dataClass.id == item.id
    }

    void 'test unique label naming for direct child dataclasses of datamodel'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)

        when: 'adding another child dataclass to the datamodel'
        dataSet.addToDataClasses(label: domain.label, createdByUser: admin)
        checkAndSave(dataSet)

        then: 'datamodel should not be valid'
        thrown(InternalSpockError)
        dataSet.errors.fieldErrors.any {it.field.contains('label') && it.code.contains('unique')}
    }

    void 'test unique label naming for direct child dataclasses of 2 datamodels'() {
        given:
        setValidDomainValues()
        DataModel dataModel = new DataModel(label: 'another mode', createdByUser: editor, folder: testFolder, authority: testAuthority)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)
        checkAndSave(dataModel)

        when: 'adding another child dataclass to the datamodel'
        dataModel.addToDataClasses(label: domain.label, createdByUser: admin)

        then: 'datamodel should be valid'
        checkAndSave(dataSet)
        checkAndSave(dataModel)
    }

    void 'test unique label naming for child dataclasses of dataclass'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)

        when: 'adding child dataclass to the dataclass with same label as parent'
        domain.addToDataClasses(label: domain.label, createdByUser: admin)

        then: 'domain should be valid'
        checkAndSave(domain)

        when: 'adding another dataclass to domain'
        domain.addToDataClasses(label: 'another', createdByUser: admin)

        then: 'domain should be valid'
        checkAndSave(domain)

        when: 'adding another dataclass to domain using same label'
        domain.addToDataClasses(label: 'another', createdByUser: admin)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.fieldErrors.any {it.field.contains('label') && it.code.contains('unique')}
    }
}
