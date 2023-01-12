/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
class DecimalIntervalHelperSpec extends MdmSpecification implements GrailsUnitTest {

    DecimalIntervalHelper dih

    def cleanup() {
        dih = null
    }

    void 'simple case'() {
        when:
        //Simple interval
        dih = new DecimalIntervalHelper(1.00, 500.0)

        then:
        checkIntervals(50.00, '0.00 - 50.00', '500.00 - 550.00')
    }

    void 'Negative minimum left of boundary'() {
        when:
        //Negative minimum left of boundary
        dih = new DecimalIntervalHelper(-30000001.00, 19999999.0)

        then:
        checkIntervals(5000000.00, '-35000000.00 - -30000000.00', '15000000.00 - 20000000.00')
    }

    void 'Negative minimum on boundary'() {
        when:
        //Negative minimum on boundary
        dih = new DecimalIntervalHelper(-30000000.00, 19999999.0)

        then:
        checkIntervals(5000000.00, '-30000000.00 - -25000000.00', '15000000.00 - 20000000.00')
    }

    void 'Negative minimum right of boundary'() {
        when:
        //Negative minimum right of boundary
        dih = new DecimalIntervalHelper(-29999999.00, 19999999.0)

        then:
        checkIntervals(5000000.00, '-30000000.00 - -25000000.00', '15000000.00 - 20000000.00')
    }

    void 'Negative max, left of boundary'() {
        when:
        //Negative max, left of boundary
        dih = new DecimalIntervalHelper(-5100.00, -1001.0)

        then:
        checkIntervals(500.00, '-5500.00 - -5000.00', '-1500.00 - -1000.00')
    }

    void 'Negative max, onboundary'() {
        when:
        //Negative max, onboundary
        dih = new DecimalIntervalHelper(-5100.00, -1000.0)

        then:
        checkIntervals(500.00, '-5500.00 - -5000.00', '-1000.00 - -500.00')
    }

    void 'Negative max, right of boundary'() {
        when:
        //Negative max, right of boundary
        dih = new DecimalIntervalHelper(-5100.00, -999.0)

        then:
        checkIntervals(500.00, '-5500.00 - -5000.00', '-1000.00 - -500.00')
    }

    void 'Zero interval'() {
        when:
        //Zero interval
        dih = new DecimalIntervalHelper(83.00, 83.0)

        then:
        checkIntervals(1.00, '83.00 - 84.00', '83.00 - 84.00')
    }

    void 'Positive min and max, both left of boundary'() {
        when:
        //Positive min and max, both left of boundary
        dih = new DecimalIntervalHelper(999.00, 5999.0)

        then:
        checkIntervals(500.00, '500.00 - 1000.00', '5500.00 - 6000.00')
    }

    void 'Positive min and max, both on boundary'() {
        when:
        //Positive min and max, both on boundary
        dih = new DecimalIntervalHelper(1000.00, 6000.0)

        then:
        checkIntervals(500.00, '1000.00 - 1500.00', '6000.00 - 6500.00')
    }

    void 'Positive min and max, both right of boundary'() {
        when:
        //Positive min and max, both right of boundary
        dih = new DecimalIntervalHelper(1001.00, 6001.0)

        then:
        checkIntervals(500.00, '1000.00 - 1500.00', '6000.00 - 6500.00')
    }

    void 'Beyond defined intervals'() {
        when:
        //Beyond defined intervals
        dih = new DecimalIntervalHelper(123.00, 558000000.0)

        then:
        checkIntervals(100000000.00, '0.00 - 100000000.00', '500000000.00 - 600000000.00')
    }

    private void checkIntervals(BigDecimal length, String firstKey, String lastKey) {
        log.warn('{}', dih)
        assertEquals "Interval length", length, dih.intervalLength
        assertEquals "First key", firstKey, dih.orderedKeys.first()
        assertEquals "Last key", lastKey, dih.orderedKeys.last()
    }
}
