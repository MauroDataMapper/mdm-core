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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff

import grails.gorm.DetachedCriteria
import grails.rest.Resource

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferencePrimitiveType extends ReferenceDataType<ReferencePrimitiveType> {

    String units

    static constraints = {
        units nullable: true
    }

    ReferencePrimitiveType() {
        domainType = ReferencePrimitiveType.simpleName
    }

    ObjectDiff<ReferencePrimitiveType> diff(ReferencePrimitiveType otherDataType, String context) {
       diff(otherDataType, context, null,null)
    }

    ObjectDiff<ReferencePrimitiveType> diff(ReferencePrimitiveType otherDataType, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(ReferencePrimitiveType, this, otherDataType, lhsDiffCache,rhsDiffCache)
            .appendString('units', this.units, otherDataType.units)
    }

    static DetachedCriteria<ReferencePrimitiveType> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ReferencePrimitiveType> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}