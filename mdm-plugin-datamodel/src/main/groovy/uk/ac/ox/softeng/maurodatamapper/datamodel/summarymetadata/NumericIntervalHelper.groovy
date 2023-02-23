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

import grails.util.Pair
import groovy.transform.CompileStatic

/**
 * @since 16/03/2022
 */
@CompileStatic
abstract class NumericIntervalHelper<N extends Number> extends AbstractIntervalHelper<N> {

    NumericIntervalHelper(N minValue, N maxValue) {
        super(minValue, maxValue)
    }

    void calculateIntervalStarts() {
        intervalStarts = []
        N currNum = firstIntervalStart
        while (currNum <= lastIntervalStart) {
            intervalStarts.add(currNum)
            currNum = safeConvert(currNum + getIntervalLength())
        }
    }

    void calculateIntervals() {
        intervalStarts.each {start ->
            N finish = safeConvert(start + getIntervalLength())
            String label = "${start}${labelSeparator}${finish}"
            addInterval(label, new Pair(start, finish))
        }
    }

    @Override
    void calculateInterval() {

        calculateIntervalLength([1.0d, 2.0d, 5.0d])

        firstIntervalStart = safeConvert(getMinValue().intdiv(getIntervalLength()) * getIntervalLength())
        //For negative minima which do not align with an interval start, shift the interval one step to the left
        if ((getMinValue() % getIntervalLength()).abs() > 0 && getMinValue() < 0) {
            firstIntervalStart = safeConvert(firstIntervalStart - getIntervalLength())
        }
        lastIntervalStart = safeConvert(getMaxValue().intdiv(getIntervalLength()) * getIntervalLength())
        if ((getMaxValue() % getIntervalLength()).abs() > 0 && getMaxValue() < 0) {
            lastIntervalStart = safeConvert(lastIntervalStart - getIntervalLength())
        }
    }

    void calculateIntervalLength(List<Double> buckets) {
        // Improved code to get as close to 10 buckets as possible in brackets of 1,2 & 5
        // We look for when the exact size of a bucket to make 10 buckets falls in to the bracket of 1, 2 or 5 by factors of 10
        // e.g. 1,2,5,10,20,50,100,200,500 etc
        N diff = safeConvert(getMaxValue() - getMinValue())
        if (diff == 0) {
            intervalLength = safeConvert(1)
            return
        }
        N simpleTenBuckets = safeConvert(diff / 10)
        float factor = 1.0f
        while (!intervalLength) {
            for (Number bucket : buckets) {
                N size = safeConvert(bucket * factor)
                if (simpleTenBuckets <= size) {
                    intervalLength = size
                    break
                }
            }
            factor = (float) (factor * 10)
        }
    }

    abstract N safeConvert(Number number)
}
