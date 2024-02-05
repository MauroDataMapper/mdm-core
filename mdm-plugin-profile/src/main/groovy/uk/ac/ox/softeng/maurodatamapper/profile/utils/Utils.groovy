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
package uk.ac.ox.softeng.maurodatamapper.profile.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.text.ParseException
import java.text.SimpleDateFormat

@CompileStatic
@Slf4j
class Utils {

    private Utils() {
    }

    static List<String> formatStrings = Arrays.asList('yyyy-MM-dd', 'dd/MM/yyyy', 'dd-MM-yyyy', 'd-M-y', 'd/M/y', 'M/y')
    static List<SimpleDateFormat> formats = formatStrings.collect {
        format -> new SimpleDateFormat(format)
    }

    static Date tryParseDate(String dateString) {
        if (!dateString) return null

        Date returnValue = null
        log.trace("parsing: ${dateString}")
        boolean found = false
        formats.each { format ->
            try {
                log.trace("Trying: ${format}")
                if (!found) {
                    returnValue = format.parse(dateString)
                    log.trace("Found: ${returnValue}")
                    found = true
                }
            } catch (ParseException ignored) {
            }
        }
        returnValue
    }
}
