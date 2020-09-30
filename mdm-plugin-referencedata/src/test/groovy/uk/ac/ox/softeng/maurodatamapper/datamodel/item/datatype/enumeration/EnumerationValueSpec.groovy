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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration


import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class EnumerationValueSpec extends ModelItemSpec<EnumerationValue> implements DomainUnitTest<EnumerationValue> {

    EnumerationType enumerationType
    DataModel dataSet

    def setup() {
        log.debug('Setting up EnumerationValueSpec unit')
        mockDomains(DataModel, EnumerationType)
        dataSet = new DataModel(createdByUser: admin, label: 'dataSet', folder: testFolder, authority: testAuthority)
        enumerationType = new EnumerationType(createdByUser: admin, label: 'et', dataModel: dataSet)
        dataSet.addToDataTypes(enumerationType)
        checkAndSave(dataSet)
    }

    @Override
    void setValidDomainOtherValues() {
        enumerationType.addToEnumerationValues(domain)
        domain.key = 'ev_key'
        domain.value = 'ev_value'
        domain
    }

    @Override
    void verifyDomainOtherConstraints(EnumerationValue domain) {
        assert domain.value == 'ev_value'
        assert domain.key == 'ev_key'
        assert domain.order == 0
    }

    @Override
    EnumerationValue createValidDomain(String label) {
        EnumerationValue ev = new EnumerationValue(key: label, value: 'another_value', createdBy: editor.emailAddress)
        enumerationType.addToEnumerationValues(ev)
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
    void setModel(EnumerationValue domain, Model model) {
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
