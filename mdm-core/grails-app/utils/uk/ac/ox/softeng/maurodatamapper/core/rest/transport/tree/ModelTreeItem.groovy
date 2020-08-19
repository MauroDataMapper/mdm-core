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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType

/**
 * @since 07/01/2020
 */
class ModelTreeItem extends TreeItem {

    Boolean finalised
    Boolean deleted
    Boolean superseded
    Version documentationVersion
    String modelType
    UUID containerId
    Version modelVersion
    String branchName

    ModelTreeItem(Model model, String containerPropertyName) {
        this(model, containerPropertyName, model.hasChildren(), model.versionLinks.any {
            it.linkType == VersionLinkType.SUPERSEDED_BY_DOCUMENTATION ||
            it.linkType == VersionLinkType.SUPERSEDED_BY_MODEL
        })
    }

    ModelTreeItem(Model model, String containerPropertyName, Boolean childrenExist, Boolean isSuperseded) {
        this(model, model."$containerPropertyName".id, childrenExist, isSuperseded)
    }

    ModelTreeItem(Model model, UUID containerId, Boolean childrenExist, Boolean isSuperseded) {
        super(model, model.id, model.label, model.domainType, childrenExist)
        this.containerId = containerId
        deleted = model.deleted
        finalised = model.finalised
        superseded = isSuperseded
        documentationVersion = model.documentationVersion
        modelType = model.modelType
        modelVersion = model.modelVersion
        branchName = model.branchName
    }

    @Override
    int compareTo(TreeItem that) {
        def res = super.compareTo(that)
        if (res == 0 && that instanceof ModelTreeItem) res = this.documentationVersion <=> that.documentationVersion
        res
    }
}
