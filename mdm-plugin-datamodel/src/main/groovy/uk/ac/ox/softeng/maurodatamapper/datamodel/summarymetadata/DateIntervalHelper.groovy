/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@CompileStatic
class DateIntervalHelper extends AbstractIntervalHelper<LocalDateTime> {

    Duration differenceDuration
    Period differencePeriod
    boolean needToMergeOrRemoveEmptyBuckets

    DateTimeFormatter getDateFormatter() {
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    DateTimeFormatter getDateTimeFormatter() {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    }

    DateTimeFormatter getMonthDateFormatter() {
        DateTimeFormatter.ofPattern("MMM yyyy")
    }

    // These determine the interval length
    int intervalLengthSize
    ChronoUnit intervalLengthDimension

    DateIntervalHelper(LocalDateTime minValue, LocalDateTime maxValue) {
        super(minValue, maxValue)
    }

    @Override
    void initialise() {
        needToMergeOrRemoveEmptyBuckets = false
        super.initialise()
        // If less than 10 buckets we can leave as-is
        if (needToMergeOrRemoveEmptyBuckets && intervals.size() <= 10) needToMergeOrRemoveEmptyBuckets = false
    }

    @Override
    void calculateInterval() {
        differenceDuration = Duration.between(minValue, maxValue)
        differencePeriod = Period.between(getMinValue().toLocalDate(), getMinValue().toLocalDate())

        long diffYears = ChronoUnit.YEARS.between(minValue, maxValue)
        long diffMonths = ChronoUnit.MONTHS.between(minValue, maxValue)
        long diffDays = ChronoUnit.DAYS.between(minValue, maxValue)
        long diffHours = ChronoUnit.HOURS.between(minValue, maxValue)
        long diffMinutes = ChronoUnit.MINUTES.between(minValue, maxValue)


        firstIntervalStart = getMinValue().withSecond(0)
        intervalLengthDimension = ChronoUnit.MINUTES
        if (diffMinutes <= 10) intervalLengthSize = 1
        else if (diffMinutes <= 20) {
            intervalLengthSize = 2
            while (firstIntervalStart.getMinute() % 2 != 0) {
                firstIntervalStart = firstIntervalStart.minusMinutes(1)
            }
        } else if (diffMinutes <= 60) {
            intervalLengthSize = 5
            while (firstIntervalStart.getMinute() % 5 != 0) {
                firstIntervalStart = firstIntervalStart.minusMinutes(1)
            }
        } else if (diffMinutes <= 120) {
            // 2 hrs
            intervalLengthSize = 15
            while (firstIntervalStart.getMinute() % 15 != 0) {
                firstIntervalStart = firstIntervalStart.minusMinutes(1)
            }
        } else if (diffHours < 6) {
            intervalLengthSize = 30
            while (firstIntervalStart.getMinute() % 30 != 0) {
                firstIntervalStart = firstIntervalStart.minusMinutes(1)
            }
        } else if (diffHours < 24) {
            firstIntervalStart = getMinValue().withMinute(0)
            intervalLengthDimension = ChronoUnit.HOURS
            intervalLengthSize = diffHours <= 12 ? 1 : 2
        } else if (diffDays < 6) {
            firstIntervalStart = LocalDateTime.of(getMinValue().toLocalDate(), LocalTime.MIDNIGHT)
            intervalLengthDimension = ChronoUnit.HOURS
            intervalLengthSize = diffDays <= 2 ? 6 : 12
        } else if (diffDays < 31) {
            firstIntervalStart = LocalDateTime.of(getMinValue().toLocalDate(), LocalTime.MIDNIGHT)
            intervalLengthDimension = ChronoUnit.DAYS
            intervalLengthSize = diffDays <= 15 ? 1 : 2
        } else if (diffMonths < 24) {
            firstIntervalStart = LocalDateTime.of(firstIntervalStart.toLocalDate(), LocalTime.MIDNIGHT).with(TemporalAdjusters.firstDayOfMonth())
            intervalLengthDimension = ChronoUnit.MONTHS
            if (diffMonths < 12) {
                intervalLengthSize = 1
            } else {
                intervalLengthSize = 2
                // We actually want to start the buckets in months like january not february
                while (firstIntervalStart.getMonthValue() % 2 == 0) {
                    firstIntervalStart = firstIntervalStart.minusDays(1).with(TemporalAdjusters.firstDayOfMonth())
                }
            }
        } else {
            buildYearBuckets(diffYears)
        }
    }

    private void buildYearBuckets(long diffYears) {

        int intervalMod = 1
        intervalLengthDimension = ChronoUnit.YEARS
        LocalDateTime firstDayOfYear = minValue.with(TemporalAdjusters.firstDayOfYear()) as LocalDateTime

        if (diffYears < 10) {
            intervalLengthSize = 1
        } else if (diffYears < 20) {
            intervalLengthSize = 2
        } else if (diffYears < 50) {
            intervalLengthSize = 5
        } else {
            intervalLengthSize = 1
            intervalLengthDimension = ChronoUnit.DECADES
            needToMergeOrRemoveEmptyBuckets = true
            intervalMod = 10
        }

        // Get a logical first interval start where the year is the start of a logical modulus bucket
        while (firstDayOfYear.getYear() % (intervalLengthSize * intervalMod) != 0) {
            firstDayOfYear = firstDayOfYear.minusDays(1)
            firstDayOfYear = firstDayOfYear.with(TemporalAdjusters.firstDayOfYear())
        }
        firstIntervalStart = LocalDateTime.of(firstDayOfYear.toLocalDate(), LocalTime.MIDNIGHT)

    }

    void calculateIntervalStarts() {
        intervalStarts = []
        LocalDateTime currDateTime = firstIntervalStart
        while (currDateTime <= maxValue) {
            intervalStarts.add(currDateTime)
            currDateTime = currDateTime.plus(intervalLengthSize, intervalLengthDimension)
        }
    }

    void calculateIntervals() {
        intervalStarts.each {start ->

            LocalDateTime finish = start.plus(intervalLengthSize, intervalLengthDimension)
            String label
            if (intervalLengthSize == 1 && intervalLengthDimension == ChronoUnit.YEARS) {
                label = "${start.getYear()}"
            } else if (intervalLengthDimension == ChronoUnit.DECADES || intervalLengthDimension == ChronoUnit.YEARS) {
                label = "${start.getYear()}${labelSeparator}${finish.getYear()}"
            } else if (intervalLengthSize == 1 && intervalLengthDimension == ChronoUnit.MONTHS) {
                label = start.format(monthDateFormatter)
            } else if (intervalLengthDimension == ChronoUnit.MONTHS) {
                label = "${start.format(monthDateFormatter)}${labelSeparator}${finish.format(monthDateFormatter)}"
            } else if (intervalLengthSize == 1 && intervalLengthDimension == ChronoUnit.DAYS) {
                label = start.format(dateFormatter)
            } else if (intervalLengthDimension == ChronoUnit.DAYS) {
                label = "${start.format(dateFormatter)}${labelSeparator}${finish.format(dateFormatter)}"
            } else if (intervalLengthSize == 1 && intervalLengthDimension == ChronoUnit.HOURS) {
                label = start.format(dateTimeFormatter)
            } else if (intervalLengthDimension == ChronoUnit.HOURS) {
                label = "${start.format(dateTimeFormatter)}${labelSeparator}${finish.format(dateTimeFormatter)}"
            } else if (intervalLengthSize == 1 && intervalLengthDimension == ChronoUnit.MINUTES) {
                label = start.format(dateTimeFormatter)
            } else {
                label = "${start.format(dateTimeFormatter)}${labelSeparator}${finish.format(dateTimeFormatter)}"
            }
            addInterval(label, new Pair(start, finish))
        }
    }
}

