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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

import grails.rest.Resource

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceType extends DataType<ReferenceType> {

    DataClass referenceClass

    static belongsTo = DataClass

    static mapping = {
        referenceClass index: 'reference_type_reference_class_idx', fetch: 'join', cascade: 'none'
    }

    ReferenceType() {
        domainType = ReferenceType.simpleName
    }

    ObjectDiff<ReferenceType> diff(ReferenceType otherDataType) {
        catalogueItemDiffBuilder(ReferenceType, this, otherDataType)
            .appendString('referenceClass.label', this.referenceClass?.label, otherDataType.referenceClass?.label)
    }

}