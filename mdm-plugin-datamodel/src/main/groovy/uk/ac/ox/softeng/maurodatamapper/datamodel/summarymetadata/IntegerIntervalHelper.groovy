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

import grails.util.Pair
import groovy.transform.CompileStatic

@CompileStatic
class IntegerIntervalHelper extends NumericIntervalHelper<Integer> {


    IntegerIntervalHelper(Integer minValue, Integer maxValue) {
        super(minValue, maxValue)
    }

    @Override
    Integer safeConvert(Number number) {
        number.toInteger()
    }

    @Override
    void calculateIntervals() {
        intervalStarts.each {start ->
            Integer labelIntervalLength = getIntervalLength() > 0 ? getIntervalLength() - 1 : getIntervalLength()
            Integer finish = safeConvert(start + getIntervalLength())
            Integer labelFinish = safeConvert(start + labelIntervalLength)
            String label = "${start}" == "${labelFinish}" ? "${start}" : "${start}${labelSeparator}${labelFinish}"
            addInterval(label, new Pair(start, finish))
        }
    }
}

