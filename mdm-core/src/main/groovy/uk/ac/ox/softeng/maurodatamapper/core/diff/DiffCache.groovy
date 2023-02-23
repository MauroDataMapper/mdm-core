/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.diff

import uk.ac.ox.softeng.maurodatamapper.path.Path

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

import java.util.concurrent.ConcurrentHashMap

/**
 * @since 21/02/2022
 */
@SuppressFBWarnings('LI_LAZY_INIT_STATIC')
class DiffCache {

    private final Map<String, Collection<? extends Diffable>> fieldCollections
    private final Map<String, DiffCache> pathDiffCaches

    DiffCache() {
        fieldCollections = new ConcurrentHashMap<>()
        pathDiffCaches = new ConcurrentHashMap<>()
    }

    def <D extends Diffable> Collection<D> getCollection(String field, Class<D> diffableClass) {
        fieldCollections[field] ?: []
    }

    DiffCache getDiffCache(Path path) {
        pathDiffCaches[path.toString()]
    }

    void addField(String field, Collection<? extends Diffable> collection) {
        // Cannot have a null value in a CHM so add empty set if no collection
        fieldCollections[field] = collection ?: Collections.emptySet()
    }

    void addDiffCache(Path path, DiffCache diffCache) {
        pathDiffCaches[path.toString()] = diffCache
    }
}
