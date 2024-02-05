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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.transform.CompileStatic

/**
 * @since 07/01/2020
 */
@CompileStatic
class ModelTreeItem extends TreeItem {

    Boolean finalised
    Boolean deleted
    Boolean superseded
    Version documentationVersion
    String modelType
    UUID containerId
    Version modelVersion
    String modelVersionTag
    String branchName

    ModelTreeItem(Model model, UUID containerId, Boolean childrenExist, Boolean isSuperseded, List<String> availableTreeActions) {
        super(model, model.label, childrenExist, availableTreeActions)
        this.containerId = containerId
        deleted = model.deleted
        finalised = model.finalised
        superseded = isSuperseded
        documentationVersion = model.documentationVersion
        modelType = model.modelType
        modelVersion = model.modelVersion
        modelVersionTag = model.modelVersionTag
        branchName = model.branchName
    }

    @Override
    boolean equals(Object o) {
        if (!super.equals(o)) return false

        ModelTreeItem treeItem = (ModelTreeItem) o
        if (documentationVersion != treeItem.documentationVersion) return false
        if (modelVersion != treeItem.modelVersion) return false
        if (branchName != treeItem.branchName) return false

        true
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (documentationVersion != null ? documentationVersion.hashCode() : 0)
        result = 31 * result + (modelVersion != null ? modelVersion.hashCode() : 0)
        result = 31 * result + (branchName != null ? branchName.hashCode() : 0)
        result
    }

    @Override
    int compareTo(TreeItem that) {
        def res = super.compareTo(that)
        if (that instanceof ModelTreeItem) {
            if (res == 0) res = this.documentationVersion <=> that.documentationVersion
            if (res == 0) res = this.modelVersion <=> that.modelVersion
            if (res == 0) res = this.branchName <=> that.branchName
        }
        res
    }
}
