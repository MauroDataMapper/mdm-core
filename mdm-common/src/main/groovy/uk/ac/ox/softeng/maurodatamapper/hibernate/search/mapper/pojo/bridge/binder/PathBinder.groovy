/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.PathBridge
import uk.ac.ox.softeng.maurodatamapper.path.Path

import org.hibernate.search.engine.backend.types.Searchable
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder

/**
 * @since 06/01/2022
 */
class PathBinder implements ValueBinder {

    @Override
    void bind(ValueBindingContext<?> context) {
        context.bridge(Path, PathBridge.instance,
                       context.typeFactory()
                           .asString()
                           .searchable(Searchable.YES)
                           .analyzer('pipe')
        )
    }
}
