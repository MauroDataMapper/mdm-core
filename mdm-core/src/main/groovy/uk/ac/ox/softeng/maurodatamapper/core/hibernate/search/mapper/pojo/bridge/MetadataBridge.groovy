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
package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.mapper.pojo.bridge

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata

import groovy.util.logging.Slf4j
import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext

/**
 * Bridge for Metadata to index the metadata into the document with dynamic field names.
 * This relies on the MetadataBinder having been used as the fieldTemplate has to exist.
 *
 * Creates a new fieldName for each combination of namespace|key and assigns the value to it of the metadata.
 * We have to make the fieldName "safe", which involves replacing all spaces and '.'
 * This is so the pathing code inside HS can map the property, and it splits on these characters.
 *
 * Note that updating metadata will NOT trigger a reindex on these fields.
 */
@Slf4j
class MetadataBridge implements PropertyBridge<Set<Metadata>> {

    MetadataBridge() {
    }

    @Override
    void write(DocumentElement target, Set<Metadata> metadata, PropertyBridgeWriteContext context) {

        if (metadata) {
            metadata.each {md ->
                String fieldName = "${md.namespace}|${md.key}"
                String fieldValue = md.value
                target.addValue(makeSafeFieldName(fieldName), fieldValue)
            }
        }
    }

    static String makeSafeFieldName(String unsafeFieldName) {
        unsafeFieldName.replaceAll('\\.', '_')
    }
}