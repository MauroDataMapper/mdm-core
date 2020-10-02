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
package uk.ac.ox.softeng.maurodatamapper.referencedata.test


import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

import java.lang.reflect.ParameterizedType

@Slf4j
abstract class ReferenceDataTypeSpec<K extends ReferenceDataType> extends ModelItemSpec<K> {

    public DataModel dataSet

    def setup() {
        log.debug('Setting up DataTypeSpec unit')
        mockDomain(DataModel)

        dataSet = new DataModel(createdByUser: admin, label: 'dataSet', folder: testFolder, authority: testAuthority)

        checkAndSave(dataSet)
        assert DataModel.count() == 1
    }

    @Override
    void setValidDomainValues() {
        super.setValidDomainValues() as K
        dataSet.addToDataTypes(domain)
    }

    @Override
    void verifyDomainConstraints(K domain) {
        super.verifyDomainConstraints(domain)
        assert domain.dataModel.id == dataSet.id
    }

    @Override
    K createValidDomain(String label) {
        K domain = ((Class<K>) getDomainUnderTest()).getDeclaredConstructor(Map).newInstance(label: label, createdBy: editor.emailAddress)
        domain.dataModel = dataSet
        domain
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        'dataModel'
    }

    @Override
    void wipeModel() {
        domain.dataModel = null
        domain.breadcrumbTree = null
    }

    @Override
    void setModel(K domain, Model model) {
        domain.dataModel = model as DataModel
    }

    void 'DT01 : test unique label naming'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)

        when: 'adding data type with same label to existing'
        dataSet.addToDataTypes(createValidDomain(domain.label))
        checkAndSave(dataSet)

        then: 'datamodel should not be valid'
        thrown(InternalSpockError)
        dataSet.errors.allErrors.size() >= 3
        dataSet.errors.fieldErrors.any {it.field.contains('label') && it.code.contains('unique')}
    }

    void 'DT02 : test unique label naming across datamodels'() {
        given:
        setValidDomainValues()
        DataModel dataModel = new DataModel(label: 'another model', createdByUser: editor, folder: testFolder, authority: testAuthority)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)
        checkAndSave(dataModel)

        when: 'adding data type with same label as existing to different model'
        dataModel.addToDataTypes(createValidDomain(domain.label))

        then:
        checkAndSave(dataSet)
        checkAndSave(dataModel)

        when: 'adding multiple data types with same label'
        dataModel.addToDataTypes(createValidDomain('a'))
        dataModel.addToDataTypes(createValidDomain('b'))
        dataModel.addToDataTypes(createValidDomain('a'))

        then: 'dataset is still valid'
        checkAndSave(dataSet)

        when: 'datamodel should be invalid'
        checkAndSave(dataModel)

        then:
        thrown(InternalSpockError)
        dataModel.errors.allErrors.size() == 3
        dataModel.errors.fieldErrors.any {it.field.contains('dataTypes') && it.code.contains('unique')}
        dataModel.errors.fieldErrors.any {it.field.contains('dataTypes[1].label') && it.code.contains('unique')}
        dataModel.errors.fieldErrors.any {it.field.contains('dataTypes[3].label') && it.code.contains('unique')}
    }

    private Class<K> getDomainUnderTest() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().genericSuperclass
        (Class<K>) parameterizedType?.actualTypeArguments[0]
    }
}
