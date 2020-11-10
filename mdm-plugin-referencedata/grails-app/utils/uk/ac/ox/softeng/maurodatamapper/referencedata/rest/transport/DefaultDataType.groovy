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
package uk.ac.ox.softeng.maurodatamapper.referencedata.rest.transport

import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue

/**
 * @since 23/03/2020
 */
class DefaultDataType {

    String domainType
    String label
    String description

    List<ReferenceEnumerationValue> enumerationValues
    String units

    private void initialise(ReferenceDataType dataType) {
        this.domainType = dataType.domainType
        this.label = dataType.label
        this.description = dataType.description
    }

    DefaultDataType(ReferencePrimitiveType primitiveType) {
        initialise(primitiveType)
        this.units = primitiveType.units
    }

    DefaultDataType(ReferenceEnumerationType enumerationType) {
        initialise(enumerationType)
        this.enumerationValues = enumerationType.referenceEnumerationValues
    }
}
