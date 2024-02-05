/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.ReferenceDataTypeSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceEnumerationTypeSpec extends ReferenceDataTypeSpec<ReferenceEnumerationType> implements DomainUnitTest<ReferenceEnumerationType> {

    ReferenceEnumerationValue ev1, ev2

    def setup() {
        log.debug('Setting up EnumerationTypeSpec unit')
        mockDomain(ReferenceEnumerationValue)
        ev1 = new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ev1', key: 'key1', value: 'val1')
        ev2 = new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ev2', key: 'key2', value: 'val2')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.addToReferenceEnumerationValues(ev1)
        domain.addToReferenceEnumerationValues(ev2)
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceEnumerationType subDomain) {
        assert subDomain.referenceEnumerationValues.size() == 2
        assert subDomain.referenceEnumerationValues.find {it.key == 'key1' && it.order == 0}
        assert subDomain.referenceEnumerationValues.find {it.key == 'key2' && it.order == 1}
    }

    @Override
    ReferenceEnumerationType createValidDomain(String label) {
        ReferenceEnumerationType et = super.createValidDomain(label)
        et.addToReferenceEnumerationValues(new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ev3', key: 'key3', value: 'val3'))
    }

    @Override
    int getExpectedBaseLevelOfDiffs() {
        3 // Enumeration values
    }

    void 'test updating enumeration values'() {
        given:
        setValidDomainValues()

        when:
        domain.addToReferenceEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ev1', key: 'key1', value: 'val12')
        checkAndSave(domain)

        then:
        noExceptionThrown()
        ReferenceEnumerationType.count() == 1
        ReferenceEnumerationValue.count() == 2

        when:
        item = findById()

        then:
        item.referenceEnumerationValues.find {it.key == 'key1'}.value == 'val12'
    }

    void 'test updating enumeration values with index'() {
        given:
        setValidDomainValues()

        when:
        ReferenceEnumerationValue ev = new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ev1', key: 'key3', value: 'val3', index: 0)
        domain.addToReferenceEnumerationValues(ev)
        checkAndSave(domain)

        then:
        noExceptionThrown()
        ReferenceEnumerationType.count() == 1
        ReferenceEnumerationValue.count() == 3

        when:
        item = findById()

        then:
        item.referenceEnumerationValues.find {it.key == 'key3'}.value == 'val3'
        item.referenceEnumerationValues.sort()[0].value == 'val3'
        item.referenceEnumerationValues.sort()[1].value == 'val1'
        item.referenceEnumerationValues.sort()[2].value == 'val2'
    }

    void 'test updating enumeration values with index 2'() {
        given:
        setValidDomainValues()

        when:
        ReferenceEnumerationValue ev = new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ev1', key: 'key3', value: 'val3', index: 1)
        domain.addToReferenceEnumerationValues(ev)
        checkAndSave(domain)

        then:
        noExceptionThrown()
        ReferenceEnumerationType.count() == 1
        ReferenceEnumerationValue.count() == 3

        when:
        item = findById()

        then:
        item.referenceEnumerationValues.find {it.key == 'key3'}.value == 'val3'
        item.referenceEnumerationValues.sort()[0].value == 'val1'
        item.referenceEnumerationValues.sort()[1].value == 'val3'
        item.referenceEnumerationValues.sort()[2].value == 'val2'
    }
}
