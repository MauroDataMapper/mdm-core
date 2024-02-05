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
package uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue

/**
 * @since 23/03/2020
 */
class DefaultDataType {

    String domainType
    String label
    String description

    List<EnumerationValue> enumerationValues
    String units

    private void initialise(DataType dataType) {
        this.domainType = dataType.domainType
        this.label = dataType.label
        this.description = dataType.description
    }

    DefaultDataType(PrimitiveType primitiveType) {
        initialise(primitiveType)
        this.units = primitiveType.units
    }

    DefaultDataType(EnumerationType enumerationType) {
        initialise(enumerationType)
        this.enumerationValues = enumerationType.enumerationValues
    }
}
