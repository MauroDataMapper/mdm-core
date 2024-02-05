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
package uk.ac.ox.softeng.maurodatamapper.datamodel.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import groovy.util.logging.Slf4j
import org.grails.testing.GrailsUnitTest

import static org.junit.Assert.assertEquals

/**
 * @since 15/03/2022
 */
@Slf4j
class IntegerIntervalHelperSpec extends MdmSpecification implements GrailsUnitTest {

    IntegerIntervalHelper iih

    def cleanup() {
        iih = null
    }

    void 'simple case'() {
        when:
        //Simple interval
        iih = new IntegerIntervalHelper(1, 500)

        then:
        checkIntervals(50, '0 - 49', '500 - 549')
    }

    void 'Negative minimum left of boundary'() {
        when:
        //Negative minimum left of boundary
        iih = new IntegerIntervalHelper(-30000001, 19999999)

        then:
        checkIntervals(5000000, '-35000000 - -30000001', '15000000 - 19999999')
    }

    void 'Negative minimum on boundary'() {
        when:
        //Negative minimum on boundary
        iih = new IntegerIntervalHelper(-30000000, 19999999)

        then:
        checkIntervals(5000000, '-30000000 - -25000001', '15000000 - 19999999')
    }

    void 'Negative minimum right of boundary'() {
        when:
        //Negative minimum right of boundary
        iih = new IntegerIntervalHelper(-29999999, 19999999)

        then:
        checkIntervals(5000000, '-30000000 - -25000001', '15000000 - 19999999')
    }

    void 'Negative max, left of boundary'() {
        when:
        //Negative max, left of boundary
        iih = new IntegerIntervalHelper(-5100, -1001)

        then:
        checkIntervals(500, '-5500 - -5001', '-1500 - -1001')
    }

    void 'Negative max, onboundary'() {
        when:
        //Negative max, onboundary
        iih = new IntegerIntervalHelper(-5100, -1000)

        then:
        checkIntervals(500, '-5500 - -5001', '-1000 - -501')
    }

    void 'Negative max, right of boundary'() {
        when:
        //Negative max, right of boundary
        iih = new IntegerIntervalHelper(-5100, -999)

        then:
        checkIntervals(500, '-5500 - -5001', '-1000 - -501')
    }

    void 'Zero interval'() {
        when:
        //Zero interval
        iih = new IntegerIntervalHelper(83, 83)

        then:
        checkIntervals(1, '83', '83')
    }

    void 'Positive min and max, both left of boundary'() {
        when:
        //Positive min and max, both left of boundary
        iih = new IntegerIntervalHelper(999, 5999)

        then:
        checkIntervals(500, '500 - 999', '5500 - 5999')
    }

    void 'Positive min and max, both on boundary'() {
        when:
        //Positive min and max, both on boundary
        iih = new IntegerIntervalHelper(1000, 6000)

        then:
        checkIntervals(500, '1000 - 1499', '6000 - 6499')
    }

    void 'Positive min and max, both right of boundary'() {
        when:
        //Positive min and max, both right of boundary
        iih = new IntegerIntervalHelper(1001, 6001)

        then:
        checkIntervals(500, '1000 - 1499', '6000 - 6499')
    }

    void 'Beyond defined intervals'() {
        when:
        //Beyond defined intervals
        iih = new IntegerIntervalHelper(123, 558000000)

        then:
        checkIntervals(100000000, '0 - 99999999', '500000000 - 599999999')
    }

    private void checkIntervals(int length, String firstKey, String lastKey) {
        log.warn('{}', iih)
        assertEquals "Interval length", length, iih.intervalLength
        assertEquals "First key", firstKey, iih.orderedKeys.first()
        assertEquals "Last key", lastKey, iih.orderedKeys.last()
    }
}
