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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge

import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.util.logging.Slf4j
import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.engine.backend.document.IndexFieldReference
import org.hibernate.search.engine.spatial.GeoPoint
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext

/**
 * @since 13/09/2021
 */
@Slf4j
class PathPropertyBridge implements PropertyBridge<Path> {

    IndexFieldReference<String> mainField
    IndexFieldReference<GeoPoint> sortField

    PathPropertyBridge(IndexFieldReference<String> mainField, IndexFieldReference<GeoPoint> sortField) {
        this.mainField = mainField
        this.sortField = sortField
    }

    @Override
    void write(DocumentElement target, Path bridgedElement, PropertyBridgeWriteContext context) {
        target.addValue(mainField, bridgedElement.toString())
        target.addValue(sortField, bridgedElement.geoPoint)
    }
}
