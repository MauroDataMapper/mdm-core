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
package uk.ac.ox.softeng.maurodatamapper.test.xml.converter

import grails.databinding.converters.ValueConverter

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * @since 22/09/2015
 */
class JavaOffsetDateTimeConverter implements ValueConverter {
    @Override
    boolean canConvert(Object value) {
        value instanceof String
    }

    @Override
    Object convert(Object value) {
        if (!value || !(value instanceof String)) return null
        try {
            return OffsetDateTime.parse(value as String, DateTimeFormatter.ISO_OFFSET_DATE_TIME).withOffsetSameInstant(ZoneOffset.UTC)
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value as String, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC)
        }
    }

    @Override
    Class<?> getTargetType() {
        OffsetDateTime
    }
}
