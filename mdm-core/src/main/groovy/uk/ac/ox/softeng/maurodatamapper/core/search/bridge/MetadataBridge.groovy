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
package uk.ac.ox.softeng.maurodatamapper.core.search.bridge

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.apache.lucene.document.Document
import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.bridge.FieldBridge
import org.hibernate.search.bridge.LuceneOptions

class MetadataBridge implements FieldBridge {

    @Override
    void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (value &&
            value instanceof Collection &&
            Utils.parentClassIsAssignableFromChild(GormEntity, value.first().getClass()) &&
            ((GormEntity) value.first()).instanceOf(Metadata)) {
            Collection<Metadata> mds = value as Collection<Metadata>
            mds.each {metadata ->
                String fieldName = metadata.namespace + ' | ' + metadata.key
                String fieldValue = metadata.value
                luceneOptions.addFieldToDocument(fieldName, fieldValue, document)

            }
        }
    }
}