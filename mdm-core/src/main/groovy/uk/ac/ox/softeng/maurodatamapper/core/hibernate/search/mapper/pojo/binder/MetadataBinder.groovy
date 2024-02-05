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
package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.mapper.pojo.binder

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.mapper.pojo.bridge.MetadataBridge

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement
import org.hibernate.search.engine.backend.types.IndexFieldType
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder

/**
 * Binds Metadata collection/set to the MetadataBridge.
 * This allows us to index the metadata as dynamic field names with values.
 * See MetadataBridge for more information.
 *
 * Note that updating metadata will NOT trigger a reindex on these fields.
 * @since 14/10/2021
 */
class MetadataBinder implements PropertyBinder {

    @Override
    void bind(PropertyBindingContext context) {

        context.dependencies()
            .use('namespace')
            .use('key')
            .use('value')

        IndexSchemaElement indexSchemaElement = context.indexSchemaElement()
        IndexFieldType<String> valueFieldType = context.typeFactory().asString().analyzer('standard').toIndexFieldType()
        // Define a template for fields applied to this context
        // Ideally this should be inside an objectField but this is not how the existing code works,
        // so we will want to update at some point
        indexSchemaElement.fieldTemplate('nsKeyTemplate', valueFieldType).multiValued()
        context.bridge(Set, new MetadataBridge())
    }
}
