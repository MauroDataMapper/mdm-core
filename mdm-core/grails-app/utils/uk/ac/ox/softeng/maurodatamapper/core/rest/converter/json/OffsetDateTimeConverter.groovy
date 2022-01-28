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
package uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json

import grails.plugin.json.builder.JsonGenerator
import org.springframework.core.Ordered

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * @since 02/10/2017
 */
class OffsetDateTimeConverter implements JsonGenerator.Converter, Ordered {

    @Override
    boolean handles(Class<?> type) {
        OffsetDateTime.isAssignableFrom(type)
    }

    @Override
    Object convert(Object value, String key) {
        toString((OffsetDateTime) value)
    }

    static String toString(OffsetDateTime offsetDateTime) {
        if (!offsetDateTime) return null
        // Grails cant handle nanoseconds on conversion so we just want to ignore them if we have them
        offsetDateTime.truncatedTo(ChronoUnit.MILLIS).withOffsetSameInstant(ZoneOffset.UTC)?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE
    }
}
