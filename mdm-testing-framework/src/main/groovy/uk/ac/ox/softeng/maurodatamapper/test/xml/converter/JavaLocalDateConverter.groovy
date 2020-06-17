/*
 * Copyright 2020 University of Oxford
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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @since 02/09/2015
 */
class JavaLocalDateConverter implements ValueConverter {
    @Override
    boolean canConvert(Object value) {
        value instanceof String || value instanceof Map
    }

    @Override
    Object convert(Object value) {
        if (!value) return null
        if (value instanceof String)
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
        Map map = value as Map
        map.year ? LocalDate.of(map.year, map.month ?: 1, map.day ?: 1) : null
    }

    @Override
    Class<?> getTargetType() {
        LocalDate
    }
}
