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
package uk.ac.ox.softeng.maurodatamapper.profile.domain

@Singleton
class ConfigurableProfileFieldTypes {

    public static final String DATE_FORMAT_KEY = 'datatype.date.formats'
    public static final String DATETIME_FORMAT_KEY = 'datatype.datetime.formats'
    public static final String TIME_FORMAT_KEY = 'datatype.time.formats'

    public static final String[] DATE_FORMAT_DEFAULT = ['dd/MM/yyyy', 'dd-MM-yyyy', 'MM/dd/yyyy', 'MM-dd-yyyy', 'yyyy/MM/dd', 'yyyy-MM-dd']
    public static final String[] DATETIME_FORMAT_DEFAULT = ['dd/MM/yyyy\'T\'HH:mm:ss', 'dd-MM-yyyy\'T\'HH:mm:ss']
    public static final String[] TIME_FORMAT_DEFAULT = ['HH:mm:ss', 'HH:mm']

    String[] dateFormats
    String[] dateTimeFormats
    String[] timeFormats

    String[] getDateFormats() {
        dateFormats ?: DATE_FORMAT_DEFAULT
    }

    String[] getDateTimeFormats() {
        dateTimeFormats ?: DATETIME_FORMAT_DEFAULT
    }

    String[] getTimeFormats() {
        timeFormats ?: TIME_FORMAT_DEFAULT
    }
}
