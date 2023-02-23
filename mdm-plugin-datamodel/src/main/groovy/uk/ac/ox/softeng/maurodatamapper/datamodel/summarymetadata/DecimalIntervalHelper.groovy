/*
 * Copyright 2020-2023 University of Oxford and NHS England
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


import groovy.transform.CompileStatic

import java.math.RoundingMode

@CompileStatic
class DecimalIntervalHelper extends NumericIntervalHelper<BigDecimal> {

    DecimalIntervalHelper(BigDecimal minValue, BigDecimal maxValue) {
        super(minValue, maxValue)
    }

    void calculateInterval() {

        calculateIntervalLength([0.01d, 0.02d, 0.05d])

        BigDecimal[] minValueDivideAndRemainder = getMinValue().divideAndRemainder(getIntervalLength())
        firstIntervalStart = minValueDivideAndRemainder[0] * getIntervalLength()
        //For negative minima which do not align with an interval start, shift the interval one step to the left
        if (minValueDivideAndRemainder[1].abs() > 0 && getMinValue() < 0) {
            firstIntervalStart = firstIntervalStart - getIntervalLength()
        }

        BigDecimal[] maxValueDivideAndRemainder = getMaxValue().divideAndRemainder(getIntervalLength())
        lastIntervalStart = maxValueDivideAndRemainder[0] * getIntervalLength()
        if (maxValueDivideAndRemainder[1].abs() > 0 && getMaxValue() < 0) {
            lastIntervalStart = lastIntervalStart - getIntervalLength()
        }

        firstIntervalStart = firstIntervalStart.setScale(2, RoundingMode.HALF_UP)
        lastIntervalStart = lastIntervalStart.setScale(2, RoundingMode.HALF_UP)
    }

    @Override
    BigDecimal safeConvert(Number number) {
        number.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
    }
}

