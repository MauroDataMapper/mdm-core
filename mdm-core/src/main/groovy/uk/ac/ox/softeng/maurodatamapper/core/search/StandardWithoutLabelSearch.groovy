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
package uk.ac.ox.softeng.maurodatamapper.core.search

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.mapper.pojo.binder.MetadataBinder
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.binder.PathBinder

/**
 * @since 27/02/2020
 */
class StandardWithoutLabelSearch {

    static search = {
        description termVector: 'with_positions'
        aliasesString searchable: 'yes', analyzer: 'pipe'
        metadata binder: MetadataBinder, indexingDependency: [reindexOnUpdate: 'shallow'], indexEmbedded: [includePaths: ['key', 'value', 'namespace']]
        classifiers indexingDependency: [reindexOnUpdate: 'shallow'], indexEmbedded: [includePaths: ['label', 'description']]
        lastUpdated searchable: 'yes'
        dateCreated searchable: 'yes'
        path binder: PathBinder
    }
}
