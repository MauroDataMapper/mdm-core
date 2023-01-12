/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

import grails.gorm.DetachedCriteria
import grails.rest.Resource

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceType extends DataType<ReferenceType> {

    static belongsTo = [referenceClass:DataClass]

    static constraints = {
        referenceClass validator: {val, obj ->
            if (!val) return ['default.null.message']
            if (val && val.model && obj.model) {
                // In the same model is okay
                if (val.model.id == obj.model.id) return true
                // Imported into model is okay
                if (obj.model.importedDataClasses.any {it.id == val.id}) return true
                ['invalid.datatype.dataclass.model']
            }
        }
    }

    static mapping = {
        referenceClass index: 'reference_type_reference_class_idx', fetch: 'join'
    }

    ReferenceType() {
        domainType = ReferenceType.simpleName
    }

    ObjectDiff<ReferenceType> diff(ReferenceType otherDataType, String context) {
        diff(otherDataType, context, null, null)
    }

    ObjectDiff<ReferenceType> diff(ReferenceType otherDataType, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(ReferenceType, this, otherDataType, lhsDiffCache, rhsDiffCache)
            .appendString('referenceClass.label', this.referenceClass.label, otherDataType.referenceClass.label)
    }

    static DetachedCriteria<ReferenceType> by() {
        new DetachedCriteria<ReferenceType>(ReferenceType)
    }

    static DetachedCriteria<ReferenceType> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ReferenceType> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

}