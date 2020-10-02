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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration


import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceEnumerationValueSpec extends ModelItemSpec<ReferenceEnumerationValue> implements DomainUnitTest<ReferenceEnumerationValue> {

    ReferenceEnumerationType enumerationType
    DataModel dataSet

    def setup() {
        log.debug('Setting up EnumerationValueSpec unit')
        mockDomains(DataModel, ReferenceEnumerationType)
        dataSet = new DataModel(createdByUser: admin, label: 'dataSet', folder: testFolder, authority: testAuthority)
        enumerationType = new ReferenceEnumerationType(createdByUser: admin, label: 'et', dataModel: dataSet)
        dataSet.addToDataTypes(enumerationType)
        checkAndSave(dataSet)
    }

    @Override
    void setValidDomainOtherValues() {
        enumerationType.addToReferenceEnumerationValues(domain)
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
        enumerationType.addToReferenceEnumerationValues(ev)
        ev
    }

    @Override
    Model getOwningModel() {
        dataSet
    }

    @Override
    String getModelFieldName() {
        'enumerationType'
    }

    @Override
    int getExpectedBaseLevelOfDiffs() {
        3 // Key and Value
    }

    @Override
    void wipeModel() {
        domain.breadcrumbTree = null
        domain.enumerationType = null
    }

    @Override
    void setModel(ReferenceEnumerationValue domain, Model model) {
        domain.enumerationType = enumerationType
    }

    @Override
    void wipeBasicConstrained() {
        domain.aliasesString = null
        domain.key = null
    }

    @Override
    int getExpectedConstrainedErrors() {
        2
    }

    @Override
    void setBasicConstrainedBlank() {
        domain.key = ''
        domain.value = ''
        domain.aliasesString = ''
    }

    @Override
    int getExpectedConstrainedBlankErrors() {
        6
    }
}
