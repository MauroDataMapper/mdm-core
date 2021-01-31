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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.DataTypeSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class EnumerationTypeSpec extends DataTypeSpec<EnumerationType> implements DomainUnitTest<EnumerationType> {

    EnumerationValue ev1, ev2

    def setup() {
        log.debug('Setting up EnumerationTypeSpec unit')
        mockDomain(EnumerationValue)
        ev1 = new EnumerationValue(createdByUser: admin, label: 'ev1', key: 'key1', value: 'val1', index: 0)
        ev2 = new EnumerationValue(createdByUser: admin, label: 'ev2', key: 'key2', value: 'val2', index: 1)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.addToEnumerationValues(ev1)
        domain.addToEnumerationValues(ev2)
    }

    @Override
    void verifyDomainOtherConstraints(EnumerationType subDomain) {
        assert subDomain.enumerationValues.size() == 2
        assert subDomain.enumerationValues.find {it.key == 'key1' && it.order == 0}
        assert subDomain.enumerationValues.find {it.key == 'key2' && it.order == 1}
    }

    @Override
    EnumerationType createValidDomain(String label) {
        EnumerationType et = super.createValidDomain(label)
        et.addToEnumerationValues(new EnumerationValue(createdByUser: admin, label: 'ev3', key: 'key3', value: 'val3'))
    }

    @Override
    int getExpectedBaseLevelOfDiffs() {
        3 // Enumeration values
    }

    void 'test updating enumeration values'() {
        given:
        setValidDomainValues()

        when:
        domain.addToEnumerationValues(createdByUser: admin, label: 'ev1', key: 'key1', value: 'val12')
        checkAndSave(domain)

        then:
        noExceptionThrown()
        EnumerationType.count() == 1
        EnumerationValue.count() == 2

        when:
        item = findById()

        then:
        item.enumerationValues.find {it.key == 'key1'}.value == 'val12'
    }

    void 'test updating enumeration values with index'() {
        given:
        setValidDomainValues()

        when:
        EnumerationValue ev = new EnumerationValue(createdByUser: admin, label: 'ev1', key: 'key3', value: 'val3', idx: 0)
        domain.addToEnumerationValues(ev)
        checkAndSave(domain)

        then:
        noExceptionThrown()
        EnumerationType.count() == 1
        EnumerationValue.count() == 3

        when:
        item = findById()

        then:
        item.enumerationValues.find {it.key == 'key3'}.value == 'val3'
        item.enumerationValues.sort()[0].value == 'val3'
        item.enumerationValues.sort()[1].value == 'val1'
        item.enumerationValues.sort()[2].value == 'val2'
    }

    void 'test updating enumeration values with index 1'() {
        given:
        setValidDomainValues()

        when:
        EnumerationValue ev = new EnumerationValue(createdByUser: admin, label: 'ev1', key: 'key3', value: 'val3', idx: 1)
        domain.addToEnumerationValues(ev)
        checkAndSave(domain)

        then:
        noExceptionThrown()
        EnumerationType.count() == 1
        EnumerationValue.count() == 3

        when:
        item = findById()

        then:
        item.enumerationValues.find {it.key == 'key3'}.value == 'val3'
        item.enumerationValues.sort()[0].value == 'val1'
        item.enumerationValues.sort()[1].value == 'val3'
        item.enumerationValues.sort()[2].value == 'val2'
    }
}
