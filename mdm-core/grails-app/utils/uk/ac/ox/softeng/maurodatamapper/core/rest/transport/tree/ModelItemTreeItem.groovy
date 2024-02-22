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

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

import groovy.transform.CompileStatic

/**
 * @since 07/01/2020
 */
@CompileStatic
class ModelItemTreeItem extends TreeItem implements Comparable<TreeItem> {

    int order
    boolean imported

    ModelItemTreeItem(ModelItem modelItem, Boolean childrenExist, List<String> availableTreeActions, boolean imported = false) {
        super(modelItem, modelItem.label, childrenExist, availableTreeActions)
        order = modelItem.getOrder()
        this.imported = imported
    }

    @Override
    int compareTo(TreeItem that) {
        def res = this.domainTypeIndex <=> that.domainTypeIndex
        if (res != 0) return res

        ModelItemTreeItem other = that as ModelItemTreeItem
        res = this.hasChildren() <=> other.hasChildren()
        if (res == 0) res = this.order <=> other.order
        if (res == 0) res = this.label?.toLowerCase() <=> other.label?.toLowerCase()
        if (res == 0) res = this.label <=> other.label
        res
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        ModelItemTreeItem that = (ModelItemTreeItem) o

        if (imported != that.imported) return false
        order == that.order
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + order
        result = 31 * result + (imported ? 1 : 0)
        result
    }
}
