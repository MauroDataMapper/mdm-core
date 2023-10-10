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

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.util.Pair
import groovy.util.logging.Slf4j
import org.grails.testing.GrailsUnitTest

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * @since 15/03/2022
 */
@Slf4j
class DateIntervalHelperSpec extends MdmSpecification implements GrailsUnitTest {

    DateIntervalHelper dih
    LinkedHashMap expectedIntervals

    LocalDateTime from
    LocalDateTime to

    def cleanup() {
        expectedIntervals = null
        dih = null
    }

    void 'Simple interval days'() {

        when:
        //Simple interval
        from = LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        to = LocalDateTime.parse('2019-12-30T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dih = new DateIntervalHelper(from, to)

        then:
        dih.intervalLengthSize == 2
        dih.intervalLengthDimension == ChronoUnit.DAYS

        when:
        LinkedHashMap expectedIntervals = new LinkedHashMap()
        expectedIntervals['01/12/2019 - 02/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-03T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['03/12/2019 - 04/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-03T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-05T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['05/12/2019 - 06/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-05T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-07T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['07/12/2019 - 08/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-07T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-09T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['09/12/2019 - 10/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-09T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-11T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['11/12/2019 - 12/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-11T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-13T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['13/12/2019 - 14/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-13T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-15T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['15/12/2019 - 16/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-15T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-17T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['17/12/2019 - 18/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-17T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-19T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['19/12/2019 - 20/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-19T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-21T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['21/12/2019 - 22/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-21T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-23T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['23/12/2019 - 24/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-23T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-25T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['25/12/2019 - 26/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-25T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-27T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['27/12/2019 - 28/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-27T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-29T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        expectedIntervals['29/12/2019 - 30/12/2019'] = new Pair(
            LocalDateTime.parse('2019-12-29T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            LocalDateTime.parse('2019-12-31T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        then:
        expectedIntervals == dih.intervals

    }

    void 'Zero interval'() {
        when:
        //Zero interval
        from = LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        to = LocalDateTime.parse('2019-12-01T00:00:00', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dih = new DateIntervalHelper(from, to)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 1
        dih.intervals.size() == 1

    }

    void 'test interval buckets sizes for minutes'() {
        given:
        from = LocalDateTime.parse('2019-02-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        when:
        to = from.plusMinutes(8)
        dih = new DateIntervalHelper(from, to)
        logRanges(8)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 1
        dih.intervals.size() == 9
        hasFirstAndLast('23/02/2019 01:04', '23/02/2019 01:12')

        when:
        to = from.plusMinutes(12)
        dih = new DateIntervalHelper(from, to)
        logRanges(12)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 2
        dih.intervals.size() == 7
        hasFirstAndLast('23/02/2019 01:04 - 23/02/2019 01:05', '23/02/2019 01:16 - 23/02/2019 01:17')

        when:
        to = from.plusMinutes(22)
        dih = new DateIntervalHelper(from, to)
        logRanges(22)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 5
        dih.intervals.size() == 6
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 01:04', '23/02/2019 01:25 - 23/02/2019 01:29')

        when:
        to = from.plusMinutes(36)
        dih = new DateIntervalHelper(from, to)
        logRanges(36)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 5
        dih.intervals.size() == 9
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 01:04', '23/02/2019 01:40 - 23/02/2019 01:44')

        when:
        to = from.plusMinutes(65)
        dih = new DateIntervalHelper(from, to)
        logRanges(65)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 15
        dih.intervals.size() == 5
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 01:14', '23/02/2019 02:00 - 23/02/2019 02:14')

        when:
        to = from.plusMinutes(125)
        dih = new DateIntervalHelper(from, to)
        logRanges(125)

        then:
        dih.intervalLengthDimension == ChronoUnit.MINUTES
        dih.intervalLengthSize == 30
        dih.intervals.size() == 5
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 01:29', '23/02/2019 03:00 - 23/02/2019 03:29')

    }

    void 'test interval buckets sizes for hours'() {
        given:
        from = LocalDateTime.parse('2019-02-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        when:
        to = from.plusHours(6)
        dih = new DateIntervalHelper(from, to)
        logRanges(6)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 7
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 01:59', '23/02/2019 07:00 - 23/02/2019 07:59')

        when:
        to = from.plusHours(12)
        dih = new DateIntervalHelper(from, to)
        logRanges(12)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 13
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 01:59', '23/02/2019 13:00 - 23/02/2019 13:59')

        when:
        to = from.plusHours(18)
        dih = new DateIntervalHelper(from, to)
        logRanges(18)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 10
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 02:59', '23/02/2019 19:00 - 23/02/2019 20:59')

        when:
        to = from.plusHours(23)
        dih = new DateIntervalHelper(from, to)
        logRanges(23)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 12
        hasFirstAndLast('23/02/2019 01:00 - 23/02/2019 02:59', '23/02/2019 23:00 - 24/02/2019 00:59')

        when:
        to = from.plusDays(1)
        dih = new DateIntervalHelper(from, to)
        logRanges(1)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 6
        dih.intervals.size() == 5
        hasFirstAndLast('23/02/2019 00:00 - 23/02/2019 05:59', '24/02/2019 00:00 - 24/02/2019 05:59')

        when:
        to = from.plusDays(2)
        dih = new DateIntervalHelper(from, to)
        logRanges(2)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 6
        dih.intervals.size() == 9
        hasFirstAndLast('23/02/2019 00:00 - 23/02/2019 05:59', '25/02/2019 00:00 - 25/02/2019 05:59')

        when:
        to = from.plusDays(5)
        dih = new DateIntervalHelper(from, to)
        logRanges(5)

        then:
        dih.intervalLengthDimension == ChronoUnit.HOURS
        dih.intervalLengthSize == 12
        dih.intervals.size() == 11
        hasFirstAndLast('23/02/2019 00:00 - 23/02/2019 11:59', '28/02/2019 00:00 - 28/02/2019 11:59')
    }

    void 'test interval buckets sizes for days'() {
        given:
        from = LocalDateTime.parse('2019-02-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        when:
        to = from.plusDays(6)
        dih = new DateIntervalHelper(from, to)
        logRanges(6)

        then:
        dih.intervalLengthDimension == ChronoUnit.DAYS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 7
        hasFirstAndLast('23/02/2019', '01/03/2019')

        when:
        to = from.plusDays(15)
        dih = new DateIntervalHelper(from, to)
        logRanges(15)

        then:
        dih.intervalLengthDimension == ChronoUnit.DAYS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 16
        hasFirstAndLast('23/02/2019', '10/03/2019')

        when:
        to = from.plusDays(16)
        dih = new DateIntervalHelper(from, to)
        logRanges(16)

        then:
        dih.intervalLengthDimension == ChronoUnit.DAYS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 9
        hasFirstAndLast('23/02/2019 - 24/02/2019', '11/03/2019 - 12/03/2019')

        when:
        // Change month to allow a 30 day check
        from = LocalDateTime.parse('2019-03-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        to = from.plusDays(30)
        dih = new DateIntervalHelper(from, to)
        logRanges(30)

        then:
        dih.intervalLengthDimension == ChronoUnit.DAYS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 16
        hasFirstAndLast('23/03/2019 - 24/03/2019', '22/04/2019 - 23/04/2019')
    }

    void 'test interval buckets sizes for months'() {
        given:
        from = LocalDateTime.parse('2019-02-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        when:
        to = from.plusDays(31)
        dih = new DateIntervalHelper(from, to)
        logRanges(31)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 2
        hasFirstAndLast('Feb 2019', 'Mar 2019')

        when:
        to = from.plusDays(35)
        dih = new DateIntervalHelper(from, to)
        logRanges(35)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 2
        hasFirstAndLast('Feb 2019', 'Mar 2019')

        when:
        to = from.plusMonths(3)
        dih = new DateIntervalHelper(from, to)
        logRanges(3)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 4
        hasFirstAndLast('Feb 2019', 'May 2019')

        when:
        to = from.plusMonths(12).minusDays(1)
        dih = new DateIntervalHelper(from, to)
        logRanges(12)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 13
        hasFirstAndLast('Feb 2019', 'Feb 2020')


        when:
        to = from.plusYears(1)
        dih = new DateIntervalHelper(from, to)
        logRanges(1)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 7
        hasFirstAndLast('Jan 2019 - Feb 2019', 'Jan 2020 - Feb 2020')

        when:
        to = from.plusMonths(18)
        dih = new DateIntervalHelper(from, to)
        logRanges(18)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 10
        hasFirstAndLast('Jan 2019 - Feb 2019', 'Jul 2020 - Aug 2020')

        when:
        to = from.plusMonths(24).minusDays(1)
        dih = new DateIntervalHelper(from, to)
        logRanges(24)

        then:
        dih.intervalLengthDimension == ChronoUnit.MONTHS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 13
        hasFirstAndLast('Jan 2019 - Feb 2019', 'Jan 2021 - Feb 2021')
    }

    void 'test interval buckets sizes for years'() {
        given:
        from = LocalDateTime.parse('2019-02-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        when:
        to = from.plusMonths(24)
        dih = new DateIntervalHelper(from, to)
        logRanges(24)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 3
        hasFirstAndLast('2019', '2021')

        when:
        to = from.plusMonths(36)
        dih = new DateIntervalHelper(from, to)
        logRanges(36)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 4
        hasFirstAndLast('2019', '2022')

        when:
        to = from.plusYears(5)
        dih = new DateIntervalHelper(from, to)
        logRanges(5)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 1
        dih.intervals.size() == 6
        hasFirstAndLast('2019', '2024')

        when:
        to = from.plusYears(15)
        dih = new DateIntervalHelper(from, to)
        logRanges(15)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 2
        dih.intervals.size() == 9
        hasFirstAndLast('2018 - 2019', '2034 - 2035')

        when:
        to = from.plusYears(20)
        dih = new DateIntervalHelper(from, to)
        logRanges(20)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 5
        dih.intervals.size() == 5
        hasFirstAndLast('2015 - 2019', '2035 - 2039')

        when:
        to = from.plusYears(35)
        dih = new DateIntervalHelper(from, to)
        logRanges(35)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 5
        dih.intervals.size() == 8
        hasFirstAndLast('2015 - 2019', '2050 - 2054')

        when:
        to = from.plusYears(50).minusDays(1)
        dih = new DateIntervalHelper(from, to)
        logRanges(50)

        then:
        dih.intervalLengthDimension == ChronoUnit.YEARS
        dih.intervalLengthSize == 5
        dih.intervals.size() == 11
        hasFirstAndLast('2015 - 2019', '2065 - 2069')

    }

    void 'test interval buckets sizes for decades'() {
        given:
        from = LocalDateTime.parse('2019-02-23T01:04:56', DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        when:
        to = from.plusYears(50)
        dih = new DateIntervalHelper(from, to)
        logRanges(50)

        then:
        dih.intervalLengthDimension == ChronoUnit.DECADES
        !dih.needToMergeOrRemoveEmptyBuckets
        dih.intervalLengthSize == 1
        dih.intervals.size() == 6
        hasFirstAndLast('2010 - 2019', '2060 - 2069')


        when:
        to = from.plusYears(99)
        dih = new DateIntervalHelper(from, to)
        logRanges(99)

        then:
        dih.intervalLengthDimension == ChronoUnit.DECADES
        dih.needToMergeOrRemoveEmptyBuckets
        dih.intervalLengthSize == 1
        dih.intervals.size() == 11
        hasFirstAndLast('2010 - 2019', '2110 - 2119')

        when:
        to = from.plusYears(100)
        dih = new DateIntervalHelper(from, to)
        logRanges(100)

        then:
        dih.intervalLengthDimension == ChronoUnit.DECADES
        dih.needToMergeOrRemoveEmptyBuckets
        dih.intervalLengthSize == 1
        hasFirstAndLast('2010 - 2019', '2110 - 2119')

        when:
        to = from.plusYears(150)
        dih = new DateIntervalHelper(from, to)
        logRanges(150)

        then:
        dih.intervalLengthDimension == ChronoUnit.DECADES
        dih.needToMergeOrRemoveEmptyBuckets
        dih.intervalLengthSize == 1
        hasFirstAndLast('2010 - 2019', '2160 - 2169')

        when:
        to = from.plusYears(500)
        dih = new DateIntervalHelper(from, to)
        logRanges(500)

        then:
        dih.intervalLengthDimension == ChronoUnit.DECADES
        dih.intervalLengthSize == 1
        hasFirstAndLast('2010 - 2019', '2510 - 2519')

    }

    private void hasFirstAndLast(String firstKey, String lastKey) {
        assert dih.orderedKeys.first() == firstKey
        assert dih.orderedKeys.last() == lastKey
    }

    private logRanges(int i) {
        log.warn('{} to: {}\n{}', i, to, dih)
    }
}
