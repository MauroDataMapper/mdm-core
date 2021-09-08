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
package uk.ac.ox.softeng.maurodatamapper.referencedata.test

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

import java.lang.reflect.ParameterizedType

@Slf4j
abstract class ReferenceDataTypeSpec<K extends ReferenceDataType> extends ModelItemSpec<K> {

    public ReferenceDataModel dataSet

    def setup() {
        log.debug('Setting up ReferenceDataTypeSpec unit')
        mockDomain(ReferenceDataModel)

        dataSet = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dataSet', folder: testFolder, authority: testAuthority)

        checkAndSave(dataSet)
        assert ReferenceDataModel.count() == 1
    }

    @Override
    void setValidDomainValues() {
        super.setValidDomainValues() as K
        dataSet.addToReferenceDataTypes(domain)
    }

    @Override
    void verifyDomainConstraints(K domain) {
        super.verifyDomainConstraints(domain)
        assert domain.referenceDataModel.id == dataSet.id
    }

    @Override
    K createValidDomain(String label) {
        K domain = ((Class<K>) getDomainUnderTest()).getDeclaredConstructor(Map).newInstance(label: label, createdBy: editor.emailAddress)
        domain.referenceDataModel = dataSet
        domain
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
    void wipeModel() {
        domain.referenceDataModel = null
        domain.breadcrumbTree = null
    }

    @Override
    void setModel(K domain, Model model) {
        domain.referenceDataModel = model as ReferenceDataModel
    }

    void 'DT01 : test unique label naming'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)

        when: 'adding data type with same label to existing'
        ReferenceDataType addtl = createValidDomain(domain.label)
        dataSet.addToReferenceDataTypes(addtl)
        // dataset is saved so validation happens at DT level
        checkAndSave(addtl)

        then: 'datamodel should not be valid'
        thrown(InternalSpockError)
        addtl.errors.allErrors.size() >= 1
        addtl.errors.fieldErrors.any { it.field.contains('label') && it.code.contains('unique') }
    }

    void 'DT02 : test unique label naming across datamodels'() {
        given:
        setValidDomainValues()
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(label: 'another model', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder,
                                                                       authority: testAuthority)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(dataSet)
        check(referenceDataModel)

        when: 'adding data type with same label as existing to different model'
        referenceDataModel.addToReferenceDataTypes(createValidDomain(domain.label))

        then:
        checkAndSave(dataSet)
        check(referenceDataModel)

        when: 'adding multiple data types with same label'
        referenceDataModel.addToReferenceDataTypes(createValidDomain('a'))
        referenceDataModel.addToReferenceDataTypes(createValidDomain('b'))
        referenceDataModel.addToReferenceDataTypes(createValidDomain('a'))

        then: 'dataset is still valid'
        checkAndSave(dataSet)

        when: 'datamodel should be invalid'
        checkAndSave(referenceDataModel)

        then:
        thrown(InternalSpockError)
        referenceDataModel.errors.allErrors.size() == 1
        referenceDataModel.errors.fieldErrors.any {it.field.contains('referenceDataTypes') && it.code.contains('unique')}
    }

    private Class<K> getDomainUnderTest() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().genericSuperclass
        (Class<K>) parameterizedType?.actualTypeArguments[0]
    }
}
