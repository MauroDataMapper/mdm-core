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
package uk.ac.ox.softeng.maurodatamapper.test.csv

import groovy.util.logging.Slf4j
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

/**
 * Compare two CSV strings, ignoring IDs and date fields, and assuming row orders are expected to be identical
 */
@Slf4j
trait CsvComparer {

    List ignoreFields = ['id', 'lastUpdated', 'exportedOn', 'dateFinalised']

    /**
     * Test that two strings representing a CSV are the same, other than for expected differences in id
     * and date fields. The following comparisons are made:
     * - Number of headers columns are the same
     * - Header names are identical and in the same order
     * - All row values are identical, other than ignored fields.
     * @param expected
     * @param actual
     * @return boolean expected and actual CSV strings are the same, other than for allowed differences
     */
    boolean compareCsv(String expected, String actual) {

        CSVFormat csvFormat = CSVFormat.newFormat((char) ',')
                .withQuote((char) '"')
                .withHeader()

        CSVParser expectedParser = csvFormat.parse(new StringReader(expected))
        CSVParser actualParser = csvFormat.parse(new StringReader(actual))

        List expectedHeaders = expectedParser.getHeaderNames()
        List actualHeaders = actualParser.getHeaderNames()

        compareHeaderSizes(expectedHeaders, actualHeaders) &&
        compareHeaders(expectedHeaders, actualHeaders) &&
        compareRows(expectedHeaders, expectedParser, actualParser)
    }

    private boolean compareHeaderSizes(List expectedHeaders, List actualHeaders) {
        if (expectedHeaders.size() == actualHeaders.size()) {
            return true
        } else {
            log.error("\nExpected number of headers to be ${expectedHeaders.size()} but actual was ${actualHeaders.size()}")
            return false
        }
    }

    private boolean compareHeaders(List expectedHeaders, List actualHeaders) {
        boolean result = true

        expectedHeaders.eachWithIndex {item, index ->
            if (item != actualHeaders[index]) {
                log.error("\nExpected header ${item} at position ${index} but actual was ${actualHeaders[index]}")
                result = false
            }
        }

        return result
    }

    private boolean compareRows(List expectedHeaders, CSVParser expectedParser, CSVParser actualParser) {
        boolean result = true

        while (expectedParser.iterator().hasNext()) {
            CSVRecord expectedRecord = expectedParser.iterator().next()

            if (!actualParser.iterator().hasNext()) {
                // Number of records in expected exceeds that in actual
                log.error('\nCould not find next actual record')
                result = false
            }

            if (result) {
                CSVRecord actualRecord = actualParser.iterator().next()

                expectedHeaders.each {header ->
                    String expectedValue = expectedRecord[header]
                    String actualValue = actualRecord[header]

                    if (!ignoreFields.contains(header)) {
                        if (expectedValue != actualValue) {
                            log.error("\nExpected ${expectedValue} but actual was ${actualValue}")
                            result = false
                        }
                    }
                }
            }
        }

        if (result && actualParser.iterator().hasNext()) {
            // Number of records in actual exceeds that in expected
            log.error('\nAdditional actual record found')
            result = false
        }

        return result
    }
}
