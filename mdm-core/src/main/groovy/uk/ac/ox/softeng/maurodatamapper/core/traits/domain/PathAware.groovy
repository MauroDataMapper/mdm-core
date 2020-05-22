/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import org.grails.datastore.gorm.GormEntity

/**
 * Technically this need to be typed, to prevent grails from messing up mapping on getPathParent,
 * however that adds an extra level of possibly unnecessary complexity, when we can just make sure getPathParent is
 * typed to the actual class rather than PathAware
 * @since 18/09/2017
 */
trait PathAware {

    String path
    Integer depth

    private UUID rootId
    private UUID parentId

    abstract UUID getId()

    abstract GormEntity getPathParent()

    String buildPath() {
        GormEntity ge = getPathParent()
        if (ge) {
            if (ge.instanceOf(PathAware)) {
                PathAware parent = ge as PathAware
                depth = parent.depth + 1
                path = "${parent.getPath()}/${parent.getId()}"
            } else {
                depth = 1
                path = "/${ge.ident().toString()}"
            }
        } else {
            depth = 0
            path = ''
        }
        path
    }

    String getPath() {
        if (!path || path.contains('null')) buildPath()
        path
    }

    Integer getDepth() {
        if (!depth) buildPath()
        depth
    }

    UUID getRootId() {
        if (!rootId) {
            def split = path.split('/').findAll()
            rootId = split ? UUID.fromString(split[0]) : null
        }
        rootId
    }

    UUID getParentId() {
        if (!parentId) {
            def split = path.split('/').findAll()
            parentId = split ? UUID.fromString(split[-1]) : null
        }
        parentId
    }

    // This does NOT cascade to associations, so this must be implemented in each class if they contain any pathaware associations
    abstract def beforeValidate()

    abstract def beforeInsert()

    abstract def beforeUpdate()
}