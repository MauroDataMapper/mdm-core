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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
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
        mockDomains(ReferenceDataType, ReferencePrimitiveType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement, ReferenceDataValue)
    }

    ReferenceDataModel buildExampleReferenceDataModel() {
        BootstrapModels.buildAndSaveExampleReferenceDataModel(messageSource, testFolder, testAuthority)
    }

    @Override
    ReferenceDataModel createValidDomain(String label) {
        new ReferenceDataModel(folder: testFolder, label: label, createdBy: UNIT_TEST, authority: testAuthority)
    }

    @Override
    void setValidDomainOtherValues() {
        
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceDataModel domain) {
        
    }

    void 'test example ReferenceDataModel valid'() {
        when:
        ReferenceDataModel simple = buildExampleReferenceDataModel()

        then:
        check(simple)

        and:
        checkAndSave(simple)
    }

    
    void 'simple diff of label'() {
        when:
        def dm1 = new ReferenceDataModel(label: 'test model 1', folder: testFolder, authority: testAuthority, createdBy: UNIT_TEST)
        def dm2 = new ReferenceDataModel(label: 'test model 2', folder: testFolder, authority: testAuthority, createdBy: UNIT_TEST)
        Diff<ReferenceDataModel> diff = dm1.diff(dm2, null)

        then:
        diff.getNumberOfDiffs() == 1

        when:
        dm2.label = 'test model 1'
        diff = dm1.diff(dm2, null)

        then:
        diff.objectsAreIdentical()
    }


    void 'test adding new ReferenceDataTypes to ReferenceDataModel'() {

        given:
        setValidDomainValues()

        when: 'adding referencedatatypes'
        domain.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'string'))
        domain.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'integer'))

        then:
        checkAndSave(domain)
        domain.count() == 1
        ReferencePrimitiveType.count() == 2

        when: 'adding invalid reference datatype and annotation'
        domain.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: UNIT_TEST, description: 'string'))
        domain.addToAnnotations(label: 'annotation', createdBy: UNIT_TEST)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.allErrors.size() == 1
    }

    
    void 'diff in ReferenceDataElement'() {
        when:
        def dm1 = new ReferenceDataModel(label: 'test model', createdBy: UNIT_TEST, folder: testFolder, authority: testAuthority)
        def dt = new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'string')
        dm1.addToReferenceDataTypes(dt)


        def dm2 = new ReferenceDataModel(label: 'test model', createdBy: UNIT_TEST, folder: testFolder, authority: testAuthority)
        dt = new ReferencePrimitiveType(createdBy: UNIT_TEST, label: 'different string')
        dm2.addToReferenceDataTypes(dt)


        Diff<ReferenceDataModel> diff = dm1.diff(dm2, null)
        then:
        diff.diffs.size() == 1
        diff.diffs.any {it.fieldName == 'referenceDataTypes'}


        when:
        dm2.label = "test model 2"
        diff = dm1.diff(dm2, null)

        then:
        diff.diffs.size() == 2
        diff.diffs.any {it.fieldName == 'referenceDataTypes'}
        diff.diffs.any {it.fieldName == 'label'}

    }
}
