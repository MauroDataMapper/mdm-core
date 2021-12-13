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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge

import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.util.logging.Slf4j
import org.hibernate.search.mapper.pojo.bridge.ValueBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext

/**
 * @since 13/09/2021
 */
@Slf4j
class PathBridge implements ValueBridge<Path, String> {

    @Override
    String toIndexedValue(Path value, ValueBridgeToIndexedValueContext context) {
        value.toString()
    }

    @Override
    Path fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
        Path.from(value)
    }
}
