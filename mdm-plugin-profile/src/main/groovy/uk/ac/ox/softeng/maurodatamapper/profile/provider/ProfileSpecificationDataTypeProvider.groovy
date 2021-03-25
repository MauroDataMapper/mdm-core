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
package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DefaultDataType

// @CompileStatic
class ProfileSpecificationDataTypeProvider extends DataTypeService {

    @Override
    String getDisplayName() {
        'Profile Specification DataTypes'
    }

    @Override
    List<DefaultDataType> getDefaultListOfDataTypes() {
        [[label: 'boolean', description: 'logical Boolean [true/false)'],
         [label: 'string', description: 'short variable-length character string (plain-text)'],
         [label: 'text', description: 'long variable-length character string (may include html / markdown)'],
         [label: 'int', description: 'integer'],
         [label: 'decimal', description: 'decimal'],
         [label: 'date', description: 'calendar date [year, month, day)'],
         [label: 'datetime', description: 'date and time, excluding time zone'],
         [label: 'time', description: 'time of day [no time zone)'],
         [label: 'folder', description: 'pointer to a folder in this Mauro instance'],
         [label: 'model', description: 'pointer to a model in this Mauro instance'],
         [label: 'json', description: 'a text field containing valid json syntax'],
        ].collect {Map<String, String> properties -> new DefaultDataType(new PrimitiveType(properties))}
    }
}
