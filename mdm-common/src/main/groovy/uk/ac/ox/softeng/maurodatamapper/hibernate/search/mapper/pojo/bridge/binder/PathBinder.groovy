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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.binder

import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.PathPropertyBridge
import uk.ac.ox.softeng.maurodatamapper.path.Path

import org.hibernate.search.engine.backend.document.IndexFieldReference
import org.hibernate.search.engine.backend.types.IndexFieldType
import org.hibernate.search.engine.backend.types.Searchable
import org.hibernate.search.engine.backend.types.Sortable
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext
import org.hibernate.search.engine.spatial.GeoPoint
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder

/**
 * @since 06/01/2022
 */
class PathBinder implements PropertyBinder {

    @Override
    void bind(PropertyBindingContext context) {
        context.dependencies()
            .use('pathNodes')
            .use('latitude')
            .use('longitude')
            .use('pathString')

        IndexFieldType<String> mainFieldType = context.typeFactory()
            .asString()
            .searchable(Searchable.YES)
            .analyzer('pipe')
            .dslConverter(Path, PathToStringDocumentFieldValueConverter.instance)
            .toIndexFieldType()
        IndexFieldType<GeoPoint> sortFieldType = context.typeFactory()
            .asGeoPoint()
            .searchable(Searchable.YES)
            .sortable(Sortable.YES)
            .toIndexFieldType()

        IndexFieldReference<String> mainField = context.indexSchemaElement().field('path', mainFieldType).toReference()
        IndexFieldReference<GeoPoint> sortField = context.indexSchemaElement().field('path_geopoint', sortFieldType).toReference()

        context.bridge(Path, new PathPropertyBridge(mainField, sortField))
    }

    @Singleton
    static class PathToStringDocumentFieldValueConverter implements ToDocumentFieldValueConverter<Path, String> {
        @Override
        String convert(Path value, ToDocumentFieldValueConvertContext convertContext) {
            value.toString()
        }
    }
}