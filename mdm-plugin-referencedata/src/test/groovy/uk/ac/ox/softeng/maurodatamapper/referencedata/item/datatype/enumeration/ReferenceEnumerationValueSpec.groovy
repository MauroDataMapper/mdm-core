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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceEnumerationValueSpec extends ModelItemSpec<ReferenceEnumerationValue> implements DomainUnitTest<ReferenceEnumerationValue> {

    ReferenceEnumerationType referenceEnumerationType
    ReferenceDataModel dataSet

    def setup() {
        log.debug('Setting up EnumerationValueSpec unit')
        mockDomains(ReferenceDataModel, ReferenceEnumerationType)
        dataSet = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dataSet', folder: testFolder, authority: testAuthority)
        referenceEnumerationType = new ReferenceEnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'et', referenceReferenceDataModel: dataSet)
        dataSet.addToReferenceDataTypes(referenceEnumerationType)
        checkAndSave(dataSet)
    }

    @Override
    void setValidDomainOtherValues() {
        referenceEnumerationType.addToReferenceEnumerationValues(domain)
        domain.key = 'ev_key'
        domain.value = 'ev_value'
        domain
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceEnumerationValue domain) {
        assert domain.value == 'ev_value'
        assert domain.key == 'ev_key'
        assert domain.order == 0
    }

    @Override
    ReferenceEnumerationValue createValidDomain(String label) {
        ReferenceEnumerationValue ev = new ReferenceEnumerationValue(key: label, value: 'another_value', createdBy: editor.emailAddress)
        referenceEnumerationType.addToReferenceEnumerationValues(ev)
        ev
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        'referenceEnumerationType'
    }

    @Override
    int getExpectedBaseLevelOfDiffs() {
        3 // Key and Value
    }

    @Override
    void wipeModel() {
        domain.breadcrumbTree = null
        domain.referenceEnumerationType = null
    }

    @Override
    void setModel(ReferenceEnumerationValue domain, Model model) {
        domain.referenceEnumerationType = referenceEnumerationType
    }

    @Override
    void wipeBasicConstrained() {
        domain.aliasesString = null
        domain.key = null
    }

    @Override
    int getExpectedConstrainedErrors() {
        3 // key label path
    }

    @Override
    void setBasicConstrainedBlank() {
        domain.key = ''
        domain.value = ''
        domain.aliasesString = ''
    }

    @Override
    int getExpectedConstrainedBlankErrors() {
        7
    }

    @Override
    String getExpectedNewlineLabel() {
        'ev_key'
    }
}
