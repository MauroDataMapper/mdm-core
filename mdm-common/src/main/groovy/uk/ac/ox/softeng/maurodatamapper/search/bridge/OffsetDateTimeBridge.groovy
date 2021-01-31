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
package uk.ac.ox.softeng.maurodatamapper.search.bridge

import org.hibernate.search.bridge.StringBridge
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * @since 20/07/2018
 */
class OffsetDateTimeBridge implements StringBridge {

    Logger logger = LoggerFactory.getLogger(OffsetDateTimeBridge)

    static DateTimeFormatter dtf = DateTimeFormatter.BASIC_ISO_DATE

    @Override
    String objectToString(Object object) {
        if (object instanceof OffsetDateTime) {
            return ((OffsetDateTime) object).format(dtf)
        }
        logger.error('Bridge set up to convert object of type {} but it is not an OffsetDateTime', object.getClass())
        return null
    }
}
